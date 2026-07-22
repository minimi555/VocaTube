package com.example.frontend.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

// 顶层单例 DataStore（生词本本地存储，不连后端）。
private val Context.wordbookDataStore: DataStore<Preferences> by preferencesDataStore(name = "wordbook")

/**
 * 生词本本地存储：以 JSON 编码的字符串列表保存在 DataStore-Preferences 里。
 * 只需要手动加词、看列表、删词。
 */
class WordbookStore(private val context: Context) {

    private val key = stringPreferencesKey("words_json")
    private val currentBookKey = stringPreferencesKey("current_wordbook")

    /** 当前选中的单词书（CET4 / CET6 / TOFEL / IELTS / SAT / kaoyan），未选时为 null。 */
    val currentBook: Flow<String?> = context.wordbookDataStore.data.map { prefs ->
        prefs[currentBookKey]
    }

    /** 生词列表流，最新加入的排在最前。 */
    val words: Flow<List<String>> = context.wordbookDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    /** 加词：去重（忽略大小写差异按原样保留首个），加到列表最前。 */
    suspend fun add(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        context.wordbookDataStore.edit { prefs ->
            val current = decode(prefs[key])
            if (current.any { it.equals(w, ignoreCase = true) }) return@edit
            prefs[key] = encode(listOf(w) + current)
        }
    }

    /** 判断某词是否已在生词本（忽略大小写）。 */
    suspend fun contains(word: String): Boolean {
        val w = word.trim()
        if (w.isEmpty()) return false
        val prefs = context.wordbookDataStore.data.first()
        return decode(prefs[key]).any { it.equals(w, ignoreCase = true) }
    }

    /** 删词。 */
    suspend fun remove(word: String) {
        context.wordbookDataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = encode(current.filterNot { it == word })
        }
    }

    /** 设置当前学习的单词书。 */
    suspend fun selectBook(book: String) {
        context.wordbookDataStore.edit { prefs ->
            prefs[currentBookKey] = book
        }
    }

    private fun encode(list: List<String>): String = Json.encodeToString(list)

    private fun decode(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
