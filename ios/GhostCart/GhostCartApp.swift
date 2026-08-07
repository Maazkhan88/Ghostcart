import SwiftUI

@main
struct GhostCartApp: App {
    @UIApplicationDelegateAdaptor(GhostCartAppDelegate.self) private var appDelegate
    @StateObject private var store = GhostCartStore()
    @StateObject private var onboarding = OnboardingState()
    @StateObject private var auth = AuthService.shared
    @StateObject private var deepLinks = DeepLinkRouter()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(onboarding)
                .environmentObject(auth)
                .environmentObject(deepLinks)
                .preferredColorScheme(store.preferences.appearance.colorScheme)
                // Nothing in this app was designed against unbounded Dynamic
                // Type - fixed-height text frames (e.g. product card titles)
                // silently misbehave past this cap on real devices with a
                // large system text size. Android has no equivalent unbounded
                // scale either, so this doesn't cost parity.
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                .task { await auth.restoreSession() }
                // A theghostcart.com/gift/{token} Universal Link. Mirrors
                // Android's MainActivity.captureGhostGiftLink - reveal works
                // signed-out, so this is handled independent of onboarding
                // state, same as Android bypassing its normal nav graph entry.
                .onOpenURL { deepLinks.handle($0) }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL { deepLinks.handle(url) }
                }
                .fullScreenCover(isPresented: Binding(
                    get: { deepLinks.pendingGiftToken != nil },
                    set: { if !$0 { deepLinks.pendingGiftToken = nil } }
                )) {
                    if let token = deepLinks.pendingGiftToken {
                        GhostGiftRevealView(token: token, onDismiss: { deepLinks.pendingGiftToken = nil })
                            .environmentObject(store)
                    }
                }
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

// Matches the approved brand handoff (docs/claude-ios-handoff-brand-icon.md):
// solid black background, the white horizontal GhostCart lockup, and the
// supporting line at ~68% white with no trailing period. Fixed to this
// exact look regardless of system light/dark mode, same as Android's launch
// screen.
private struct BrandedLaunchView: View {
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 12) {
                GhostCartLogoView(tint: .white).frame(width: 220, height: 72)
                Text("For everything you almost bought")
                    .font(.caption)
                    .foregroundStyle(Color.white.opacity(0.68))
            }
        }
        .preferredColorScheme(.dark)
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
        .environment(\.colorScheme, .light)
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
                        image.resizable().scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
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
