package com.soulbrowser.app
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
object WebArchiveSaver {
    suspend fun saveOfflineSnapshot(context: Context, url: String, title: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = URI(url)
            require((parsed.scheme.equals("http", true) || parsed.scheme.equals("https", true)) && !parsed.host.isNullOrBlank())
            val dir = File(context.filesDir, "offline_pages").apply { mkdirs() }
            val safeName = title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "page" }
            val outFile = File(dir, "${safeName}_${System.currentTimeMillis()}.html")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000; readTimeout = 6000; instanceFollowRedirects = false
            }
            val html = try {
                require(conn.responseCode in 200..299) { "HTTP response rejected" }
                conn.inputStream.bufferedReader().use { it.readText().take(5_000_000) }
            } finally { conn.disconnect() }
            outFile.writeText(html, Charsets.UTF_8)
            outFile
        }.getOrNull()
    }
    fun listSavedSnapshots(context: Context): List<File> =
        File(context.filesDir, "offline_pages").listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}
