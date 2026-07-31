import Foundation

// Mirrors Android's onboarding gate order (Navigation.kt): consent, then
// profile select + personalization once, then the tutorial auto-launches
// for first-time signed-in users. Persisted locally like the rest of the
// app's non-credential state (GhostCartStore).
enum ProfilePick: String, Codable {
    case male
    case female
}

struct OnboardingProgress: Codable {
    var consentAccepted: Bool = false
    var selectedProfile: ProfilePick?
    var personalizationComplete: Bool = false
    var tutorialComplete: Bool = false
}

@MainActor
final class OnboardingState: ObservableObject {
    @Published var progress: OnboardingProgress {
        didSet { save() }
    }

    private let key = "ghostcart.v2.onboarding-progress"

    init() {
        if let data = UserDefaults.standard.data(forKey: key),
           let decoded = try? JSONDecoder().decode(OnboardingProgress.self, from: data) {
            progress = decoded
        } else {
            progress = OnboardingProgress()
        }
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(progress) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}
