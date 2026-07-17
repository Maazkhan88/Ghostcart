package com.example.ghostcart.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductImportRepositoryTest {
    @Test
    fun extractsAmazonLinkFromSharedText() {
        val result = extractSupportedRetailerUrl("Look at this https://www.amazon.ae/example/dp/B0ABC12345?ref=share")
        assertEquals("https://www.amazon.ae/example/dp/B0ABC12345?ref=share", result)
    }

    @Test
    fun extractsNoonLinkAndTrimsPunctuation() {
        val result = extractSupportedRetailerUrl("Try https://www.noon.com/uae-en/product/p/?o=abc123).")
        assertEquals("https://www.noon.com/uae-en/product/p/?o=abc123", result)
    }

    @Test
    fun ignoresUnsupportedLinks() {
        assertNull(extractSupportedRetailerUrl("https://example.com/product/123"))
    }
}