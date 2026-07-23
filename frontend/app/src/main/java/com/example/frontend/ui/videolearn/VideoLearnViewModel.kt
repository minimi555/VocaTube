package com.example.frontend.ui.videolearn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.local.WordbookStore
import com.example.frontend.data.remote.Network
import com.example.frontend.data.remote.QuizGenerateRequest
import com.example.frontend.data.remote.QuizGradeRequest
import com.example.frontend.data.remote.QuizReadingQuestion
import com.example.frontend.data.remote.SubtitleApi
import com.example.frontend.data.remote.VideoTitle
import com.example.frontend.data.subtitle.Cue
import com.example.frontend.data.subtitle.SrtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 视频列表（抽屉）的加载状态。 */
sealed interface VideoListState {
    data object Loading : VideoListState
    data class Success(val videos: List<VideoTitle>) : VideoListState
    data class Error(val message: String) : VideoListState
}

/**
 * 当前选中视频的播放数据：
 * - [videoUrl] 拼好的完整播放地址（已 URL 编码）
 * - [zhCues] / [enCues] 解析后的中/英字幕
 */
data class PlaybackState(
    val id: Int,
    val title: String,
    val videoUrl: String,
    val zhCues: List<Cue> = emptyList(),
    val enCues: List<Cue> = emptyList(),
)

/** 练习区 UI 状态。 */
sealed interface QuizUiState {
    /** 初始态：显示"生成练习"按钮（需先选视频 + 设定单词书）。 */
    data object Idle : QuizUiState
    /** 调用 /quiz/generate 中。 */
    data object Generating : QuizUiState
    /** 题目已生成，用户作答中。 */
    data class Answering(
        val quizId: String,
        val clozePassage: String,
        val clozeCount: Int,
        val readingPassage: String,
        val readingQuestions: List<QuizReadingQuestion>,
    ) : QuizUiState
    /** 调用 /quiz/grade 中。 */
    data object Grading : QuizUiState
    /** 批改结果展示。 */
    data class Result(
        val score: com.example.frontend.data.remote.QuizScore,
        val clozeResults: List<com.example.frontend.data.remote.QuizClozeResult>,
        val readingResults: List<com.example.frontend.data.remote.QuizReadingResult>,
    ) : QuizUiState
    /** 错误态。 */
    data class Error(val message: String) : QuizUiState
}

class VideoLearnViewModel(app: Application) : AndroidViewModel(app) {

    private val wordbookStore = WordbookStore(app.applicationContext)

    private val _listState = MutableStateFlow<VideoListState>(VideoListState.Loading)
    val listState: StateFlow<VideoListState> = _listState.asStateFlow()

    private val _playback = MutableStateFlow<PlaybackState?>(null)
    val playback: StateFlow<PlaybackState?> = _playback.asStateFlow()

    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    /** 当前选中的单词书（CET4 / CET6 等），未选时为 null。 */
    val currentBook: StateFlow<String?> = wordbookStore.currentBook.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    init {
        loadVideos()
    }

    fun loadVideos() {
        _listState.value = VideoListState.Loading
        viewModelScope.launch {
            try {
                val videos = Network.api.getVideos()
                _listState.value = VideoListState.Success(videos)
            } catch (e: Exception) {
                _listState.value = VideoListState.Error("加载视频列表失败：${e.message ?: "无法连接后端"}")
            }
        }
    }

    /** 点击某个 title：拿视频详情 + 下载解析中英字幕。 */
    fun selectVideo(id: Int) {
        // 切换视频时重置练习区。
        _quizState.value = QuizUiState.Idle
        viewModelScope.launch {
            try {
                val detail = Network.api.getVideo(id)
                // 先放出视频地址，字幕随后填充（不阻塞播放）。
                val videoUrl = Network.assetUrl(detail.videoUrl)
                _playback.value = PlaybackState(
                    id = detail.id,
                    title = detail.title,
                    videoUrl = videoUrl,
                )
                val zh = detail.subtitleZhUrl?.let { loadCues(it) } ?: emptyList()
                val en = detail.subtitleEnUrl?.let { loadCues(it) } ?: emptyList()
                _playback.value = _playback.value?.copy(zhCues = zh, enCues = en)
            } catch (e: Exception) {
                // 播放加载失败时保持列表可用；这里简单忽略，UI 侧显示空。
            }
        }
    }

    /** 从已解析的英文字幕拼接纯文本，调用 /quiz/generate。 */
    fun generateQuiz() {
        val pb = _playback.value ?: return
        val categoryCode = currentBook.value ?: return
        val subtitleText = pb.enCues.joinToString(" ") { it.text }
        if (subtitleText.isBlank()) return

        _quizState.value = QuizUiState.Generating
        viewModelScope.launch {
            try {
                val resp = Network.api.quizGenerate(
                    QuizGenerateRequest(subtitleText, categoryCode)
                )
                _quizState.value = QuizUiState.Answering(
                    quizId = resp.quizId,
                    clozePassage = resp.clozePassage,
                    clozeCount = resp.clozeCount,
                    readingPassage = resp.readingPassage,
                    readingQuestions = resp.readingQuestions,
                )
            } catch (e: Exception) {
                _quizState.value = QuizUiState.Error("生成题目失败：${e.message}")
            }
        }
    }

    /** 提交用户答案，调用 /quiz/grade。 */
    fun submitAnswers(clozeAnswers: Map<String, String>, readingAnswers: Map<String, String>) {
        val answering = _quizState.value as? QuizUiState.Answering ?: return
        _quizState.value = QuizUiState.Grading
        viewModelScope.launch {
            try {
                val resp = Network.api.quizGrade(
                    QuizGradeRequest(answering.quizId, clozeAnswers, readingAnswers)
                )
                _quizState.value = QuizUiState.Result(
                    score = resp.score,
                    clozeResults = resp.clozeResults,
                    readingResults = resp.readingResults,
                )
            } catch (e: Exception) {
                _quizState.value = QuizUiState.Error("批改失败：${e.message}")
            }
        }
    }

    /** 重置到空闲，允许重新生成。 */
    fun resetQuiz() {
        _quizState.value = QuizUiState.Idle
    }

    private suspend fun loadCues(relativeUrl: String): List<Cue> = withContext(Dispatchers.IO) {
        runCatching {
            val srt = SubtitleApi.download(Network.assetUrl(relativeUrl))
            SrtParser.parse(srt)
        }.getOrDefault(emptyList())
    }
}
