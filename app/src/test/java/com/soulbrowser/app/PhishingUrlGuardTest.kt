package com.soulbrowser.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhishingUrlGuardTest {
    @Test
    fun detectsSpoofedAndPunycodeUrls() {
        assertTrue(PhishingUrlGuard.inspect("https://google.com@evil.example/login").isSuspicious)
        assertTrue(PhishingUrlGuard.inspect("https://xn--pple-43d.com/").isSuspicious)
        assertFalse(PhishingUrlGuard.inspect("https://github.com/drsumon200/soul-browser").isSuspicious)
    }
}
