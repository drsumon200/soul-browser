package com.soulbrowser.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession
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

data class SoulTab(
    val id: String,
    val title: String,
    val url: String,
    val session: GeckoSession?,
    val workspaceId: String,
    val containerId: String,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 100,
    val isLoading: Boolean = false,
    val isPinned: Boolean = false,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isHibernated: Boolean = false
)

data class SavedTab(
    val id: String,
    val title: String,
    val url: String,
    val workspaceId: String,
    val containerId: String
)

data class BrowserBookmark(
    val url: String,
    val title: String
)

object SoulStore {
    private const val PREFS = "soul_browser_store"
    private const val CONTAINERS = "containers"
    private const val WORKSPACES = "workspaces"
    private const val TABS = "tabs"
    private const val BOOKMARKS = "bookmarks"

    private fun defaultContainers(): List<BrowserContainer> =
        listOf(BrowserContainer(id = "personal", name = "Personal", color = 0xFF9BA8FF))

    private fun defaultWorkspaces(defaultContainerId: String): List<BrowserWorkspace> =
        listOf(BrowserWorkspace(id = "general", name = "General", containerId = defaultContainerId))

    fun loadContainers(context: Context): List<BrowserContainer> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(CONTAINERS, null)
            ?: return defaultContainers()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BrowserContainer(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    color = o.getLong("color"),
                    isPrivate = o.optBoolean("private", false)
                )
            }.ifEmpty { defaultContainers() }
        }.getOrDefault(defaultContainers())
    }

    fun saveContainers(context: Context, items: List<BrowserContainer>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("color", it.color)
                    .put("private", it.isPrivate)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(CONTAINERS, array.toString()).apply()
    }

    fun loadWorkspaces(context: Context, defaultContainerId: String): List<BrowserWorkspace> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(WORKSPACES, null)
            ?: return defaultWorkspaces(defaultContainerId)
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BrowserWorkspace(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    containerId = o.getString("containerId")
                )
            }.ifEmpty { defaultWorkspaces(defaultContainerId) }
        }.getOrDefault(defaultWorkspaces(defaultContainerId))
    }

    fun saveWorkspaces(context: Context, items: List<BrowserWorkspace>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("containerId", it.containerId)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(WORKSPACES, array.toString()).apply()
    }

    fun loadTabs(context: Context): List<SavedTab> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(TABS, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SavedTab(
                    id = o.getString("id"),
                    title = o.optString("title", "New Tab"),
                    url = o.optString("url", "about:blank"),
                    workspaceId = o.getString("workspaceId"),
                    containerId = o.getString("containerId")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveTabs(context: Context, items: List<SavedTab>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("url", it.url)
                    .put("workspaceId", it.workspaceId)
                    .put("containerId", it.containerId)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(TABS, array.toString()).apply()
    }

    fun loadBookmarks(context: Context): List<BrowserBookmark> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(BOOKMARKS, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val url = o.getString("url")
                BrowserBookmark(url = url, title = o.optString("title", url))
            }
        }.getOrDefault(emptyList())
    }

    fun saveBookmarks(context: Context, items: List<BrowserBookmark>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("url", it.url)
                    .put("title", it.title)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(BOOKMARKS, array.toString()).apply()
    }
}