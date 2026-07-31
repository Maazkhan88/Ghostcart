import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: GhostCartStore
    @State private var banners: [ContentBlock] = []
    @State private var stories: [ContentBlock] = []
    @State private var products: [MarketplaceProduct] = []
    @State private var favoriteIds: Set<String> = FavoritesStore.load()
    @State private var showLeaderboardComingSoon = false
    let onGhostSomething: () -> Void
    let onViewCooldowns: () -> Void
    let onOpenProfile: () -> Void

    private var nextItems: [AlmostBuy] {
        Array((store.readyItems + store.coolingItems).prefix(3))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack {
                    GhostCartLogoView()
                        .frame(width: 142, height: 42)
                    Spacer()
                    if !store.readyItems.isEmpty {
                        Button(action: onViewCooldowns) {
                            Label("\(store.readyItems.count) ready", systemImage: "bell.badge.fill")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color.inkColor)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 9)
                                .background(Color.ghostGreenColor)
                                .clipShape(Capsule())
                        }
                    }
                    // Matches Android's header bell (ProductDiscovery.kt) -
                    // was missing entirely on iOS. Spec: "Notification
                    // control routes to Profile/settings."
                    Button(action: onOpenProfile) {
                        Image(systemName: "bell")
                            .font(.title3)
                            .foregroundStyle(Color.primary)
                    }
                }

                if !banners.isEmpty {
                    PromoBannerCarousel(banners: banners)
                }

                MarketplaceSection(
                    products: products,
                    favoriteIds: favoriteIds,
                    onToggleFavorite: toggleFavorite,
                    onAddToCart: addToCart
                )

                if !stories.isEmpty {
                    GhostCartStoriesSection(stories: stories)
                }

                CommunityLeaderboardBanner(onTap: { showLeaderboardComingSoon = true })

                VStack(alignment: .leading, spacing: 18) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 9) {
                            // Exact copy from Android strings.xml
                            // (simulation_only/home_hero_title/home_hero_body)
                            // - verified against a live device, not guessed.
                            Label("Simulation only", systemImage: "lock.fill")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(Color.white.opacity(0.72))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Color.white.opacity(0.12))
                                .clipShape(Capsule())
                            Text("Pause the purchase. Keep control.")
                                .font(.system(size: 34, weight: .black, design: .rounded))
                                .foregroundStyle(Color.white)
                                .fixedSize(horizontal: false, vertical: true)
                            Text("Capture what you want, let it cool, then decide intentionally.")
                                .font(.subheadline)
                                .foregroundStyle(Color.white.opacity(0.68))
                        }
                        Spacer(minLength: 8)
                        GhostMascotView(poseName: "wave")
                            .frame(width: 72, height: 72)
                            .accessibilityHidden(true)
                    }

                    Button(action: onGhostSomething) {
                        Label("Ghost something", systemImage: "plus")
                    }
                    .buttonStyle(GhostPrimaryButtonStyle())
                }
                .padding(22)
                .background(Color.inkColor)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

                HonestProgressStrip(snapshot: store.progress)

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(
                        title: store.readyItems.isEmpty ? "Active cooldowns" : "Ready to decide",
                        actionTitle: nextItems.isEmpty ? nil : "View all",
                        action: nextItems.isEmpty ? nil : onViewCooldowns
                    )

                    if nextItems.isEmpty {
                        EmptyStateView(
                            image: "timer",
                            title: "Nothing is waiting",
                            message: "Your next almost-buy can cool here instead of sitting in a real cart."
                        )
                    } else {
                        ForEach(nextItems) { item in
                            HomeCooldownRow(item: item, onOpen: onViewCooldowns)
                        }
                    }
                }

                if !store.recentDecisions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeading(title: "Recent decisions")
                        ForEach(store.recentDecisions.prefix(3)) { item in
                            HStack(spacing: 12) {
                                Image(systemName: item.state == .resolvedSkipped ? "checkmark" : "checkmark.seal")
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(item.state == .resolvedSkipped ? Color.ghostGreenColor : Color.secondary)
                                    .frame(width: 38, height: 38)
                                    .background(Color.primary.opacity(0.05))
                                    .clipShape(Circle())
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.name).font(.subheadline.weight(.bold))
                                    Text(item.state.title)
                                        .font(.caption)
                                        .foregroundStyle(Color.secondary)
                                }
                                Spacer()
                                Text(AmountFormatter.string(item.amount))
                                    .font(.subheadline.weight(.bold))
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }
                }

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .onAppear { Task { await loadContentBlocks() } }
        .refreshable { await loadContentBlocks() }
        .alert("Coming soon", isPresented: $showLeaderboardComingSoon) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("The Community Leaderboard isn't wired up on iOS yet.")
        }
    }

    private func loadContentBlocks() async {
        async let blocksTask = ContentBlocksService.fetch()
        async let productsTask = MarketplaceService.fetchProducts()
        let (blocks, fetchedProducts) = await (blocksTask, productsTask)
        await MainActor.run {
            banners = ContentBlocksService.banners(from: blocks)
            stories = ContentBlocksService.stories(from: blocks)
            products = fetchedProducts
        }
    }

    private func toggleFavorite(_ product: MarketplaceProduct) {
        if favoriteIds.contains(product.id) {
            favoriteIds.remove(product.id)
        } else {
            favoriteIds.insert(product.id)
        }
        FavoritesStore.save(favoriteIds)
    }

    private func addToCart(_ product: MarketplaceProduct) {
        store.stageCapture(
            CaptureSeed(
                name: product.name,
                amount: product.priceCents > 0 ? product.priceAmount : nil,
                category: AlmostBuyCategory(serverName: product.category),
                sourceURL: nil,
                imageURL: product.imageUrl,
                sourceDomain: nil,
                retailer: nil,
                note: nil,
                offerCommunityShare: false
            )
        )
        onGhostSomething()
    }
}

