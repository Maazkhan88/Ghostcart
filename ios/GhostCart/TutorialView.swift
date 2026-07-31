import SwiftUI

// Simplified port of Android's TutorialViewModel/TutorialScreen. Android
// runs a full interactive coach-mark state machine (WELCOME -> PRACTICE_INTRO
// -> PRODUCT -> CART -> COOLDOWN -> FAKE_CHECKOUT -> COOLING -> DECISION ->
// GHOST_RECEIPT -> COMPLETE -> DELIVERY) spotlighting real controls with a
// practice "Coffee & Donut Combo" item. That requires wiring coach marks
// into every screen it touches (capture, cooldown, checkout) - checkout
// itself doesn't exist on iOS yet (see docs/current-state.md gap list), so
// this first pass is a swipeable walkthrough covering the same beats at a
// glance. Upgrading it to match Android's spotlighted-tap version is a
// follow-up once Checkout/Fake Delivery ships on iOS.
struct TutorialView: View {
    let onFinish: () -> Void

    private struct Beat: Identifiable {
        let id = UUID()
        let pose: String
        let title: String
        let message: String
    }

    private let beats = [
        Beat(pose: "cart", title: "Ghost something", message: "See a temptation? Capture it in seconds - name, price, where it's from."),
        Beat(pose: "cooldown", title: "Let it cool", message: "Pick a cooldown instead of buying now. Ghost Cart reminds you when it's time to decide."),
        Beat(pose: "wallet", title: "Decide with a clear head", message: "When the cooldown ends, skip it and keep the money, or go buy it intentionally."),
        Beat(pose: "thumbsup", title: "Watch it add up", message: "Every skip counts toward Money Kept on your Progress tab. Nothing here is a real purchase.")
    ]

    @State private var index = 0

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            TabView(selection: $index) {
                ForEach(Array(beats.enumerated()), id: \.element.id) { offset, beat in
                    VStack(spacing: 18) {
                        GhostMascotView(poseName: beat.pose)
                            .frame(width: 140, height: 140)
                        Text(beat.title)
                            .font(.title2.weight(.black))
                        Text(beat.message)
                            .font(.subheadline)
                            .foregroundStyle(Color.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                    .tag(offset)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .frame(height: 360)
            Spacer()

            if index == beats.count - 1 {
                Button("Start Ghosting", action: onFinish)
                    .buttonStyle(GhostPrimaryButtonStyle())
            } else {
                Button("Next") { withAnimation { index += 1 } }
                    .buttonStyle(GhostPrimaryButtonStyle())
                Button("Skip", action: onFinish)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.secondary)
            }
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
    }
}

#Preview {
    TutorialView(onFinish: {})
}
