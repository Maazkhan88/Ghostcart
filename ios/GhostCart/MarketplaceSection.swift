import SwiftUI

// Mirrors Android's ProductDiscoverySection (ui/v2/ProductDiscovery.kt),
// verified against a live device screenshot, not just source reading:
// search bar, category chips, a "Marketplace products" rail with an
// All/User Ghosted filter, a "Food & delivery" rail, and a "Your favorites"
// rail. All filtering (category/food/favorites/search) is client-side over
// one merged catalog+community product list, matching Android.
struct MarketplaceProduct: Identifiable, Equatable, Hashable {
    let id: String
    let name: String
    let category: String
    let priceCents: Int
    let imageUrl: String?
    let isUserGhosted: Bool
    var productDescription: String? = nil
    var brand: String? = nil
    var sourceURL: String? = nil

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
                isUserGhosted: false,
                productDescription: item["description"] as? String,
                brand: item["brand"] as? String,
                sourceURL: item["sourceUrl"] as? String
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
                isUserGhosted: true,
                productDescription: item["note"] as? String,
                brand: item["retailer"] as? String,
                sourceURL: item["sourceUrl"] as? String
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

private enum MarketplaceBrowseKind: Hashable {
    case all
    case food
    case favorites
    case userGhosted
    case category(String)

    var title: String {
        switch self {
        case .all: return "Almost-Spent Catalog"
        case .food: return "Food & drinks"
        case .favorites: return "Your Favorites"
        case .userGhosted: return "Community Products"
        case .category(let value): return value
        }
    }

    var subtitle: String {
        switch self {
        case .all: return "Browse simulation items you can add to Ghost Cart."
        case .food: return "Ghost delivery cravings before they reach checkout."
        case .favorites: return "Everything you saved for a calmer decision."
        case .userGhosted: return "Anonymous products other Ghost Cart users chose to ghost."
        case .category: return "Put tempting products somewhere safe before buying."
        }
    }
}

struct MarketplaceSection: View {
    let products: [MarketplaceProduct]
    var favoriteIds: Set<String>
    let onToggleFavorite: (MarketplaceProduct) -> Void
    let onAddToCart: (MarketplaceProduct) -> Void
    let onOpenCart: () -> Void
    // Tutorial support: products in this set render with a "Tutorial item"
    // badge and route their "Ghost it" tap through onGhostItDirect (a
    // straight-to-cooling path) instead of the normal add-to-cart/checkout
    // flow - the tutorial never touches real cart/checkout state.
    var tutorialProductIDs: Set<String> = []
    var onGhostItDirect: ((MarketplaceProduct) -> Void)? = nil

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
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color.secondary)
                    .accessibilityHidden(true)
                TextField("Search products", text: $query)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if !query.isEmpty {
                    Button {
                        query = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Color.secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Clear search")
                }
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 50)
            .modifier(GlassCardBackground(cornerRadius: 14))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color.primary.opacity(0.10), lineWidth: 1)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(BrowseCategory.allCases) { option in
                        Button(option.title) { category = option }
                            .font(.caption.weight(.bold))
                            .foregroundStyle(category == option ? Color.inkColor : Color.primary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background {
                                if category == option {
                                    Capsule().fill(Color.ghostGreenColor)
                                } else {
                                    Capsule().fill(.clear).modifier(GlassPillBackground())
                                }
                            }
                    }
                }
            }

            productRailSection(
                title: "Marketplace products",
                subtitle: "browse every temptation",
                products: marketplaceRow,
                filterToggle: true,
                browseKind: .all
            )

            if !foodRow.isEmpty {
                productRailSection(
                    title: "Food & delivery",
                    subtitle: "Ghost lunch, dinner or a delivery craving",
                    products: foodRow,
                    filterToggle: false,
                    browseKind: .food
                )
            }

            productRailSection(
                title: "Your favorites",
                subtitle: "saved for a calmer decision",
                products: favoritesRow,
                filterToggle: false,
                browseKind: .favorites,
                emptyMessage: "Favorite an item to keep it close without buying it."
            )
        }
    }

    @ViewBuilder
    private func productRailSection(
        title: String,
        subtitle: String,
        products: [MarketplaceProduct],
        filterToggle: Bool,
        browseKind: MarketplaceBrowseKind,
        emptyMessage: String? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(title).font(.title3.weight(.bold))
                Spacer()
                NavigationLink {
                    ProductListingView(
                        kind: browseKind,
                        products: productsForListing(browseKind),
                        favoriteIds: favoriteIds,
                        onToggleFavorite: onToggleFavorite,
                        onAddToCart: onAddToCart,
                        onOpenCart: onOpenCart
                    )
                } label: {
                    Text("View all")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.ghostGreenColor)
                }
            }
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
                                onAddToCart: { onAddToCart(product) },
                                onOpenCart: onOpenCart,
                                isTutorial: tutorialProductIDs.contains(product.id),
                                onGhostItDirect: tutorialProductIDs.contains(product.id) ? { onGhostItDirect?(product) } : nil
                            )
                        }
                    }
                }
            }
        }
    }

    private func productsForListing(_ kind: MarketplaceBrowseKind) -> [MarketplaceProduct] {
        switch kind {
        case .all: return products
        case .food: return products.filter(\.isFood)
        case .favorites: return products.filter { favoriteIds.contains($0.id) }
        case .userGhosted: return products.filter(\.isUserGhosted)
        case .category(let value): return products.filter { $0.category.localizedCaseInsensitiveContains(value) }
        }
    }
}

