package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserUrlTest {
    @Test
    fun blankInputStaysBlank() {
        assertEquals("", normalizeAddress("   "))
    }

    @Test
    fun plainWordsBecomeSearch() {
        assertEquals(
            "https://duckduckgo.com/?q=sylhet+weather",
            normalizeAddress("sylhet weather")
        )
    }

    @Test
    fun bareDomainGetsHttps() {
        assertEquals("https://example.com", normalizeAddress("example.com"))
    }

    @Test
    fun hostWithPathGetsHttps() {
        assertEquals("https://example.com/path", normalizeAddress("example.com/path"))
    }

    @Test
    fun hostWithQueryGetsHttps() {
        assertEquals("https://example.com?x=1", normalizeAddress("example.com?x=1"))
    }

    @Test
    fun explicitSchemeIsPreserved() {
        assertEquals("http://example.com", normalizeAddress("http://example.com"))
        assertEquals("https://example.com", normalizeAddress("https://example.com"))
    }

    @Test
    fun aboutUriIsPreserved() {
        assertEquals("about:blank", normalizeAddress("about:blank"))
    }

    @Test
    fun localhostWithoutDotIsSearched() {
        assertEquals("https://duckduckgo.com/?q=localhost", normalizeAddress("localhost"))
    }

    @Test
    fun localhostWithPortGetsHttp() {
        assertEquals("http://localhost:3000", normalizeAddress("localhost:3000"))
        assertEquals("http://localhost:8080/api", normalizeAddress("localhost:8080/api"))
    }

    @Test
    fun ipv4AddressGetsHttp() {
        assertEquals("http://192.168.1.1", normalizeAddress("192.168.1.1"))
        assertEquals("http://192.168.1.1:8080/admin", normalizeAddress("192.168.1.1:8080/admin"))
    }
}
