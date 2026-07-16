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

data class MarketplaceCategory(
    val id: String,
    val label: String,
    val iconName: String
)

data class SponsoredBrand(val name: String, val tagline: String)

/**
 * Picks a generic icon that actually matches the product's title, instead of a
 * single hardcoded icon per category (which previously showed e.g. a headphones
 * icon on a desk fan, or a sneaker icon on a windbreaker). Falls back to a
 * category-level default, and finally to whatever was stored on the product.
 */
fun iconForProduct(product: MarketplaceProduct): String {
    val n = product.name.lowercase()
    return when {
        n.contains("latte") || n.contains("macchiato") || n.contains("matcha") || n.contains("boba") || n.contains("tea") -> "coffee"
        n.contains("donut") || n.contains("cupcake") || n.contains("croissant") -> "donut"
        n.contains("burger") || n.contains("fries") || n.contains("sushi") || n.contains("toast") -> "burger"
        n.contains("lipstick") || n.contains("lip glow") || n.contains("lip ") -> "lipstick"
        n.contains("serum") || n.contains("mist") || n.contains("oil") || n.contains("cologne") || n.contains("perfume") -> "perfume"
        n.contains("mask") || n.contains("blush") || n.contains("palette") -> "jar"
        n.contains("incense") -> "incense"
        n.contains("sunglasses") -> "sunglasses"
        n.contains("speaker") -> "speaker"
        n.contains("watch") -> "watch"
        n.contains("tablet") -> "tablet"
        n.contains("headphone") || n.contains("earbud") -> "headphones"
        n.contains("sneaker") -> "sneaker"
        n.contains("jacket") || n.contains(" tee") || n.contains("jeans") || n.contains("hoodie") ||
            n.contains("beanie") || n.contains("socks") || n.contains("belt") || n.contains("windbreaker") -> "shirt"
        n.contains("tote") || n.contains("bag") -> "bag"
        n.contains("mount") || n.contains("mat") || n.contains("hub") || n.contains("fan") ||
            n.contains("strip") || n.contains("light") || n.contains("pad") || n.contains("sleeve") ||
            n.contains("adapter") -> "gadget"
        else -> when {
            product.category.contains("Food", ignoreCase = true) ||
                product.category.contains("Fast Food", ignoreCase = true) ||
                product.category.contains("Healthy", ignoreCase = true) ||
                product.category.contains("Coffee", ignoreCase = true) -> "burger"
            product.category.contains("Beauty", ignoreCase = true) -> "perfume"
            product.category.contains("Fashion", ignoreCase = true) -> "shirt"
            product.category.contains("Gadgets", ignoreCase = true) || product.category.contains("Tech", ignoreCase = true) -> "gadget"
            product.category.contains("Delivery", ignoreCase = true) -> "burger"
            else -> product.iconName
        }
    }
}

object Marketplace {
    const val currency = "AED"

    val browseCategories = listOf(
        MarketplaceCategory("all", "All", "bag"),
        MarketplaceCategory("electronics", "Electronics", "devices"),
        MarketplaceCategory("apparel", "Apparel", "checkroom"),
        MarketplaceCategory("music", "Music instruments", "headphones"),
        MarketplaceCategory("jewelry", "Jewellery", "star"),
        MarketplaceCategory("gaming", "Gaming", "devices"),
        MarketplaceCategory("beauty", "Beauty", "spa"),
        MarketplaceCategory("home", "Home", "chair"),
        MarketplaceCategory("food", "Food & drinks", "coffee")
    )

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

