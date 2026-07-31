import SwiftUI

// Android's tutorial is an isolated, durable 11-state practice journey. This
// port deliberately owns no GhostCartStore state: the coffee-and-donut item,
// checkout, receipt, decision, and delivery never enter production totals.
private enum TutorialStep: String, CaseIterable {
    case welcome, practiceIntro, product, cart, cooldown, fakeCheckout
    case cooling, decision, ghostReceipt, complete, delivery
}

private enum TutorialDecision: String {
    case ghosted, coolLonger, stillBuy
}

private enum TutorialSession {
    static let stepKey = "ghostcart.tutorial-v1.step"
    static let decisionKey = "ghostcart.tutorial-v1.decision"
    static let inCartKey = "ghostcart.tutorial-v1.in-cart"
    static let cooldownKey = "ghostcart.tutorial-v1.cooldown-selected"
    static let coolingEndKey = "ghostcart.tutorial-v1.cooling-end"

    static func loadStep() -> TutorialStep {
        UserDefaults.standard.string(forKey: stepKey).flatMap(TutorialStep.init(rawValue:)) ?? .welcome
    }

    static func save(
        step: TutorialStep,
        inCart: Bool,
        cooldownSelected: Bool,
        decision: TutorialDecision?,
        coolingEnd: Date?
    ) {
        let defaults = UserDefaults.standard
        defaults.set(step.rawValue, forKey: stepKey)
        defaults.set(inCart, forKey: inCartKey)
        defaults.set(cooldownSelected, forKey: cooldownKey)
        defaults.set(decision?.rawValue, forKey: decisionKey)
        defaults.set(coolingEnd?.timeIntervalSince1970, forKey: coolingEndKey)
    }

    static func clear() {
        [stepKey, decisionKey, inCartKey, cooldownKey, coolingEndKey]
            .forEach(UserDefaults.standard.removeObject(forKey:))
    }
}

struct TutorialView: View {
    let onFinish: () -> Void

    static func resetSavedSession() {
        TutorialSession.clear()
    }

