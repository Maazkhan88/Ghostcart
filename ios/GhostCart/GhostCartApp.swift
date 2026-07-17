import SwiftUI

@main
struct GhostCartApp: App {
    @UIApplicationDelegateAdaptor(GhostCartAppDelegate.self) private var appDelegate
    @StateObject private var store = GhostCartStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .preferredColorScheme(store.preferences.appearance.colorScheme)
        }
    }
}
