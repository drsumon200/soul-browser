package com.soulbrowser.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class CustomSearchEngine(val name: String, val keyword: String, val urlTemplate: String) {
    fun buildUrl(query: String): String =
        urlTemplate.replace("%s", URLEncoder.encode(query, "UTF-8"))
}

object CustomSearchEngineStore {
    private const val PREFS = "soul_custom_engines"
    private const val KEY = "items"

    fun load(context: Context): List<CustomSearchEngine> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name").trim()
                val keyword = item.optString("kw").trim()
                val template = item.optString("tpl").trim()
                if (name.isBlank() || keyword.isBlank() || "%s" !in template) null
                else CustomSearchEngine(name, keyword, template)
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, items: List<CustomSearchEngine>) {
        val array = JSONArray()
        items.forEach { engine ->
            array.put(JSONObject().put("name", engine.name)
                .put("kw", engine.keyword).put("tpl", engine.urlTemplate))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
}
