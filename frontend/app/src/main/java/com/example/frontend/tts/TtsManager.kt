package com.example.frontend.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

/**
 * TTS 管理器,用 Android 的 TextToSpeech 实现英/美音朗读。
 * 实例化后异步初始化,用 awaitReady() 确认就绪。
 *
 * 优先请求 Google TTS 引擎:荣耀/华为等厂商内置引擎通常只提供一套通用英语语音数据,
 * setLanguage(Locale.UK) 和 setLanguage(Locale.US) 会被映射到同一个音色,导致英美发音听起来一样。
 * Google 引擎为 en-GB/en-US 提供了不同的语音,能真正区分英式/美式发音。
 * 若设备未安装 Google TTS,则自动回退到系统默认引擎。
 */
class TtsManager(private val context: Context) {

    private val readyDeferred = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null

    init {
        initEngine(GOOGLE_TTS_ENGINE_PACKAGE)
    }

    private fun initEngine(enginePackage: String?) {
        val appContext = context.applicationContext
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                readyDeferred.complete(true)
            } else if (enginePackage != null) {
                // 指定引擎不可用(未安装 Google TTS 等),回退到系统默认引擎重试一次。
                tts?.shutdown()
                initEngine(null)
            } else {
                readyDeferred.complete(false)
            }
        }
        tts = if (enginePackage != null) {
            TextToSpeech(appContext, listener, enginePackage)
        } else {
            TextToSpeech(appContext, listener)
        }
    }

    /**
     * 等待 TTS 引擎初始化完成。返回 true = 成功,false = 失败。
     * 在调 speak 之前先等这个。
     */
    suspend fun awaitReady(): Boolean = readyDeferred.await()

    /**
     * 朗读 [word]。
     * @param locale [Locale.UK] = 英音,[Locale.US] = 美音。
     */
    fun speak(word: String, locale: Locale = Locale.US) {
        tts?.apply {
            // 优先按 locale 精确匹配一个具体的 Voice;不同引擎/厂商对 setLanguage(Locale) 的
            // 处理并不可靠(有的会忽略国家码,只按语言选一个通用音色),setVoice 更精确。
            val matchedVoice = voices?.firstOrNull {
                it.locale.language == locale.language &&
                    it.locale.country == locale.country &&
                    !it.isNetworkConnectionRequired
            } ?: voices?.firstOrNull {
                it.locale.language == locale.language && it.locale.country == locale.country
            }
            if (matchedVoice != null) {
                voice = matchedVoice
            } else {
                // 找不到对应 country 的 Voice,退回旧接口按 locale 设置语言。
                setLanguage(locale)
            }
            speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val GOOGLE_TTS_ENGINE_PACKAGE = "com.google.android.tts"
    }
}
