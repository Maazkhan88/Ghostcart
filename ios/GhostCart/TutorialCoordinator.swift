import SwiftUI

// Drives the tutorial through TutorialState's step machine. Where real
// production scaffolding already exists (MarketplaceProductCard's
// isTutorial/onGhostItDirect, GhostDeliveryTrackerView/DecisionView's
// previewItem/onTutorialDecision), this coordinator uses the REAL screen
// with a spotlight over the real control - never a fake mockup of it. The
// connective steps (Welcome, Practice intro, Cart summary, Cooldown
// confirm, Fake checkout confirm) use small dedicated confirmation cards
// rather than deep clones of the real cart/checkout screens, since those
// real screens are tightly coupled to GhostCartStore.cartItems mutation and
// safely intercepting that without risking the live checkout flow is a
// larger, separate piece of work - this matches Android's own tutorial
// design intent too (TutorialScreen.kt's TutorialPage layouts for these
// same connective steps are simple confirmation cards, not full screen
// clones, even in its own reference implementation).
@MainActor
final class TutorialCoordinator: ObservableObject {
    @Published private(set) var state: TutorialState
    @Published var targetFrames: [String: CGRect] = [:]

    private let repository = TutorialRepository.shared

    init() {
        state = repository.load()
    }

    var isActive: Bool { state.status == .inProgress }
    var shouldAutoLaunch: Bool { repository.shouldAutoLaunch() }

    /// The synthetic practice item - never written into GhostCartStore.items,
    /// exactly like Android's tutorial (data/TutorialState.kt's own doc
    /// comment: "it intentionally owns no production Product, cart,
    /// AlmostBuy... state").
    var tutorialItem: AlmostBuy {
        var item = AlmostBuy(
            name: "Ghost Cart Coffee & Donut Combo",
            amount: 15,
            category: .food,
            trigger: .other,
            source: .manual,
            capturedAt: state.startedAt ?? Date()
        )
        if state.currentStep.ordinal >= TutorialStep.fakeCheckout.ordinal {
            item.coolingStartedAt = state.startedAt
            item.decisionAt = state.coolingEndsAt
            item.state = .cooling
        }
        if let decision = state.decision {
            item.state = decision == .ghosted ? .resolvedSkipped : .resolvedBought
            item.resolvedAt = state.completedAt ?? Date()
        }
        return item
    }

    func startIfNeeded() {
        let current = repository.load()
        state = current.status == .inProgress ? current : repository.start()
        GhostAnalytics.tutorialStarted()
    }

    func replay() {
        state = repository.start(replay: true)
        GhostAnalytics.tutorialReplayed()
    }

    func continueFromWelcome() { advance(.welcome, .practiceIntro) }
    func openPracticeProduct() { advance(.practiceIntro, .product) }

    func addPracticeItemToCart() {
        guard state.currentStep == .product else { return }
        state = repository.addPracticeItemToCart()
        GhostAnalytics.tutorialStepViewed(TutorialStep.product.rawValue)
    }

    func openTutorialCart() {
        guard state.currentStep == .product, state.practiceItemInCart else { return }
        state = repository.openTutorialCart()
    }

    func openCooldownPicker() { advance(.cart, .cooldown) }

    func selectTutorialCooldown() {
        guard state.currentStep == .cooldown else { return }
        state = repository.selectTutorialCooldown()
    }

    func continueToFakeCheckout() { advance(.cooldown, .fakeCheckout) }

    func completeFakeCheckout() {
        guard state.currentStep == .fakeCheckout else { return }
        state = repository.completeFakeCheckout()
        GhostAnalytics.tutorialStepViewed(TutorialStep.fakeCheckout.rawValue)
    }

    func finishCooling() {
        guard state.currentStep == .cooling else { return }
        state = repository.finishCooling()
        GhostAnalytics.tutorialStepViewed(TutorialStep.cooling.rawValue)
    }

    /// Matches onTutorialDecision's String contract from the already-scaffolded
    /// GhostDeliveryDecisionView - maps its action identifiers onto TutorialDecision.
    func handleTutorialDecision(_ action: String) {
        guard state.currentStep == .decision else { return }
        let decision: TutorialDecision = action == "skip" ? .ghosted : (action == "restart" ? .coolLonger : .stillBuy)
        state = repository.chooseDecision(decision)
    }

    func continueFromReceipt() { advance(.ghostReceipt, .complete) }
    func startTutorialDelivery() { advance(.complete, .delivery) }

    func complete() {
        guard state.currentStep == .delivery else { return }
        state = repository.complete()
        GhostAnalytics.tutorialCompleted()
    }

    func skip() {
        let exitedAt = state.currentStep.rawValue
        state = repository.skip()
        GhostAnalytics.tutorialSkipped(atStep: exitedAt)
    }

    private func advance(_ expected: TutorialStep, _ next: TutorialStep) {
        guard state.currentStep == expected else { return }
        state = repository.advance(expected: expected, next: next)
        GhostAnalytics.tutorialStepViewed(expected.rawValue)
    }

    /// Reads a spotlight target frame reported by .tutorialTarget(id).
    func targetFrame(for id: String) -> CGRect? { targetFrames[id] }
}

