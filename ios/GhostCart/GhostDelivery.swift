import Foundation
import CoreLocation
import SwiftUI

// The Ghost Delivery state machine: purely a function of elapsed time
// between an item's coolingStartedAt and decisionAt (its Ghost Delivery
// time). No background timers or continuously running processes - the
// stage is recomputed wherever it's needed (tracker view, notifications,
// Home's active-delivery card) from these two timestamps, so it survives
// app termination, device restart, timezone changes and offline periods.
enum DeliveryStage: Int, CaseIterable, Identifiable {
    case placed
    case preparing
    case riderPickingUp
    case outForDelivery
    case riderNearby
    case delivered

    var id: Int { rawValue }

    // Fraction of total Ghost Delivery duration at which this stage begins.
    var thresholdFraction: Double {
        switch self {
        case .placed: return 0
        case .preparing: return 0.10
        case .riderPickingUp: return 0.25
        case .outForDelivery: return 0.45
        case .riderNearby: return 0.80
        case .delivered: return 1.0
        }
    }

    var title: String {
        switch self {
        case .placed: return "Ghost Order placed"
        case .preparing: return "Being prepared"
        case .riderPickingUp: return "Ghost Rider picking up"
        case .outForDelivery: return "Out for Ghost Delivery"
        case .riderNearby: return "Ghost Rider nearby"
        case .delivered: return "Delivered"
        }
    }

    var timelineSubtitle: String {
        switch self {
        case .placed: return "Your decision is cooling."
        case .preparing: return "Giving the impulse some space."
        case .riderPickingUp: return "Your almost-buy is being collected."
        case .outForDelivery: return "No action is needed yet."
        case .riderNearby: return "Your decision is almost ready."
        case .delivered: return "Time to choose what happens next."
        }
    }

    var notificationTitle: String {
        switch self {
        case .placed: return "Ghost Order placed"
        case .preparing: return "Your Ghost Order is being prepared"
        case .riderPickingUp: return "Ghost Rider is picking up your order"
        case .outForDelivery: return "Out for Ghost Delivery"
        case .riderNearby: return "Your Ghost Rider is nearby"
        case .delivered: return "Your Ghost Order has arrived"
        }
    }

    func notificationBody(itemName: String, durationLabel: String) -> String {
        switch self {
        case .placed: return "\(itemName) is cooling for \(durationLabel). Nothing has been purchased."
        case .preparing: return "Giving this almost-buy some space."
        case .riderPickingUp: return "Your decision is still cooling."
        case .outForDelivery: return "Your \(itemName) decision is on the way."
        case .riderNearby: return "Your decision will be ready soon."
        case .delivered: return "Skip it, buy it, cool it again or ask a friend."
        }
    }
}

extension AlmostBuy {
    // nil before cooling has ever started (still just "captured"), or once
    // resolved/expired (no active delivery to show).
    func deliveryStage(at date: Date = Date()) -> DeliveryStage? {
        guard state.isWaiting, let decisionAt else { return nil }
        let start = coolingStartedAt ?? capturedAt
        guard decisionAt > start else { return .delivered }
        let total = decisionAt.timeIntervalSince(start)
        guard total > 0 else { return .delivered }
        let elapsed = date.timeIntervalSince(start)
        let fraction = min(max(elapsed / total, 0), 1)

        var current = DeliveryStage.placed
        for stage in DeliveryStage.allCases where fraction >= stage.thresholdFraction {
            current = stage
        }
        return current
    }

    // Progress within the current stage's window, for smooth rider
    // interpolation rather than a stage-locked jump.
    func deliveryFraction(at date: Date = Date()) -> Double {
        guard let decisionAt, !state.isResolved else { return 1 }
        let start = coolingStartedAt ?? capturedAt
        let total = decisionAt.timeIntervalSince(start)
        guard total > 0 else { return 1 }
        return min(max(date.timeIntervalSince(start) / total, 0), 1)
    }
}

// MARK: - Deterministic simulated route

// A fictional origin/destination pair and a short polyline, generated
// deterministically from the order's UUID so the same Ghost Order always
// regenerates the identical route on relaunch without persisting
// coordinates. Centered near Dubai to match Ghost Cart's existing AED /
// UAE-flavored simulation (fake delivery address is already "Al Wasl,
// Dubai" elsewhere in the app).
enum GhostRouteGenerator {
    private static let centerLat = 25.2048
    private static let centerLng = 55.2708

    static func route(for id: UUID) -> (origin: CLLocationCoordinate2D, destination: CLLocationCoordinate2D, waypoints: [CLLocationCoordinate2D]) {
        var generator = SeededGenerator(seed: id)
        func jitter(_ range: ClosedRange<Double>) -> Double {
            Double.random(in: range, using: &generator)
        }

        let origin = CLLocationCoordinate2D(
            latitude: centerLat + jitter(-0.06...0.06),
            longitude: centerLng + jitter(-0.06...0.06)
        )
        let destination = CLLocationCoordinate2D(
            latitude: centerLat + jitter(-0.06...0.06),
            longitude: centerLng + jitter(-0.06...0.06)
        )

        // Two gentle midpoints so the route reads as a real path rather
        // than a straight line.
        let mid1 = CLLocationCoordinate2D(
            latitude: origin.latitude + (destination.latitude - origin.latitude) * 0.35 + jitter(-0.01...0.01),
            longitude: origin.longitude + (destination.longitude - origin.longitude) * 0.35 + jitter(-0.01...0.01)
        )
        let mid2 = CLLocationCoordinate2D(
            latitude: origin.latitude + (destination.latitude - origin.latitude) * 0.7 + jitter(-0.01...0.01),
            longitude: origin.longitude + (destination.longitude - origin.longitude) * 0.7 + jitter(-0.01...0.01)
        )

        return (origin, destination, [origin, mid1, mid2, destination])
    }

