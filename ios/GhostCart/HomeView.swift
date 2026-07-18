import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: GhostCartStore
    @StateObject private var communityFeed = CommunityFeedModel()
    let onGhostSomething: () -> Void
    let onViewCooldowns: () -> Void

    private var nextItems: [AlmostBuy] {
        Array((store.readyItems + store.coolingItems).prefix(3))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack {
                    GhostCartLogoView()
                        .frame(width: 142, height: 42)
                        .padding(.horizontal, 10)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    Spacer()
                    if !store.readyItems.isEmpty {
                        Button(action: onViewCooldowns) {
                            Label("\(store.readyItems.count) ready", systemImage: "bell.badge.fill")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color.inkColor)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 9)
                                .background(Color.ghostGreenColor)
                                .clipShape(Capsule())
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 18) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 9) {
                            Text("BEFORE YOU SPEND")
                                .font(.caption2.weight(.black))
                                .tracking(1.2)
                                .foregroundStyle(Color.ghostGreenColor)
                            Text("Give the craving a place to cool.")
                                .font(.system(size: 34, weight: .black, design: .rounded))
                                .foregroundStyle(Color.white)
                                .fixedSize(horizontal: false, vertical: true)
                            Text("Capture anything from anywhere, wait, then decide intentionally.")
                                .font(.subheadline)
                                .foregroundStyle(Color.white.opacity(0.68))
                        }
                        Spacer(minLength: 8)
                        GhostMascotView(poseName: "wave")
                            .frame(width: 72, height: 72)
                            .accessibilityHidden(true)
                    }

                    Button(action: onGhostSomething) {
                        Label("Ghost something", systemImage: "plus")
                    }
                    .buttonStyle(GhostPrimaryButtonStyle())

                    Text("No account details or purchase information are required.")
                        .font(.caption)
                        .foregroundStyle(Color.white.opacity(0.55))
                }
                .padding(22)
                .background(Color.inkColor)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

                HonestProgressStrip(snapshot: store.progress)

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(
                        title: store.readyItems.isEmpty ? "Active cooldowns" : "Ready to decide",
                        actionTitle: nextItems.isEmpty ? nil : "View all",
                        action: nextItems.isEmpty ? nil : onViewCooldowns
                    )

                    if nextItems.isEmpty {
                        EmptyStateView(
                            image: "timer",
                            title: "Nothing is waiting",
                            message: "Your next almost-buy can cool here instead of sitting in a real cart."
                        )
                    } else {
                        ForEach(nextItems) { item in
                            HomeCooldownRow(item: item, onOpen: onViewCooldowns)
                        }
                    }
                }

                if !store.recentDecisions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeading(title: "Recent decisions")
                        ForEach(store.recentDecisions.prefix(3)) { item in
                            HStack(spacing: 12) {
                                Image(systemName: item.state == .resolvedSkipped ? "checkmark" : "checkmark.seal")
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(item.state == .resolvedSkipped ? Color.ghostGreenColor : Color.secondary)
                                    .frame(width: 38, height: 38)
                                    .background(Color.primary.opacity(0.05))
                                    .clipShape(Circle())
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.name).font(.subheadline.weight(.bold))
                                    Text(item.state.title)
                                        .font(.caption)
                                        .foregroundStyle(Color.secondary)
                                }
                                Spacer()
                                Text(AmountFormatter.string(item.amount))
                                    .font(.subheadline.weight(.bold))
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }
                }

                CommunityShelf(feed: communityFeed, onCoolIt: coolCommunityItem)

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .onAppear { communityFeed.loadIfNeeded() }
        .refreshable { await communityFeed.reload() }
    }

    private func coolCommunityItem(_ product: CommunityProduct) {
        store.stageCapture(
            CaptureSeed(
                name: product.title,
                amount: product.amount > 0 ? product.amount : nil,
                category: AlmostBuyCategory(serverName: product.category),
                sourceURL: product.sourceURL,
                imageURL: product.imageURL,
                sourceDomain: product.sourceDomain,
                retailer: nil,
                note: nil,
                offerCommunityShare: false
            )
        )
        onGhostSomething()
    }
}

