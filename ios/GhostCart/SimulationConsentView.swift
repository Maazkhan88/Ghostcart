import SwiftUI

// Blocking, no dismiss/skip - matches Android's SimulationConsentScreen,
// which Navigation.kt renders ahead of every other screen until accepted.
struct SimulationConsentView: View {
    let onAccept: () -> Void

    private let consentText = """
    Ghost Cart is a simulation-only app for practicing spending decisions. \
    It never processes real payment, never places a real order, and never \
    stores money or a payment card. Everything you "buy" here is a rehearsal \
    so you can decide with a clear head when it's real.
    """

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            GhostMascotView(poseName: "wave")
                .frame(width: 120, height: 120)
            Text("Before we begin")
                .font(.largeTitle.weight(.black))
            Text(consentText)
                .font(.subheadline)
                .foregroundStyle(Color.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
            Label("Simulation only. No real payment. No real delivery.", systemImage: "checkmark.shield.fill")
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.ghostGreenColor)
            Spacer()
            Button("I understand — continue", action: onAccept)
                .buttonStyle(GhostPrimaryButtonStyle())
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
    }
}

#Preview {
    SimulationConsentView(onAccept: {})
}
