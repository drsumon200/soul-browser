package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferrerSanitizerTest {
    @Test
    fun stripsCrossOriginPathAndDowngrades() {
        assertEquals("https://a.com/", ReferrerSanitizer.computeSafeReferrer("https://a.com/secret/page?x=1", "https://b.com/"))
        assertEquals("https://a.com/p1", ReferrerSanitizer.computeSafeReferrer("https://a.com/p1", "https://a.com/p2"))
        assertNull(ReferrerSanitizer.computeSafeReferrer("https://a.com/secure", "http://b.com/plain"))
    }
}
