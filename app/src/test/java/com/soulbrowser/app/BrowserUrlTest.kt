package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUrlTest {
    @Test fun blankInputStaysBlank() = assertEquals("", normalizeAddress("   "))

    @Test fun plainWordsUseSelectedSearchEngine() {
        assertEquals("https://duckduckgo.com/?q=sylhet+weather", normalizeAddress("sylhet weather"))
        assertEquals(
            "https://www.google.com/search?q=sylhet+weather",
            normalizeAddress("sylhet weather", SearchEngine.GOOGLE)
        )
    }

    @Test fun bareDomainsAndPathsGetHttps() {
        assertEquals("https://example.com", normalizeAddress("example.com"))
        assertEquals("https://example.com/path", normalizeAddress("example.com/path"))
        assertEquals("https://example.com?x=1", normalizeAddress("example.com?x=1"))
    }

    @Test fun explicitSchemesArePreserved() {
        assertEquals("http://example.com", normalizeAddress("http://example.com"))
        assertEquals("https://example.com", normalizeAddress("https://example.com"))
        assertEquals("about:blank", normalizeAddress("about:blank"))
    }

    @Test fun localhostAndIpv4AreHandled() {
        assertEquals("https://duckduckgo.com/?q=localhost", normalizeAddress("localhost"))
        assertEquals("http://localhost:3000", normalizeAddress("localhost:3000"))
        assertEquals("http://192.168.1.1", normalizeAddress("192.168.1.1"))
        assertEquals("http://192.168.1.1:8080/admin", normalizeAddress("192.168.1.1:8080/admin"))
        assertEquals("https://duckduckgo.com/?q=999.1.1.1", normalizeAddress("999.1.1.1"))
    }

    @Test fun displayHostIsCleaned() {
        assertEquals("github.com", extractDisplayHost("https://www.github.com/drsumon200/soul-browser"))
        assertEquals("", extractDisplayHost("about:blank"))
    }

    @Test fun httpSchemesAreMatchedExactly() {
        assertTrue(isHttpUrl("http://example.com"))
        assertTrue(isHttpUrl("https://example.com/path"))
        assertFalse(isHttpUrl("httpx://example.com"))
        assertFalse(isHttpUrl("mailto:test@example.com"))
        assertFalse(isHttpUrl("http:example.com"))
        assertTrue(isHttpsUrl("https://example.com"))
        assertFalse(isHttpsUrl("http://example.com"))
    }

    @Test fun externalSchemesAreDetected() {
        assertTrue(isExternalAppUri("mailto:test@example.com"))
        assertTrue(isExternalAppUri("tel:+8801700000000"))
        assertTrue(isExternalAppUri("intent://open#Intent;end"))
        assertFalse(isExternalAppUri("https://example.com"))
    }
}
