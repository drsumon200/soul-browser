package com.soulbrowser.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
data class DownloadRecord(
    val id: String, val fileName: String, val contentUri: String, val mimeType: String,
    val sizeBytes: Long, val completedAt: Long = System.currentTimeMillis()
)
object DownloadHistoryStore {
    private const val PREFS = "soul_downloads_history"
    private const val KEY = "records"
    fun load(context: Context): List<DownloadRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DownloadRecord(o.getString("id"), o.getString("name"), o.getString("uri"),
                    o.optString("mime", "*/*"), o.optLong("size", 0L), o.optLong("at", 0L))
            }
        }.getOrDefault(emptyList())
    }
    fun add(context: Context, record: DownloadRecord): List<DownloadRecord> {
        val updated = (listOf(record) + load(context)).take(200)
        save(context, updated); return updated
    }
    fun remove(context: Context, id: String): List<DownloadRecord> {
        val updated = load(context).filterNot { it.id == id }
        save(context, updated); return updated
    }
    private fun save(context: Context, items: List<DownloadRecord>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.fileName).put("uri", it.contentUri)
            .put("mime", it.mimeType).put("size", it.sizeBytes).put("at", it.completedAt)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
