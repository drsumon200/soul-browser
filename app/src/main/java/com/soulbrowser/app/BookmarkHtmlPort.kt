package com.soulbrowser.app
object BookmarkHtmlPort {
    fun exportNetscapeHtml(bookmarks: List<BrowserBookmark>): String = buildString {
        appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        appendLine("<TITLE>Soul Browser Bookmarks</TITLE>")
        appendLine("<H1>Soul Browser Bookmarks</H1>")
        appendLine("<DL><p>")
        bookmarks.forEach { bm ->
            val safeTitle = bm.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val safeUrl = bm.url.replace("&", "&amp;").replace("\"", "%22")
            appendLine("    <DT><A HREF=\"$safeUrl\">$safeTitle</A>")
        }
        appendLine("</DL><p>")
    }
    fun importNetscapeHtml(html: String): List<BrowserBookmark> {
        val linkRegex = Regex("""<A\s+[^>]*HREF="([^"]+)"[^>]*>(.*?)</A>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return linkRegex.findAll(html).mapNotNull { match ->
            val url = match.groupValues[1].trim()
            if (!(url.startsWith("https://", true) || url.startsWith("http://", true))) return@mapNotNull null
            val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").trim().ifBlank { url }
            BrowserBookmark(url = url, title = title)
        }.distinctBy { it.url }.toList()
    }
}
