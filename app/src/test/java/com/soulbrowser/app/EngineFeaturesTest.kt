package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineFeaturesTest {

    @Test
    fun omniboxCalculatorEvaluatesExpressionsCorrectly() {
        assertEquals("4", OmniboxCalculator.evaluate("2 + 2"))
        assertEquals("120", OmniboxCalculator.evaluate("15 * 8"))
        assertEquals("25", OmniboxCalculator.evaluate("100 / 4"))
        assertEquals("35", OmniboxCalculator.evaluate("50 - 15"))
        assertEquals("20", OmniboxCalculator.evaluate("200 % 10"))
        assertNull(OmniboxCalculator.evaluate("10 / 0"))
        assertNull(OmniboxCalculator.evaluate("not a math expression"))
        assertNull(OmniboxCalculator.evaluate(""))
    }

    @Test
    fun bangRegistryResolvesBuiltInEngines() {
        assertEquals(
            "https://www.google.com/search?q=kotlin+compose",
            BangRegistry.resolve("!g kotlin compose")
        )
        assertEquals(
            "https://m.youtube.com/results?search_query=lofi+beats",
            BangRegistry.resolve("!yt lofi beats")
        )
        assertEquals(
            "https://github.com/search?q=soul-browser",
            BangRegistry.resolve("!gh soul-browser")
        )
        assertEquals(
            "https://en.wikipedia.org/w/index.php?search=Quantum+computing",
            BangRegistry.resolve("!w Quantum computing")
        )
        assertNull(BangRegistry.resolve("plain search query"))
        assertNull(BangRegistry.resolve("!g"))
    }

    @Test
    fun phishingGuardDetectsThreats() {
        val credThreat = PhishingUrlGuard.inspect("https://admin:secret@bank.com/login")
        assertTrue(credThreat.isSuspicious)

        val punyThreat = PhishingUrlGuard.inspect("https://xn--pple-43d.com")
        assertTrue(punyThreat.isSuspicious)

        val safeReport = PhishingUrlGuard.inspect("https://github.com/drsumon200/soul-browser")
        assertFalse(safeReport.isSuspicious)
    }

    @Test
    fun bookmarkHtmlPortImportsAndExportsCorrectly() {
        val original = listOf(
            BrowserBookmark(url = "https://github.com", title = "GitHub"),
            BrowserBookmark(url = "https://mozilla.org", title = "Mozilla")
        )
        val exportedHtml = BookmarkHtmlPort.exportNetscapeHtml(original)
        assertTrue(exportedHtml.contains("<!DOCTYPE NETSCAPE-Bookmark-file-1>"))
        assertTrue(exportedHtml.contains("https://github.com"))

        val imported = BookmarkHtmlPort.importNetscapeHtml(exportedHtml)
        assertEquals(2, imported.size)
        assertEquals("https://github.com", imported[0].url)
        assertEquals("GitHub", imported[0].title)
    }

    @Test
    fun adblockEngineBlocksKnownTrackersAndAllowsLegitSites() {
        assertTrue(AdblockFilterEngine.isBlocked("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdblockFilterEngine.isBlocked("https://google-analytics.com/analytics.js"))
        assertFalse(AdblockFilterEngine.isBlocked("https://wikipedia.org/wiki/Kotlin"))
    }
}
