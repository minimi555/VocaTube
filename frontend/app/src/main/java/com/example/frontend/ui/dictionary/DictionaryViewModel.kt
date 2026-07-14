package com.example.frontend.ui.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.local.WordbookStore
import com.example.frontend.data.remote.Network
import com.example.frontend.data.remote.WordDetail
import com.example.frontend.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale

/** 查词版块的扁平 UI 状态。 */
data class DictionaryUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val detail: WordDetail? = null,
    val inWordbook: Boolean = false,
)

/**
 * 查词 ViewModel（功能1）：查询后端单词详情 + 英/美音朗读（Android TTS）+ 加入生词本。
 * 用 AndroidViewModel 是因为 TTS 与本地生词本都需要 Context。
 */
class DictionaryViewModel(app: Application) : AndroidViewModel(app) {

    private val store = WordbookStore(app.applicationContext)
    private val tts = TtsManager(app.applicationContext)

    private val _state = MutableStateFlow(DictionaryUiState())
    val state: StateFlow<DictionaryUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun search() {
        val word = _state.value.query.trim()
        if (word.isEmpty()) return
        _state.value = _state.value.copy(loading = true, error = null, detail = null)
        viewModelScope.launch {
            try {
                val detail = Network.api.getWord(word)
                val inWb = store.contains(detail.word)
                _state.value = _state.value.copy(loading = false, detail = detail, inWordbook = inWb)
            } catch (e: HttpException) {
                val msg = if (e.code() == 404) "未找到单词「$word」" else "服务器错误（${e.code()}）"
                _state.value = _state.value.copy(loading = false, error = msg)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "网络错误：${e.message ?: "无法连接后端"}"
                )
            }
        }
    }

    /** 英音朗读（Locale.UK）。 */
    fun speakUk() = speak(Locale.UK)

    /** 美音朗读（Locale.US）。 */
    fun speakUs() = speak(Locale.US)

    private fun speak(locale: Locale) {
        val word = _state.value.detail?.word ?: return
        viewModelScope.launch {
            if (tts.awaitReady()) tts.speak(word, locale)
        }
    }

    fun addToWordbook() {
        val word = _state.value.detail?.word ?: return
        viewModelScope.launch {
            store.add(word)
            _state.value = _state.value.copy(inWordbook = true)
        }
    }

    override fun onCleared() {
        tts.shutdown()
    }
}
