package com.soulbrowser.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object FaviconCacheLoader {
    private val cache = LruCache<String, Bitmap>(64)

    suspend fun loadForUrl(pageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        val host = runCatching { URL(pageUrl).host }.getOrNull().orEmpty()
        if (host.isBlank()) return@withContext null
        cache.get(host)?.let { return@withContext it }
        runCatching {
            val connection = (URL("https://$host/favicon.ico").openConnection() as HttpURLConnection).apply {
                connectTimeout = 2_000
                readTimeout = 2_000
                instanceFollowRedirects = true
            }
            try {
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    ?.also { cache.put(host, it) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
