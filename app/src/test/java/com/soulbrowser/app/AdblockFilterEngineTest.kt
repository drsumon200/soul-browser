package com.soulbrowser.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdblockFilterEngineTest {
    @Test
    fun blocksTrackersAndRespectsAllowlist() {
        assertTrue(AdblockFilterEngine.isBlocked("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "news.com"))
        AdblockFilterEngine.setSiteAllowlisted("news.com", true)
        assertFalse(AdblockFilterEngine.isBlocked("https://pagead2.googlesyndication.com/ad.js", "news.com"))
        AdblockFilterEngine.setSiteAllowlisted("news.com", false)
        assertFalse(AdblockFilterEngine.isBlocked("https://github.com/drsumon200/soul-browser", "github.com"))
    }
}
