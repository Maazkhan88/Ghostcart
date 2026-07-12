import Foundation

struct Product: Identifiable, Hashable {
    let id: String
    let name: String
    let note: String
    let category: String
    let toneColorHex: String
    let vectorIconName: String
}

struct DemoCatalog {
    static let products = [
        Product(
            id: "sneakers",
            name: "The white sneakers",
            note: "Saved after a late-night scroll",
            category: "Fashion",
            toneColorHex: "D0E5FF", // Ice Blue
            vectorIconName: "sneaker"
        ),
        Product(
            id: "perfume",
            name: "The blind-buy perfume",
            note: "A review made it feel urgent",
            category: "Beauty",
            toneColorHex: "FFF3D6", // Warm Gold
            vectorIconName: "perfume"
        ),
        Product(
            id: "burger",
            name: "The midnight combo",
            note: "Built from boredom, not hunger",
            category: "Delivery",
            toneColorHex: "FFD6D6", // Light Red
            vectorIconName: "burger"
        ),
        Product(
            id: "headphones",
            name: "The extra headphones",
            note: "A deal you did not need yesterday",
            category: "Tech",
            toneColorHex: "D6FFE5", // Mint Green
            vectorIconName: "headphones"
        )
    ]
}
