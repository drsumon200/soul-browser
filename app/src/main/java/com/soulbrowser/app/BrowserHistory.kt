package com.soulbrowser.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)

object HistoryStore {
    private const val PREFS = "soul_browser_history"
    private const val KEY_ITEMS = "entries"
    private const val MAX_ENTRIES = 500

    fun load(context: Context): List<HistoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val url = item.getString("url")
                HistoryEntry(url, item.optString("title", url), item.optLong("visitedAt", 0L))
            }
        }.getOrDefault(emptyList())
    }

    fun record(context: Context, url: String, title: String?, isPrivate: Boolean): List<HistoryEntry> {
        if (isPrivate || !url.startsWith("http", ignoreCase = true)) return load(context)
        val entry = HistoryEntry(
            url = url,
            title = title?.takeIf { it.isNotBlank() } ?: extractDisplayHost(url).ifBlank { url }
        )
        val updated = (listOf(entry) + load(context).filterNot { it.url == url }).take(MAX_ENTRIES)
        save(context, updated)
        return updated
    }

    fun remove(context: Context, url: String): List<HistoryEntry> {
        val updated = load(context).filterNot { it.url == url }
        save(context, updated)
        return updated
    }

    fun clear(context: Context): List<HistoryEntry> {
        save(context, emptyList())
        return emptyList()
    }

    private fun save(context: Context, entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("url", entry.url)
                    .put("title", entry.title)
                    .put("visitedAt", entry.visitedAt)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
