import SwiftUI

enum AppTab: Hashable {
    case home
    case cooldowns
    case capture
    case progress
    case profile
}

struct ContentView: View {
    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab: AppTab = .home

    var body: some View {
        screen
            .safeAreaInset(edge: .bottom, spacing: 0) {
                GhostBottomNav(selectedTab: $selectedTab)
            }
        .onReceive(NotificationCenter.default.publisher(for: .ghostCartNotificationDestination)) { notification in
            guard let destination = notification.userInfo?["destination"] as? String else { return }
            selectedTab = destination == "capture" ? .capture : .cooldowns
        }
        .onAppear { handleSharedImport() }
        .onChange(of: scenePhase) { phase in
            if phase == .active { handleSharedImport() }
        }
    }

    @ViewBuilder
    private var screen: some View {
        switch selectedTab {
        case .home:
            NavigationStack {
                HomeView(
                    onGhostSomething: { selectedTab = .capture },
                    onViewCooldowns: { selectedTab = .cooldowns },
                    onOpenProfile: { selectedTab = .profile }
                )
            }
        case .cooldowns:
            NavigationStack { CooldownsView() }
        case .capture:
            NavigationStack { CaptureView(onComplete: { selectedTab = .cooldowns }) }
        case .progress:
            NavigationStack { ProgressView() }
        case .profile:
            NavigationStack { ProfileView() }
        }
    }

    // A link shared into the GhostCartShare extension is written to the App
    // Group container. When the app becomes active we consume it once, preview
    // it, and stage a pre-filled capture on the Ghost + tab.
    private func handleSharedImport() {
        guard let pending = SharedImportBridge.takePending() else { return }
        Task {
            let result = await ProductImportService.previewLink(
                sourceURL: pending.sourceURL,
                sharedTitle: pending.sharedTitle,
                sharedImageURL: pending.sharedImageURL
            )
            let seed = Self.seed(from: pending, result: result)
            await MainActor.run {
                store.stageCapture(seed)
                selectedTab = .capture
            }
        }
    }

    private static func seed(from pending: PendingSharedImport, result: Result<LinkImportResult, ApiError>) -> CaptureSeed {
        let sharedName = pending.sharedTitle.flatMap { titleLooksLikeFallback($0) ? nil : $0 } ?? ""
        if case .success(.product(let product)) = result {
            return CaptureSeed(
                name: titleLooksLikeFallback(product.title) ? sharedName : product.title,
                amount: product.amount,
                category: AlmostBuyCategory(serverName: product.category),
                sourceURL: product.sourceURL.isEmpty ? pending.sourceURL : product.sourceURL,
                imageURL: product.imageURL ?? pending.sharedImageURL,
                sourceDomain: product.sourceDomain,
                retailer: product.retailer,
                note: product.note,
                offerCommunityShare: true
            )
        }
        // Listing pages and failed previews still open the capture form with the
        // link filled in, so the user can pick or enter details manually.
        return CaptureSeed(
            name: sharedName,
            amount: nil,
            category: .other,
            sourceURL: pending.sourceURL,
            imageURL: pending.sharedImageURL,
            sourceDomain: nil,
            retailer: nil,
            note: nil,
            offerCommunityShare: true
        )
    }
}

// Custom bottom nav, not SwiftUI's native TabView chrome. Two separate bugs
// (an oversized mascot image, then solid-colored-square icons) appeared when
// custom template images were used inside .tabItem - that API path seems to
// mishandle custom images on this SwiftUI/OS version, while the exact same
// images render correctly as normal views everywhere else in this app
// (headers, product thumbnails). Building the bar directly sidesteps that
// and matches Android's actual GhostBottomNav (Navigation.kt:997-1065),
// including the raised circular Cart button, more faithfully than tabItem
// ever could anyway.
private struct GhostBottomNav: View {
    @Binding var selectedTab: AppTab

    var body: some View {
        HStack(spacing: 0) {
            navItem(.home, icon: "HomeIcon", label: "Home")
            // No badge here - mirror plan slice 4 fix. This tab previously
            // carried a "ready to decide" count badge, but Android's badge
            // on this row is cart-item count, on the CENTER tab, not this
            // one (GhostBottomNav, Navigation.kt:1039-1055; confirmed via
            // deep audit docs/handoffs/2026-07-31-android-ios-deep-audit-
            // and-plan.md §F). iOS's label mismatch the same audit also
            // flagged ("Orders" vs expected "Cooldowns") was independently
            // re-verified against the live strings.xml value and a real
            // device screenshot - Android's actual displayed string is
            // "Orders" (R.string.nav_cooldowns = "Orders", not "Cooldowns"
            // despite the key name), so no label change was made here.
            navItem(.cooldowns, icon: "OrdersIcon", label: "Orders")
            cartItem
            navItem(.progress, icon: "WalletIcon", label: "Wallet")
            navItem(.profile, icon: "ProfileIcon", label: "Profile")
        }
        .padding(.horizontal, 8)
        .padding(.top, 10)
        .padding(.bottom, 26)
        // Liquid Glass floating capsule is intentional per the user - only
        // the icons/labels/spacing should match Android, not the bar
        // material/shape.
        .background(.regularMaterial, in: Capsule())
        .padding(.horizontal, 14)
    }

    private func navItem(_ tab: AppTab, icon: String, label: String, badge: Int = 0) -> some View {
        let selected = selectedTab == tab
        return Button(action: { selectedTab = tab }) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    Image(icon)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 25, height: 25)
                    if badge > 0 {
                        Text("\(badge)")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(3)
                            .background(Color.red)
                            .clipShape(Circle())
                            .offset(x: 8, y: -6)
                    }
                }
                Text(label).font(.system(size: 10, weight: .semibold))
            }
            .foregroundStyle(selected ? Color.ghostGreenColor : Color.secondary)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    // Android's central Cart tab: raised 48dp circular Ghost-green button
    // with the cart mascot, no text label (GhostBottomNav,
    // Navigation.kt:1014-1038). Real Android badge here is cart-item
    // count, capped "9+" - not added yet because there is no real cart
    // model on iOS to count (this tab still opens the capture form; a real
    // cart is mirror-plan slice 11). Deliberately not faking a number here
    // - no badge is more honest than a wrong one.
    private var cartItem: some View {
        Button(action: { selectedTab = .capture }) {
            Image("MascotCart")
                .resizable()
                .scaledToFit()
                .frame(width: 32, height: 32)
                .frame(width: 48, height: 48)
                .background(Color.ghostGreenColor)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
                .offset(y: -10)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityLabel("Cart")
    }
}

#Preview {
    ContentView()
        .environmentObject(GhostCartStore())
}
