import SwiftUI

// Blocking, no dismiss/skip - matches Android's SimulationConsentScreen,
// which Navigation.kt renders ahead of every other screen until accepted.
struct SimulationConsentView: View {
    let consentText: String
    let isSubmitting: Bool
    let errorMessage: String?
    let onAccept: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            GhostMascotView(poseName: "wave")
                .frame(width: 120, height: 120)
            Text("Before we begin")
                .font(.largeTitle.weight(.black))
                .foregroundStyle(Color.inkColor)
            Text(consentText)
                .font(.subheadline)
                .foregroundStyle(Color.inkColor.opacity(0.66))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
            Label("Simulation only. No real payment. No real delivery.", systemImage: "checkmark.shield.fill")
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.ghostGreenColor)
            if let errorMessage {
                Text(errorMessage)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.red)
                    .multilineTextAlignment(.center)
            }
            Spacer()
            Button(action: onAccept) {
                if isSubmitting {
                    ProgressView().tint(.white)
                } else {
                    Text("I understand — continue")
                }
            }
                .buttonStyle(GhostPrimaryButtonStyle())
                .disabled(isSubmitting)
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
    }
}

#Preview {
    SimulationConsentView(
        consentText: "Ghost Cart is a simulation-only app. No real payment or delivery occurs.",
        isSubmitting: false,
        errorMessage: nil,
        onAccept: {}
    )
}
