package com.example.frontend.ui.videolearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.remote.Network
import com.example.frontend.data.remote.SubtitleApi
import com.example.frontend.data.remote.VideoTitle
import com.example.frontend.data.subtitle.Cue
import com.example.frontend.data.subtitle.SrtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class VideoLearnViewModel : ViewModel() {

    private val _listState = MutableStateFlow<VideoListState>(VideoListState.Loading)
    val listState: StateFlow<VideoListState> = _listState.asStateFlow()

    private val _playback = MutableStateFlow<PlaybackState?>(null)
    val playback: StateFlow<PlaybackState?> = _playback.asStateFlow()

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

    private suspend fun loadCues(relativeUrl: String): List<Cue> = withContext(Dispatchers.IO) {
        runCatching {
            val srt = SubtitleApi.download(Network.assetUrl(relativeUrl))
            SrtParser.parse(srt)
        }.getOrDefault(emptyList())
    }
}