private struct CommunityShelf: View {
    @ObservedObject var feed: CommunityFeedModel
    let onCoolIt: (CommunityProduct) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "User Ghosted")
            Text("Public retailer links others chose to cool. Everyone here is anonymous, and nothing shows who shared it.")
                .font(.caption)
                .foregroundStyle(Color.secondary)

            switch feed.state {
            case .idle, .loading:
                GhostCard {
                    HStack(spacing: 12) {
                        SwiftUI.ProgressView().tint(Color.ghostGreenColor)
                        Text("Loading community items...")
                            .font(.caption)
                            .foregroundStyle(Color.secondary)
                    }
                }
            case .failed:
                GhostCard {
                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: "person.3.sequence.fill")
                            .font(.title2)
                            .foregroundStyle(Color.ghostGreenColor)
                        VStack(alignment: .leading, spacing: 5) {
                            Text("Community items are resting")
                                .font(.subheadline.weight(.bold))
                            Text("They appear once enough people validate Ghost actions. Try again in a moment.")
                                .font(.caption)
                                .foregroundStyle(Color.secondary)
                        }
                    }
                }
            case .loaded(let products) where products.isEmpty:
                GhostCard {
                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: "person.3.sequence.fill")
                            .font(.title2)
                            .foregroundStyle(Color.ghostGreenColor)
                        VStack(alignment: .leading, spacing: 5) {
                            Text("No community items yet")
                                .font(.subheadline.weight(.bold))
                            Text("Be the first to cool one. Share anonymously from Ghost + to seed the shelf.")
                                .font(.caption)
                                .foregroundStyle(Color.secondary)
                        }
                    }
                }
            case .loaded(let products):
                ForEach(products) { product in
                    CommunityShelfRow(product: product, onCoolIt: onCoolIt)
                }
            }
        }
    }
}

private struct CommunityShelfRow: View {
    let product: CommunityProduct
    let onCoolIt: (CommunityProduct) -> Void

    var body: some View {
        HStack(spacing: 14) {
            // Community images come from arbitrary remote retailer hosts. Until
            // a Ghost Cart-controlled image proxy exists, we intentionally show
            // a category glyph instead of loading untrusted remote artwork.
            Image(systemName: AlmostBuyCategory(serverName: product.category).systemImage)
                .font(.headline)
                .foregroundStyle(Color.ghostGreenColor)
                .frame(width: 44, height: 44)
                .background(Color.ghostGreenColor.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(product.title)
                    .font(.subheadline.weight(.bold))
                    .lineLimit(2)
                HStack(spacing: 8) {
                    if product.priceCents > 0 {
                        Text(AmountFormatter.string(product.amount) + " UAE dirham")
                    }
                    Text(product.activityTag)
                }
                .font(.caption2)
                .foregroundStyle(Color.secondary)
            }
            Spacer(minLength: 8)
            Button("Cool it") { onCoolIt(product) }
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.inkColor)
                .padding(.horizontal, 14)
                .padding(.vertical, 9)
                .background(Color.ghostGreenColor)
                .clipShape(Capsule())
        }
        .padding(14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

private struct HonestProgressStrip: View {
    let snapshot: ProgressSnapshot

    var body: some View {
        HStack(spacing: 0) {
            ProgressMiniMetric(title: "Almost spent", value: snapshot.almostSpent)
            Divider().frame(height: 44)
            ProgressMiniMetric(title: "Cooling", value: snapshot.cooling)
            Divider().frame(height: 44)
            ProgressMiniMetric(title: "Money kept", value: snapshot.moneyKept, accent: true)
        }
        .padding(.vertical, 14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Progress in UAE dirhams. Almost spent \(AmountFormatter.string(snapshot.almostSpent)), cooling \(AmountFormatter.string(snapshot.cooling)), money kept \(AmountFormatter.string(snapshot.moneyKept))")
    }
}

private struct ProgressMiniMetric: View {
    let title: String
    let value: Double
    var accent = false

    var body: some View {
        VStack(spacing: 4) {
            Text(AmountFormatter.string(value))
                .font(.subheadline.weight(.black))
                .foregroundStyle(accent ? Color.ghostGreenColor : Color.primary)
            Text(title)
                .font(.caption2)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct HomeCooldownRow: View {
    let item: AlmostBuy
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 14) {
                Image(systemName: item.category.systemImage)
                    .font(.headline)
                    .foregroundStyle(Color.ghostGreenColor)
                    .frame(width: 42, height: 42)
                    .background(Color.ghostGreenColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.primary)
                    Text(item.isReady() ? "Ready for your decision" : "Decide \(item.decisionAt?.compactDayAndTime ?? "later")")
                        .font(.caption)
                        .foregroundStyle(item.isReady() ? Color.ghostGreenColor : Color.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.secondary)
            }
            .padding(14)
            .background(Color.primary.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
