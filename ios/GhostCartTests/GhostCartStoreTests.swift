import XCTest
import UIKit
@testable import GhostCart

final class AvatarPresetsTests: XCTestCase {
    func testEveryAvatarPresetImageLoads() {
        for preset in AvatarPreset.all {
            XCTAssertNotNil(UIImage(named: preset.imageName), "Missing asset for avatar preset \(preset.id): \(preset.imageName)")
        }
    }
}

final class GhostCartStoreTests: XCTestCase {
    func testStoreInitializesWithoutCrashing() {
        // GhostCartStore reads real persisted UserDefaults state on init (no
        // dependency-injected store for tests yet - a real gap, tracked
        // separately, not silently worked around here), so this only
        // asserts what the plan actually specified: it initializes without
        // crashing, not a specific item count.
        let store = GhostCartStore()
        XCTAssertNotNil(store)
    }

    func testCaptureAddsItemAndAmountFormatterMatchesAndroid() {
        let store = GhostCartStore()
        let id = store.capture(
            name: "Test item",
            amount: 8399,
            category: .electronics,
            trigger: .fomo,
            source: .manual,
            sourceURL: nil
        )
        XCTAssertEqual(store.items.first?.id, id)
        // Android always shows 2 decimals ("8,399.00"), verified against a
        // live device - this guards against that regressing.
        XCTAssertEqual(AmountFormatter.string(8399), "8,399.00")
    }
}

// Slice 9: unit tests for the sync merge logic (GhostCartStore.mergeRemoteItems),
// previously untested. Tests the pure function directly rather than
// syncFromServer() itself, since that method also makes a real network call
// via AlmostBuySyncService - there's no mocking layer for that, and adding
// one just to reach this logic would be a bigger, riskier change than the
// tests themselves justify.
final class AlmostBuySyncMergeTests: XCTestCase {
    private func item(name: String, serverId: String?, capturedAt: Date) -> AlmostBuy {
        AlmostBuy(
            name: name,
            amount: 100,
            category: .other,
            trigger: .other,
            source: .manual,
            capturedAt: capturedAt,
            serverId: serverId
        )
    }

    func testMergeIsPurelyAdditive_neverDropsLocalItems() {
        let local = [item(name: "Local only", serverId: nil, capturedAt: Date())]
        let merged = GhostCartStore.mergeRemoteItems(local: local, remote: [])
        XCTAssertEqual(merged.count, 1)
        XCTAssertEqual(merged.first?.name, "Local only")
    }

    func testMergeSkipsRemoteItemsAlreadyKnownByServerId() {
        let now = Date()
        let local = [item(name: "Existing", serverId: "server-1", capturedAt: now)]
        // Same serverId, different local name - simulates a remote copy of
        // an item this device already has; the local copy must win (never
        // overwritten), matching the "never deletes or overwrites a local
        // item" contract.
        let remoteDuplicate = item(name: "Existing (remote copy)", serverId: "server-1", capturedAt: now)
        let merged = GhostCartStore.mergeRemoteItems(local: local, remote: [remoteDuplicate])
        XCTAssertEqual(merged.count, 1)
        XCTAssertEqual(merged.first?.name, "Existing")
    }

    func testMergeAddsGenuinelyNewRemoteItems() {
        let local = [item(name: "Local", serverId: "server-1", capturedAt: Date())]
        let remoteNew = item(name: "From another device", serverId: "server-2", capturedAt: Date())
        let merged = GhostCartStore.mergeRemoteItems(local: local, remote: [remoteNew])
        XCTAssertEqual(merged.count, 2)
        XCTAssertTrue(merged.contains { $0.serverId == "server-2" })
    }

    func testMergeSortsByCapturedAtDescending() {
        let older = item(name: "Older", serverId: "s1", capturedAt: Date(timeIntervalSinceNow: -3600))
        let newer = item(name: "Newer", serverId: "s2", capturedAt: Date())
        let merged = GhostCartStore.mergeRemoteItems(local: [older], remote: [newer])
        XCTAssertEqual(merged.first?.name, "Newer")
        XCTAssertEqual(merged.last?.name, "Older")
    }

    func testMergeTreatsItemsWithNilServerIdAsAlwaysNew() {
        // Two local items with no serverId (never synced) plus a remote item
        // that also happens to have serverId == nil should never be
        // deduplicated against each other purely by nil == nil.
        let local = [item(name: "Local unsynced", serverId: nil, capturedAt: Date())]
        let remoteAlsoNil = item(name: "Remote with no id", serverId: nil, capturedAt: Date())
        let merged = GhostCartStore.mergeRemoteItems(local: local, remote: [remoteAlsoNil])
        XCTAssertEqual(merged.count, 2, "Items with no serverId must not be deduplicated against each other")
    }
}
