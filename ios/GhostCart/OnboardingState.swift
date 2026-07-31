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
    var consentVersion: Int?
    var selectedProfile: ProfilePick?
    var personalizationComplete: Bool = false
    var tutorialComplete: Bool = false
}

@MainActor
final class OnboardingState: ObservableObject {
    @Published var progress: OnboardingProgress {
        didSet { save() }
    }
    @Published private(set) var consentStatus: SimulationConsentStatus?
    @Published private(set) var consentLoading = false
    @Published private(set) var consentSubmitting = false
    @Published private(set) var consentError: String?

    private let key = "ghostcart.v2.onboarding-progress"

    init() {
        if let data = UserDefaults.standard.data(forKey: key),
           let decoded = try? JSONDecoder().decode(OnboardingProgress.self, from: data) {
            progress = decoded
        } else {
            progress = OnboardingProgress()
        }
    }

    func refreshConsent() async {
        guard !consentLoading else { return }
        consentLoading = true
        consentError = nil
        defer { consentLoading = false }

        do {
            let status = try await SimulationConsentService.fetchStatus()
            consentStatus = status
            progress.consentAccepted = status.accepted
            progress.consentVersion = status.accepted ? status.version : nil
        } catch {
            consentStatus = nil
            consentError = "We couldn't verify the simulation notice. Check your connection and try again."
        }
    }

    func acceptConsent() async {
        guard let status = consentStatus, !consentSubmitting else { return }
        consentSubmitting = true
        consentError = nil
        defer { consentSubmitting = false }

        do {
            try await SimulationConsentService.submitAcceptance(version: status.version)
            consentStatus = SimulationConsentStatus(
                version: status.version,
                consentText: status.consentText,
                accepted: true
            )
            progress.consentVersion = status.version
            progress.consentAccepted = true
        } catch {
            consentError = "We couldn't save your acceptance. Please try again."
        }
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(progress) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

struct SimulationConsentStatus {
    let version: Int
    let consentText: String
    let accepted: Bool
}

private enum InstallationIdentity {
    private static let key = "ghostcart.installation-id"

    static var current: String {
        if let existing = UserDefaults.standard.string(forKey: key), !existing.isEmpty {
            return existing
        }
        let generated = UUID().uuidString.lowercased()
        UserDefaults.standard.set(generated, forKey: key)
        return generated
    }
}

private enum SimulationConsentService {
    private static var headers: [String: String] {
        ["X-Ghost-Cart-Installation-Id": InstallationIdentity.current]
    }

    static func fetchStatus() async throws -> SimulationConsentStatus {
        let response = try await ApiClient.shared.getJSON(
            path: "/api/simulation-consent",
            additionalHeaders: headers
        )
        guard let version = response["version"] as? Int else {
            throw ApiError(message: "Invalid simulation consent response.")
        }
        return SimulationConsentStatus(
            version: version,
            consentText: (response["consentText"] as? String) ?? "",
            accepted: (response["accepted"] as? Bool) ?? false
        )
    }

    static func submitAcceptance(version: Int) async throws {
        _ = try await ApiClient.shared.postJSON(
            path: "/api/simulation-consent",
            body: [
                "version": version,
                "locale": Locale.current.identifier.replacingOccurrences(of: "_", with: "-")
            ],
            additionalHeaders: headers
        )
    }
}
