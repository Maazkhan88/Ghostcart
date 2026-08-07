import Foundation

// Mirrors Android's ShareQueueItem (AlmostBuyModels.kt:11-22). A staged
// share-sheet item awaiting bulk confirmation, distinct from a durable
// AlmostBuy - nothing here counts toward Almost Spent until bulkCoolShareQueue
// turns the active items into real AlmostBuy records.
struct ShareQueueItem: Identifiable, Codable, Equatable {
    enum DuplicateAction: String, Codable {
        case none
        case flagged
        case merge
        case keepBoth = "keep_both"
        case remove
    }

    var id: String
    var name: String
    var amount: Double
    var category: AlmostBuyCategory
    var imageURL: String?
    var sourceURL: String?
    var brand: String?
    var sourceDomain: String?
    var timestamp: Date
    var duplicateAction: DuplicateAction

    init(
        id: String = UUID().uuidString,
        name: String,
        amount: Double,
        category: AlmostBuyCategory,
        imageURL: String? = nil,
        sourceURL: String? = nil,
        brand: String? = nil,
        sourceDomain: String? = nil,
        timestamp: Date = Date(),
        duplicateAction: DuplicateAction = .none
    ) {
        self.id = id
        self.name = name
        self.amount = amount
        self.category = category
        self.imageURL = imageURL
        self.sourceURL = sourceURL
        self.brand = brand
        self.sourceDomain = sourceDomain
        self.timestamp = timestamp
        self.duplicateAction = duplicateAction
    }
}

// MARK: - Duplicate detection (ported from Android's isLikelyDuplicateProduct,
// AppViewModel.kt:779-798)

func isLikelyDuplicateShareItem(_ existing: ShareQueueItem, _ incoming: ShareQueueItem) -> Bool {
    if let existingURL = normalizedURLForDedup(existing.sourceURL),
       let incomingURL = normalizedURLForDedup(incoming.sourceURL),
       existingURL == incomingURL {
        return true
    }

    let existingTitle = normalizedTextForDedup(existing.name)
    let incomingTitle = normalizedTextForDedup(incoming.name)
    if !existingTitle.isEmpty, existingTitle == incomingTitle {
        if let existingBrand = existing.brand, let incomingBrand = incoming.brand {
            return existingBrand.lowercased() == incomingBrand.lowercased()
        }
        return true
    }

    if let existingImage = existing.imageURL, let incomingImage = incoming.imageURL, existingImage == incomingImage {
        let existingWords = Set(existingTitle.split(separator: " "))
        let incomingWords = Set(incomingTitle.split(separator: " "))
        guard !existingWords.isEmpty, !incomingWords.isEmpty else { return false }
        let overlap = Double(existingWords.intersection(incomingWords).count) / Double(min(existingWords.count, incomingWords.count))
        return overlap >= 0.6
    }

    return false
}

private func normalizedURLForDedup(_ urlString: String?) -> String? {
    guard let urlString, let url = URL(string: urlString), var host = url.host?.lowercased() else { return nil }
    if host.hasPrefix("www.") { host.removeFirst(4) }
    var path = url.path
    if path.hasSuffix("/") { path.removeLast() }
    return host + path
}

private func normalizedTextForDedup(_ text: String) -> String {
    let lowered = text.lowercased()
    let collapsed = lowered.replacingOccurrences(of: "[^a-z0-9]+", with: " ", options: .regularExpression)
    return collapsed.trimmingCharacters(in: .whitespaces)
}

// MARK: - GhostCartStore queue operations (mirrors AppViewModel.kt's
// appendToShareQueue / migratePendingCaptureSeedToQueue / loadShareQueue /
// saveShareQueue / updateShareQueueItem / removeShareQueueItem /
// clearShareQueue / bulkCoolShareQueue)

extension GhostCartStore {
    static let shareQueueCap = 20
    private static let shareQueuePersistenceKey = "ghostcart.v2.share-queue"

    // True once a second share should be merged into the queue instead of
    // overwriting whatever is already staged - either the queue already has
    // items, or a share-originated seed is sitting unconfirmed.
    var hasPendingShare: Bool {
        !shareQueue.isEmpty || (captureSeed?.isFromShare ?? false)
    }

    func loadShareQueue() {
        guard let data = UserDefaults.standard.data(forKey: Self.shareQueuePersistenceKey),
              let decoded = try? JSONDecoder().decode([ShareQueueItem].self, from: data) else { return }
        shareQueue = decoded
    }

    func saveShareQueue() {
        guard let data = try? JSONEncoder().encode(shareQueue) else { return }
        UserDefaults.standard.set(data, forKey: Self.shareQueuePersistenceKey)
    }

    // Converts a still-unconfirmed share-originated captureSeed into the
    // first queue item so a second incoming share has something to join.
    func migratePendingCaptureSeedToQueue() {
        guard let seed = captureSeed, seed.isFromShare else { return }
        let item = ShareQueueItem(
            name: seed.name,
            amount: seed.amount ?? 0,
            category: seed.category,
            imageURL: seed.imageURL,
            sourceURL: seed.sourceURL,
            sourceDomain: seed.sourceDomain
        )
        captureSeed = nil
        appendToShareQueue(item)
    }

    func appendToShareQueue(_ item: ShareQueueItem) {
        guard shareQueue.count < Self.shareQueueCap else { return }
        let isDuplicate = shareQueue.contains { isLikelyDuplicateShareItem($0, item) }
        var finalItem = item
        finalItem.duplicateAction = isDuplicate ? .flagged : .none
        shareQueue.append(finalItem)
        saveShareQueue()
    }

    func updateShareQueueItem(_ item: ShareQueueItem) {
        guard let index = shareQueue.firstIndex(where: { $0.id == item.id }) else { return }
        shareQueue[index] = item
        saveShareQueue()
    }

    func removeShareQueueItem(id: String) {
        shareQueue.removeAll { $0.id == id }
        saveShareQueue()
    }

    func clearShareQueue() {
        shareQueue.removeAll()
        saveShareQueue()
    }

    // Confirms every non-removed queue item at once: one shared ghostOrderId,
    // each item's own category cooldown, and the queue-level community
    // opt-in. Mirrors Android's bulkCoolShareQueue (AppViewModel.kt:1789-1819).
    func bulkCoolShareQueue(shareWithCommunity: Bool) {
        let activeItems = shareQueue.filter { $0.duplicateAction != .remove }
        guard !activeItems.contains(where: { $0.duplicateAction == .flagged }) else { return }

        let orderGroupId = "GO-\(UUID().uuidString)"
        for item in activeItems {
            let id = capture(
                name: item.name,
                amount: item.amount,
                category: item.category,
                trigger: .fomo,
                source: .url,
                sourceURL: item.sourceURL,
                imageURL: item.imageURL,
                ghostOrderId: orderGroupId
            )
            startCooling(id: id, minutes: item.category.recommendedCooldownMinutes)
            if shareWithCommunity, let sourceURL = item.sourceURL {
                let name = item.name
                let category = item.category
                let amount = item.amount
                let imageURL = item.imageURL
                Task {
                    await ProductImportService.publish(
                        title: name, category: category, amount: amount,
                        sourceURL: sourceURL, imageURL: imageURL
                    )
                }
            }
        }
        clearShareQueue()
    }
}
