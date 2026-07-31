import XCTest
@testable import GhostCart

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
