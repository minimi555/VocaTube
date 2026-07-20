package com.example.frontend.ui.consult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.remote.Network
import com.example.frontend.data.remote.AskRequest
import com.example.frontend.data.remote.SchoolItem
import com.example.frontend.data.remote.SchoolSearchHistoryItem
import com.example.frontend.data.remote.SchoolSearchRequest
import com.example.frontend.data.remote.SourceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** 学习咨询版块的两个子功能 Tab。 */
enum class ConsultTab { TestPrep, SchoolSearch }

/** 学习咨询版块的扁平 UI 状态。 */
data class ConsultUiState(
    val activeTab: ConsultTab = ConsultTab.TestPrep,

    // --- 备考咨询 (RAG) ---
    val prepQuery: String = "",
    val prepLoading: Boolean = false,
    val prepAnswer: String? = null,
    val prepSources: List<SourceItem> = emptyList(),
    val prepError: String? = null,

    // --- 学校查询 ---
    val schoolQuery: String = "",
    val schoolLoading: Boolean = false,
    val schoolAnswer: String? = null,
    val schoolError: String? = null,
    val schools: List<SchoolItem> = emptyList(),
    val history: List<SchoolSearchHistoryItem> = emptyList(),
    val showHistory: Boolean = false,
)

/**
 * 学习咨询 ViewModel：备考咨询走 RAG（POST /ask），学校查询走
 * LangChain Agent（POST /school/search），另加载 QS 学校列表与搜索历史。
 */
class ConsultViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConsultUiState())
    val state: StateFlow<ConsultUiState> = _state.asStateFlow()

    fun switchTab(tab: ConsultTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    // ---- 备考咨询 (RAG) ----

    fun onPrepQueryChange(value: String) {
        _state.value = _state.value.copy(prepQuery = value)
    }

    fun askPrep() {
        val question = _state.value.prepQuery.trim()
        if (question.isEmpty() || _state.value.prepLoading) return
        _state.value = _state.value.copy(prepLoading = true, prepError = null, prepAnswer = null)
        viewModelScope.launch {
            try {
                val resp = Network.api.ask(AskRequest(question = question))
                _state.value = _state.value.copy(
                    prepLoading = false,
                    prepAnswer = resp.answer,
                    prepSources = resp.sources,
                )
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    503 -> "知识库尚未初始化，请联系管理员"
                    else -> "服务器错误（${e.code()}）"
                }
                _state.value = _state.value.copy(prepLoading = false, prepError = msg)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    prepLoading = false,
                    prepError = "网络错误：${e.message ?: "无法连接后端"}"
                )
            }
        }
    }

    // ---- 学校查询 ----

    fun onSchoolQueryChange(value: String) {
        _state.value = _state.value.copy(schoolQuery = value)
    }

    fun searchSchool() {
        val question = _state.value.schoolQuery.trim()
        if (question.isEmpty() || _state.value.schoolLoading) return
        _state.value = _state.value.copy(
            schoolLoading = true,
            schoolError = null,
            schoolAnswer = null,
        )
        viewModelScope.launch {
            try {
                val resp = Network.api.schoolSearch(SchoolSearchRequest(question = question))
                _state.value = _state.value.copy(schoolLoading = false, schoolAnswer = resp.answer)
                refreshHistory()
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    502 -> "学校搜索服务暂时不可用，请稍后再试"
                    else -> "服务器错误（${e.code()}）"
                }
                _state.value = _state.value.copy(schoolLoading = false, schoolError = msg)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    schoolLoading = false,
                    schoolError = "网络错误：${e.message ?: "无法连接后端"}"
                )
            }
        }
    }

    /** 首次进入学校查询 Tab 时加载 QS 学校列表；已有数据则不重复请求。 */
    fun loadSchools() {
        if (_state.value.schools.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val schools = Network.api.getSchools()
                _state.value = _state.value.copy(schools = schools)
            } catch (_: Exception) {
                // 列表仅是辅助展示，加载失败静默忽略，下次进入 Tab 再重试
            }
        }
    }

    fun toggleHistory() {
        val show = !_state.value.showHistory
        _state.value = _state.value.copy(showHistory = show)
        if (show) refreshHistory()
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            try {
                val history = Network.api.schoolHistory()
                _state.value = _state.value.copy(history = history)
            } catch (_: Exception) {
                // 历史加载失败不打断主流程
            }
        }
    }
}
