package com.example.ghostcart.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun productApiUsesTheDeployedJsonEndpoint() {
        assertEquals("https://ghost-cart-preview.maaz-n-khan.chatgpt.site", ApiConfig.PRODUCT_API_BASE_URL)
    }

    @Test
    fun replacesNonJsonServerErrorsWithManualEntryGuidance() {
        val message = nonJsonProductApiFallback("Not Found").orEmpty()
        assertTrue(message.contains("enter the details manually", ignoreCase = true))
        assertTrue(!message.contains("JSONObject"))
    }

    @Test
    fun permitsJsonResponsesToReachTheAndroidParser() {
        assertNull(nonJsonProductApiFallback("  {\"product\":{}}"))
    }

    @Test
    fun extractsAmazonTitlePriceAndHighResolutionImageFromDeviceHtml() {
        val html = """
            <title>Schecter C-7 FR-S Apocalypse - Red Reign: Buy Online at Best Price in UAE - Amazon.ae</title>
            <span class="a-price"><span class="a-offscreen">AED&nbsp;12,131.29</span></span>
            <script>window.images=["https://m.media-amazon.com/images/I/71Xud7FK0UL._AC_SL1500_.jpg"];</script>
        """.trimIndent()
        val metadata = extractRetailerHtmlMetadata(html)
        assertEquals("Schecter C-7 FR-S Apocalypse - Red Reign", metadata.title)
        assertEquals(1_213_129L, metadata.priceCents)
        assertEquals("AED", metadata.currencyCode)
        assertEquals("https://m.media-amazon.com/images/I/71Xud7FK0UL._AC_SL1500_.jpg", metadata.imageUrl)
    }

    @Test
    fun extractsNoonOpenGraphImageAndPriceMetadata() {
        val html = """
            <meta property="og:title" content="Schecter 1742 Electric Guitar Synyster Custom" />
            <meta property="og:image" content="https://f.nooncdn.com/p/pnsku/N70000000V/45/_/1720000000/guitar.jpg" />
            <meta property="product:price:amount" content="5499.00" />
            <meta property="product:price:currency" content="AED" />
        """.trimIndent()
        val metadata = extractRetailerHtmlMetadata(html)
        assertEquals("Schecter 1742 Electric Guitar Synyster Custom", metadata.title)
        assertEquals(549_900L, metadata.priceCents)
        assertEquals("AED", metadata.currencyCode)
        assertEquals(
            "https://f.nooncdn.com/p/pnsku/N70000000V/45/_/1720000000/guitar.jpg",
            metadata.imageUrl
        )
    }

    @Test
    fun keepsThumbnailAndTitleSuppliedByAndroidShareIntent() {
        val imported = ImportedProduct(
            title = "B07DL85",
            priceCents = null,
            currencyCode = null,
            category = "Other",
            imageUrl = null,
            sourceUrl = "https://www.amazon.ae/dp/B07DL85",
            sourceDomain = "www.amazon.ae",
            retailer = "Amazon",
            status = "needs_input",
            note = "The retailer did not expose every detail."
        )
        val merged = mergeSharedMetadata(
            product = imported,
            sharedTitle = "Schecter C-7 FR-S Apocalypse - Red Reign",
            sharedImageUrl = "file:///data/user/0/com.ghostcart.app/files/shared-product-images/share.jpg"
        )
        assertEquals("Schecter C-7 FR-S Apocalypse - Red Reign", merged.title)
        assertEquals(
            "file:///data/user/0/com.ghostcart.app/files/shared-product-images/share.jpg",
            merged.imageUrl
        )
        assertEquals("partial", merged.status)
    }
}
