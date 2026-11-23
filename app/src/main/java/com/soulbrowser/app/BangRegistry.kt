package com.soulbrowser.app

import java.net.URLEncoder

object BangRegistry {
    private val builtIn = linkedMapOf(
        "!g" to "https://www.google.com/search?q=%s",
        "!ddg" to "https://duckduckgo.com/?q=%s",
        "!yt" to "https://m.youtube.com/results?search_query=%s",
        "!w" to "https://en.wikipedia.org/w/index.php?search=%s",
        "!gh" to "https://github.com/search?q=%s",
        "!r" to "https://www.reddit.com/search/?q=%s",
        "!so" to "https://stackoverflow.com/search?q=%s",
        "!maps" to "https://maps.google.com/?q=%s",
        "!tr" to "https://translate.google.com/?sl=auto&tl=en&text=%s",
        "!npm" to "https://www.npmjs.com/search?q=%s",
        "!pypi" to "https://pypi.org/search/?q=%s",
        "!arch" to "https://wiki.archlinux.org/index.php?search=%s"
    )

    fun resolve(rawInput: String, customEngines: List<CustomSearchEngine> = emptyList()): String? {
        val trimmed = rawInput.trim()
        if (!trimmed.startsWith("!") || !trimmed.contains(' ')) return null
        val prefix = trimmed.substringBefore(' ').lowercase()
        val query = trimmed.substringAfter(' ').trim()
        if (query.isEmpty()) return null
        val custom = customEngines.firstOrNull {
            "!" + it.keyword.trim().lowercase() == prefix
        }
        if (custom != null) return custom.buildUrl(query)
        val template = builtIn[prefix] ?: return null
        return template.replace("%s", URLEncoder.encode(query, "UTF-8"))
    }
}
