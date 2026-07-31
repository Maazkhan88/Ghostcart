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
    @State private var launchStoryFinished = false

    private var onboardingDone: Bool {
        onboarding.consentStatus?.accepted == true && onboarding.progress.tutorialComplete
    }

    var body: some View {
        Group {
            if onboarding.consentStatus == nil,
               onboarding.consentLoading || onboarding.consentError == nil {
                BrandedLaunchView()
            } else if onboarding.consentStatus == nil {
                ConsentLoadFailureView(
                    message: onboarding.consentError,
                    onRetry: { Task { await onboarding.refreshConsent() } }
                )
            } else if onboarding.consentStatus?.accepted == true && !launchStoryFinished {
                StorySplashView { launchStoryFinished = true }
            } else if onboardingDone {
                ContentView()
            } else {
                OnboardingFlowView(onComplete: {})
            }
        }
        .task {
            if onboarding.consentStatus == nil {
                await onboarding.refreshConsent()
            }
        }
    }
}

private struct BrandedLaunchView: View {
    var body: some View {
        ZStack {
            Color.paperColor.ignoresSafeArea()
            VStack(spacing: 12) {
                GhostCartLogoView().frame(width: 220, height: 72)
                Text("For everything you almost bought.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
            }
        }
        .preferredColorScheme(.light)
    }
}

private struct ConsentLoadFailureView: View {
    let message: String?
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            GhostMascotView(poseName: "wave")
                .frame(width: 120, height: 120)
            Text("Connection needed")
                .font(.title.bold())
                .foregroundStyle(Color.inkColor)
            Text(message ?? "We couldn't verify the simulation notice.")
                .font(.subheadline)
                .foregroundStyle(Color.inkColor.opacity(0.66))
                .multilineTextAlignment(.center)
            Spacer()
            Button("Try again", action: onRetry)
                .buttonStyle(GhostPrimaryButtonStyle())
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
        .preferredColorScheme(.light)
    }
}

// Android's RandomStorySplashScreen: one random image story, full-bleed,
// branded loading/error fallback, Skip after 3s, advance after 5s. Video
// stories remain available in the Home viewer but are excluded at launch.
private struct StorySplashView: View {
    let onFinished: () -> Void
    @State private var story: ContentBlock?
    @State private var showSkip = false
    @State private var resolved = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            BrandedLaunchView()
            if let story {
                AsyncImage(url: story.imageURL) { phase in
                    if case .success(let image) = phase {
                        image.resizable().scaledToFill()
                    }
                }
                .ignoresSafeArea()
            }
            if showSkip {
                Button("Skip", action: finish)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 11)
                    .background(Color.black.opacity(0.46), in: Capsule())
                    .padding(24)
                    .transition(.opacity)
            }
        }
        .task {
            let blocks = await ContentBlocksService.fetch()
            story = ContentBlocksService.stories(from: blocks)
                .filter { $0.mediaType != "video" }
                .randomElement()
            if story == nil {
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                finish()
                return
            }
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            withAnimation { showSkip = true }
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            finish()
        }
    }

    private func finish() {
        guard !resolved else { return }
        resolved = true
        onFinished()
    }
}