// The step-driving overlay + connective confirmation cards. Mounted once at
// the ContentView root (see ContentView.swift) so it can spotlight controls
// on any real screen underneath - Home's marketplace card for PRODUCT, the
// Delivery Tracker/Decision screens for COOLING/DECISION.
struct TutorialOverlayHost: View {
    @ObservedObject var coordinator: TutorialCoordinator
    let onOpenTracker: () -> Void
    let onExit: () -> Void

    @State private var showExitConfirm = false

    var body: some View {
        Group {
            switch coordinator.state.currentStep {
            case .welcome:
                TutorialCard(step: "Welcome", title: "Welcome to Ghost Cart 👻", message: "Ghost Cart gives everything you almost bought somewhere safe to go while you decide.", primaryTitle: "Start the tutorial", onPrimary: coordinator.continueFromWelcome, onSkip: { showExitConfirm = true })
            case .practiceIntro:
                TutorialCard(step: "Practice item", title: "Your first Ghost is on the house ☕🍩", message: "A fictional coffee-and-donut combo so you can practice the complete flow without buying anything. Tutorial item only - no real order, payment, or delivery.", primaryTitle: "Show me the product", onPrimary: coordinator.openPracticeProduct, onSkip: { showExitConfirm = true })
            case .product:
                TutorialGuideOverlay(
                    targetFrame: coordinator.targetFrame(for: "tutorial.ghostIt"),
                    guide: TutorialGuideSpec(message: "Tap \"Ghost it\" to add the practice combo to your Ghost Cart.", stepLabel: "Step 3 of 11"),
                    onSkip: { showExitConfirm = true }
                )
            case .cart:
                TutorialCard(step: "Your practice cart", title: "Your practice Ghost Cart", message: "The practice item is isolated from your real cart and never counts toward your real activity.", primaryTitle: "Proceed to checkout", onPrimary: coordinator.openCooldownPicker, onSkip: { showExitConfirm = true })
            case .cooldown:
                TutorialCard(step: "Choose a duration", title: "Give the craving a little time", message: "Cooling gives the impulse time to fade. For this practice run, we'll use a 10-second cooldown so you can see the whole flow quickly.", primaryTitle: "Use the 10-second tutorial duration", onPrimary: { coordinator.selectTutorialCooldown(); coordinator.continueToFakeCheckout() }, onSkip: { showExitConfirm = true })
            case .fakeCheckout:
                TutorialCard(step: "Fake Checkout", title: "Complete Fake Checkout", message: "Feel the checkout moment without spending real money. Nothing is charged, ordered, or delivered.", primaryTitle: "Place Ghost Order", onPrimary: { coordinator.completeFakeCheckout(); onOpenTracker() }, onSkip: { showExitConfirm = true })
            case .cooling, .decision:
                // Real GhostDeliveryTrackerView/GhostDeliveryDecisionView are
                // driven directly by ContentView via previewItem +
                // onTutorialDecision - nothing to render here, this state
                // only exists so `isActive` and the exit-confirm dialog stay
                // available while those real screens are on top.
                Color.clear
            case .ghostReceipt:
                TutorialCard(step: "Ghost Receipt", title: "Here's your Ghost Receipt", message: coordinator.state.decision == .ghosted ? "You skipped it - that's a real decision-making rep in your Ghost Cart history." : "Every decision counts as practice, even if you'd have bought it for real.", primaryTitle: "Continue", onPrimary: coordinator.continueFromReceipt, onSkip: { showExitConfirm = true })
            case .complete:
                TutorialCard(step: "One more thing", title: "See it as a real delivery", message: "Real Ghost Orders show a live delivery tracker. Want a quick look?", primaryTitle: "Show me", onPrimary: { coordinator.startTutorialDelivery(); onOpenTracker() }, onSkip: { coordinator.complete() })
            case .delivery:
                Color.clear
            }
        }
        .confirmationDialog("Leave the tutorial?", isPresented: $showExitConfirm, titleVisibility: .visible) {
            Button("Exit tutorial", role: .destructive) {
                coordinator.skip()
                onExit()
            }
            Button("Continue tutorial", role: .cancel) {}
        } message: {
            Text("The practice coffee and donut will disappear, but you can replay the tutorial later from Profile.")
        }
    }
}

private struct TutorialCard: View {
    let step: String
    let title: String
    let message: String
    let primaryTitle: String
    let onPrimary: () -> Void
    let onSkip: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.7).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 14) {
                Text(step).font(.caption2.weight(.black)).foregroundStyle(Color.ghostGreenColor)
                Text(title).font(.title3.weight(.black)).foregroundStyle(Color.white)
                Text(message).font(.subheadline).foregroundStyle(Color.white.opacity(0.8))
                Button(primaryTitle, action: onPrimary)
                    .buttonStyle(GhostPrimaryButtonStyle())
                Button("Skip for now", action: onSkip)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.white.opacity(0.6))
                    .frame(maxWidth: .infinity)
            }
            .padding(24)
            .background(Color.black, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(Color.white.opacity(0.12), lineWidth: 1) }
            .padding(24)
        }
    }
}