// Auto-advancing promo banner carousel, real remote images via AsyncImage.
// Mirrors Android's PromoBannerCarousel (ProductDiscovery.kt).
private struct PromoBannerCarousel: View {
    let banners: [ContentBlock]
    @State private var page = 0

    var body: some View {
        TabView(selection: $page) {
            ForEach(Array(banners.enumerated()), id: \.element.id) { index, banner in
                AsyncImage(url: banner.imageURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        Rectangle().fill(Color.primary.opacity(0.06))
                    }
                }
                .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .always))
        .aspectRatio(3, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .onReceive(Timer.publish(every: 4, on: .main, in: .common).autoconnect()) { _ in
            guard !banners.isEmpty else { return }
            withAnimation { page = (page + 1) % banners.count }
        }
    }
}

// Admin-curated marketing cards, portrait "story" shaped. Mirrors Android's
// GhostCartStoriesSection - deliberately not real user content (see
// ProductDiscovery.kt's doc comment on that composable).
private struct GhostCartStoriesSection: View {
    let stories: [ContentBlock]
    @State private var openIndex: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "Ghost Cart Stories")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(stories.enumerated()), id: \.element.id) { index, story in
                        Button {
                            openIndex = index
                        } label: {
                            AsyncImage(url: story.imageURL) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Rectangle().fill(Color.primary.opacity(0.06))
                                }
                            }
                            .frame(width: 108, height: 192)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .fullScreenCover(isPresented: Binding(
                get: { openIndex != nil },
                set: { if !$0 { openIndex = nil } }
            )) {
                StoryViewerView(stories: stories, startIndex: openIndex ?? 0)
            }
        }
    }
}

// Full-screen story viewer. Behavior per
// docs/handoffs/2026-07-31-android-asset-icon-and-interaction-manifest.md
// section 8 (canonical source: StoryViewer.kt) - implemented: pure black
// background, aspect-fit (not fill), exactly 7000ms per image, segmented
// progress bar, X close, tap left-third = previous / tap rest = next,
// press-and-hold pauses, swipe down >140pt closes. NOT implemented yet
// (deferred, not faked): swipe-up Like/Share action tray, pinch-to-zoom,
// video playback, analytics events - those need real product decisions
// (what does Share actually link to?) and more testing time than this pass
// has had.
private struct StoryViewerView: View {
    let stories: [ContentBlock]
    let startIndex: Int
    @Environment(\.dismiss) private var dismiss
    @State private var index: Int
    @State private var progress: Double = 0
    @State private var paused = false
    @State private var dragOffset: CGFloat = 0

    init(stories: [ContentBlock], startIndex: Int) {
        self.stories = stories
        self.startIndex = startIndex
        _index = State(initialValue: startIndex)
    }

    private let duration: TimeInterval = 7

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if stories.indices.contains(index) {
                AsyncImage(url: stories[index].imageURL) { phase in
                    if case .success(let image) = phase {
                        image.resizable().scaledToFit()
                    }
                }
                .id(index)
            }

