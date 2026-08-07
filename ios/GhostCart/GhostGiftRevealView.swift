import SwiftUI

// Mirrors Android's GhostGiftRevealScreen: reached only via the
// theghostcart.com/gift/{token} Universal Link (email), never a manually
// entered token. Two states - a consent gate, then the revealed product with
// a "Ghost it too" action that starts the recipient's own cooldown.
struct GhostGiftRevealView: View {
    let token: String
    let onDismiss: () -> Void

    @EnvironmentObject private var store: GhostCartStore
    @State private var revealed: RevealedGhostGift?
    @State private var acceptedPrivacy = false
    @State private var isRevealing = false
    @State private var errorMessage: String?
    @State private var didGhostIt = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if let revealed {
                        revealedContent(revealed)
                    } else {
                        preRevealContent
                    }
                }
                .padding(20)
                .padding(.bottom, 24)
            }
            .background(Color(.systemBackground))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                }
            }
        }
    }

    private var preRevealContent: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(spacing: 14) {
                GhostMascotView(poseName: "wave").frame(width: 80, height: 80)
                Text("A private gift is waiting.")
                    .font(.title2.weight(.black))
                    .multilineTextAlignment(.center)
                Text("The product stays hidden until you choose to reveal it. Ghost Cart will not identify you to the sender.")
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(24)
            .background(Color.inkColor, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .foregroundStyle(.white)

            Button {
                acceptedPrivacy.toggle()
            } label: {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: acceptedPrivacy ? "checkmark.square.fill" : "square")
                        .foregroundStyle(acceptedPrivacy ? Color.ghostGreenColor : Color.secondary)
                    Text("I understand this is a simulation-only gift. Revealing it records that this private invitation was opened; it creates no purchase, payment, or delivery.")
                        .font(.caption)
                        .foregroundStyle(Color.primary)
                        .multilineTextAlignment(.leading)
                }
            }
            .buttonStyle(.plain)

            if let errorMessage {
                Text(errorMessage).font(.caption.weight(.bold)).foregroundStyle(Color.red)
            }

            Button {
                Task { await reveal() }
            } label: {
                if isRevealing {
                    SwiftUI.ProgressView().tint(Color.inkColor)
                } else {
                    Text("Reveal gift")
                }
            }
            .buttonStyle(GhostPrimaryButtonStyle())
            .disabled(isRevealing)
        }
    }

    private func revealedContent(_ gift: RevealedGhostGift) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("\(gift.senderName) thought of you.")
                .font(.subheadline.weight(.black))
                .foregroundStyle(Color.ghostGreenColor)

            ProductThumbnail(imageURL: gift.imageURL, systemImage: AlmostBuyCategory(serverName: gift.category).systemImage, size: 300, cornerRadius: 24)
                .frame(maxWidth: .infinity)

            Text(gift.category).font(.caption.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            Text(gift.title).font(.title2.weight(.black))
            if gift.amountCents > 0 {
                DirhamAmount(value: gift.amount, font: .title3.weight(.black), iconSize: 15)
            }

            GhostCard {
                VStack(alignment: .leading, spacing: 6) {
                    Text("A thought, not a purchase").font(.subheadline.weight(.bold))
                    Text(gift.disclosure).font(.caption).foregroundStyle(Color.secondary)
                }
            }

            if didGhostIt {
                Label("Added to your cooldowns", systemImage: "checkmark.circle.fill")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.ghostGreenColor)
            } else {
                Button("Ghost it too — start 24-hour cooldown") {
                    store.ghostRevealedGift(gift)
                    didGhostIt = true
                }
                .buttonStyle(GhostPrimaryButtonStyle())
            }

            Text("The sender can see that the invitation was revealed, but not what you decide next.")
                .font(.caption2)
                .foregroundStyle(Color.secondary)
        }
    }

    private func reveal() async {
        guard acceptedPrivacy else {
            errorMessage = "Accept the privacy notice to continue."
            return
        }
        isRevealing = true
        errorMessage = nil
        defer { isRevealing = false }
        do {
            revealed = try await GhostGiftService.reveal(token: token)
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Could not reveal this gift."
        }
    }
}
