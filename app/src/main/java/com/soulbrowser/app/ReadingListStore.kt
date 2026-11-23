package com.soulbrowser.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
data class ReadingListItem(val url: String, val title: String, val isRead: Boolean = false, val addedAt: Long = System.currentTimeMillis())
object ReadingListStore {
    private const val PREFS = "soul_reading_list"
    private const val KEY = "items"
    fun load(context: Context): List<ReadingListItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ReadingListItem(o.getString("url"), o.getString("title"), o.optBoolean("read", false), o.optLong("at", 0L))
            }
        }.getOrDefault(emptyList())
    }
    fun add(context: Context, url: String, title: String): List<ReadingListItem> {
        val updated = listOf(ReadingListItem(url, title.ifBlank { url })) + load(context).filterNot { it.url == url }
        save(context, updated); return updated
    }
    fun markRead(context: Context, url: String, read: Boolean): List<ReadingListItem> {
        val updated = load(context).map { if (it.url == url) it.copy(isRead = read) else it }
        save(context, updated); return updated
    }
    fun remove(context: Context, url: String): List<ReadingListItem> {
        val updated = load(context).filterNot { it.url == url }
        save(context, updated); return updated
    }
    private fun save(context: Context, items: List<ReadingListItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("url", it.url).put("title", it.title).put("read", it.isRead).put("at", it.addedAt)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