private struct MarketplaceProductCard: View {
    let product: MarketplaceProduct
    let isFavorite: Bool
    let onToggleFavorite: () -> Void
    let onAddToCart: () -> Void
    let onOpenCart: () -> Void
    var isTutorial: Bool = false
    var onGhostItDirect: (() -> Void)? = nil

    private var shareText: String {
        if let source = product.sourceURL, !source.isEmpty { return "Check this out on Ghost Cart:\n\(source)" }
        return "Check out \(product.name) on Ghost Cart."
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .topTrailing) {
                // Full card width x ~112pt, a wide rectangle - matches
                // Android's DiscoveryProductCard image box exactly
                // (ProductDiscovery.kt: "full width x 112dp"), not a square.
                NavigationLink {
                    ProductDetailView(
                        product: product,
                        initiallyFavorite: isFavorite,
                        onToggleFavorite: onToggleFavorite,
                        onAddToCart: onAddToCart,
                        onOpenCart: onOpenCart
                    )
                } label: {
                    ProductThumbnail(imageURL: product.imageUrl, systemImage: AlmostBuyCategory(serverName: product.category).systemImage, productName: product.name, fillWidthHeight: 112, cornerRadius: 14)
                }
                .buttonStyle(.plain)
                Button(action: onToggleFavorite) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .font(.subheadline)
                        .foregroundStyle(Color.inkColor)
                        .padding(7)
                        .modifier(GlassCircleBackground())
                }
                .padding(6)
                .accessibilityLabel(isFavorite ? "Remove from favorites" : "Add to favorites")

                if isTutorial {
                    Text("Tutorial item")
                        .font(.system(size: 8, weight: .black))
                        .foregroundStyle(Color.inkColor)
                        .padding(.horizontal, 6).padding(.vertical, 3)
                        .background(Color.ghostGreenColor, in: Capsule())
                        .padding(6)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                }
            }

            NavigationLink {
                ProductDetailView(
                    product: product,
                    initiallyFavorite: isFavorite,
                    onToggleFavorite: onToggleFavorite,
                    onAddToCart: onAddToCart,
                    onOpenCart: onOpenCart
                )
            } label: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(product.category).font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
                    Text(product.name)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.primary)
                        .lineLimit(2)
                        .frame(minHeight: 32, alignment: .top)
                    if product.priceCents > 0 {
                        DirhamAmount(value: product.priceAmount, font: .caption.weight(.heavy))
                    } else {
                        Text("Add price").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                    }
                }
            }
            .buttonStyle(.plain)

            HStack(spacing: 6) {
                Button {
                    if let onGhostItDirect { onGhostItDirect() } else { onAddToCart() }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "cart").font(.caption.weight(.bold))
                        Text("Ghost it").font(.caption.weight(.bold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                .foregroundStyle(Color.inkColor)
                .background(Color.ghostGreenColor)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .accessibilityHint("Starts a simulated Ghost Order for this item")

                ShareLink(item: shareText) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.subheadline)
                        .foregroundStyle(Color.inkColor)
                        .frame(width: 34, height: 34)
                        .modifier(GlassCircleBackground())
                }
                .simultaneousGesture(TapGesture().onEnded {
                    GhostAnalytics.productShared(product.id)
                })
            }
        }
        .padding(12)
        .frame(width: 168)
        .modifier(GlassCardBackground(cornerRadius: 20))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color.primary.opacity(0.09), lineWidth: 1)
        }
    }
}

