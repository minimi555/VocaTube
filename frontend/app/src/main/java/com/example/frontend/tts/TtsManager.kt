package com.example.frontend.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

/**
 * TTS 管理器，用 Android 的 TextToSpeech 实现英/美音朗读。
 * 实例化后异步初始化，用 awaitReady() 确认就绪。
 */
class TtsManager(context: Context) {

    private val readyDeferred = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            readyDeferred.complete(status == TextToSpeech.SUCCESS)
        }
    }

    /**
     * 等待 TTS 引擎初始化完成。返回 true = 成功，false = 失败。
     * 在调 speak 之前先等这个。
     */
    suspend fun awaitReady(): Boolean = readyDeferred.await()

    /**
     * 朗读 [word]。
     * @param locale [Locale.UK] = 英音，[Locale.US] = 美音。
     */
    fun speak(word: String, locale: Locale = Locale.US) {
        tts?.apply {
            // 尝试设置该 locale；若设备不支持，回退到默认。
            val result = setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Locale 不支持，用默认语言
            }
            speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
