import Foundation

// Durable tutorial state machine - a deliberate 1:1 port of Android's
// TutorialRepository (data/TutorialState.kt), same step names, same
// transition rules, same status values, same session-vs-durable key split.
// Ported this way on purpose so an Android engineer (or a future agent)
// reading this file recognizes it immediately and the two platforms never
// drift into two different tutorial vocabularies. It intentionally owns no
// production Product, cart, AlmostBuy, order, receipt, wallet, or
// notification-scheduling state - see TutorialCoordinator for how the real
// screens read this state without it reading them back.
let tutorialVersion = 1
let tutorialConsentVersion = 1
let tutorialProductID = "tutorial_coffee_donut_v1"
let tutorialCooldownSeconds: TimeInterval = 10

enum TutorialStatus: String, Codable {
    case notStarted = "NOT_STARTED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case skipped = "SKIPPED"
}

enum TutorialStep: String, Codable, CaseIterable {
    case welcome = "WELCOME"
    case practiceIntro = "PRACTICE_INTRO"
    case product = "PRODUCT"
    case cart = "CART"
    case cooldown = "COOLDOWN"
    case fakeCheckout = "FAKE_CHECKOUT"
    case cooling = "COOLING"
    case decision = "DECISION"
    case ghostReceipt = "GHOST_RECEIPT"
    case complete = "COMPLETE"
    case delivery = "DELIVERY"

    var ordinal: Int { Self.allCases.firstIndex(of: self) ?? 0 }
}

enum TutorialDecision: String, Codable {
    case ghosted = "GHOSTED"
    case coolLonger = "COOL_LONGER"
    case stillBuy = "STILL_BUY"
}

struct TutorialState: Codable, Equatable {
    var tutorialVersion: Int = GhostCart.tutorialVersion
    var consentVersion: Int = 0
    var status: TutorialStatus = .notStarted
    var currentStep: TutorialStep = .welcome
    var startedAt: Date?
    var completedAt: Date?
    var sessionId: String?
    var practiceItemInCart: Bool = false
    var selectedCooldownSeconds: TimeInterval?
    var coolingEndsAt: Date?
    var decision: TutorialDecision?
    var isReplay: Bool = false
}

// Matches Android's ALLOWED_TRANSITIONS exactly - the state machine refuses
// any advance() call that isn't in this map, same fail-closed behavior.
private let allowedTransitions: [TutorialStep: Set<TutorialStep>] = [
    .welcome: [.practiceIntro],
    .practiceIntro: [.product],
    .product: [.cart],
    .cart: [.cooldown],
    .cooldown: [.fakeCheckout],
    .fakeCheckout: [.cooling],
    .cooling: [.decision],
    .decision: [.ghostReceipt],
    .ghostReceipt: [.complete],
    .complete: [.delivery],
]

final class TutorialRepository {
    static let shared = TutorialRepository()

    private let defaults = UserDefaults.standard
    // Same key names as Android's SharedPreferences store (PREFS_NAME +
    // KEY_* constants in TutorialState.kt), prefixed for this app's
    // UserDefaults namespace - deliberately not renamed, so the two
    // platforms' persisted state shape stays directly comparable.
    private enum Key {
        static let version = "ghost_cart_tutorial.tutorial_version"
        static let consentVersion = "ghost_cart_tutorial.tutorial_consent_version"
        static let status = "ghost_cart_tutorial.tutorial_status"
        static let step = "ghost_cart_tutorial.current_tutorial_step"
        static let startedAt = "ghost_cart_tutorial.tutorial_started_at"
        static let completedAt = "ghost_cart_tutorial.tutorial_completed_at"
        static let sessionId = "ghost_cart_tutorial.tutorial_session_id"
        static let inCart = "ghost_cart_tutorial.tutorial_item_in_cart"
        static let cooldownSeconds = "ghost_cart_tutorial.tutorial_cooldown_seconds"
        static let coolingEndsAt = "ghost_cart_tutorial.tutorial_cooling_ends_at"
        static let decision = "ghost_cart_tutorial.tutorial_decision"
        static let isReplay = "ghost_cart_tutorial.tutorial_is_replay"
    }
    private static let sessionKeys = [Key.sessionId, Key.inCart, Key.cooldownSeconds, Key.coolingEndsAt, Key.decision, Key.isReplay]

    private init() {}

    func load() -> TutorialState {
        let storedVersion = defaults.object(forKey: Key.version) as? Int
        if let storedVersion, storedVersion != GhostCart.tutorialVersion {
            resetForNewVersion()
        }
        return readState()
    }

    func shouldAutoLaunch() -> Bool {
        let status = load().status
        return status == .notStarted || status == .inProgress
    }

    @discardableResult
    func recordConsentAccepted(version: Int = tutorialConsentVersion) -> TutorialState {
        var state = load()
        state.consentVersion = max(version, 1)
        return persist(state)
    }

    @discardableResult
    func start(replay: Bool = false) -> TutorialState {
        clearSession()
        var state = TutorialState()
        state.consentVersion = loadConsentVersion()
        state.status = .inProgress
        state.currentStep = .welcome
        state.startedAt = Date()
        state.sessionId = UUID().uuidString
        state.isReplay = replay
        return persist(state)
    }

