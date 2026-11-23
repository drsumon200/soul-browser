package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BangRegistryTest {
    @Test
    fun resolvesBuiltInAndCustomBangs() {
        assertEquals("https://github.com/search?q=compose", BangRegistry.resolve("!gh compose"))
        val custom = listOf(CustomSearchEngine("PubMed", "pm", "https://pubmed.ncbi.nlm.nih.gov/?term=%s"))
        assertEquals("https://pubmed.ncbi.nlm.nih.gov/?term=cardiology", BangRegistry.resolve("!pm cardiology", custom))
        assertNull(BangRegistry.resolve("!unknown test"))
    }
}
