import SwiftUI

// Mirrors Android's GiftsScreen: a Received/Sent segmented list backed by
// GET /api/ghost-gifts. No withdraw action is exposed here, matching
// Android's own UI (the withdraw endpoint exists server-side but no Android
// screen calls it either).
struct GiftsView: View {
    private enum Tab { case received, sent }

    @State private var tab: Tab = .received
    @State private var received: [GiftListItem] = []
    @State private var sent: [GiftListItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                ScreenHeader(
                    eyebrow: "Gifts",
                    title: "Gifts",
                    subtitle: "Private gifts you received and simulated gifts you sent."
                )

                Picker("", selection: $tab) {
                    Text("Received (\(received.count))").tag(Tab.received)
                    Text("Sent (\(sent.count))").tag(Tab.sent)
                }
                .pickerStyle(.segmented)

                if isLoading {
                    SwiftUI.ProgressView().frame(maxWidth: .infinity).padding(.top, 40)
                } else if let errorMessage {
                    Text(errorMessage).font(.caption).foregroundStyle(Color.red)
                } else {
                    content
                }
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Gifts")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    @ViewBuilder
    private var content: some View {
        switch tab {
        case .received:
            if received.isEmpty {
                EmptyStateView(
                    image: "gift",
                    title: "No revealed gifts yet",
                    message: "Open a gift from its private email link to add it here."
                )
            } else {
                ForEach(received) { item in giftRow(item, showSender: true) }
            }
        case .sent:
            if sent.isEmpty {
                EmptyStateView(
                    image: "gift",
                    title: "No sent gifts yet",
                    message: "Choose \"Send as a gift\" during Fake Checkout."
                )
            } else {
                ForEach(sent) { item in giftRow(item, showSender: false) }
            }
        }
    }

    private func giftRow(_ item: GiftListItem, showSender: Bool) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ProductThumbnail(imageURL: item.imageURL, systemImage: "gift", size: 76, cornerRadius: 16)
            VStack(alignment: .leading, spacing: 4) {
                Text(showSender ? "From \(item.senderName ?? "a Ghost Cart member")" : item.status.capitalized)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.ghostGreenColor)
                Text(item.itemTitle).font(.subheadline.weight(.bold)).lineLimit(2)
                Text(item.category).font(.caption).foregroundStyle(Color.secondary)
                if item.amountCents > 0 {
                    DirhamAmount(value: item.amount, font: .caption.weight(.bold), iconSize: 9, color: Color.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let lists = try await GhostGiftService.list()
            sent = lists.sent
            received = lists.received
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Unable to load gifts."
        }
        isLoading = false
    }
}
