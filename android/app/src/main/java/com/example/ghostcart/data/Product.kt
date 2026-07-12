package com.example.ghostcart.data

data class Product(
    val id: String,
    val name: String,
    val note: String,
    val category: String,
    val toneColorHex: String,
    val vectorIconName: String
)

object DemoCatalog {
    val products = listOf(
        Product(
            id = "sneakers",
            name = "The white sneakers",
            note = "Saved after a late-night scroll",
            category = "Fashion",
            toneColorHex = "FFD0E5FF", // Light Blue/Ice
            vectorIconName = "sneaker"
        ),
        Product(
            id = "perfume",
            name = "The blind-buy perfume",
            note = "A review made it feel urgent",
            category = "Beauty",
            toneColorHex = "FFFFF3D6", // Light Gold/Smoke
            vectorIconName = "perfume"
        ),
        Product(
            id = "burger",
            name = "The midnight combo",
            note = "Built from boredom, not hunger",
            category = "Delivery",
            toneColorHex = "FFFFD6D6", // Light Red/Warm
            vectorIconName = "burger"
        ),
        Product(
            id = "headphones",
            name = "The extra headphones",
            note = "A deal you did not need yesterday",
            category = "Tech",
            toneColorHex = "FFD6FFE5", // Light Green/Mint
            vectorIconName = "headphones"
        )
    )
}
