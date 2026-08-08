import SwiftUI

struct CooldownsView: View {
    @EnvironmentObject private var store: GhostCartStore
    @State private var deleteTarget: AlmostBuy?
    @State private var trackingItemID: UUID?

    private var expiredItems: [AlmostBuy] {
        store.items.filter { $0.state == .expired }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                ScreenHeader(
                    eyebrow: "Decide intentionally",
                    title: "Cooldowns",
                    subtitle: "Waiting is part of the decision. Nothing here counts as Money Kept until you confirm that you skipped it."
                )

                if store.readyItems.isEmpty && store.coolingItems.isEmpty && store.capturedItems.isEmpty && expiredItems.isEmpty {
                    EmptyStateView(
                        image: "timer",
                        title: "No active almost-buys",
                        message: "Capture something from the Ghost + tab when the next temptation appears."
                    )
                }

                if !store.readyItems.isEmpty {
                    CooldownSection(title: "Ready to decide", count: store.readyItems.count) {
                        ForEach(store.readyItems) { item in
                            ReadyDecisionCard(item: item, onOpenDecision: { trackingItemID = item.id })
                        }
                    }
                }

                if !store.coolingItems.isEmpty {
                    CooldownSection(title: "Cooling now", count: store.coolingItems.count) {
                        ForEach(store.coolingItems) { item in
                            CoolingCard(item: item, onDelete: { deleteTarget = item }, onTrack: { trackingItemID = item.id })
                        }
                    }
                }

                if !store.capturedItems.isEmpty {
                    CooldownSection(title: "Captured, not cooling", count: store.capturedItems.count) {
                        ForEach(store.capturedItems) { item in
                            CapturedCard(
                                item: item,
                                onStart: {
                                    store.startCooling(id: item.id, minutes: item.category.recommendedCooldownMinutes)
                                },
                                onDelete: { deleteTarget = item }
                            )
                        }
                    }
                }

                if !expiredItems.isEmpty {
                    CooldownSection(title: "Expired", count: expiredItems.count) {
                        ForEach(expiredItems) { item in
                            GhostCard {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(item.name).font(.subheadline.weight(.bold))
                                        Text("No decision was recorded")
                                            .font(.caption)
                                            .foregroundStyle(Color.secondary)
                                    }
                                    Spacer()
                                    Button("Remove", role: .destructive) { deleteTarget = item }
                                        .font(.caption.weight(.bold))
                                }
                            }
                        }
                    }
                }

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        // Had no pull-to-refresh at all - the one screen a genuinely new
        // server-side item (share-extension mini-capture, another device)
        // needs to show up on, with no way to ask for fresh data short of
        // backgrounding/reopening the whole app.
        .refreshable { await store.syncFromServer() }
        .fullScreenCover(isPresented: Binding(
            get: { trackingItemID != nil },
            set: { if !$0 { trackingItemID = nil } }
        )) {
            if let trackingItemID {
                NavigationStack {
                    GhostDeliveryTrackerView(itemID: trackingItemID, onClose: { self.trackingItemID = nil })
                }
            }
        }
        .confirmationDialog(
            "Remove this almost-buy?",
            isPresented: Binding(
                get: { deleteTarget != nil },
                set: { if !$0 { deleteTarget = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Remove", role: .destructive) {
                if let deleteTarget { store.delete(id: deleteTarget.id) }
                deleteTarget = nil
            }
            Button("Cancel", role: .cancel) { deleteTarget = nil }
        }
    }
}

private struct CooldownSection<Content: View>: View {
    let title: String
    let count: Int
    let content: Content

    init(title: String, count: Int, @ViewBuilder content: () -> Content) {
        self.title = title
        self.count = count
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title).font(.title3.weight(.bold))
                Text("\(count)")
                    .font(.caption.weight(.black))
                    .foregroundStyle(Color.inkColor)
                    .frame(minWidth: 24, minHeight: 24)
                    .background(Color.ghostGreenColor)
                    .clipShape(Circle())
            }
            content
        }
    }
}

private struct ReadyDecisionCard: View {
    let item: AlmostBuy
    let onOpenDecision: () -> Void

    var body: some View {
        GhostCard {
            VStack(alignment: .leading, spacing: 15) {
                ItemSummary(item: item, stateText: "Delivered", accent: true)
                Text("Your decision has arrived")
                    .font(.headline.weight(.bold))
                Button("Skip, buy, or decide", action: onOpenDecision)
                    .buttonStyle(GhostPrimaryButtonStyle())
                Text("Only a confirmed skip contributes to Money Kept.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
            }
        }
    }
}

private struct CoolingCard: View {
    let item: AlmostBuy
    let onDelete: () -> Void
    let onTrack: () -> Void

    var body: some View {
        GhostCard {
            VStack(alignment: .leading, spacing: 12) {
                ItemSummary(item: item, stateText: item.state.title, accent: false)
                if let decisionAt = item.decisionAt {
                    TimelineView(.periodic(from: .now, by: 60)) { context in
                        Text(decisionAt > context.date ? "Ghost Delivery arrives \(decisionAt.relativeDescription(from: context.date))" : "Ready now")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(decisionAt > context.date ? Color.secondary : Color.ghostGreenColor)
                    }
                }
                Button("Track Ghost Delivery", action: onTrack)
                    .buttonStyle(GhostSecondaryButtonStyle())
                Button("Remove almost-buy", role: .destructive, action: onDelete)
                    .font(.caption.weight(.bold))
            }
        }
    }
}

private struct CapturedCard: View {
    let item: AlmostBuy
    let onStart: () -> Void
    let onDelete: () -> Void

    var body: some View {
        GhostCard {
            VStack(alignment: .leading, spacing: 13) {
                ItemSummary(item: item, stateText: "Waiting for a cooling period", accent: false)
                Button("Start recommended cooldown", action: onStart)
                    .buttonStyle(GhostPrimaryButtonStyle())
                HStack {
                    Text(recommendedCooldownText(for: item.category))
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                    Spacer()
                    Button("Remove", role: .destructive, action: onDelete)
                        .font(.caption.weight(.bold))
                }
            }
        }
    }

    private func recommendedCooldownText(for category: AlmostBuyCategory) -> String {
        let minutes = category.recommendedCooldownMinutes
        if minutes < 60 { return "Recommended: \(minutes) minutes" }
        if minutes < 24 * 60 { return "Recommended: \(minutes / 60) hours" }
        return "Recommended: \(minutes / (24 * 60)) days"
    }
}

private struct ItemSummary: View {
    let item: AlmostBuy
    let stateText: String
    let accent: Bool

    var body: some View {
        HStack(spacing: 13) {
            ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 46)
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name).font(.headline.weight(.bold))
                Text("\(item.category.title) · \(stateText)")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(AmountFormatter.string(item.amount))
                    .font(.headline.weight(.black))
                Text("UAE dirhams")
                    .font(.caption2)
                    .foregroundStyle(Color.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