    val featuredCatalog = listOf(
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
        MarketplaceProduct("smartwatch", "Smartwatch Pro", "Gadgets & Tech", 699, "watch"),
        MarketplaceProduct("tablet", "Tablet Mini 6", "Gadgets & Tech", 1199, "tablet"),
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

    val dummyCatalog = listOf(
        MarketplaceProduct("dummy_food___coffee_1", "Caramel Macchiato #1", "Food & Coffee", 22, "coffee"),
        MarketplaceProduct("dummy_beauty_2", "Hydrating Lip Glow #2", "Beauty", 29, "perfume"),
        MarketplaceProduct("dummy_fashion_3", "Retro Windbreaker #3", "Fashion", 36, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_4", "Magnetic Phone Mount #4", "Gadgets & Tech", 43, "headphones"),
        MarketplaceProduct("dummy_food___coffee_5", "Matcha Latte #5", "Food & Coffee", 50, "coffee"),
        MarketplaceProduct("dummy_beauty_6", "Vitamin C Serum #6", "Beauty", 57, "perfume"),
        MarketplaceProduct("dummy_fashion_7", "Classic White Tee #7", "Fashion", 64, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_8", "RGB Desk Mat #8", "Gadgets & Tech", 71, "headphones"),
        MarketplaceProduct("dummy_food___coffee_9", "Avocado Toast #9", "Food & Coffee", 78, "coffee"),
        MarketplaceProduct("dummy_beauty_10", "Rosewater Face Mist #10", "Beauty", 85, "perfume"),
        MarketplaceProduct("dummy_fashion_11", "Black Denim Jacket #11", "Fashion", 92, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_12", "Bluetooth Speaker #12", "Gadgets & Tech", 99, "headphones"),
        MarketplaceProduct("dummy_food___coffee_13", "Chocolate Croissant #13", "Food & Coffee", 106, "coffee"),
        MarketplaceProduct("dummy_beauty_14", "Matte Lipstick #14", "Beauty", 113, "perfume"),
        MarketplaceProduct("dummy_fashion_15", "Distressed Jeans #15", "Fashion", 120, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_16", "USB-C Hub adapter #16", "Gadgets & Tech", 127, "headphones"),
        MarketplaceProduct("dummy_food___coffee_17", "Strawberry Donut #17", "Food & Coffee", 134, "coffee"),
        MarketplaceProduct("dummy_beauty_18", "Vanilla Oud Cologne #18", "Beauty", 141, "perfume"),
        MarketplaceProduct("dummy_fashion_19", "Canvas Tote Bag #19", "Fashion", 148, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_20", "Ring Light Stand #20", "Gadgets & Tech", 155, "headphones"),
        MarketplaceProduct("dummy_food___coffee_21", "Iced Boba Tea #21", "Food & Coffee", 162, "coffee"),
        MarketplaceProduct("dummy_beauty_22", "Gold Glow Face Oil #22", "Beauty", 169, "perfume"),
        MarketplaceProduct("dummy_fashion_23", "Wool Beanie #23", "Fashion", 176, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_24", "Mini Desktop Fan #24", "Gadgets & Tech", 183, "headphones"),
        MarketplaceProduct("dummy_food___coffee_25", "Truffle Fries #25", "Food & Coffee", 190, "coffee"),
        MarketplaceProduct("dummy_beauty_26", "Sandalwood Incense #26", "Beauty", 197, "perfume"),
        MarketplaceProduct("dummy_fashion_27", "Polarized Sunglasses #27", "Fashion", 204, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_28", "Smart LED Strip #28", "Gadgets & Tech", 211, "headphones"),
        MarketplaceProduct("dummy_food___coffee_29", "Double Cheeseburger #29", "Food & Coffee", 218, "coffee"),
        MarketplaceProduct("dummy_beauty_30", "Peachy Blush Palette #30", "Beauty", 225, "perfume"),
        MarketplaceProduct("dummy_fashion_31", "Crew Socks Pack #31", "Fashion", 232, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_32", "Wireless Charging Pad #32", "Gadgets & Tech", 239, "headphones"),
        MarketplaceProduct("dummy_food___coffee_33", "Sushi Combo #33", "Food & Coffee", 246, "coffee"),
        MarketplaceProduct("dummy_beauty_34", "Argan Hair Mask #34", "Beauty", 253, "perfume"),
        MarketplaceProduct("dummy_fashion_35", "Leather Belt #35", "Fashion", 260, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_36", "Laptop Sleeve Case #36", "Gadgets & Tech", 267, "headphones"),
        MarketplaceProduct("dummy_food___coffee_37", "Red Velvet Cupcake #37", "Food & Coffee", 274, "coffee"),
        MarketplaceProduct("dummy_beauty_38", "Clay Face Mask #38", "Beauty", 281, "perfume"),
        MarketplaceProduct("dummy_fashion_39", "Cozy Oversized Hoodie #39", "Fashion", 288, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_40", "Gaming Mousepad #40", "Gadgets & Tech", 295, "headphones"),
        MarketplaceProduct("dummy_food___coffee_41", "Caramel Macchiato #41", "Food & Coffee", 302, "coffee"),
        MarketplaceProduct("dummy_beauty_42", "Hydrating Lip Glow #42", "Beauty", 309, "perfume"),
        MarketplaceProduct("dummy_fashion_43", "Retro Windbreaker #43", "Fashion", 316, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_44", "Magnetic Phone Mount #44", "Gadgets & Tech", 323, "headphones"),
        MarketplaceProduct("dummy_food___coffee_45", "Matcha Latte #45", "Food & Coffee", 330, "coffee"),
        MarketplaceProduct("dummy_beauty_46", "Vitamin C Serum #46", "Beauty", 337, "perfume"),
        MarketplaceProduct("dummy_fashion_47", "Classic White Tee #47", "Fashion", 344, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_48", "RGB Desk Mat #48", "Gadgets & Tech", 351, "headphones"),
        MarketplaceProduct("dummy_food___coffee_49", "Avocado Toast #49", "Food & Coffee", 358, "coffee"),
        MarketplaceProduct("dummy_beauty_50", "Rosewater Face Mist #50", "Beauty", 15, "perfume"),
        MarketplaceProduct("dummy_fashion_51", "Black Denim Jacket #51", "Fashion", 22, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_52", "Bluetooth Speaker #52", "Gadgets & Tech", 29, "headphones"),
        MarketplaceProduct("dummy_food___coffee_53", "Chocolate Croissant #53", "Food & Coffee", 36, "coffee"),
        MarketplaceProduct("dummy_beauty_54", "Matte Lipstick #54", "Beauty", 43, "perfume"),
        MarketplaceProduct("dummy_fashion_55", "Distressed Jeans #55", "Fashion", 50, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_56", "USB-C Hub adapter #56", "Gadgets & Tech", 57, "headphones"),
        MarketplaceProduct("dummy_food___coffee_57", "Strawberry Donut #57", "Food & Coffee", 64, "coffee"),
        MarketplaceProduct("dummy_beauty_58", "Vanilla Oud Cologne #58", "Beauty", 71, "perfume"),
        MarketplaceProduct("dummy_fashion_59", "Canvas Tote Bag #59", "Fashion", 78, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_60", "Ring Light Stand #60", "Gadgets & Tech", 85, "headphones"),
        MarketplaceProduct("dummy_food___coffee_61", "Iced Boba Tea #61", "Food & Coffee", 92, "coffee"),
        MarketplaceProduct("dummy_beauty_62", "Gold Glow Face Oil #62", "Beauty", 99, "perfume"),
        MarketplaceProduct("dummy_fashion_63", "Wool Beanie #63", "Fashion", 106, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_64", "Mini Desktop Fan #64", "Gadgets & Tech", 113, "headphones"),
        MarketplaceProduct("dummy_food___coffee_65", "Truffle Fries #65", "Food & Coffee", 120, "coffee"),
        MarketplaceProduct("dummy_beauty_66", "Sandalwood Incense #66", "Beauty", 127, "perfume"),
        MarketplaceProduct("dummy_fashion_67", "Polarized Sunglasses #67", "Fashion", 134, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_68", "Smart LED Strip #68", "Gadgets & Tech", 141, "headphones"),
        MarketplaceProduct("dummy_food___coffee_69", "Double Cheeseburger #69", "Food & Coffee", 148, "coffee"),
        MarketplaceProduct("dummy_beauty_70", "Peachy Blush Palette #70", "Beauty", 155, "perfume"),
        MarketplaceProduct("dummy_fashion_71", "Crew Socks Pack #71", "Fashion", 162, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_72", "Wireless Charging Pad #72", "Gadgets & Tech", 169, "headphones"),
        MarketplaceProduct("dummy_food___coffee_73", "Sushi Combo #73", "Food & Coffee", 176, "coffee"),
        MarketplaceProduct("dummy_beauty_74", "Argan Hair Mask #74", "Beauty", 183, "perfume"),
        MarketplaceProduct("dummy_fashion_75", "Leather Belt #75", "Fashion", 190, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_76", "Laptop Sleeve Case #76", "Gadgets & Tech", 197, "headphones"),
        MarketplaceProduct("dummy_food___coffee_77", "Red Velvet Cupcake #77", "Food & Coffee", 204, "coffee"),
        MarketplaceProduct("dummy_beauty_78", "Clay Face Mask #78", "Beauty", 211, "perfume"),
        MarketplaceProduct("dummy_fashion_79", "Cozy Oversized Hoodie #79", "Fashion", 218, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_80", "Gaming Mousepad #80", "Gadgets & Tech", 225, "headphones"),
        MarketplaceProduct("dummy_food___coffee_81", "Caramel Macchiato #81", "Food & Coffee", 232, "coffee"),
        MarketplaceProduct("dummy_beauty_82", "Hydrating Lip Glow #82", "Beauty", 239, "perfume"),
        MarketplaceProduct("dummy_fashion_83", "Retro Windbreaker #83", "Fashion", 246, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_84", "Magnetic Phone Mount #84", "Gadgets & Tech", 253, "headphones"),
        MarketplaceProduct("dummy_food___coffee_85", "Matcha Latte #85", "Food & Coffee", 260, "coffee"),
        MarketplaceProduct("dummy_beauty_86", "Vitamin C Serum #86", "Beauty", 267, "perfume"),
        MarketplaceProduct("dummy_fashion_87", "Classic White Tee #87", "Fashion", 274, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_88", "RGB Desk Mat #88", "Gadgets & Tech", 281, "headphones"),
        MarketplaceProduct("dummy_food___coffee_89", "Avocado Toast #89", "Food & Coffee", 288, "coffee"),
        MarketplaceProduct("dummy_beauty_90", "Rosewater Face Mist #90", "Beauty", 295, "perfume"),
        MarketplaceProduct("dummy_fashion_91", "Black Denim Jacket #91", "Fashion", 302, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_92", "Bluetooth Speaker #92", "Gadgets & Tech", 309, "headphones"),
        MarketplaceProduct("dummy_food___coffee_93", "Chocolate Croissant #93", "Food & Coffee", 316, "coffee"),
        MarketplaceProduct("dummy_beauty_94", "Matte Lipstick #94", "Beauty", 323, "perfume"),
        MarketplaceProduct("dummy_fashion_95", "Distressed Jeans #95", "Fashion", 330, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_96", "USB-C Hub adapter #96", "Gadgets & Tech", 337, "headphones"),
        MarketplaceProduct("dummy_food___coffee_97", "Strawberry Donut #97", "Food & Coffee", 344, "coffee"),
        MarketplaceProduct("dummy_beauty_98", "Vanilla Oud Cologne #98", "Beauty", 351, "perfume"),
        MarketplaceProduct("dummy_fashion_99", "Canvas Tote Bag #99", "Fashion", 358, "sneaker"),
        MarketplaceProduct("dummy_gadgets___tech_100", "Ring Light Stand #100", "Gadgets & Tech", 15, "headphones")
    )


    val almostSpentThisWeek = 642
    val savedThisWeekAcaiTotal = 150

    fun productsForCategory(
        categoryId: String,
        products: List<MarketplaceProduct>
    ): List<MarketplaceProduct> = products.filter { product ->
        val category = product.category.lowercase()
        val name = product.name.lowercase()

        when (categoryId) {
            "food" -> listOf("food", "fast", "coffee", "delivery", "healthy").any(category::contains)
            "beauty" -> category.contains("beauty")
            "apparel", "fashion" -> category.contains("fashion") || category.contains("apparel")
            "electronics", "gadgets" -> category.contains("gadget") || category.contains("tech") || category.contains("electronic")
            "music" -> category.contains("music") || listOf("guitar", "keyboard", "drum", "microphone", "instrument").any(name::contains)
            "jewelry" -> category.contains("jewel") || listOf("ring", "necklace", "bracelet", "earring").any(name::contains)
            "gaming" -> category.contains("gaming") || listOf("gaming", "console", "controller").any(name::contains)
            "home" -> category.contains("home") || category.contains("decor")
            // Live rankings come from the anonymous ghost-events API. Never
            // infer popularity from static catalog fields or prices.
            "most_ghosted" -> false
            "flash_deals" -> fakeFlashDeals.any { it.id == product.id }
            "all" -> true
            else -> false
        }
    }
}
