import SwiftUI

struct ProgressView: View {
    @EnvironmentObject private var store: GhostCartStore
    @AppStorage("ghostcart.wallet.simulated-balance") private var simulatedBalance = 10_000
    @State private var showAddBalance = false

    private var topTriggers: [(SpendingTrigger, Int)] {
        let grouped = Dictionary(grouping: store.items, by: \.trigger)
        return grouped.map { ($0.key, $0.value.count) }.sorted { $0.1 > $1.1 }
    }

    private var resolvedCount: Int {
        store.items.filter { $0.state.isResolved }.count
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                ScreenHeader(
                    eyebrow: "Simulation only",
                    title: "Ghost Wallet",
                    subtitle: "Your simulated balance, membership card and decision progress, together."
                )

                VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("SIMULATED BALANCE")
                                .font(.caption2.weight(.black))
                                .tracking(1)
                                .foregroundStyle(Color.white.opacity(0.65))
                            Spacer()
                            Text("DEMO WALLET")
                                .font(.system(size: 9, weight: .black))
                                .foregroundStyle(Color.inkColor)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.ghostGreenColor, in: Capsule())
                        }
                        Text(AmountFormatter.string(Double(simulatedBalance)))
                            .font(.system(size: 36, weight: .black, design: .rounded))
                            .foregroundStyle(Color.ghostGreenColor)
                            .minimumScaleFactor(0.7)
                            .lineLimit(1)
                        Text("Internal Ghost Cart credit only. No real money is stored.")
                            .font(.caption)
                            .foregroundStyle(Color.white.opacity(0.65))
                        Button {
                            showAddBalance = true
                        } label: {
                            Label("Add simulated balance", systemImage: "plus")
                        }
                        .buttonStyle(GhostPrimaryButtonStyle())
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.inkColor)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

                GhostWalletMembershipCard(profile: store.membership)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ProgressMetricCard(
                        title: "Money Kept",
                        value: store.progress.moneyKept,
                        note: "Confirmed skipped",
                        accent: true
                    )
                    ProgressMetricCard(
                        title: "Almost Spent",
                        value: store.progress.almostSpent,
                        note: "All captured value"
                    )
                    ProgressMetricCard(
                        title: "Cooling",
                        value: store.progress.cooling,
                        note: "Awaiting a decision"
                    )
                    ProgressMetricCard(
                        title: "Bought Intentionally",
                        value: store.progress.boughtIntentionally,
                        note: "Reflected, then bought"
                    )
                }

                GhostCard {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Decision health").font(.headline.weight(.bold))
                            Spacer()
                            Text("\(resolvedCount) resolved")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color.ghostGreenColor)
                        }
                        if resolvedCount == 0 {
                            Text("Resolve your first cooldown to see an honest outcome pattern.")
                                .font(.subheadline)
                                .foregroundStyle(Color.secondary)
                        } else {
                            let skipped = store.items.filter { $0.state == .resolvedSkipped }.count
                            let bought = store.items.filter { $0.state == .resolvedBought }.count
                            OutcomeBar(skipped: skipped, bought: bought)
                            HStack {
                                Label("\(skipped) skipped", systemImage: "checkmark")
                                Spacer()
                                Label("\(bought) bought intentionally", systemImage: "checkmark.seal")
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color.secondary)
                        }
                    }
                }

                if !topTriggers.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeading(title: "What tends to trigger you")
                        ForEach(Array(topTriggers.prefix(4)), id: \.0) { trigger, count in
                            HStack {
                                Text(trigger.title).font(.subheadline.weight(.semibold))
                                Spacer()
                                Text("\(count) item\(count == 1 ? "" : "s")")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(Color.secondary)
                            }
                            .padding(.horizontal, 15)
                            .padding(.vertical, 13)
                            .background(Color.primary.opacity(0.04))
                            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(title: "Ghost Receipt history")
                    if store.recentDecisions.isEmpty {
                        EmptyStateView(
                            image: "doc.text",
                            title: "No decisions yet",
                            message: "A Ghost Receipt appears only after you resolve an almost-buy."
                        )
                    } else {
                        ForEach(store.recentDecisions) { item in
                            NavigationLink {
                                GhostReceiptDetailView(item: item)
                            } label: {
                                HStack(spacing: 13) {
                                    Image(systemName: "doc.text")
                                        .foregroundStyle(Color.ghostGreenColor)
                                        .frame(width: 40, height: 40)
                                        .background(Color.ghostGreenColor.opacity(0.12))
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(item.name)
                                            .font(.subheadline.weight(.bold))
                                            .foregroundStyle(Color.primary)
                                        Text("\(item.state.title) · \((item.resolvedAt ?? item.capturedAt).formatted(date: .abbreviated, time: .omitted))")
                                            .font(.caption)
                                            .foregroundStyle(Color.secondary)
                                    }
                                    Spacer()
                                    Text(AmountFormatter.string(item.amount))
                                        .font(.subheadline.weight(.black))
                                        .foregroundStyle(item.state == .resolvedSkipped ? Color.ghostGreenColor : Color.primary)
                                    Image(systemName: "chevron.right")
                                        .font(.caption.weight(.bold))
                                        .foregroundStyle(Color.secondary)
                                }
                                .padding(14)
                                .background(Color.primary.opacity(0.04))
                                .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Text("Money Kept is the listed value of items you confirmed you skipped. It is not cash or a bank balance.")
                    .font(.caption2)
                    .foregroundStyle(Color.secondary)

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 112)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .onAppear { GhostAnalytics.walletOpened() }
        .sheet(isPresented: $showAddBalance) {
            AddSimulatedBalanceView { amount in
                simulatedBalance = min(simulatedBalance + amount, 1_000_000)
                showAddBalance = false
            }
            .presentationDetents([.medium])
        }
    }
}