// Shared iOS 26 Liquid Glass card/button backgrounds, falling back to
// .regularMaterial pre-26 - mirrors GhostBottomNav's own gating
// (ContentView.swift) so glass adoption stays consistent app-wide.
struct GlassCardBackground: ViewModifier {
    var cornerRadius: CGFloat

    // iOS 26's real glassEffect() renders its visual shape without reliably
    // matching the content's actual padded layout bounds here - text kept
    // correct layout position but the glass "wall" rendered tighter, making
    // card text look like it had no left margin. .regularMaterial doesn't
    // have this failure mode, so it's used unconditionally now rather than
    // only as a pre-26 fallback.
    func body(content: Content) -> some View {
        content.background(.regularMaterial, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}

struct GlassCircleBackground: ViewModifier {
    func body(content: Content) -> some View {
        content.background(.regularMaterial, in: Circle())
    }
}

struct GlassPillBackground: ViewModifier {
    func body(content: Content) -> some View {
        content.background(.regularMaterial, in: Capsule())
    }
}

private enum MarketplaceSort: String, CaseIterable, Identifiable {
    case trending = "Trending"
    case priceLow = "Price: Low to High"
    case priceHigh = "Price: High to Low"
    case name = "Name"
    var id: String { rawValue }
}

private struct ProductListingView: View {
    let kind: MarketplaceBrowseKind
    let products: [MarketplaceProduct]
    let onToggleFavorite: (MarketplaceProduct) -> Void
    let onAddToCart: (MarketplaceProduct) -> Void
    let onOpenCart: () -> Void

    @State private var favoriteIds: Set<String>
    @State private var query = ""
    @State private var selectedCategory = "All"
    @State private var sort: MarketplaceSort = .trending
    @State private var userGhostedOnly = false
    @State private var recentlyAdded: Set<String> = []

    init(
        kind: MarketplaceBrowseKind,
        products: [MarketplaceProduct],
        favoriteIds: Set<String>,
        onToggleFavorite: @escaping (MarketplaceProduct) -> Void,
        onAddToCart: @escaping (MarketplaceProduct) -> Void,
        onOpenCart: @escaping () -> Void
    ) {
        self.kind = kind
        self.products = products
        self.onToggleFavorite = onToggleFavorite
        self.onAddToCart = onAddToCart
        self.onOpenCart = onOpenCart
        _favoriteIds = State(initialValue: favoriteIds)
    }

    private var categories: [String] {
        ["All"] + Array(Set(products.map(\.category))).sorted()
    }

    private var visibleProducts: [MarketplaceProduct] {
        let filtered = products
            .filter { kind != .favorites || favoriteIds.contains($0.id) }
            .filter { kind != .userGhosted || $0.isUserGhosted }
            .filter {
                query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                $0.name.localizedCaseInsensitiveContains(query) ||
                $0.category.localizedCaseInsensitiveContains(query) ||
                ($0.brand?.localizedCaseInsensitiveContains(query) ?? false)
            }
            .filter { selectedCategory == "All" || $0.category == selectedCategory }
            .filter { !userGhostedOnly || $0.isUserGhosted }

        switch sort {
        case .trending: return filtered.sorted { $0.isUserGhosted && !$1.isUserGhosted }
        case .priceLow: return filtered.sorted { $0.priceCents < $1.priceCents }
        case .priceHigh: return filtered.sorted { $0.priceCents > $1.priceCents }
        case .name: return filtered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(kind.title).font(.title2.weight(.black))
                Text(kind.subtitle).font(.caption).foregroundStyle(Color.secondary)

                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundStyle(Color.secondary)
                    TextField("What are you tempted to buy?", text: $query)
                }
                .ghostTextField()

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(categories, id: \.self) { option in
                            Button(option) { selectedCategory = option }
                                .font(.caption.weight(.bold))
                                .foregroundStyle(selectedCategory == option ? Color.inkColor : Color.primary)
                                .padding(.horizontal, 13).padding(.vertical, 8)
                                .background(selectedCategory == option ? Color.ghostGreenColor : Color.primary.opacity(0.05), in: Capsule())
                        }
                    }
                }

                HStack {
                    Menu {
                        ForEach(MarketplaceSort.allCases) { option in
                            Button {
                                sort = option
                            } label: {
                                if sort == option { Label(option.rawValue, systemImage: "checkmark") }
                                else { Text(option.rawValue) }
                            }
                        }
                    } label: {
                        Label("Sort", systemImage: "arrow.up.arrow.down")
                    }
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.inkColor)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.ghostGreenColor, in: Capsule())

