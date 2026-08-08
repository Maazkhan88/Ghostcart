import SwiftUI
#if os(macOS)
import AppKit
#endif

// The delivered decision screen: shown once a Ghost Delivery reaches 100%.
// Action hierarchy is intentionally weighted, not five equal buttons -
// Skip is the primary filled action, Send it around again is secondary
// outlined, Buy from source is a neutral link-style action, and Bought
// already / Ask a friend / View product live under "More options".
struct GhostDeliveryDecisionView: View {
    let itemID: UUID
    let onClose: () -> Void
    // Tutorial support - see GhostDeliveryTrackerView. When previewItem is
    // set, every action below reports to onTutorialDecision instead of
    // touching GhostCartStore, so the practice item can never affect Money
    // Kept, Wallet, Leaderboard, or sync to the backend.
    var previewItem: AlmostBuy? = nil
    var onTutorialDecision: ((String) -> Void)? = nil

    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.openURL) private var openURL
    @State private var showSkipConfirm = false
    @State private var showRestartPicker = false
    @State private var showBuyFromSourceDisclosure = false
    @State private var showBoughtAlreadyConfirm = false
    @State private var showMoreOptions = false
    @State private var shareItems: [Any]?

    private var isTutorial: Bool { previewItem != nil }
    private var item: AlmostBuy? { previewItem ?? store.items.first { $0.id == itemID } }

    var body: some View {
        ScrollView {
            if let item {
                VStack(alignment: .leading, spacing: 20) {
                    header

                    ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 96, cornerRadius: 20)
                        .frame(maxWidth: .infinity)

                    VStack(spacing: 6) {
                        Text("Your decision has arrived")
                            .font(.title2.weight(.black))
                        if let waited = waitedLabel(item) {
                            Text("You waited \(waited). What would you like to do?")
                                .font(.subheadline)
                                .foregroundStyle(Color.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)

                    GhostCard {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(item.name).font(.headline.weight(.bold))
                            DirhamAmount(value: item.amount, font: .subheadline.weight(.bold), iconSize: 11, color: Color.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    VStack(spacing: 11) {
                        Button("Skip the purchase") { showSkipConfirm = true }
                            .buttonStyle(GhostPrimaryButtonStyle())

                        Button("Send it around again") { showRestartPicker = true }
                            .buttonStyle(GhostSecondaryButtonStyle())

                        Button("Buy from source") { showBuyFromSourceDisclosure = true }
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color.primary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .disabled(item.sourceURL == nil)
                            .opacity(item.sourceURL == nil ? 0.4 : 1)

                        Button(showMoreOptions ? "Fewer options" : "More options") {
                            withAnimation { showMoreOptions.toggle() }
                        }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.secondary)
                    }

                    if showMoreOptions {
                        VStack(spacing: 10) {
                            Button("I bought it already") { showBoughtAlreadyConfirm = true }
                                .buttonStyle(GhostSecondaryButtonStyle())
                            Button("Ask a friend") { shareItems = askFriendItems(item) }
                                .buttonStyle(GhostSecondaryButtonStyle())
                            if let sourceURL = item.sourceURL, let url = URL(string: sourceURL) {
                                Link(destination: url) {
                                    Text("View product details").frame(maxWidth: .infinity)
                                }
                                .buttonStyle(GhostSecondaryButtonStyle())
                            }
                        }
                        .transition(.opacity)
                    }

                    Text("Only \"Skip the purchase\" contributes to Money Kept.")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    SimulationDisclosure()
                }
                .padding(20)
                .padding(.bottom, 24)
            }
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .onAppear { GhostAnalytics.ghostOrderDelivered() }
        .alert("Skip this purchase?", isPresented: $showSkipConfirm) {
            Button("Skip it", role: .none) {
                store.resolve(id: itemID, outcome: .resolvedSkipped)
                GhostAnalytics.decisionSkipped()
                onClose()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This confirms you're skipping the purchase and adds it to your Money Kept.")
        }
        .alert("Did you buy this already?", isPresented: $showBoughtAlreadyConfirm) {
            Button("Yes, I bought it") {
                store.resolve(id: itemID, outcome: .resolvedBought)
                GhostAnalytics.decisionBoughtAlready()
                onClose()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This is recorded without judgment and does not add to Money Kept.")
        }
        .confirmationDialog(
            "Ghost Cart does not sell this product. You are opening the original source.",
            isPresented: $showBuyFromSourceDisclosure,
            titleVisibility: .visible
        ) {
            Button("Open source") { openSource() }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog(
            "Send it around again",
            isPresented: $showRestartPicker,
            titleVisibility: .visible
        ) {
            if let item {
                ForEach(GhostDeliveryDuration.presets(for: item.category)) { duration in
                    if let minutes = duration.totalMinutes {
                        Button(duration.label) {
                            store.restartGhostDelivery(id: itemID, minutes: minutes)
                            GhostAnalytics.decisionRestarted()
                        }
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: Binding(
            get: { shareItems != nil },
            set: { if !$0 { shareItems = nil } }
        )) {
            if let shareItems {
                ActivityShareSheet(items: shareItems)
            }
        }
    }

    private var header: some View {
        HStack {
            Button(action: onClose) { Image(systemName: "chevron.left") }
            Spacer()
        }
    }

    private func waitedLabel(_ item: AlmostBuy) -> String? {
        guard let decisionAt = item.decisionAt else { return nil }
        let start = item.coolingStartedAt ?? item.capturedAt
        let minutes = max(1, Int(decisionAt.timeIntervalSince(start) / 60))
        if minutes < 60 { return "\(minutes) min" }
        if minutes < 24 * 60 { return "\(minutes / 60) hr" }
        return "\(minutes / (24 * 60)) day\(minutes == 24 * 60 ? "" : "s")"
    }

    private func openSource() {
        guard let sourceURL = item?.sourceURL, let url = URL(string: sourceURL) else { return }
        openURL(url)
        GhostAnalytics.decisionBuyFromSource()
        // Recorded as an outbound action only - never auto-marks as bought.
        // The user confirms that explicitly via "I bought it already".
    }

    private func askFriendItems(_ item: AlmostBuy) -> [Any] {
        GhostAnalytics.askFriendStarted()
        var items: [Any] = ["I'm cooling this before I decide. What do you think?\n\n\(item.name)"]
        if let sourceURL = item.sourceURL, let url = URL(string: sourceURL) {
            items.append(url)
        }
        return items
    }
}

#if os(iOS)
private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
#elseif os(macOS)
// macOS has no direct UIActivityViewController equivalent - the standard
// counterpart is NSSharingServicePicker, shown relative to a view rather
// than presented as its own screen. Presents itself as soon as its host
// view appears (this is already reached via a .sheet(...) presentation at
// the call site, unchanged), and closes that sheet afterward either way.
private struct ActivityShareSheet: NSViewRepresentable {
    let items: [Any]
    @Environment(\.dismiss) private var dismiss

    func makeNSView(context: Context) -> NSView {
        let view = NSView(frame: .zero)
        DispatchQueue.main.async {
            let picker = NSSharingServicePicker(items: items)
            picker.show(relativeTo: .zero, of: view, preferredEdge: .minY)
        }
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {}
}
#endif
