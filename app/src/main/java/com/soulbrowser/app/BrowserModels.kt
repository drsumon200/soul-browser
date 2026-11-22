package com.soulbrowser.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BrowserContainer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long,
    val isPrivate: Boolean = false
)

data class BrowserWorkspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val containerId: String
)

object SoulStore {
    private const val PREFS = "soul_browser_store"
    private const val CONTAINERS = "containers"
    private const val WORKSPACES = "workspaces"
    private const val TABS = "tabs"
    private const val BOOKMARKS = "bookmarks"

    fun loadContainers(activity: android.content.Context): List<BrowserContainer> {
        val raw = activity.getSharedPreferences(PREFS, 0).getString(CONTAINERS, null)
            ?: return listOf(BrowserContainer(id = "personal", name = "Personal", color = 0xFF9BA8FF))
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BrowserContainer(o.getString("id"), o.getString("name"), o.getLong("color"), o.optBoolean("private", false))
            }
        }.getOrDefault(listOf(BrowserContainer(id = "personal", name = "Personal", color = 0xFF9BA8FF)))
    }

    fun saveContainers(activity: android.content.Context, items: List<BrowserContainer>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("private", it.isPrivate)) }
        activity.getSharedPreferences(PREFS, 0).edit().putString(CONTAINERS, array.toString()).apply()
    }

    fun loadWorkspaces(activity: android.content.Context, defaultContainerId: String): List<BrowserWorkspace> {
        val raw = activity.getSharedPreferences(PREFS, 0).getString(WORKSPACES, null)
            ?: return listOf(BrowserWorkspace(id = "general", name = "General", containerId = defaultContainerId))
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BrowserWorkspace(o.getString("id"), o.getString("name"), o.getString("containerId"))
            }
        }.getOrDefault(listOf(BrowserWorkspace(id = "general", name = "General", containerId = defaultContainerId)))
    }

    fun loadTabs(activity: android.content.Context): List<SavedTab> {
        val raw = activity.getSharedPreferences(PREFS, 0).getString(TABS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SavedTab(o.getString("id"), o.optString("title", "New Tab"), o.optString("url", "about:blank"), o.getString("workspaceId"), o.getString("containerId"))
            }
        }.getOrDefault(emptyList())
    }

    fun loadBookmarks(activity: android.content.Context): List<BrowserBookmark> {
        val raw = activity.getSharedPreferences(PREFS, 0).getString(BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BrowserBookmark(o.getString("url"), o.optString("title", o.getString("url")))
            }
        }.getOrDefault(emptyList())
    }

    fun saveBookmarks(activity: android.content.Context, items: List<BrowserBookmark>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("url", it.url).put("title", it.title)) }
        activity.getSharedPreferences(PREFS, 0).edit().putString(BOOKMARKS, array.toString()).apply()
    }

    fun saveTabs(activity: android.content.Context, items: List<SavedTab>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("workspaceId", it.workspaceId).put("containerId", it.containerId)) }
        activity.getSharedPreferences(PREFS, 0).edit().putString(TABS, array.toString()).apply()
    }

    fun saveWorkspaces(activity: android.content.Context, items: List<BrowserWorkspace>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("containerId", it.containerId)) }
        activity.getSharedPreferences(PREFS, 0).edit().putString(WORKSPACES, array.toString()).apply()
    }
}



data class SoulTab(
    val id: String,
    val title: String,
    val url: String,
    val session: org.mozilla.geckoview.GeckoSession?,
    val workspaceId: String,
    val containerId: String
)

data class SavedTab(val id: String, val title: String, val url: String, val workspaceId: String, val containerId: String)

data class BrowserBookmark(val url: String, val title: String)
