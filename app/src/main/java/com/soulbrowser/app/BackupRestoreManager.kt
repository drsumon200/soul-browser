package com.soulbrowser.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {
    fun exportJson(context: Context): String {
        val containers = SoulStore.loadContainers(context)
        val root = JSONObject().put("version", 2)
        root.put("bookmarks", JSONArray().apply {
            SoulStore.loadBookmarks(context).forEach { put(JSONObject().put("url", it.url).put("title", it.title)) }
        })
        root.put("speedDials", JSONArray().apply {
            SoulStore.loadSpeedDials(context).forEach { put(JSONObject().put("title", it.title).put("url", it.url)) }
        })
        root.put("containers", JSONArray().apply {
            containers.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("private", it.isPrivate)) }
        })
        root.put("workspaces", JSONArray().apply {
            SoulStore.loadWorkspaces(context, containers.first().id).forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("containerId", it.containerId))
            }
        })
        return root.toString(2)
    }

    fun importJson(context: Context, jsonText: String): Boolean = runCatching {
        val root = JSONObject(jsonText)
        root.optJSONArray("bookmarks")?.let { array ->
            val items = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                val url = item.getString("url")
                BrowserBookmark(url, item.optString("title", url))
            }
            SoulStore.saveBookmarks(context, items)
        }
        root.optJSONArray("speedDials")?.let { array ->
            val items = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                SpeedDialItem(item.getString("title"), item.getString("url"))
            }
            SoulStore.saveSpeedDials(context, items)
        }
        root.optJSONArray("containers")?.let { array ->
            val items = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                BrowserContainer(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    color = item.optLong("color", 0xFF9BA8FF),
                    isPrivate = item.optBoolean("private", false)
                )
            }
            if (items.isNotEmpty()) SoulStore.saveContainers(context, items)
        }
        root.optJSONArray("workspaces")?.let { array ->
            val items = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                BrowserWorkspace(item.getString("id"), item.getString("name"), item.getString("containerId"))
            }
            if (items.isNotEmpty()) SoulStore.saveWorkspaces(context, items)
        }
        true
    }.getOrDefault(false)
}