    /// Matches Android's advance(expected, next): a no-op if the state has
    /// already moved on (double-tap safe), a hard failure if the transition
    /// isn't in allowedTransitions (a genuine programmer error, not a
    /// recoverable one - same contract as Android's require()).
    @discardableResult
    func advance(expected: TutorialStep, next: TutorialStep) -> TutorialState {
        var state = load()
        guard state.status == .inProgress, state.currentStep == expected else { return state }
        precondition(allowedTransitions[expected]?.contains(next) == true, "Invalid tutorial transition: \(expected) -> \(next)")
        state.currentStep = next
        return persist(state)
    }

    @discardableResult
    func addPracticeItemToCart() -> TutorialState {
        var state = load()
        guard state.currentStep == .product, state.status == .inProgress, !state.practiceItemInCart else { return state }
        state.practiceItemInCart = true
        return persist(state)
    }

    @discardableResult
    func openTutorialCart() -> TutorialState {
        var state = load()
        guard state.currentStep == .product, state.status == .inProgress, state.practiceItemInCart else { return state }
        state.currentStep = .cart
        return persist(state)
    }

    @discardableResult
    func selectTutorialCooldown() -> TutorialState {
        var state = load()
        guard state.currentStep == .cooldown, state.status == .inProgress else { return state }
        state.selectedCooldownSeconds = tutorialCooldownSeconds
        return persist(state)
    }

    @discardableResult
    func completeFakeCheckout() -> TutorialState {
        var state = load()
        guard state.currentStep == .fakeCheckout, state.status == .inProgress,
              state.practiceItemInCart, state.selectedCooldownSeconds == tutorialCooldownSeconds else { return state }
        state.currentStep = .cooling
        state.coolingEndsAt = Date().addingTimeInterval(tutorialCooldownSeconds)
        return persist(state)
    }

    @discardableResult
    func finishCooling() -> TutorialState {
        var state = load()
        guard state.currentStep == .cooling, state.status == .inProgress else { return state }
        state.currentStep = .decision
        state.coolingEndsAt = nil
        return persist(state)
    }

    @discardableResult
    func chooseDecision(_ decision: TutorialDecision) -> TutorialState {
        var state = load()
        guard state.currentStep == .decision, state.status == .inProgress else { return state }
        state.currentStep = .ghostReceipt
        state.decision = decision
        return persist(state)
    }

    @discardableResult
    func complete() -> TutorialState {
        var state = load()
        guard state.currentStep == .delivery, state.status == .inProgress else { return state }
        state.status = .completed
        state.completedAt = Date()
        clearSession()
        return persist(withoutSessionObjects(state))
    }

    @discardableResult
    func skip() -> TutorialState {
        var state = load()
        clearSession()
        state.status = .skipped
        state.currentStep = .welcome
        return persist(withoutSessionObjects(state))
    }

    func clearSession() {
        for key in Self.sessionKeys { defaults.removeObject(forKey: key) }
    }

    // MARK: - Private

    private func withoutSessionObjects(_ state: TutorialState) -> TutorialState {
        var copy = state
        copy.sessionId = nil
        copy.practiceItemInCart = false
        copy.selectedCooldownSeconds = nil
        copy.coolingEndsAt = nil
        copy.decision = nil
        copy.isReplay = false
        return copy
    }

    private func loadConsentVersion() -> Int {
        (defaults.object(forKey: Key.consentVersion) as? Int) ?? 0
    }

    private func resetForNewVersion() {
        clearSession()
        var state = TutorialState()
        state.consentVersion = loadConsentVersion()
        persist(state)
    }

    private func readState() -> TutorialState {
        var state = TutorialState()
        state.tutorialVersion = (defaults.object(forKey: Key.version) as? Int) ?? GhostCart.tutorialVersion
        state.consentVersion = loadConsentVersion()
        state.status = (defaults.string(forKey: Key.status)).flatMap(TutorialStatus.init) ?? .notStarted
        state.currentStep = (defaults.string(forKey: Key.step)).flatMap(TutorialStep.init) ?? .welcome
        state.startedAt = defaults.object(forKey: Key.startedAt) as? Date
        state.completedAt = defaults.object(forKey: Key.completedAt) as? Date
        state.sessionId = defaults.string(forKey: Key.sessionId)
        state.practiceItemInCart = defaults.bool(forKey: Key.inCart)
        state.selectedCooldownSeconds = defaults.object(forKey: Key.cooldownSeconds) as? TimeInterval
        state.coolingEndsAt = defaults.object(forKey: Key.coolingEndsAt) as? Date
        state.decision = (defaults.string(forKey: Key.decision)).flatMap(TutorialDecision.init)
        state.isReplay = defaults.bool(forKey: Key.isReplay)
        return state
    }

    @discardableResult
    private func persist(_ state: TutorialState) -> TutorialState {
        defaults.set(state.tutorialVersion, forKey: Key.version)
        defaults.set(state.consentVersion, forKey: Key.consentVersion)
        defaults.set(state.status.rawValue, forKey: Key.status)
        defaults.set(state.currentStep.rawValue, forKey: Key.step)
        defaults.set(state.startedAt, forKey: Key.startedAt)
        defaults.set(state.completedAt, forKey: Key.completedAt)
        defaults.set(state.sessionId, forKey: Key.sessionId)
        defaults.set(state.practiceItemInCart, forKey: Key.inCart)
        defaults.set(state.selectedCooldownSeconds, forKey: Key.cooldownSeconds)
        defaults.set(state.coolingEndsAt, forKey: Key.coolingEndsAt)
        defaults.set(state.decision?.rawValue, forKey: Key.decision)
        defaults.set(state.isReplay, forKey: Key.isReplay)
        return state
    }
}
