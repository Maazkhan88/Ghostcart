import SwiftUI

// Gate order mirrors Android's Navigation.kt: consent blocks everything;
// then Landing -> Auth -> ProfileSelect -> Personalization -> Tutorial ->
// main app. "Continue as Guest" (from Landing or Auth) skips straight past
// Auth into ProfileSelect, same as Android's onGuest.
private enum OnboardingStep {
    case landing
    case auth
    case profileSelect
    case personalization
    case tutorial
}

struct OnboardingFlowView: View {
    @EnvironmentObject private var onboarding: OnboardingState
    @EnvironmentObject private var auth: AuthService
    let onComplete: () -> Void

    @State private var step: OnboardingStep = .landing
    @State private var selectedCategories: Set<AlmostBuyCategory> = []
    @State private var savingsGoal: Int?

    var body: some View {
        Group {
            if let consent = onboarding.consentStatus, !consent.accepted {
                SimulationConsentView(
                    consentText: consent.consentText,
                    isSubmitting: onboarding.consentSubmitting,
                    errorMessage: onboarding.consentError,
                    onAccept: { Task { await onboarding.acceptConsent() } }
                )
            } else {
                switch step {
                case .landing:
                    OnboardingLandingView(
                        onStart: { step = .auth },
                        onGuest: { auth.continueAsGuest(); step = .profileSelect }
                    )
                case .auth:
                    AuthView(
                        onAuthSuccess: { step = .profileSelect },
                        onGuest: { auth.continueAsGuest(); step = .profileSelect }
                    )
                case .profileSelect:
                    ProfileSelectView(
                        selection: $onboarding.progress.selectedProfile,
                        onContinue: { step = .personalization },
                        onSkip: { step = .personalization }
                    )
                case .personalization:
                    PersonalizationView(
                        selectedCategories: $selectedCategories,
                        savingsGoal: $savingsGoal,
                        onContinue: {
                            onboarding.progress.personalizationComplete = true
                            step = .tutorial
                        }
                    )
                case .tutorial:
                    TutorialView(onFinish: {
                        onboarding.progress.tutorialComplete = true
                        onComplete()
                    })
                }
            }
        }
        .animation(.default, value: step)
        .animation(.default, value: onboarding.progress.consentAccepted)
        .onAppear {
            if onboarding.progress.personalizationComplete && !onboarding.progress.tutorialComplete {
                step = .tutorial
            } else if onboarding.progress.selectedProfile != nil && !onboarding.progress.personalizationComplete {
                step = .personalization
            }
        }
        // Every onboarding screen forces a bright, always-white background
        // (Color.paperColor) by design - pin the color scheme to light here
        // too, so semantic colors (Color.primary/.secondary) resolve to
        // their light-mode values instead of flipping to unreadable white
        // text on a white background under system dark mode.
        .preferredColorScheme(.light)
        .environment(\.colorScheme, .light)
    }
}