                    Button {
                        userGhostedOnly.toggle()
                    } label: {
                        Label("User Ghosted", systemImage: userGhostedOnly ? "checkmark.circle.fill" : "line.3.horizontal.decrease.circle")
                    }
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.inkColor)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(userGhostedOnly ? Color.ghostGreenColor : Color.primary.opacity(0.05), in: Capsule())
                }

                if visibleProducts.isEmpty {
                    EmptyStateView(
                        image: "bag",
                        title: kind == .favorites ? "No favorites yet" : "No demo items here yet",
                        message: kind == .favorites ? "Tap the heart on a product to save it here." : "Try clearing a filter or searching for something else."
                    )
                } else {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: 12)], spacing: 12) {
                        ForEach(visibleProducts) { product in
                            listingCard(product)
                        }
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 28)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Ghost Cart")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func shareText(for product: MarketplaceProduct) -> String {
        if let source = product.sourceURL, !source.isEmpty { return "Check this out on Ghost Cart:\n\(source)" }
        return "Check out \(product.name) on Ghost Cart."
    }

    private func listingCard(_ product: MarketplaceProduct) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            ZStack(alignment: .topTrailing) {
                NavigationLink {
                    detail(product)
                } label: {
                    ProductThumbnail(
                        imageURL: product.imageUrl,
                        systemImage: AlmostBuyCategory(serverName: product.category).systemImage,
                        productName: product.name,
                        fillWidthHeight: 112,
                        cornerRadius: 14
                    )
                }.buttonStyle(.plain)
                Button { toggleFavorite(product) } label: {
                    Image(systemName: favoriteIds.contains(product.id) ? "heart.fill" : "heart")
                        .foregroundStyle(Color.inkColor).padding(7).modifier(GlassCircleBackground())
                }.padding(5)
            }
            NavigationLink { detail(product) } label: {
                VStack(alignment: .leading, spacing: 5) {
                    Text(product.category).font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor).lineLimit(1)
                    Text(product.name).font(.caption.weight(.bold)).foregroundStyle(Color.primary).lineLimit(2).frame(minHeight: 32, alignment: .top)
                    if product.priceCents > 0 {
                        DirhamAmount(value: product.priceAmount, font: .caption.weight(.black), iconSize: 10)
                    } else {
                        Text("Add price").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                    }
                }
            }.buttonStyle(.plain)

            HStack(spacing: 6) {
                Button {
                    onAddToCart(product)
                    recentlyAdded.insert(product.id)
                } label: {
                    HStack(spacing: 6) {
                        if !recentlyAdded.contains(product.id) {
                            Image(systemName: "cart").font(.caption.weight(.bold))
                        }
                        Text(recentlyAdded.contains(product.id) ? "Added ✓" : "Ghost it").font(.caption.weight(.bold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .foregroundStyle(Color.inkColor).background(Color.ghostGreenColor, in: RoundedRectangle(cornerRadius: 12))

                ShareLink(item: shareText(for: product)) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.subheadline)
                        .foregroundStyle(Color.inkColor)
                        .frame(width: 34, height: 34)
                        .modifier(GlassCircleBackground())
                }
                .simultaneousGesture(TapGesture().onEnded {
                    GhostAnalytics.productShared(product.id)
                })
            }
        }
        .padding(10)
        .modifier(GlassCardBackground(cornerRadius: 18))
        .overlay { RoundedRectangle(cornerRadius: 18).stroke(Color.primary.opacity(0.09)) }
    }

    private func detail(_ product: MarketplaceProduct) -> some View {
        ProductDetailView(
            product: product,
            initiallyFavorite: favoriteIds.contains(product.id),
            onToggleFavorite: { toggleFavorite(product) },
            onAddToCart: {
                onAddToCart(product)
                recentlyAdded.insert(product.id)
            },
            onOpenCart: onOpenCart
        )
    }

    private func toggleFavorite(_ product: MarketplaceProduct) {
        if favoriteIds.contains(product.id) { favoriteIds.remove(product.id) }
        else { favoriteIds.insert(product.id) }
        FavoritesStore.save(favoriteIds)
        onToggleFavorite(product)
    }
}

private struct ProductDetailView: View {
    @EnvironmentObject private var store: GhostCartStore
    let product: MarketplaceProduct
    let onToggleFavorite: () -> Void
    let onAddToCart: () -> Void
    let onOpenCart: () -> Void

    @State private var isFavorite: Bool
    @State private var justAdded = false