    @State private var step = TutorialSession.loadStep()
    @State private var practiceItemInCart = UserDefaults.standard.bool(forKey: TutorialSession.inCartKey)
    @State private var cooldownSelected = UserDefaults.standard.bool(forKey: TutorialSession.cooldownKey)
    @State private var decision = UserDefaults.standard.string(forKey: TutorialSession.decisionKey).flatMap(TutorialDecision.init(rawValue:))
    @State private var coolingEnd: Date? = {
        let seconds = UserDefaults.standard.double(forKey: TutorialSession.coolingEndKey)
        return seconds > 0 ? Date(timeIntervalSince1970: seconds) : nil
    }()
    @State private var secondsRemaining = 10
    @State private var deliverySeconds = 8
    @State private var showExitDialog = false

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.paperColor.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 0) {
                    TutorialProgressBar(step: step)
                    content
                }
                .padding(.horizontal, 22)
                .padding(.top, 18)
                .padding(.bottom, 30)
            }
            if step != .welcome {
                Button { showExitDialog = true } label: {
                    Image(systemName: "xmark")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.inkColor)
                        .frame(width: 38, height: 38)
                        .background(Color.paperColor.opacity(0.92), in: Circle())
                }
                .padding(.top, 48)
                .padding(.leading, 18)
            }
        }
        .preferredColorScheme(.light)
        .alert("Leave the tutorial?", isPresented: $showExitDialog) {
            Button("Continue tutorial", role: .cancel) {}
            Button("Exit tutorial", role: .destructive) { finish() }
        } message: {
            Text("The practice coffee and donut will disappear, but you can replay the tutorial later from Profile.")
        }
        .task(id: step) {
            if step == .cooling { await runCoolingTimer() }
            if step == .delivery { await runDeliveryTimer() }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch step {
        case .welcome: welcome
        case .practiceIntro: practiceIntro
        case .product: product
        case .cart: cart
        case .cooldown: cooldown
        case .fakeCheckout: fakeCheckout
        case .cooling: cooling
        case .decision: decisionView
        case .ghostReceipt: receipt
        case .complete: complete
        case .delivery: delivery
        }
    }

    private var welcome: some View {
        TutorialPage(
            title: "Welcome to Ghost Cart 👻",
            subtitle: "Pause the craving. Keep the control.",
            image: "TutorialTeacherBoard",
            imageSize: 160
        ) {
            Text("Ghost Cart gives everything you almost bought somewhere safe to go while you decide.")
                .tutorialBody()
            Button("Start the tutorial") { advance(.practiceIntro) }.buttonStyle(GhostPrimaryButtonStyle())
            Button("Skip for now", action: finish).tutorialSecondaryButton()
        }
    }

    private var practiceIntro: some View {
        TutorialPage(
            title: "Your first Ghost is on the house ☕🍩",
            subtitle: "Thank you for trying Ghost Cart. We’ve added a fictional coffee-and-donut combo so you can practise the complete flow without buying anything.",
            image: "TutorialTeacherChecklist",
            imageSize: 126
        ) {
            Image("TutorialCoffeeDonutCombo").tutorialProduct(size: 190)
            TutorialDisclosure("Tutorial item only. No real order, payment or delivery.")
            Button("Ghost this combo") { advance(.product) }.buttonStyle(GhostPrimaryButtonStyle())
        }
    }

    private var product: some View {
        TutorialPage(
            title: "Ghost Cart Coffee & Donut Combo",
            subtitle: "A warm coffee and glazed donut, on the house for your first Ghost Cart practice run.",
            image: "TutorialCoffeeDonutCombo",
            imageSize: 235
        ) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Ghost Cart Café").foregroundStyle(Color.ghostGreenColor).font(.caption.weight(.black))
                    Text("Food & delivery").foregroundStyle(Color.inkColor.opacity(0.58)).font(.caption)
                }
                Spacer()
                DirhamAmount(value: 15, font: .title3.weight(.black), iconSize: 17, color: Color.inkColor)
            }
            TutorialCoachMark(practiceItemInCart ? "The practice item is isolated from your real cart." : "Start by adding the item to your Ghost Cart.") {
                Button(practiceItemInCart ? "Open Ghost Cart" : "Add to cart") {
                    if practiceItemInCart { advance(.cart) }
                    else { practiceItemInCart = true; persist() }
                }
                .buttonStyle(GhostPrimaryButtonStyle())
            }
            TutorialDisclosure("Simulation only. This tutorial product cannot be purchased.")
        }
    }

    private var cart: some View {
        TutorialPage(title: "Your practice Ghost Cart", subtitle: "The practice item is isolated from your real cart.") {
            HStack(spacing: 14) {
                Image("TutorialCoffeeDonutCombo").tutorialProduct(size: 82)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Ghost Cart Coffee & Donut Combo").font(.subheadline.weight(.black)).foregroundStyle(Color.inkColor)
                    Text("Tutorial item · Qty 1").font(.caption).foregroundStyle(Color.inkColor.opacity(0.58))
                }
                Spacer()
                DirhamAmount(value: 15, font: .caption.weight(.bold), iconSize: 10, color: Color.inkColor)
            }
            .padding(16).background(Color.inkColor.opacity(0.045), in: RoundedRectangle(cornerRadius: 22))
            TutorialCoachMark("This is your simulated cart. Nothing here will be ordered or charged.") {
                Button("Proceed to checkout") { advance(.cooldown) }.buttonStyle(GhostPrimaryButtonStyle())
            }
        }
    }

    private var cooldown: some View {
        TutorialPage(
            title: "Give the craving a little time",
            subtitle: "Cooling gives the impulse time to fade. For this practice run, choose 10 seconds.",
            image: "TutorialTeacherBoard",
            imageSize: 140
        ) {
            TutorialCoachMark("Choose the special tutorial duration.") {
                cooldownRow("10 seconds — Tutorial", supporting: "Recommended for this practice run", selected: cooldownSelected, enabled: true) {
                    cooldownSelected = true; persist()
                }
            }
            cooldownRow("24 hours", supporting: "Default for real Ghost items", selected: false, enabled: false) {}
            cooldownRow("3 days", supporting: "For a longer pause", selected: false, enabled: false) {}
            Button("Continue to Fake Checkout") { if cooldownSelected { advance(.fakeCheckout) } }
                .buttonStyle(GhostPrimaryButtonStyle()).disabled(!cooldownSelected).opacity(cooldownSelected ? 1 : 0.42)
            if !cooldownSelected {
                Text("Select the 10-second tutorial option to continue.").font(.caption2).foregroundStyle(Color.inkColor.opacity(0.58))
            }
        }
    }

    private var fakeCheckout: some View {
        TutorialPage(title: "Fake Checkout", subtitle: "Complete the checkout feeling without spending real money.") {
            TutorialDisclosure("Simulation only. No real payment or delivery.")
            VStack(spacing: 14) {
                tutorialSummary("Practice item", "Coffee & Donut Combo")
                tutorialSummary("Cooldown", "10 seconds — Tutorial")
                HStack { Text("Simulated total").fontWeight(.black); Spacer(); DirhamAmount(value: 15, font: .headline.weight(.black), iconSize: 14, color: Color.inkColor) }
            }
            .padding(18).background(Color.inkColor.opacity(0.045), in: RoundedRectangle(cornerRadius: 22))
            TutorialCoachMark("Complete the checkout feeling without spending real money.") {
                Button("Complete Fake Checkout") {
                    coolingEnd = Date().addingTimeInterval(10)
                    secondsRemaining = 10
                    advance(.cooling)
                }.buttonStyle(GhostPrimaryButtonStyle())
            }
        }
    }

    private var cooling: some View {
        TutorialPage(
            title: "Let it cool…",
            subtitle: "Take a breath. The item will still be here when the timer ends.",
            image: "TutorialTeacherPointer",
            imageSize: 155
        ) {
            Image("TutorialCoffeeDonutCombo").tutorialProduct(size: 190)
            Text("\(secondsRemaining)s").font(.system(size: 42, weight: .black)).foregroundStyle(Color.inkColor)
            SwiftUI.ProgressView(value: Double(10 - secondsRemaining), total: 10).tint(Color.ghostGreenColor).scaleEffect(y: 2)
            Button("Skip countdown for tutorial") { finishCooling() }.tutorialSecondaryButton()
            TutorialDisclosure("This in-session timer creates no background job or notification.")
        }
    }

    private var decisionView: some View {
        TutorialPage(
            title: "Still want it?",
            subtitle: "Any answer is useful. Ghost Cart helps you make an intentional decision, not a perfect one.",
            image: "TutorialTeacherChecklist",
            imageSize: 145
        ) {
            decisionCard("Ghost it", supporting: "Skip the simulated purchase.", recommended: true, value: .ghosted)
            decisionCard("Cool it longer", supporting: "Give the decision more time.", value: .coolLonger)
            decisionCard("I’d still buy it", supporting: "That’s okay — the goal is an intentional decision.", value: .stillBuy)
            TutorialDisclosure("Your practice choice will not affect Wallet, Money Kept, history or leaderboards.")
        }
    }

    private var receipt: some View {
        TutorialPage(
            title: "Your first Ghost Receipt",
            subtitle: "A one-time tutorial record",
            image: "TutorialTeacherChecklist",
            imageSize: 140
        ) {
            VStack(alignment: .leading, spacing: 13) {
                Text("TUTORIAL GHOST RECEIPT").font(.caption.weight(.black)).foregroundStyle(Color.ghostGreenColor)
                tutorialSummary("Product", "Ghost Cart Coffee & Donut Combo")
                tutorialSummary("Result", decisionResult)
                tutorialSummary("Cooling period", "10-second tutorial")
                HStack { Text("Simulated price").foregroundStyle(Color.inkColor.opacity(0.58)); Spacer(); DirhamAmount(value: 15, font: .subheadline.weight(.bold), iconSize: 12, color: Color.inkColor) }
                Text("Tutorial record only. No purchase, payment or delivery occurred.").font(.caption2).italic().foregroundStyle(Color.inkColor.opacity(0.58))
            }
            .padding(18).background(Color.inkColor.opacity(0.045), in: RoundedRectangle(cornerRadius: 22))
            Button("Continue") { advance(.complete) }.buttonStyle(GhostPrimaryButtonStyle())
        }
    }

    private var complete: some View {
        TutorialPage(
            title: "You’re ready to Ghost 👻",
            subtitle: "You’ve learned the full Ghost Cart flow: capture it, cool it and decide when the impulse is quieter.",
            image: "TutorialTeacherBoard",
            imageSize: 155
        ) {
            TutorialDisclosure("The guided lesson is complete. Your tutorial-only simulated delivery starts next.")
            Button("Start simulated delivery") { deliverySeconds = 8; advance(.delivery) }.buttonStyle(GhostPrimaryButtonStyle())
            Button("Replay tutorial") { reset() }.tutorialSecondaryButton()
        }
    }

    private var delivery: some View {
        TutorialPage(
            title: deliveryStatus,
            subtitle: "Nothing is being purchased or delivered. This is the final tutorial simulation.",
            image: deliverySeconds == 0 ? "TutorialTeacherConfetti" : "TutorialTeacherPointer",
            imageSize: 155
        ) {
            Image("TutorialCoffeeDonutCombo").tutorialProduct(size: 180)
            SwiftUI.ProgressView(value: Double(8 - deliverySeconds), total: 8).tint(Color.ghostGreenColor).scaleEffect(y: 2)
            Text(deliverySeconds > 0 ? "\(deliverySeconds) seconds" : "The coffee and donut have vanished like a proper ghost.")
                .font(.subheadline.weight(.bold)).foregroundStyle(deliverySeconds > 0 ? Color.inkColor : Color.ghostGreenColor)
                .multilineTextAlignment(.center)
            if deliverySeconds == 0 {
                Button("Explore Ghost Cart", action: finish).buttonStyle(GhostPrimaryButtonStyle())
            }
        }
    }

    private func cooldownRow(_ title: String, supporting: String, selected: Bool, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.subheadline.weight(.bold))
                    Text(supporting).font(.caption2).opacity(0.58)
                }
                Spacer()
            }
            .foregroundStyle(enabled ? Color.inkColor : Color.inkColor.opacity(0.42))
            .padding(13).background(selected ? Color.ghostGreenColor.opacity(0.16) : Color.inkColor.opacity(0.04), in: RoundedRectangle(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).stroke(selected ? Color.ghostGreenColor : Color.inkColor.opacity(0.08), lineWidth: selected ? 2 : 1) }
        }.buttonStyle(.plain).disabled(!enabled)
    }

    private func decisionCard(_ title: String, supporting: String, recommended: Bool = false, value: TutorialDecision) -> some View {
        Button {
            decision = value; advance(.ghostReceipt)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: recommended ? "checkmark.circle.fill" : "clock")
                    .foregroundStyle(recommended ? Color.ghostGreenColor : Color.inkColor.opacity(0.58))
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(.subheadline.weight(.black))
                    Text(supporting).font(.caption).opacity(0.58)
                }
                Spacer()
                if recommended { Text("Recommended").font(.system(size: 9, weight: .black)).foregroundStyle(Color.ghostGreenColor) }
            }
            .foregroundStyle(Color.inkColor).padding(16)
            .background(recommended ? Color.ghostGreenColor.opacity(0.14) : Color.inkColor.opacity(0.04), in: RoundedRectangle(cornerRadius: 18))
            .overlay { RoundedRectangle(cornerRadius: 18).stroke(recommended ? Color.ghostGreenColor : Color.inkColor.opacity(0.08), lineWidth: recommended ? 2 : 1) }
        }.buttonStyle(.plain)
    }

    private func tutorialSummary(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(label).foregroundStyle(Color.inkColor.opacity(0.58)); Spacer()
            Text(value).fontWeight(.bold).multilineTextAlignment(.trailing)
        }.font(.caption).foregroundStyle(Color.inkColor)
    }

    private var decisionResult: String {
        switch decision {
        case .ghosted: return "Ghosted — skipped the simulated purchase"
        case .coolLonger: return "Chose more cooling time"
        case .stillBuy: return "Would still buy — an intentional decision"
        case nil: return "Practice decision complete"
        }
    }

    private var deliveryStatus: String {
        switch deliverySeconds {
        case 6...: return "Preparing your imaginary order"
        case 3...: return "Ghost Rider is gliding your way"
        case 1...: return "Almost at your imaginary doorstep"
        default: return "Simulated delivery complete"
        }
    }

    private func advance(_ next: TutorialStep) {
        step = next
        persist()
    }

    private func persist() {
        TutorialSession.save(step: step, inCart: practiceItemInCart, cooldownSelected: cooldownSelected, decision: decision, coolingEnd: coolingEnd)
    }

    private func reset() {
        TutorialSession.clear()
        practiceItemInCart = false; cooldownSelected = false; decision = nil; coolingEnd = nil
        step = .welcome
    }

    private func finish() {
        TutorialSession.clear()
        onFinish()
    }

    private func finishCooling() {
        coolingEnd = nil; secondsRemaining = 0; advance(.decision)
    }

    @MainActor
    private func runCoolingTimer() async {
        let end = coolingEnd ?? Date().addingTimeInterval(10)
        coolingEnd = end; persist()
        while !Task.isCancelled {
            let remaining = max(0, Int(ceil(end.timeIntervalSinceNow)))
            secondsRemaining = remaining
            if remaining == 0 { finishCooling(); return }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
    }

    @MainActor
    private func runDeliveryTimer() async {
        deliverySeconds = 8
        while deliverySeconds > 0, !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            if !Task.isCancelled { deliverySeconds -= 1 }
        }
    }
}

