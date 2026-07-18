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
        TabView(selection: $selectedTab) {
            NavigationStack {
                HomeView(
                    onGhostSomething: { selectedTab = .capture },
                    onViewCooldowns: { selectedTab = .cooldowns }
                )
            }
            .tabItem { Label("Home", systemImage: "house") }
            .tag(AppTab.home)

            NavigationStack {
                CooldownsView()
            }
            .tabItem { Label("Cooldowns", systemImage: "timer") }
            .badge(store.readyItems.count)
            .tag(AppTab.cooldowns)

            NavigationStack {
                CaptureView(onComplete: { selectedTab = .cooldowns })
            }
            .tabItem { Label("Ghost +", systemImage: "plus.circle.fill") }
            .tag(AppTab.capture)

            NavigationStack {
                ProgressView()
            }
            .tabItem { Label("Progress", systemImage: "chart.line.uptrend.xyaxis") }
            .tag(AppTab.progress)

            NavigationStack {
                ProfileView()
            }
            .tabItem { Label("Profile", systemImage: "person.crop.circle") }
            .tag(AppTab.profile)
        }
        .tint(.ghostGreenColor)
        .onReceive(NotificationCenter.default.publisher(for: .ghostCartNotificationDestination)) { notification in
            guard let destination = notification.userInfo?["destination"] as? String else { return }
            selectedTab = destination == "capture" ? .capture : .cooldowns
        }
        .onAppear { handleSharedImport() }
        .onChange(of: scenePhase) { phase in
            if phase == .active { handleSharedImport() }
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

#Preview {
    ContentView()
        .environmentObject(GhostCartStore())
}
