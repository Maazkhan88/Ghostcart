import Foundation
import ActivityKit

// Shared between the GhostCart app target and the GhostCartWidgets
// extension (added to both targets' Sources build phase) - this is the
// entire contract between the two processes, so it stays intentionally
// minimal: only what the Cooling Off Live Activity actually renders today.
//
// Only one activity type ships in this release: Cooling Off. Ghost Cart
// doesn't yet have a live friend-poll or challenge feature to drive a
// second kind, so GhostLiveActivityKind isn't pre-populated with unused
// cases - when a timed poll or challenge feature exists, add a case here
// and decide then whether it needs its own ActivityAttributes type (a poll's
// vote tally and a challenge's day-count are different enough from a
// countdown that a dedicated attributes/dynamic-island layout is likely
// cleaner than overloading this one - see the "Future activity types"
// section of the Android Live Updates plan doc for the intended shared
// cross-platform lifecycle these would map onto).
enum GhostLiveActivityKind: String, Codable, Hashable {
    case coolingOff
}

// COMPLETED = user chose to Ghost it (skip) - money kept.
// BOUGHT = user chose to buy from source anyway.
// CANCELLED = the cooling-off ended without a decision (e.g. item deleted).
// "Almost done" is intentionally not a status: the widget derives it purely
// from how close `endTime` is to now inside a TimelineView, so reaching that
// threshold never requires an ActivityKit update from the app.
enum GhostLiveActivityStatus: String, Codable, Hashable {
    case active
    case completed
    case bought
    case cancelled
}

@available(iOS 16.2, *)
struct GhostLiveActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var status: GhostLiveActivityStatus
        // The countdown's end date. The widget renders the remaining time
        // with Text(timerInterval:) / ProgressView(timerInterval:), which the
        // system keeps ticking on its own - this value only needs to change
        // when the user actually extends/restarts the cooldown.
        var endTime: Date
        // Short supporting copy ("Still thinking?", "Ghosted successfully ·
        // AED 1,299 avoided", "Cooling-off ended") - centralized here so the
        // widget stays copy-agnostic and all wording lives in one place
        // (LiveActivityManager) matching Ghost Cart's existing tone.
        var statusMessage: String
    }

    let kind: GhostLiveActivityKind
    // AlmostBuy.id.uuidString - lets LiveActivityManager find/replace the
    // activity for a given item without keeping its own separate lookup
    // table.
    let itemID: String
    let productName: String
    let categorySystemImage: String
    let priceCents: Int
    let currencyCode: String
    // Relative path appended to https://theghostcart.com/ for the widget's
    // widgetURL - reuses the app's existing Universal Links / associated
    // domain (applinks:theghostcart.com), not a new deep-link scheme.
    let deepLinkPath: String
    let startTime: Date
}