    // Interpolates the rider's position along the waypoint path for a given
    // 0...1 fraction of the overall Ghost Delivery.
    static func riderPosition(waypoints: [CLLocationCoordinate2D], fraction: Double) -> CLLocationCoordinate2D {
        guard waypoints.count > 1 else { return waypoints.first ?? CLLocationCoordinate2D(latitude: centerLat, longitude: centerLng) }
        let clamped = min(max(fraction, 0), 1)
        let segments = waypoints.count - 1
        let scaled = clamped * Double(segments)
        let index = min(Int(scaled), segments - 1)
        let segmentFraction = scaled - Double(index)
        let a = waypoints[index]
        let b = waypoints[index + 1]
        return CLLocationCoordinate2D(
            latitude: a.latitude + (b.latitude - a.latitude) * segmentFraction,
            longitude: a.longitude + (b.longitude - a.longitude) * segmentFraction
        )
    }
}

// A simple deterministic PRNG seeded from a UUID, so the same order always
// produces the same route without persisting coordinates.
private struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64

    init(seed: UUID) {
        let bytes = withUnsafeBytes(of: seed.uuid) { Array($0) }
        state = bytes.prefix(8).reduce(UInt64(0)) { ($0 << 8) | UInt64($1) }
        if state == 0 { state = 0x9E3779B97F4A7C15 }
    }

    mutating func next() -> UInt64 {
        state ^= state << 13
        state ^= state >> 7
        state ^= state << 17
        return state
    }
}

// MARK: - Ghost Delivery duration presets (spec: category-sensitive ordering)

enum GhostDeliveryDuration: Identifiable {
    case minutes(Int, label: String)
    case custom

    var id: String {
        switch self {
        case .minutes(let value, _): return "m\(value)"
        case .custom: return "custom"
        }
    }

    var label: String {
        switch self {
        case .minutes(_, let label): return label
        case .custom: return "Custom"
        }
    }

    var totalMinutes: Int? {
        if case .minutes(let value, _) = self { return value }
        return nil
    }

    static func formattedCustomLabel(minutes: Int) -> String {
        if minutes < 60 { return "\(minutes) min" }
        if minutes < 24 * 60 {
            let hours = minutes / 60
            return "\(hours) hour\(hours == 1 ? "" : "s")"
        }
        let days = minutes / (24 * 60)
        return "\(days) day\(days == 1 ? "" : "s")"
    }

    // Matches Android's real live preset set (confirmed against a device
    // screenshot, not source - the checked-in Android source this repo has
    // is behind what's actually shipping): the same five options regardless
    // of category, with Custom actually functional (see
    // GhostCheckoutView.deliveryTimeSection / CustomDurationSheet).
    static func presets(for category: AlmostBuyCategory) -> [GhostDeliveryDuration] {
        [
            .minutes(15, label: "15 minutes"),
            .minutes(60, label: "1 hour"),
            .minutes(24 * 60, label: "24 hours"),
            .minutes(7 * 24 * 60, label: "1 week"),
            .custom,
        ]
    }
}

private enum CustomDurationUnit: String, CaseIterable, Identifiable {
    case minutes, hours, days
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
    var minutesPerUnit: Int {
        switch self {
        case .minutes: return 1
        case .hours: return 60
        case .days: return 24 * 60
        }
    }
}

// Custom Ghost Delivery duration entry - a number + unit picker, since a
// bare minutes text field is unfriendly for "1 week"-scale durations.
// Bounds match the app's existing presets range (1 minute to 30 days) so a
// custom value can't produce a nonsensical or unbounded cooldown.
struct CustomDurationSheet: View {
    let onConfirm: (Int) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var value: Int
    @State private var unit: CustomDurationUnit

    init(initialMinutes: Int, onConfirm: @escaping (Int) -> Void) {
        self.onConfirm = onConfirm
        if initialMinutes % (24 * 60) == 0 && initialMinutes >= 24 * 60 {
            _value = State(initialValue: initialMinutes / (24 * 60))
            _unit = State(initialValue: .days)
        } else if initialMinutes % 60 == 0 && initialMinutes >= 60 {
            _value = State(initialValue: initialMinutes / 60)
            _unit = State(initialValue: .hours)
        } else {
            _value = State(initialValue: max(1, initialMinutes))
            _unit = State(initialValue: .minutes)
        }
    }

    private var totalMinutes: Int { value * unit.minutesPerUnit }
    private var maxValue: Int {
        switch unit {
        case .minutes: return 59
        case .hours: return 23
        case .days: return 30
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Custom Ghost Delivery time").font(.title3.weight(.black))
                Text("Choose exactly how long this order should cool off before you decide.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)

                Picker("Value", selection: $value) {
                    ForEach(1...maxValue, id: \.self) { number in
                        Text("\(number)").tag(number)
                    }
                }
                .pickerStyle(.wheel)
                .onChange(of: unit) { _ in value = min(value, maxValue) }

                Picker("Unit", selection: $unit) {
                    ForEach(CustomDurationUnit.allCases) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.segmented)

                Button("Use \(value) \(unit.label.lowercased())") {
                    onConfirm(totalMinutes)
                }
                .buttonStyle(GhostPrimaryButtonStyle())
            }
            .padding(24)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
