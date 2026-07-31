import SwiftUI

// Mirrors Android's ProfileSelectScreen: a one-time, skippable gender/avatar
// pick that only personalizes mascot art - never gated behind it.
struct ProfileSelectView: View {
    @Binding var selection: ProfilePick?
    let onContinue: () -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            ScreenHeader(
                eyebrow: "Personalize",
                title: "Pick your Ghost",
                subtitle: "This only personalizes your Ghost Cart experience."
            )

            HStack(spacing: 16) {
                pickCard(.male, pose: "male", title: "Male")
                pickCard(.female, pose: "female", title: "Female")
            }

            Spacer()

            VStack(spacing: 10) {
                Button("Continue", action: onContinue)
                    .buttonStyle(GhostPrimaryButtonStyle())
                Button("Skip for now", action: onSkip)
                    .buttonStyle(GhostSecondaryButtonStyle())
            }
        }
        .padding(24)
        .background(Color.paperColor.ignoresSafeArea())
    }

    private func pickCard(_ pick: ProfilePick, pose: String, title: String) -> some View {
        let isSelected = selection == pick
        return Button {
            selection = pick
        } label: {
            VStack(spacing: 10) {
                GhostMascotView(poseName: pose)
                    .frame(height: 110)
                Text(title).font(.headline.weight(.bold))
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color.ghostGreenColor)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(16)
            .background(Color.primary.opacity(isSelected ? 0.08 : 0.04))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(isSelected ? Color.ghostGreenColor : Color.primary.opacity(0.09), lineWidth: isSelected ? 2 : 1)
            }
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ProfileSelectView(selection: .constant(nil), onContinue: {}, onSkip: {})
}
