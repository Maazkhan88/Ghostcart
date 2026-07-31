import SwiftUI

// Mirrors Android's onboarding Splash card: wordmark, tagline, feature
// bullets, "Start Ghosting" and a "Continue as Guest" fallback.
struct OnboardingLandingView: View {
    let onStart: () -> Void
    let onGuest: () -> Void

    private let bullets = [
        ("timer", "Capture the urge, then let it cool"),
        ("checkmark.shield", "No real payment, ever"),
        ("chart.line.uptrend.xyaxis", "Watch the money you kept add up")
    ]

    var body: some View {
        VStack(spacing: 28) {
            Spacer()
            GhostCartLogoView()
                .frame(width: 96, height: 96)
            Text("Ghost Cart")
                .font(.largeTitle.weight(.black))
            Text("Before you buy it, ghost it.")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Color.secondary)

            VStack(alignment: .leading, spacing: 14) {
                ForEach(bullets, id: \.1) { icon, text in
                    Label(text, systemImage: icon)
                        .font(.subheadline.weight(.semibold))
                }
            }
            .padding(.top, 8)

            Spacer()

            VStack(spacing: 10) {
                Button("Start Ghosting", action: onStart)
                    .buttonStyle(GhostPrimaryButtonStyle())
                Button("Continue as Guest", action: onGuest)
                    .buttonStyle(GhostSecondaryButtonStyle())
            }

            SimulationDisclosure()
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
    }
}

#Preview {
    OnboardingLandingView(onStart: {}, onGuest: {})
}
