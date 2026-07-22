package com.example.frontend.ui.wordbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.local.WordbookStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 生词本 ViewModel：纯本地，不连后端。
 * 用 [WordbookStore]（DataStore）读写手动加入的生词。
 */
class WordbookViewModel(app: Application) : AndroidViewModel(app) {

    private val store = WordbookStore(app.applicationContext)

    val words: StateFlow<List<String>> = store.words.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val currentBook: StateFlow<String?> = store.currentBook.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun add(word: String) {
        viewModelScope.launch { store.add(word) }
    }

    fun remove(word: String) {
        viewModelScope.launch { store.remove(word) }
    }

    fun selectBook(book: String) {
        viewModelScope.launch { store.selectBook(book) }
    }
}