    init(
        product: MarketplaceProduct,
        initiallyFavorite: Bool,
        onToggleFavorite: @escaping () -> Void,
        onAddToCart: @escaping () -> Void,
        onOpenCart: @escaping () -> Void
    ) {
        self.product = product
        self.onToggleFavorite = onToggleFavorite
        self.onAddToCart = onAddToCart
        self.onOpenCart = onOpenCart
        _isFavorite = State(initialValue: initiallyFavorite)
    }

    private var isInCart: Bool { store.cartItems.contains { $0.id == product.id } }
    private var shareText: String {
        if let source = product.sourceURL, !source.isEmpty { return "Check this out on Ghost Cart:\n\(source)" }
        return "Check out \(product.name) on Ghost Cart."
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Ghost Cart").font(.headline.weight(.black))
                    Spacer()
                    ShareLink(item: shareText) {
                        Image(systemName: "square.and.arrow.up").detailRoundButton()
                    }
                    .simultaneousGesture(TapGesture().onEnded {
                        GhostAnalytics.productShared(product.id)
                    })
                    Button {
                        isFavorite.toggle(); onToggleFavorite()
                    } label: {
                        Image(systemName: isFavorite ? "heart.fill" : "heart").detailRoundButton()
                    }
                }

                Text(product.name).font(.system(size: 27, weight: .black)).fixedSize(horizontal: false, vertical: true)
                if let description = product.productDescription, !description.isEmpty {
                    Text(description).font(.subheadline).foregroundStyle(Color.secondary)
                }
                if product.priceCents > 0 {
                    DirhamAmount(value: product.priceAmount, font: .title2.weight(.black), iconSize: 20)
                }
                Label("Safe to Ghost", systemImage: "checkmark.shield.fill")
                    .font(.caption.weight(.bold)).foregroundStyle(Color.ghostGreenColor)

                ProductThumbnail(
                    imageURL: product.imageUrl,
                    systemImage: AlmostBuyCategory(serverName: product.category).systemImage,
                    productName: product.name,
                    fillWidthHeight: 230,
                    cornerRadius: 20
                )

                detailRow("Category", value: product.category)
                if let brand = product.brand, !brand.isEmpty { detailRow("Brand", value: brand) }
                if product.isUserGhosted { detailRow("Source", value: "User Ghosted") }

                HStack(spacing: 0) {
                    highlight("bag.fill", title: "Add to cart", caption: "Review it later\nin your cart.")
                    highlight("bell.fill", title: "Get reminded", caption: "Choose a cooldown\nat checkout.")
                    highlight("checkmark.shield.fill", title: "Decide calmly", caption: "Skip, buy, or\ncool longer.")
                }
                .padding(.vertical, 16).background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 14))

                Button(isInCart ? "View Ghost Cart" : (justAdded ? "Added to Ghost Cart ✓" : "Add to cart")) {
                    if isInCart { onOpenCart() }
                    else { onAddToCart(); justAdded = true }
                }
                .buttonStyle(GhostPrimaryButtonStyle())

                if isInCart {
                    Button("Choose cooldown in Cart", action: onOpenCart)
                        .buttonStyle(GhostSecondaryButtonStyle())
                    Text("The item is waiting in your Ghost Cart. Its cooldown starts after Fake Checkout.")
                        .font(.caption).foregroundStyle(Color.secondary)
                } else {
                    Text("Adds to your Ghost Cart. You stay on this page, and the cooldown starts once you check out.")
                        .font(.caption).foregroundStyle(Color.secondary)
                }
                SimulationDisclosure()
            }
            .padding(20).padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func detailRow(_ label: String, value: String) -> some View {
        HStack { Text(label).font(.caption.weight(.bold)).foregroundStyle(Color.secondary); Spacer(); Text(value).font(.caption.weight(.bold)) }
    }

    private func highlight(_ symbol: String, title: String, caption: String) -> some View {
        VStack(spacing: 5) {
            Image(systemName: symbol).foregroundStyle(Color.ghostGreenColor)
            Text(title).font(.system(size: 10, weight: .bold)).multilineTextAlignment(.center)
            Text(caption).font(.system(size: 8)).foregroundStyle(Color.secondary).multilineTextAlignment(.center)
        }.frame(maxWidth: .infinity)
    }
}

private extension Image {
    func detailRoundButton() -> some View {
        self.font(.subheadline.weight(.bold))
            .foregroundStyle(Color.primary)
            .frame(width: 38, height: 38)
            .modifier(GlassCircleBackground())
    }
}
