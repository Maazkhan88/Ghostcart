import SwiftUI

// Mirrors Android's ProductDiscoverySection (ui/v2/ProductDiscovery.kt),
// verified against a live device screenshot, not just source reading:
// search bar, category chips, a "Marketplace products" rail with an
// All/User Ghosted filter, a "Food & delivery" rail, and a "Your favorites"
// rail. All filtering (category/food/favorites/search) is client-side over
// one merged catalog+community product list, matching Android.
struct MarketplaceProduct: Identifiable, Equatable {
    let id: String
    let name: String
    let category: String
    let priceCents: Int
    let imageUrl: String?
    let isUserGhosted: Bool

    var priceAmount: Double { Double(priceCents) / 100 }

    var isFood: Bool {
        let lowered = category.lowercased()
        return ["food", "fast", "coffee", "delivery", "healthy"].contains { lowered.contains($0) }
    }
}

enum MarketplaceService {
    static func fetchProducts() async -> [MarketplaceProduct] {
        async let catalog = fetchCatalog()
        async let community = fetchCommunity()
        // Matches Android's unifiedMarketplaceProducts()
        // (AppViewModel.kt:372-393): .sortedByDescending { isUserGhosted } -
        // community/user-ghosted items sort first, ahead of the catalog.
        let (catalogItems, communityItems) = await (catalog, community)
        return communityItems + catalogItems
    }

    private static func fetchCatalog() async -> [MarketplaceProduct] {
        guard let object = try? await ApiClient.shared.getJSON(path: "/api/products"),
              let items = object["products"] as? [[String: Any]] else { return [] }
        return items.compactMap { item -> MarketplaceProduct? in
            guard (item["isActive"] as? Bool) != false,
                  let id = item["id"] as? Int, let name = item["name"] as? String else { return nil }
            return MarketplaceProduct(
                id: "catalog_\(id)",
                name: name,
                category: item["category"] as? String ?? "Other",
                priceCents: (item["priceCents"] as? Int) ?? 0,
                imageUrl: item["imageUrl"] as? String,
                isUserGhosted: false
            )
        }
    }

    private static func fetchCommunity() async -> [MarketplaceProduct] {
        guard let object = try? await ApiClient.shared.getJSON(path: "/api/community-products?limit=30"),
              let items = object["products"] as? [[String: Any]] else { return [] }
        return items.compactMap { item -> MarketplaceProduct? in
            guard let id = item["id"] as? String, let title = item["title"] as? String else { return nil }
            return MarketplaceProduct(
                id: "community_\(id)",
                name: title,
                category: item["category"] as? String ?? "Other",
                priceCents: (item["priceCents"] as? Int) ?? 0,
                imageUrl: item["imageUrl"] as? String,
                isUserGhosted: true
            )
        }
    }
}

// Local-only for now, matching Android's guest behavior (FavoriteRepository
// early-returns without a session token) - server sync is task #2, pending.
enum FavoritesStore {
    private static let key = "ghostcart.v2.favorite-product-ids"

    static func load() -> Set<String> {
        Set(UserDefaults.standard.stringArray(forKey: key) ?? [])
    }

    static func save(_ ids: Set<String>) {
        UserDefaults.standard.set(Array(ids), forKey: key)
    }
}

private enum BrowseCategory: String, CaseIterable, Identifiable {
    case all, electronics, apparel, music, jewelry, gaming, beauty, home

    var id: String { rawValue }
    var title: String {
        switch self {
        case .all: return "All"
        case .electronics: return "Electronics"
        case .apparel: return "Apparel"
        case .music: return "Music instruments"
        case .jewelry: return "Jewellery"
        case .gaming: return "Gaming"
        case .beauty: return "Beauty"
        case .home: return "Home"
        }
    }

    func matches(_ category: String) -> Bool {
        if self == .all { return true }
        return category.lowercased().contains(rawValue)
    }
}

struct MarketplaceSection: View {
    let products: [MarketplaceProduct]
    var favoriteIds: Set<String>
    let onToggleFavorite: (MarketplaceProduct) -> Void
    let onAddToCart: (MarketplaceProduct) -> Void

    @State private var query = ""
    @State private var category: BrowseCategory = .all
    @State private var userGhostedOnly = false

