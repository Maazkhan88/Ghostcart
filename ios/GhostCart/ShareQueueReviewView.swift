import SwiftUI

// Mirrors Android's ShareQueueReviewScreen (ui/v2/ShareQueueReviewScreen.kt):
// shown instead of the single-item capture form whenever the share queue is
// non-empty. List with per-item edit/remove, a duplicate-detection banner
// with Merge/Keep both/Remove, and one bulk "Ghost N items" confirm button -
// there is no per-item confirm.
struct ShareQueueReviewView: View {
    @EnvironmentObject private var store: GhostCartStore
    let onComplete: () -> Void

    @State private var shareWithCommunity = true
    @State private var editingItem: ShareQueueItem?

    private var hasUnresolvedDuplicates: Bool {
        store.shareQueue.contains { $0.duplicateAction == .flagged }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                ScreenHeader(
                    eyebrow: "Staged",
                    title: "Shared review queue",
                    subtitle: "Review the products, then Ghost them together for the standard cooldown."
                )

                if store.shareQueue.isEmpty {
                    EmptyStateView(
                        image: "bag",
                        title: "Queue is empty",
                        message: "Share items to Ghost Cart to stack them here."
                    )
                } else {
                    ForEach(store.shareQueue) { item in
                        queueItemCard(item)
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            if !store.shareQueue.isEmpty {
                footer
            }
        }
        .sheet(item: $editingItem) { item in
            EditQueueItemView(item: item) { updated in
                store.updateShareQueueItem(updated)
                editingItem = nil
            }
        }
    }

    private func queueItemCard(_ item: ShareQueueItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                ProductThumbnail(imageURL: item.imageURL, systemImage: item.category.systemImage, size: 54, cornerRadius: 12)
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name).font(.subheadline.weight(.bold)).lineLimit(2)
                    HStack(spacing: 4) {
                        DirhamAmount(value: item.amount, font: .caption.weight(.bold), iconSize: 9, color: Color.secondary)
                        Text("· \(item.category.title)").font(.caption).foregroundStyle(Color.secondary)
                    }
                }
                Spacer(minLength: 0)
                Button { editingItem = item } label: {
                    Image(systemName: "pencil.circle").font(.title3).foregroundStyle(Color.secondary)
                }
                .buttonStyle(.plain)
                Button { store.removeShareQueueItem(id: item.id) } label: {
                    Image(systemName: "trash.circle").font(.title3).foregroundStyle(Color.red)
                }
                .buttonStyle(.plain)
            }
            if item.duplicateAction == .flagged {
                duplicateBanner(item)
            }
        }
        .padding(14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func duplicateBanner(_ item: ShareQueueItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Duplicate product detected in queue").font(.caption.weight(.bold))
            Text("Would you like to merge this with the existing item, keep both, or remove this duplicate?")
                .font(.caption2)
                .foregroundStyle(Color.secondary)
            HStack(spacing: 18) {
                Button("Merge") { store.removeShareQueueItem(id: item.id) }
                Button("Keep both") {
                    var updated = item
                    updated.duplicateAction = .none
                    store.updateShareQueueItem(updated)
                }
                Button("Remove") { store.removeShareQueueItem(id: item.id) }
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color.orange)
        }
        .padding(12)
        .background(Color.orange.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var footer: some View {
        VStack(spacing: 12) {
            Toggle(isOn: $shareWithCommunity) {
                Text("Share anonymously with community feed")
                    .font(.caption.weight(.bold))
            }
            .tint(Color.ghostGreenColor)

            Button {
                store.bulkCoolShareQueue(shareWithCommunity: shareWithCommunity)
                onComplete()
            } label: {
                Text(hasUnresolvedDuplicates
                     ? "Resolve duplicates to continue"
                     : "Ghost \(store.shareQueue.count) item\(store.shareQueue.count == 1 ? "" : "s")")
            }
            .buttonStyle(GhostPrimaryButtonStyle())
            .disabled(hasUnresolvedDuplicates)
            .opacity(hasUnresolvedDuplicates ? 0.5 : 1)
        }
        .padding(16)
        .background(.regularMaterial)
    }
}

private struct EditQueueItemView: View {
    let item: ShareQueueItem
    let onSave: (ShareQueueItem) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var name: String
    @State private var amountText: String
    @State private var category: AlmostBuyCategory

    init(item: ShareQueueItem, onSave: @escaping (ShareQueueItem) -> Void) {
        self.item = item
        self.onSave = onSave
        _name = State(initialValue: item.name)
        _amountText = State(initialValue: item.amount == item.amount.rounded() ? String(Int(item.amount)) : String(item.amount))
        _category = State(initialValue: item.category)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Name") {
                    TextField("Name", text: $name)
                }
                Section("Price (AED)") {
                    TextField("0", text: $amountText).keyboardType(.decimalPad)
                }
                Section("Category") {
                    Picker("Category", selection: $category) {
                        ForEach(AlmostBuyCategory.allCases) { option in
                            Label(option.title, systemImage: option.systemImage).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                }
            }
            .navigationTitle("Edit item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        var updated = item
                        updated.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        updated.amount = Double(amountText.replacingOccurrences(of: ",", with: ".")) ?? item.amount
                        updated.category = category
                        onSave(updated)
                    }
                }
            }
        }
    }
}
