import AVKit
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: GhostCartStore
    @EnvironmentObject private var tutorialCoordinator: TutorialCoordinator
    @EnvironmentObject private var auth: AuthService
    @State private var banners: [ContentBlock] = []
    @State private var stories: [ContentBlock] = []
    @State private var products: [MarketplaceProduct] = []
    @State private var favoriteIds: Set<String> = FavoritesStore.load()
    @State private var showLeaderboard = false
    @State private var showNotifications = false
    @State private var hasUnreadNotification = false
    @State private var trackingItemID: UUID?
    let onGhostSomething: () -> Void
    let onOpenCart: () -> Void
    let onViewCooldowns: () -> Void

    private var nextItems: [AlmostBuy] {
        Array((store.readyItems + store.coolingItems).prefix(3))
    }

    // The single most-imminent active Ghost Delivery, if any - surfaced as a
    // hero card per the product spec's Home section order (right after the
    // promo banner, before Marketplace). store.coolingItems is already
    // sorted by decisionAt ascending.
    private var activeGhostDelivery: AlmostBuy? {
        store.coolingItems.first
    }

    // Injected into the real marketplace list only while the tutorial is at
    // PRODUCT - the same real card, real "Ghost it" button, real
    // ProductThumbnail rendering every other card uses, just spotlighted.
    // Never written into GhostCartStore.items/products; purely a display-time
    // addition to this one @State array.
    private var isTutorialProductStep: Bool {
        tutorialCoordinator.isActive && tutorialCoordinator.state.currentStep == .product
    }
    private var displayProducts: [MarketplaceProduct] {
        guard isTutorialProductStep else { return products }
        let tutorialProduct = MarketplaceProduct(
            id: tutorialProductID,
            name: "Ghost Cart Coffee & Donut Combo",
            category: "Food & delivery",
            priceCents: 1500,
            imageUrl: nil,
            isUserGhosted: false
        )
        return [tutorialProduct] + products
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                ZStack {
                    GhostCartLogoView()
                        .frame(width: 142, height: 42)
                    HStack {
                        Spacer()
                        // One bell, two signals: a green "N ready" pill when
                        // items are waiting for a decision (still opens
                        // Cooldowns directly - that's a more useful action
                        // than the notifications list for something this
                        // time-sensitive), otherwise a plain bell that opens
                        // the notifications feed and turns Ghost Green when
                        // there's something unread there.
                        if !store.readyItems.isEmpty {
                            Button(action: onViewCooldowns) {
                                Label("\(store.readyItems.count) ready", systemImage: "bell.badge.fill")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(Color.inkColor)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 8)
                                    .background(Color.ghostGreenColor)
                                    .clipShape(Capsule())
                            }
                        } else {
                            Button(action: { showNotifications = true }) {
                                Image(systemName: "bell")
                                    .font(.title3)
                                    .foregroundStyle(hasUnreadNotification ? Color.ghostGreenColor : Color.primary)
                                    .frame(width: 36, height: 36)
                                    .contentShape(Circle())
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 42)

                if !banners.isEmpty {
                    PromoBannerCarousel(banners: banners)
                }

                if let activeGhostDelivery {
                    ActiveGhostDeliveryCard(item: activeGhostDelivery) {
                        trackingItemID = activeGhostDelivery.id
                    }
                }

                MarketplaceSection(
                    products: displayProducts,
                    favoriteIds: favoriteIds,
                    onToggleFavorite: toggleFavorite,
                    onAddToCart: addToCart,
                    onOpenCart: onOpenCart,
                    tutorialProductIDs: isTutorialProductStep ? [tutorialProductID] : [],
                    onGhostItDirect: { _ in
                        tutorialCoordinator.addPracticeItemToCart()
                        tutorialCoordinator.openTutorialCart()
                    }
                )

                if !stories.isEmpty {
                    GhostCartStoriesSection(stories: stories)
                }

                CommunityLeaderboardBanner(onTap: { showLeaderboard = true })

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
        .onAppear {
            Task { await loadContentBlocks() }
            // Mirrors Android's Home composable: ask every time Home is
            // reached while permission isn't granted yet (covers guest and
            // signed-up onboarding paths, and anyone who dismissed/denied
            // before). requestAuthorizationIfNeeded() no-ops once the OS
            // has an answer, so this never re-nags someone who already
            // said yes or no - don't gate this behind a persisted
            // "already asked" flag, which can get stuck true.
            Task { await NotificationService.shared.requestAuthorizationIfNeeded() }
            Task { await refreshUnreadNotificationState() }
        }
        .refreshable { await loadContentBlocks() }
        .sheet(isPresented: $showLeaderboard) {
            NavigationStack {
                LeaderboardView()
            }
        }
        .sheet(isPresented: $showNotifications) {
            NavigationStack {
                NotificationsView(onMarkedSeen: { hasUnreadNotification = false })
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { trackingItemID != nil },
            set: { if !$0 { trackingItemID = nil } }
        )) {
            if let trackingItemID {
                NavigationStack {
                    GhostDeliveryTrackerView(itemID: trackingItemID, onClose: { self.trackingItemID = nil })
                }
            }
        }
    }

    // Fetches the full feed just to compare its newest row against the
    // local "last seen" cursor - matches the backend contract (there is no
    // separate unread-count endpoint, deliberately, per the notifications
    // handoff) and Android's own AppViewModel.refreshNotifications.
    private func refreshUnreadNotificationState() async {
        guard auth.isSignedIn else { hasUnreadNotification = false; return }
        guard let items = await NotificationsService.fetch() else { return }
        hasUnreadNotification = NotificationsSeenCursor.hasUnread(newest: items.first?.createdAt)
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
        let nowFavorited: Bool
        if favoriteIds.contains(product.id) {
            favoriteIds.remove(product.id)
            nowFavorited = false
        } else {
            favoriteIds.insert(product.id)
            nowFavorited = true
        }
        FavoritesStore.save(favoriteIds)
        GhostAnalytics.productFavorited(product.id, favorited: nowFavorited)
    }

    private func addToCart(_ product: MarketplaceProduct) {
        store.addToCart(product)
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

// Full-screen story viewer mirroring Android StoryViewer.kt: black stage,
// aspect-fit media, seven-second image progress, real-duration video,
// tap navigation, hold-to-pause, swipe down to close, swipe up for actions,
// pinch zoom that snaps back, session-only Like, and native Share.
private struct StoryViewerView: View {
    let stories: [ContentBlock]
    let startIndex: Int
    @Environment(\.dismiss) private var dismiss
    @State private var index: Int
    @State private var progress: Double = 0
    @State private var paused = false
    @State private var dragOffset: CGFloat = 0
    @State private var scale: CGFloat = 1
    @State private var zoomOffset: CGSize = .zero
    @State private var showActions = false
    @State private var likedStoryIDs: Set<Int> = []
    @State private var player: AVPlayer?

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
                Group {
                    if stories[index].mediaType == "video", let player {
                        VideoPlayer(player: player)
                            .disabled(true)
                            .scaledToFit()
                    } else {
                        AsyncImage(url: stories[index].imageURL) { phase in
                            if case .success(let image) = phase {
                                image.resizable().scaledToFit()
                            }
                        }
                        .id(index)
                    }
                }
                .scaleEffect(scale)
                .offset(zoomOffset)
                .animation(.easeOut(duration: 0.22), value: scale)
                .animation(.easeOut(duration: 0.22), value: zoomOffset)
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
                            .modifier(GlassCircleBackground())
                    }
                }
                Spacer()
            }

            if showActions, stories.indices.contains(index) {
                VStack {
                    Spacer()
                    HStack(spacing: 34) {
                        Button {
                            let id = stories[index].id
                            if likedStoryIDs.contains(id) {
                                likedStoryIDs.remove(id)
                            } else {
                                likedStoryIDs.insert(id)
                            }
                        } label: {
                            storyAction(
                                symbol: likedStoryIDs.contains(stories[index].id) ? "heart.fill" : "heart",
                                label: "Like",
                                tint: likedStoryIDs.contains(stories[index].id)
                                    ? Color(red: 1, green: 0.3, blue: 0.4)
                                    : .white
                            )
                        }
                        .buttonStyle(.plain)

                        if let url = stories[index].imageURL {
                            ShareLink(
                                item: "Check this out on Ghost Cart:\n\(url.absoluteString)",
                                subject: Text("Ghost Cart Story")
                            ) {
                                storyAction(symbol: "square.and.arrow.up", label: "Share", tint: .white)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    Button("Tap to close") {
                        withAnimation { showActions = false }
                        paused = false
                    }
                    .font(.caption)
                    .foregroundStyle(Color.white.opacity(0.55))
                    .padding(.top, 14)
                }
                .frame(maxWidth: .infinity)
                .padding(20)
                .background(Color.black.opacity(0.86))
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .offset(y: max(0, dragOffset))
        .opacity(1 - Double(max(0, dragOffset)) / 400)
        .gesture(
            DragGesture()
                .onChanged { value in
                    guard scale == 1 else {
                        zoomOffset = value.translation
                        return
                    }
                    if value.translation.height > 0 { dragOffset = value.translation.height }
                }
                .onEnded { value in
                    if scale > 1 {
                        withAnimation {
                            scale = 1
                            zoomOffset = .zero
                        }
                    } else if value.translation.height > 140 {
                        dismiss()
                    } else if value.translation.height < -140 {
                        withAnimation { showActions = true }
                        paused = true
                    } else {
                        withAnimation { dragOffset = 0 }
                    }
                }
        )
        .simultaneousGesture(
            MagnificationGesture()
                .onChanged { value in
                    paused = true
                    scale = min(max(value, 1), 4)
                }
                .onEnded { _ in
                    withAnimation {
                        scale = 1
                        zoomOffset = .zero
                    }
                    paused = showActions
                }
        )
        .onReceive(Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()) { _ in
            guard !paused, !showActions, dragOffset == 0 else { return }
            if stories.indices.contains(index), stories[index].mediaType == "video", let player {
                let seconds = player.currentTime().seconds
                let total = player.currentItem?.duration.seconds ?? 0
                if total.isFinite, total > 0 {
                    progress = min(max(seconds / total, 0), 1)
                }
                if total.isFinite, total > 0, seconds >= total - 0.05 {
                    step(by: 1)
                }
            } else {
                progress += 0.05 / duration
                if progress >= 1 { step(by: 1) }
            }
        }
        .onAppear { configureMedia() }
        .onDisappear { player?.pause() }
        .onChange(of: paused) { isPaused in
            if isPaused || showActions {
                player?.pause()
            } else {
                player?.play()
            }
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
        showActions = false
        scale = 1
        zoomOffset = .zero
        configureMedia()
    }

    @ViewBuilder
    private func storyAction(symbol: String, label: String, tint: Color) -> some View {
        VStack(spacing: 5) {
            Image(systemName: symbol)
                .font(.title2.weight(.semibold))
                .foregroundStyle(tint)
                .frame(width: 52, height: 52)
                .background(Color.white.opacity(0.12))
                .clipShape(Circle())
            Text(label).font(.caption2.weight(.bold)).foregroundStyle(.white)
        }
    }

    private func configureMedia() {
        player?.pause()
        player = nil
        guard stories.indices.contains(index),
              stories[index].mediaType == "video",
              let url = stories[index].imageURL else { return }
        let nextPlayer = AVPlayer(url: url)
        player = nextPlayer
        if !paused && !showActions { nextPlayer.play() }
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

private struct LeaderboardEntry: Identifiable {
    var id: String { username }
    let username: String
    let avatarURL: URL?
    let avatarPresetID: String?
    let moneyKeptCents: Int
    let ghostedCount: Int
    let coolingCount: Int
}

private enum LeaderboardService {
    static func fetch() async throws -> [LeaderboardEntry] {
        let response = try await ApiClient.shared.getJSON(path: "/api/community/leaderboard")
        guard let rows = response["leaderboard"] as? [[String: Any]] else { return [] }
        return rows.compactMap { row in
            guard let username = row["username"] as? String, !username.isEmpty else { return nil }
            let rawAvatar = row["avatarUrl"] as? String
            let avatarURL = rawAvatar.flatMap {
                URL(string: $0.hasPrefix("http") ? $0 : "\(ApiConfig.baseURL)\($0)")
            }
            return LeaderboardEntry(
                username: username,
                avatarURL: avatarURL,
                avatarPresetID: row["avatarPresetId"] as? String,
                moneyKeptCents: (row["moneyKeptCents"] as? NSNumber)?.intValue ?? 0,
                ghostedCount: (row["ghostedCount"] as? NSNumber)?.intValue ?? 0,
                coolingCount: (row["coolingCount"] as? NSNumber)?.intValue ?? 0
            )
        }
    }
}

struct LeaderboardView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var entries: [LeaderboardEntry] = []
    @State private var loading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                Text("Ranked by items ghosted. Money cooled and kept is shown too, but doesn't affect rank. Only members who opt in from Profile appear here.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                    .padding(.bottom, 4)

                if loading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 80)
                } else if let errorMessage {
                    EmptyStateView(image: "wifi.exclamationmark", title: "Leaderboard unavailable", message: errorMessage)
                    Button("Try again") { Task { await load() } }
                        .buttonStyle(GhostPrimaryButtonStyle())
                } else if entries.isEmpty {
                    EmptyStateView(
                        image: "trophy",
                        title: "No one's on the board yet",
                        message: "Opt in from your Profile to be the first."
                    )
                } else {
                    ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                        NavigationLink {
                            LeaderboardDetailView(entry: entry, rank: index + 1)
                        } label: {
                            LeaderboardRow(entry: entry, rank: index + 1)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Community Leaderboard")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Close") { dismiss() }
            }
        }
        .task { await load() }
        .refreshable { await load() }
        .onAppear { GhostAnalytics.leaderboardOpened() }
    }

    private func load() async {
        loading = true
        errorMessage = nil
        do {
            entries = try await LeaderboardService.fetch()
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }
}

private struct LeaderboardRow: View {
    let entry: LeaderboardEntry
    let rank: Int

    var body: some View {
        HStack(spacing: 12) {
            Text("#\(rank)")
                .font(.subheadline.weight(.black))
                .foregroundStyle(rank == 1 ? Color.ghostGreenColor : Color.secondary)
                .frame(width: 32)
            LeaderboardAvatar(entry: entry, size: 44)
            VStack(alignment: .leading, spacing: 5) {
                Text("@\(entry.username)").font(.subheadline.weight(.bold)).foregroundStyle(Color.primary)
                HStack(spacing: 10) {
                    Label("\(entry.ghostedCount) ghosted", systemImage: "checkmark.circle")
                    Label("\(entry.coolingCount) cooling", systemImage: "snowflake")
                }
                .font(.caption2)
                .foregroundStyle(Color.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(AmountFormatter.string(Double(entry.moneyKeptCents) / 100))
                    .font(.caption.weight(.black))
                    .foregroundStyle(Color.ghostGreenColor)
                Text("kept").font(.caption2).foregroundStyle(Color.secondary)
            }
            Image(systemName: "chevron.right").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
        }
        .padding(14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(rank == 1 ? Color.ghostGreenColor : Color.primary.opacity(0.08), lineWidth: rank == 1 ? 2 : 1)
        }
    }
}

private struct LeaderboardAvatar: View {
    let entry: LeaderboardEntry
    let size: CGFloat

    var body: some View {
        Group {
            if let preset = AvatarPreset.byId(entry.avatarPresetID) {
                Image(preset.imageName).resizable().scaledToFit().padding(4)
            } else if let url = entry.avatarURL {
                AsyncImage(url: url) { phase in
                    if case .success(let image) = phase { image.resizable().scaledToFill() }
                    else { fallback }
                }
            } else {
                fallback
            }
        }
        .frame(width: size, height: size)
        .background(Color.primary.opacity(0.06))
        .clipShape(Circle())
    }

    private var fallback: some View {
        Text(entry.username.prefix(1).uppercased())
            .font(.headline.weight(.black))
            .foregroundStyle(Color.primary)
    }
}

private struct LeaderboardDetailView: View {
    let entry: LeaderboardEntry
    let rank: Int

    var body: some View {
        ScrollView {
            VStack(spacing: 22) {
                LeaderboardAvatar(entry: entry, size: 92)
                VStack(spacing: 4) {
                    Text("@\(entry.username)").font(.title2.weight(.black))
                    Text("Community rank #\(rank)").font(.caption).foregroundStyle(Color.secondary)
                }
                HStack(spacing: 10) {
                    stat("Ghosted", "\(entry.ghostedCount)")
                    stat("Cooling", "\(entry.coolingCount)")
                    stat("Money kept", AmountFormatter.string(Double(entry.moneyKeptCents) / 100), accent: true)
                }
                Text("Only activity this member chose to share is represented on the community leaderboard. Email addresses and real names are never shown here.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(24)
        }
        .navigationTitle("Leaderboard details")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func stat(_ title: String, _ value: String, accent: Bool = false) -> some View {
        VStack(spacing: 6) {
            Text(title).font(.caption2).foregroundStyle(Color.secondary)
            Text(value).font(.subheadline.weight(.black)).foregroundStyle(accent ? Color.ghostGreenColor : Color.primary)
                .minimumScaleFactor(0.65).lineLimit(1)
        }
        .frame(maxWidth: .infinity, minHeight: 72)
        .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 14))
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

// The Home hero card for the single most-imminent active Ghost Delivery.
// Mirrors the product spec's example: "YOUR GHOST DELIVERY / [title] / Out
// for Ghost Delivery · 6h 20m / Track".
private struct ActiveGhostDeliveryCard: View {
    let item: AlmostBuy
    let onTrack: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { context in
            let stage = item.deliveryStage(at: context.date) ?? .placed
            HStack(spacing: 14) {
                ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 50, cornerRadius: 14)
                VStack(alignment: .leading, spacing: 4) {
                    Text("YOUR GHOST DELIVERY")
                        .font(.system(size: 10, weight: .black))
                        .foregroundStyle(Color.ghostGreenColor)
                    Text(item.name).font(.subheadline.weight(.bold)).lineLimit(1)
                    Text("\(stage.title) · \(remainingLabel(item, at: context.date))")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                }
                Spacer()
                Button("Track", action: onTrack)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.inkColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.ghostGreenColor, in: Capsule())
            }
            .padding(16)
            .background(Color.primary.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .accessibilityElement(children: .combine)
        }
    }

    private func remainingLabel(_ item: AlmostBuy, at date: Date) -> String {
        guard let decisionAt = item.decisionAt else { return "" }
        let seconds = max(0, Int(decisionAt.timeIntervalSince(date)))
        if seconds < 60 { return "under a minute" }
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        if hours > 0 { return "\(hours)h \(minutes)m" }
        return "\(minutes)m"
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
