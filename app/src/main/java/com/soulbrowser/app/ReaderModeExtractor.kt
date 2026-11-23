package com.soulbrowser.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
data class ReaderArticle(val title: String, val host: String, val paragraphs: List<String>)
object ReaderModeExtractor {
    suspend fun fetchAndExtract(url: String): ReaderArticle? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = URI(url)
            require((parsed.scheme.equals("http", true) || parsed.scheme.equals("https", true)) && !parsed.host.isNullOrBlank())
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000; readTimeout = 5000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android 15; Mobile; rv:150.0) Gecko/150.0 Firefox/150.0")
            }
            val html = try {
                require(conn.responseCode in 200..299) { "HTTP response rejected" }
                conn.inputStream.bufferedReader().use { it.readText().take(2_000_000) }
            } finally { conn.disconnect() }
            val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim() ?: extractDisplayHost(url)
            val cleaned = html.replace(Regex("<(script|style|nav|footer|header|aside)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            val paragraphs = Regex("<p[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .findAll(cleaned).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim() }
                .filter { it.length >= 40 }.toList()
            if (paragraphs.isEmpty()) null else ReaderArticle(title, extractDisplayHost(url), paragraphs)
        }.getOrNull()
    }
}
