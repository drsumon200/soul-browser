package com.soulbrowser.app
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
class PageTextToSpeech(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) tts?.language = Locale.getDefault()
        }
    }
    fun speak(text: String, rate: Float = 1.0f) {
        if (!isReady || text.isBlank()) return
        tts?.setSpeechRate(rate.coerceIn(0.6f, 2.0f))
        tts?.speak(text.take(3800), TextToSpeech.QUEUE_FLUSH, null, "soul_tts")
    }
    fun stop() { tts?.stop() }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
