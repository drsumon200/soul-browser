package com.soulbrowser.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SearchSuggestionClient {
    suspend fun fetchSuggestions(query: String, limit: Int = 5): List<String> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            val safeLimit = limit.coerceIn(1, 10)
            if (clean.length < 2 || isLikelyUrl(clean)) return@withContext emptyList()
            runCatching {
                val encoded = URLEncoder.encode(clean, "UTF-8")
                val connection = (URL("https://duckduckgo.com/ac/?q=$encoded&type=list")
                    .openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    useCaches = false
                }
                val body = try {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                val root = JSONArray(body)
                (0 until minOf(root.length(), safeLimit)).mapNotNull { index ->
                    root.optJSONObject(index)?.optString("phrase")?.takeIf(String::isNotBlank)
                }
            }.getOrDefault(emptyList())
        }
}
