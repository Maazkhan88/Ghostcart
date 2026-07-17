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
    }
}

#Preview {
    ContentView()
        .environmentObject(GhostCartStore())
}
