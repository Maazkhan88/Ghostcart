import SwiftUI

@main
struct GhostCartApp: App {
    @UIApplicationDelegateAdaptor(GhostCartAppDelegate.self) private var appDelegate
    @StateObject private var store = GhostCartStore()
    @StateObject private var onboarding = OnboardingState()
    @StateObject private var auth = AuthService.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(onboarding)
                .environmentObject(auth)
                .preferredColorScheme(store.preferences.appearance.colorScheme)
                .task { await auth.restoreSession() }
        }
    }
}

private struct RootView: View {
    @EnvironmentObject private var onboarding: OnboardingState

    private var onboardingDone: Bool {
        onboarding.progress.consentAccepted && onboarding.progress.tutorialComplete
    }

    var body: some View {
        if onboardingDone {
            ContentView()
        } else {
            OnboardingFlowView(onComplete: {})
        }
    }
}
