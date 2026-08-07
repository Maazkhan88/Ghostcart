import XCTest
@testable import GhostCart

final class GhostDeliveryTests: XCTestCase {
    private func makeItem(startedSecondsAgo: TimeInterval, totalSeconds: TimeInterval) -> AlmostBuy {
        let start = Date().addingTimeInterval(-startedSecondsAgo)
        return AlmostBuy(
            name: "Test",
            amount: 100,
            category: .other,
            trigger: .fomo,
            source: .manual,
            capturedAt: start,
            decisionAt: start.addingTimeInterval(totalSeconds),
            coolingStartedAt: start,
            state: .cooling
        )
    }

    func testStageAtZeroFractionIsPlaced() {
        let item = makeItem(startedSecondsAgo: 0, totalSeconds: 900)
        XCTAssertEqual(item.deliveryStage(), .placed)
    }

    func testStageProgressesThroughAllSixStagesByElapsedFraction() {
        let total: TimeInterval = 1000
        let item = makeItem(startedSecondsAgo: 0, totalSeconds: total)
        let start = item.coolingStartedAt!

        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 0.0)), .placed)
        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 0.15)), .preparing)
        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 0.30)), .riderPickingUp)
        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 0.50)), .outForDelivery)
        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 0.85)), .riderNearby)
        XCTAssertEqual(item.deliveryStage(at: start.addingTimeInterval(total * 1.0)), .delivered)
    }

    func testStageNeverMovesBackwardsPastDelivered() {
        let item = makeItem(startedSecondsAgo: 10_000, totalSeconds: 900)
        // Far past the end - should clamp at delivered, not overflow/loop.
        XCTAssertEqual(item.deliveryStage(), .delivered)
    }

    func testResolvedItemsHaveNoActiveStage() {
        var item = makeItem(startedSecondsAgo: 100, totalSeconds: 900)
        item.state = .resolvedSkipped
        XCTAssertNil(item.deliveryStage())
    }

    func testCapturedButNotCoolingHasNoActiveStage() {
        let item = AlmostBuy(name: "Test", amount: 50, category: .other, trigger: .fomo, source: .manual, state: .captured)
        XCTAssertNil(item.deliveryStage())
    }

    func testRouteGenerationIsDeterministicForSameID() {
        let id = UUID()
        let routeA = GhostRouteGenerator.route(for: id)
        let routeB = GhostRouteGenerator.route(for: id)
        XCTAssertEqual(routeA.origin.latitude, routeB.origin.latitude)
        XCTAssertEqual(routeA.origin.longitude, routeB.origin.longitude)
        XCTAssertEqual(routeA.destination.latitude, routeB.destination.latitude)
        XCTAssertEqual(routeA.waypoints.count, routeB.waypoints.count)
    }

    func testRouteGenerationDiffersForDifferentIDs() {
        let routeA = GhostRouteGenerator.route(for: UUID())
        let routeB = GhostRouteGenerator.route(for: UUID())
        XCTAssertNotEqual(routeA.origin.latitude, routeB.origin.latitude)
    }

    func testDeliveryDurationPresetsAreFoodSensitive() {
        let food = GhostDeliveryDuration.presets(for: .food).compactMap(\.totalMinutes)
        let other = GhostDeliveryDuration.presets(for: .electronics).compactMap(\.totalMinutes)
        XCTAssertTrue(food.contains(15))
        XCTAssertTrue(food.contains(30))
        XCTAssertFalse(other.contains(30))
        XCTAssertTrue(other.contains(7 * 24 * 60))
    }

    func testRestartGhostDeliveryDoesNotDuplicateMoneyKept() {
        let store = GhostCartStore()
        let id = store.capture(name: "Restart test", amount: 200, category: .other, trigger: .fomo, source: .manual, sourceURL: nil)
        store.startCooling(id: id, minutes: 15)
        let beforeMoneyKept = store.progress.moneyKept
        store.restartGhostDelivery(id: id, minutes: 60)
        // Restarting must never add to Money Kept - only a confirmed skip does.
        XCTAssertEqual(store.progress.moneyKept, beforeMoneyKept)
        XCTAssertEqual(store.items.first { $0.id == id }?.state, .cooling)
        store.delete(id: id)
    }

    func testConfirmedSkipAddsToMoneyKeptExactlyOnce() {
        let store = GhostCartStore()
        let id = store.capture(name: "Skip test", amount: 300, category: .other, trigger: .fomo, source: .manual, sourceURL: nil)
        store.startCooling(id: id, minutes: 15)
        let before = store.progress.moneyKept
        store.resolve(id: id, outcome: .resolvedSkipped)
        XCTAssertEqual(store.progress.moneyKept, before + 300)
        // Resolving again (idempotency guard) must not double-count.
        store.resolve(id: id, outcome: .resolvedSkipped)
        XCTAssertEqual(store.progress.moneyKept, before + 300)
        store.delete(id: id)
    }

    func testBoughtAlreadyDoesNotAddMoneyKept() {
        let store = GhostCartStore()
        let id = store.capture(name: "Bought test", amount: 400, category: .other, trigger: .fomo, source: .manual, sourceURL: nil)
        store.startCooling(id: id, minutes: 15)
        let before = store.progress.moneyKept
        store.resolve(id: id, outcome: .resolvedBought)
        XCTAssertEqual(store.progress.moneyKept, before)
        store.delete(id: id)
    }
}