    private var searched: [MarketplaceProduct] {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else { return products }
        return products.filter {
            $0.name.localizedCaseInsensitiveContains(query) || $0.category.localizedCaseInsensitiveContains(query)
        }
    }

    private var marketplaceRow: [MarketplaceProduct] {
        searched
            .filter { !$0.isFood && category.matches($0.category) }
            .filter { !userGhostedOnly || $0.isUserGhosted }
    }

    private var foodRow: [MarketplaceProduct] {
        searched.filter { $0.isFood }
    }

    private var favoritesRow: [MarketplaceProduct] {
        searched.filter { favoriteIds.contains($0.id) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            TextField("Search products", text: $query)
                .ghostTextField()

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(BrowseCategory.allCases) { option in
                        Button(option.title) { category = option }
                            .font(.caption.weight(.bold))
                            .foregroundStyle(category == option ? Color.inkColor : Color.primary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(category == option ? Color.ghostGreenColor : Color.primary.opacity(0.05))
                            .clipShape(Capsule())
                    }
                }
            }

            productRailSection(
                title: "Marketplace products",
                subtitle: "browse every temptation",
                products: marketplaceRow,
                filterToggle: true
            )

            if !foodRow.isEmpty {
                productRailSection(title: "Food & delivery", subtitle: "Ghost lunch, dinner or a delivery craving", products: foodRow, filterToggle: false)
            }

            productRailSection(
                title: "Your favorites",
                subtitle: "saved for a calmer decision",
                products: favoritesRow,
                filterToggle: false,
                emptyMessage: "Favorite an item to keep it close without buying it."
            )
        }
    }

    @ViewBuilder
    private func productRailSection(title: String, subtitle: String, products: [MarketplaceProduct], filterToggle: Bool, emptyMessage: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: title)
            Text(subtitle).font(.caption).foregroundStyle(Color.secondary)

            if filterToggle {
                HStack(spacing: 8) {
                    Button("All") { userGhostedOnly = false }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(!userGhostedOnly ? Color.inkColor : Color.primary)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .background(!userGhostedOnly ? Color.ghostGreenColor : Color.primary.opacity(0.05))
                        .clipShape(Capsule())
                    Button("User Ghosted") { userGhostedOnly = true }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(userGhostedOnly ? Color.inkColor : Color.primary)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .background(userGhostedOnly ? Color.ghostGreenColor : Color.primary.opacity(0.05))
                        .clipShape(Capsule())
                }
            }

            if products.isEmpty {
                if let emptyMessage {
                    Text(emptyMessage).font(.caption).foregroundStyle(Color.secondary)
                }
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(products) { product in
                            MarketplaceProductCard(
                                product: product,
                                isFavorite: favoriteIds.contains(product.id),
                                onToggleFavorite: { onToggleFavorite(product) },
                                onAddToCart: { onAddToCart(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private struct MarketplaceProductCard: View {
    let product: MarketplaceProduct
    let isFavorite: Bool
    let onToggleFavorite: () -> Void
    let onAddToCart: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .topTrailing) {
                // Full card width x ~112pt, a wide rectangle - matches
                // Android's DiscoveryProductCard image box exactly
                // (ProductDiscovery.kt: "full width x 112dp"), not a square.
                ProductThumbnail(imageURL: product.imageUrl, systemImage: AlmostBuyCategory(serverName: product.category).systemImage, width: 168, height: 112, cornerRadius: 14)
                Button(action: onToggleFavorite) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .font(.subheadline)
                        .foregroundStyle(Color.inkColor)
                        .padding(7)
                        .background(Color.white)
                        .clipShape(Circle())
                }
                .padding(6)
            }

            Text(product.category).font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            Text(product.name)
                .font(.caption.weight(.bold))
                .lineLimit(2)
                .frame(height: 32, alignment: .top)
            if product.priceCents > 0 {
                DirhamAmount(value: product.priceAmount, font: .caption.weight(.heavy))
            } else {
                Text("Add price").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
            }

            Button(action: onAddToCart) {
                VStack(spacing: 1) {
                    Text("Add to cart").font(.caption.weight(.bold))
                    Text("Cooldown starts at checkout").font(.system(size: 9)).opacity(0.68)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            .foregroundStyle(Color.inkColor)
            .background(Color.ghostGreenColor)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .padding(12)
        .frame(width: 168)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color.primary.opacity(0.09), lineWidth: 1)
        }
    }
}
