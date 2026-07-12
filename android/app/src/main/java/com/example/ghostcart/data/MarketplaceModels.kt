package com.example.ghostcart.data

data class MarketplaceProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val iconName: String,
    val description: String = "",
    val scentOrType: String = "",
    val size: String = "",
    val brand: String = "",
    val highEmotion: Boolean = false
)

data class OverspendCategory(
    val id: String,
    val label: String,
    val iconName: String
)

data class SponsoredBrand(val name: String, val tagline: String)

object Marketplace {
    const val currency = "AED"

    val overspendCategories = listOf(
        OverspendCategory("food", "Food & Coffee", "coffee"),
        OverspendCategory("beauty", "Beauty & Perfume", "spa"),
        OverspendCategory("fashion", "Fashion & Shoes", "checkroom"),
        OverspendCategory("gadgets", "Gadgets & Tech", "devices"),
        OverspendCategory("delivery", "Delivery Cravings", "scooter"),
        OverspendCategory("luxury", "Luxury", "star"),
        OverspendCategory("home", "Home Decor", "chair"),
        OverspendCategory("music", "Music Gear", "headphones"),
        OverspendCategory("goals", "Big Life Goals", "target"),
        OverspendCategory("lateNight", "Random Late-Night Shopping", "bag")
    )

    val savingsGoalPresets = listOf(500, 1000, 2500)

    val mostGhostedToday = listOf(
        MarketplaceProduct("latte", "Spanish Latte", "Food & Coffee", 38, "coffee"),
        MarketplaceProduct("burgerMeal", "Midnight Burger Meal", "Delivery", 75, "burger"),
        MarketplaceProduct("perfumeBlind", "Luxury Perfume Blind Buy", "Beauty", 420, "perfume",
            description = "Almost bought because of a TikTok review?",
            scentOrType = "Floral • Musk • Vanilla", size = "100 ml", brand = "Premium Niche",
            highEmotion = true),
        MarketplaceProduct("sneakersWhite", "White Sneakers", "Fashion", 549, "sneaker")
    )

    val fakeFlashDeals = listOf(
        MarketplaceProduct("earbuds", "Wireless Earbuds", "Gadgets & Tech", 399, "headphones"),
        MarketplaceProduct("smartwatch", "Smartwatch Pro", "Gadgets & Tech", 699, "wallet"),
        MarketplaceProduct("tablet", "Tablet Mini 6", "Gadgets & Tech", 1199, "wallet"),
        MarketplaceProduct("headphonesNc", "Noise Cancelling Headphones", "Gadgets & Tech", 279, "headphones")
    )

    val sponsoredBrands = listOf(
        SponsoredBrand("SHEIN", "Up to 70% off"),
        SponsoredBrand("noon", "Deals you don't need"),
        SponsoredBrand("Namshi", "Trendy, not necessary"),
        SponsoredBrand("Amazon.ae", "Add, don't buy")
    )

    val foodAndCoffeeCatalog = listOf(
        MarketplaceProduct("lateCombo", "Late Combo", "Fast Food", 38, "burger"),
        MarketplaceProduct("midnightBurger", "Midnight Burger Meal", "Fast Food", 75, "burger"),
        MarketplaceProduct("pizzaCombo", "Pizza Combo", "Fast Food", 95, "burger"),
        MarketplaceProduct("acaiBowl", "Acai Bowl", "Healthy", 55, "leaf"),
        MarketplaceProduct("spanishLatte", "Spanish Latte", "Coffee & Drinks", 38, "coffee")
    )

    val almostSpentThisWeek = 642
    val savedThisWeekAcaiTotal = 150
}
