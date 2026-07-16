package com.example.ghostcart.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class MarketplaceModelsTest {
    private val catalog = listOf(
        MarketplaceProduct("phone", "Sample phone", "Electronics", 0, "devices"),
        MarketplaceProduct("shirt", "Sample shirt", "Fashion", 0, "checkroom"),
        MarketplaceProduct("guitar", "Sample guitar", "Music", 0, "headphones"),
        MarketplaceProduct("ring", "Sample ring", "Jewellery", 0, "star"),
        MarketplaceProduct("controller", "Sample controller", "Gaming", 0, "devices")
    )

    @Test
    fun browseCategories_includeRequestedMarketplaceSections() {
        assertEquals(
            listOf("all", "electronics", "apparel", "music", "jewelry", "gaming", "beauty", "home", "food"),
            Marketplace.browseCategories.map { it.id }
        )
    }

    @Test
    fun productsForCategory_filtersEachRequestedCategory() {
        assertEquals(listOf("phone"), Marketplace.productsForCategory("electronics", catalog).map { it.id })
        assertEquals(listOf("shirt"), Marketplace.productsForCategory("apparel", catalog).map { it.id })
        assertEquals(listOf("guitar"), Marketplace.productsForCategory("music", catalog).map { it.id })
        assertEquals(listOf("ring"), Marketplace.productsForCategory("jewelry", catalog).map { it.id })
        assertEquals(listOf("controller"), Marketplace.productsForCategory("gaming", catalog).map { it.id })
    }

    @Test
    fun mostGhostedDemo_doesNotInferPopularityFromPrice() {
        val expensiveUnrankedProduct = MarketplaceProduct(
            id = "not-ranked",
            name = "Sample item",
            category = "Electronics",
            price = 9999,
            iconName = "devices"
        )

        assertTrue(Marketplace.productsForCategory("most_ghosted", listOf(expensiveUnrankedProduct)).isEmpty())
    }
}
