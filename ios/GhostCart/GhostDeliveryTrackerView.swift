import SwiftUI
import CoreLocation

// The "Ghost Delivery · Simulation" tracker: recomputes the delivery stage
// from item.coolingStartedAt/decisionAt on every periodic tick rather than
// running its own timer/animation loop, so it always shows the correct
// stage immediately on appear (including after backgrounding, relaunch, or
// a timezone change) and only re-renders every few seconds.
struct GhostDeliveryTrackerView: View {
    let itemID: UUID
    let onClose: () -> Void
    // Tutorial support: when set, the tracker renders this ephemeral value
    // directly instead of looking the id up in GhostCartStore - the
    // tutorial's practice item never enters store.items, so it can never
    // reach Wallet, Leaderboard, or the backend sync path. onTutorialDecision
    // is forwarded to the decision screen so the coordinator (not the store)
    // handles what happens next.
    var previewItem: AlmostBuy? = nil
    var onTutorialDecision: ((String) -> Void)? = nil
    var refreshInterval: TimeInterval = 5

    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dismiss) private var dismiss

    private var item: AlmostBuy? { previewItem ?? store.items.first { $0.id == itemID } }

    var body: some View {
        Group {
            if let item {
                content(for: item)
            } else {
                EmptyStateView(image: "shippingbox", title: "Ghost Order not found", message: "This item may have been removed.")
            }
        }
        .background(Color(.systemBackground))
        .onAppear { GhostAnalytics.trackerOpened() }
    }

    @ViewBuilder
    private func content(for item: AlmostBuy) -> some View {
        // Ticking every 5s is plenty for a delivery that spans 15 minutes to
        // a week - this is display refresh only, not a scheduling mechanism.
        // The tutorial passes a much shorter refreshInterval so its ~12s
        // accelerated delivery still animates smoothly.
        TimelineView(.periodic(from: .now, by: refreshInterval)) { context in
            let stage = item.deliveryStage(at: context.date)
            if stage == .delivered || item.state.isResolved {
                GhostDeliveryDecisionView(
                    itemID: item.id,
                    onClose: onClose,
                    previewItem: previewItem,
                    onTutorialDecision: onTutorialDecision
                )
            } else if let stage {
                trackerBody(item: item, stage: stage, now: context.date)
            } else {
                EmptyStateView(image: "shippingbox", title: "Not cooling yet", message: "Start a Ghost Delivery from Ghost + to track it here.")
            }
        }
    }

    private func trackerBody(item: AlmostBuy, stage: DeliveryStage, now: Date) -> some View {
        let route = GhostRouteGenerator.route(for: item.id)
        let fraction = item.deliveryFraction(at: now)
        let riderPosition = GhostRouteGenerator.riderPosition(waypoints: route.waypoints, fraction: fraction)

        return ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header

                HStack(spacing: 12) {
                    ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 58, cornerRadius: 14)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(item.name).font(.headline.weight(.bold)).lineLimit(2)
                        DirhamAmount(value: item.amount, font: .subheadline.weight(.bold), iconSize: 11, color: Color.secondary)
                    }
                    Spacer()
                }

                Text(stage.title)
                    .font(.title2.weight(.black))
                Text(stage.timelineSubtitle)
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)

                mapCard(route: route, riderPosition: riderPosition, fraction: fraction)

                if let decisionAt = item.decisionAt {
                    HStack {
                        Image(systemName: "clock")
                        Text("Expected \(decisionAt.formatted(date: .abbreviated, time: .shortened))")
                            .font(.caption.weight(.bold))
                        Spacer()
                    }
                    .foregroundStyle(Color.secondary)
                }

                timeline(currentStage: stage)

                if let sourceURL = item.sourceURL, let url = URL(string: sourceURL) {
                    Link(destination: url) {
                        Label("View original product", systemImage: "link")
                            .font(.caption.weight(.bold))
                    }
                }

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button(action: onClose) { Image(systemName: "chevron.left") }
            VStack(alignment: .leading, spacing: 1) {
                Text("Ghost Delivery").font(.headline.weight(.black))
                Text("Simulation").font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            }
            Spacer()
        }
    }

    private func mapCard(
        route: (origin: CLLocationCoordinate2D, destination: CLLocationCoordinate2D, waypoints: [CLLocationCoordinate2D]),
        riderPosition: CLLocationCoordinate2D,
        fraction: Double
    ) -> some View {
        GhostRouteMapView(
            origin: route.origin,
            destination: route.destination,
            waypoints: route.waypoints,
            riderPosition: riderPosition,
            reduceMotion: reduceMotion
        )
        .frame(height: 260)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(alignment: .topLeading) {
            Text("No real product or rider is involved.")
                .font(.caption2.weight(.bold))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(10)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Simulated Ghost Rider map, \(Int(fraction * 100)) percent of the way to delivery")
    }

    private func timeline(currentStage: DeliveryStage) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            ForEach(DeliveryStage.allCases) { stage in
                let reached = stage.rawValue <= currentStage.rawValue
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: reached ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(reached ? Color.ghostGreenColor : Color.secondary.opacity(0.4))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(stage.title).font(.caption.weight(.bold))
                        Text(stage.timelineSubtitle).font(.caption2).foregroundStyle(Color.secondary)
                    }
                    Spacer()
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("\(stage.title), \(reached ? "reached" : "not yet reached")")
            }
        }
    }
}
