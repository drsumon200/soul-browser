package com.soulbrowser.app
import java.util.UUID
data class BookmarkFolder(val id: String = UUID.randomUUID().toString(), val name: String, val parentId: String? = null)
data class StructuredBookmark(
    val id: String = UUID.randomUUID().toString(), val url: String, val title: String,
    val folderId: String = "root", val tags: List<String> = emptyList()
)