private struct TutorialPage<Content: View>: View {
    let title: String
    let subtitle: String?
    let image: String?
    let imageSize: CGFloat
    @ViewBuilder let content: Content

    init(title: String, subtitle: String? = nil, image: String? = nil, imageSize: CGFloat = 0, @ViewBuilder content: () -> Content) {
        self.title = title; self.subtitle = subtitle; self.image = image; self.imageSize = imageSize; self.content = content()
    }

    var body: some View {
        VStack(spacing: 18) {
            if let image {
                Image(image).resizable().scaledToFit().frame(width: imageSize, height: imageSize)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .padding(.top, 20)
            }
            Text(title).font(.system(size: 30, weight: .black)).foregroundStyle(Color.inkColor).multilineTextAlignment(.center)
            if let subtitle { Text(subtitle).tutorialBody() }
            content
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 12)
    }
}

private struct TutorialProgressBar: View {
    let step: TutorialStep
    private var progress: Double {
        Double((TutorialStep.allCases.firstIndex(of: step) ?? 0) + 1) / Double(TutorialStep.allCases.count)
    }
    var body: some View {
        SwiftUI.ProgressView(value: progress).tint(Color.ghostGreenColor).scaleEffect(y: 1.5)
    }
}

private struct TutorialDisclosure: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Label(text, systemImage: "lock.fill")
            .font(.caption).foregroundStyle(Color.inkColor.opacity(0.62))
            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.inkColor.opacity(0.045), in: RoundedRectangle(cornerRadius: 15))
    }
}

