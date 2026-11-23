package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkHtmlPortTest {
    @Test
    fun roundTripsBookmarksViaNetscapeHtml() {
        val original = listOf(BrowserBookmark("https://github.com", "GitHub"), BrowserBookmark("https://mozilla.org", "Mozilla"))
        val parsed = BookmarkHtmlPort.importNetscapeHtml(BookmarkHtmlPort.exportNetscapeHtml(original))
        assertEquals(2, parsed.size)
        assertEquals("https://github.com", parsed[0].url)
        assertEquals("Mozilla", parsed[1].title)
    }
}
