package com.soulbrowser.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {
    fun exportJson(context: Context): String {
        val containers = SoulStore.loadContainers(context).filterNot { it.isPrivate }
        val safeContainers = containers.ifEmpty {
            listOf(BrowserContainer(id = "personal", name = "Personal", color = 0xFF9BA8FF))
        }
        val containerIds = safeContainers.mapTo(mutableSetOf()) { it.id }
        val workspaces = SoulStore.loadWorkspaces(context, safeContainers.first().id)
            .filter { it.containerId in containerIds }
        val root = JSONObject().put("version", 2)
        root.put("bookmarks", JSONArray().apply {
            SoulStore.loadBookmarks(context).filter { isHttpUrl(it.url) }.forEach {
                put(JSONObject().put("url", it.url).put("title", it.title))
            }
        })
        root.put("speedDials", JSONArray().apply {
            SoulStore.loadSpeedDials(context).filter { isHttpUrl(it.url) }.forEach {
                put(JSONObject().put("title", it.title).put("url", it.url))
            }
        })
        root.put("containers", JSONArray().apply {
            safeContainers.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("private", false))
            }
        })
        root.put("workspaces", JSONArray().apply {
            workspaces.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("containerId", it.containerId))
            }
        })
        return root.toString(2)
    }

    fun importJson(context: Context, jsonText: String): Boolean = runCatching {
        val root = JSONObject(jsonText)
        require(root.optInt("version", 1) in 1..2) { "Unsupported backup version" }

        val bookmarks = root.optJSONArray("bookmarks")?.let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val url = item.getString("url")
                require(isHttpUrl(url))
                BrowserBookmark(url, item.optString("title", url))
            }
        }
        val speedDials = root.optJSONArray("speedDials")?.let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val url = item.getString("url")
                require(isHttpUrl(url))
                SpeedDialItem(item.optString("title", url), url)
            }
        }
        val containers = root.optJSONArray("containers")?.let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                BrowserContainer(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    color = item.optLong("color", 0xFF9BA8FF),
                    isPrivate = false
                )
            }.also { list ->
                require(list.isNotEmpty())
                require(list.map { it.id }.distinct().size == list.size)
            }
        }
        val workspaces = root.optJSONArray("workspaces")?.let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                BrowserWorkspace(item.getString("id"), item.getString("name"), item.getString("containerId"))
            }.also { list ->
                require(list.map { it.id }.distinct().size == list.size)
                val validContainerIds = (containers ?: SoulStore.loadContainers(context).filterNot { it.isPrivate })
                    .mapTo(mutableSetOf()) { it.id }
                require(list.all { it.containerId in validContainerIds })
            }
        }

        // Validate the complete document before changing persisted browser data.
        bookmarks?.let { SoulStore.saveBookmarks(context, it) }
        speedDials?.let { SoulStore.saveSpeedDials(context, it) }
        containers?.let { SoulStore.saveContainers(context, it) }
        workspaces?.let { SoulStore.saveWorkspaces(context, it) }
        true
    }.getOrDefault(false)
}