private struct TutorialCoachMark<Content: View>: View {
    let text: String
    @ViewBuilder let content: Content
    init(_ text: String, @ViewBuilder content: () -> Content) { self.text = text; self.content = content() }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(text).font(.caption.weight(.bold)).foregroundStyle(Color.inkColor)
            content
        }
        .padding(12).background(Color.ghostGreenColor.opacity(0.14), in: RoundedRectangle(cornerRadius: 20))
        .overlay { RoundedRectangle(cornerRadius: 20).stroke(Color.ghostGreenColor, lineWidth: 2) }
    }
}

private extension View {
    func tutorialBody() -> some View {
        self.font(.subheadline).foregroundStyle(Color.inkColor.opacity(0.64)).multilineTextAlignment(.center)
    }
    func tutorialSecondaryButton() -> some View {
        self.font(.subheadline.weight(.bold)).foregroundStyle(Color.inkColor.opacity(0.62)).frame(maxWidth: .infinity).padding(.vertical, 12)
    }
}

private extension Image {
    func tutorialProduct(size: CGFloat) -> some View {
        self.resizable().scaledToFit().frame(width: size, height: size).padding(6).background(Color.white, in: RoundedRectangle(cornerRadius: 22))
    }
}

#Preview { TutorialView(onFinish: {}) }