private struct GhostWalletMembershipCard: View {
    let profile: MembershipProfile

    var body: some View {
        HStack(spacing: 14) {
            GhostMascotView(poseName: "wallet")
                .frame(width: 70, height: 70)
            VStack(alignment: .leading, spacing: 6) {
                Text("GHOST MEMBERSHIP").font(.caption2.weight(.black)).foregroundStyle(Color.white.opacity(0.62))
                Text(profile.displayName).font(.headline.weight(.black)).foregroundStyle(Color.white)
                Text("Ghost ID  \(profile.ghostID)").font(.caption2.monospaced()).foregroundStyle(Color.white.opacity(0.62))
                Text("Not a payment card").font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            }
            Spacer()
        }
        .padding(18)
        .background(Color.inkColor)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct AddSimulatedBalanceView: View {
    let onAdd: (Int) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var amount = 500

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Capsule().fill(Color.secondary.opacity(0.3)).frame(width: 38, height: 5).frame(maxWidth: .infinity)
            Text("Add simulated balance").font(.title2.weight(.black))
            Text("This changes demo credit only. It does not add, collect, or move real money.")
                .font(.subheadline).foregroundStyle(Color.secondary)
            Picker("Amount", selection: $amount) {
                ForEach([100, 500, 1_000, 5_000], id: \.self) { value in
                    Text(AmountFormatter.string(Double(value))).tag(value)
                }
            }
            .pickerStyle(.segmented)
            Button("Add \(AmountFormatter.string(Double(amount)))", action: { onAdd(amount) })
                .buttonStyle(GhostPrimaryButtonStyle())
            Button("Cancel") { dismiss() }.frame(maxWidth: .infinity)
        }
        .padding(22)
    }
}

private struct ProgressMetricCard: View {
    let title: String
    let value: Double
    let note: String
    var accent = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.secondary)
            Text(AmountFormatter.string(value))
                .font(.title2.weight(.black))
                .foregroundStyle(accent ? Color.ghostGreenColor : Color.primary)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text("UAE dirhams")
                .font(.caption2)
                .foregroundStyle(Color.secondary)
            Text(note)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(Color.secondary)
                .padding(.top, 3)
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 132, alignment: .leading)
        .background(accent ? Color.ghostGreenColor.opacity(0.12) : Color.primary.opacity(0.045))
        .clipShape(RoundedRectangle(cornerRadius: 19, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 19, style: .continuous)
                .stroke(Color.primary.opacity(0.08), lineWidth: 1)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct OutcomeBar: View {
    let skipped: Int
    let bought: Int

    private var skippedFraction: CGFloat {
        let total = max(skipped + bought, 1)
        return CGFloat(skipped) / CGFloat(total)
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.primary.opacity(0.10))
                Capsule()
                    .fill(Color.ghostGreenColor)
                    .frame(width: geometry.size.width * skippedFraction)
            }
        }
        .frame(height: 10)
        .accessibilityLabel("\(skipped) skipped and \(bought) bought intentionally")
    }
}

struct GhostReceiptDetailView: View {
    let item: AlmostBuy

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                GhostMascotView(poseName: item.state == .resolvedSkipped ? "thumbsup" : "wave")
                    .frame(width: 104, height: 104)
                    .accessibilityHidden(true)

                VStack(spacing: 8) {
                    Text("GHOST RECEIPT")
                        .font(.caption2.weight(.black))
                        .tracking(1.2)
                        .foregroundStyle(Color.ghostGreenColor)
                    Text(item.state == .resolvedSkipped ? "You skipped it." : "You decided intentionally.")
                        .font(.largeTitle.weight(.black))
                        .multilineTextAlignment(.center)
                    Text(item.name)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.secondary)
                }

                GhostCard {
                    VStack(spacing: 14) {
                        ReceiptRow(label: "Outcome", value: item.state.title)
                        Divider()
                        ReceiptRow(label: "Amount", value: "\(AmountFormatter.string(item.amount)) UAE dirhams")
                        Divider()
                        ReceiptRow(label: "Trigger", value: item.trigger.title)
                        Divider()
                        ReceiptRow(label: "Captured", value: item.capturedAt.formatted(date: .abbreviated, time: .shortened))
                    }
                }

                Text(item.state == .resolvedSkipped
                     ? "This amount contributes to Money Kept because you confirmed that you skipped the purchase."
                     : "This amount does not contribute to Money Kept. Ghost Cart records the reflection without judging the decision.")
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)

                SimulationDisclosure()
            }
            .padding(20)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Ghost Receipt")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct ReceiptRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(label).foregroundStyle(Color.secondary)
            Spacer()
            Text(value)
                .fontWeight(.semibold)
                .multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
    }
}
