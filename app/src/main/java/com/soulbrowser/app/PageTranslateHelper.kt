package com.soulbrowser.app
import java.net.URLEncoder
import java.util.Locale
object PageTranslateHelper {
    fun buildTranslatedUrl(sourceUrl: String, targetLang: String = Locale.getDefault().language): String {
        val encoded = URLEncoder.encode(sourceUrl, "UTF-8")
        val lang = targetLang.ifBlank { "en" }.take(12)
        return "https://translate.google.com/translate?sl=auto&tl=$lang&u=$encoded"
    }
}