            // Left third = previous, remaining two-thirds = next.
            GeometryReader { geo in
                HStack(spacing: 0) {
                    Color.clear.contentShape(Rectangle())
                        .frame(width: geo.size.width / 3)
                        .onTapGesture { step(by: -1) }
                    Color.clear.contentShape(Rectangle())
                        .onTapGesture { step(by: 1) }
                }
            }
            .simultaneousGesture(
                LongPressGesture(minimumDuration: 0.15)
                    .onChanged { _ in paused = true }
                    .onEnded { _ in paused = false }
                    .sequenced(before: DragGesture(minimumDistance: 0))
            )

            VStack {
                HStack(spacing: 4) {
                    ForEach(stories.indices, id: \.self) { segment in
                        Capsule()
                            .fill(Color.white.opacity(0.3))
                            .overlay(alignment: .leading) {
                                GeometryReader { geo in
                                    Capsule()
                                        .fill(Color.white)
                                        .frame(width: geo.size.width * segmentProgress(segment))
                                }
                            }
                            .frame(height: 3)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)

                HStack {
                    Spacer()
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .padding(10)
                    }
                }
                Spacer()
            }
        }
        .offset(y: max(0, dragOffset))
        .opacity(1 - Double(max(0, dragOffset)) / 400)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.translation.height > 0 { dragOffset = value.translation.height }
                }
                .onEnded { value in
                    if value.translation.height > 140 {
                        dismiss()
                    } else {
                        withAnimation { dragOffset = 0 }
                    }
                }
        )
        .onReceive(Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()) { _ in
            guard !paused, dragOffset == 0 else { return }
            progress += 0.05 / duration
            if progress >= 1 { step(by: 1) }
        }
    }

    private func segmentProgress(_ segment: Int) -> Double {
        if segment < index { return 1 }
        if segment == index { return progress }
        return 0
    }

    private func step(by delta: Int) {
        let next = index + delta
        guard stories.indices.contains(next) else {
            dismiss()
            return
        }
        index = next
        progress = 0
    }
}

// Replaces the old per-product "User Ghosted" shelf - Android moved that to
// a filter chip inside its full marketplace catalog (not yet built on iOS,
// see docs/current-state.md gap list) and put a single Leaderboard teaser
// banner on Home instead (CommunityLeaderboardBanner, GhostCartV2Screens.kt).
private struct CommunityLeaderboardBanner: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Text("🏆").font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Community Leaderboard").font(.subheadline.weight(.bold)).foregroundStyle(Color.inkColor)
                    Text("See who's kept the most money this month")
                        .font(.caption)
                        .foregroundStyle(Color.inkColor.opacity(0.6))
                }
                Spacer()
                Text("View →").font(.caption.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            }
            .padding(14)
            // Verified on a live device: this card stays paper/white even in
            // dark mode, like the logo pill - not a system-adaptive surface.
            .background(Color.paperColor)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct HonestProgressStrip: View {
    let snapshot: ProgressSnapshot

    var body: some View {
        HStack(spacing: 0) {
            ProgressMiniMetric(title: "Almost spent", value: snapshot.almostSpent)
            Divider().frame(height: 44)
            ProgressMiniMetric(title: "Cooling", value: snapshot.cooling)
            Divider().frame(height: 44)
            ProgressMiniMetric(title: "Money kept", value: snapshot.moneyKept, accent: true)
        }
        .padding(.vertical, 14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Progress in UAE dirhams. Almost spent \(AmountFormatter.string(snapshot.almostSpent)), cooling \(AmountFormatter.string(snapshot.cooling)), money kept \(AmountFormatter.string(snapshot.moneyKept))")
    }
}

private struct ProgressMiniMetric: View {
    let title: String
    let value: Double
    var accent = false

    var body: some View {
        VStack(spacing: 4) {
            DirhamAmount(
                value: value,
                font: .subheadline.weight(.black),
                iconSize: 12,
                color: accent ? Color.ghostGreenColor : Color.primary
            )
            Text(title)
                .font(.caption2)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct HomeCooldownRow: View {
    let item: AlmostBuy
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 14) {
                ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 42, cornerRadius: 13)
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.primary)
                    Text(item.isReady() ? "Ready for your decision" : "Decide \(item.decisionAt?.compactDayAndTime ?? "later")")
                        .font(.caption)
                        .foregroundStyle(item.isReady() ? Color.ghostGreenColor : Color.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.secondary)
            }
            .padding(14)
            .background(Color.primary.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
