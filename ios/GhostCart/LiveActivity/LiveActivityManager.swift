import Foundation
import ActivityKit
import os.log

// [GHOSTCART LIVE] debug logging for the ActivityKit lifecycle - development
// visibility only (Console/Xcode device log), never gated behind a persisted
// buffer since this is far lower-volume than push notifications. Token
// values are always redacted; nothing here is sensitive enough to strip in
// Release, but the file is still worth keeping terse there.
enum LiveActivityDebugLog {
    private static let logger = Logger(subsystem: "com.ghostcart.app", category: "LiveActivity")

    static func log(_ message: String) {
        let line = "[GHOSTCART LIVE] \(message)"
        logger.notice("\(line, privacy: .public)")
        #if DEBUG
        print(line)
        #endif
    }
}

// Owns the local (non-remote) ActivityKit lifecycle for Ghost Cart's Cooling
// Off Live Activity. "Local" here means every update is triggered by an
// in-app state change (start, resolve, cancel) - never a periodic tick and
// never a server push. The countdown itself is rendered entirely by the
// system from `endTime` (Text/ProgressView(timerInterval:) in
// GhostCartWidgets), so this type only ever calls into ActivityKit when the
// underlying AlmostBuy actually changes.
//
// Remote updates: NOT implemented. Every mutation that could affect a
// Cooling Off Live Activity (start, extend, resolve, cancel) already
// happens inside this same app process, so there is currently no scenario
// where Ghost Cart needs a push to arrive while the app didn't cause the
// change itself. If a future feature changes that (e.g. a friend can
// remotely nudge someone's cooldown), see the "Remote Live Activity
// updates" section of ANDROID_LIVE_UPDATES_IMPLEMENTATION_PLAN.md's iOS
// counterpart notes for exactly what backend/APNs work that would require.
@available(iOS 16.2, *)
enum LiveActivityManager {
    private static let statusMessageActive = "Still thinking?"

    /// Starts a Cooling Off Live Activity for `item`, replacing any activity
    /// already running for the same item (Ghost Cart never shows two Live
    /// Activities for one AlmostBuy). No-ops silently if Live Activities are
    /// unavailable (disabled in Settings, or the device/OS doesn't support
    /// them) - the app has no other dependency on this succeeding, since
    /// normal local/push notifications already cover the "cooldown
    /// finished" moment regardless.
    static func startCoolingOff(for item: AlmostBuy) {
        guard item.state == .cooling, let endTime = item.decisionAt else { return }
        Task {
            await endActivities(for: item.id, status: .cancelled, message: "Cooling-off ended", dismissalPolicy: .immediate)

            guard ActivityAuthorizationInfo().areActivitiesEnabled else {
                LiveActivityDebugLog.log("START skipped - Live Activities disabled in Settings")
                return
            }

            let attributes = GhostLiveActivityAttributes(
                kind: .coolingOff,
                itemID: item.id.uuidString,
                productName: item.name,
                categorySystemImage: item.category.systemImage,
                priceCents: Int((item.amount * 100).rounded()),
                currencyCode: "AED",
                deepLinkPath: "cooldown/\(item.id.uuidString)",
                startTime: item.coolingStartedAt ?? Date()
            )
            let state = GhostLiveActivityAttributes.ContentState(
                status: .active,
                endTime: endTime,
                statusMessage: statusMessageActive
            )
            do {
                // pushType: .token mints an ActivityKit push token purely so
                // the debug log below can confirm the plumbing works: no
                // backend endpoint exists yet to receive it (see the
                // "Remote updates: NOT implemented" note above), so nothing
                // is uploaded. Safe to leave as .token rather than nil - it
                // costs nothing when unused and means turning on remote
                // updates later doesn't require re-requesting activities.
                let activity = try Activity.request(
                    attributes: attributes,
                    content: ActivityContent(state: state, staleDate: endTime),
                    pushType: .token
                )
                LiveActivityDebugLog.log("START id=\(activity.id) item=\(item.id.uuidString) end=\(endTime)")
                observePushToken(for: activity)
            } catch {
                LiveActivityDebugLog.log("START FAILED item=\(item.id.uuidString): \(error.localizedDescription)")
            }
        }
    }

    /// Called when a running cooldown is extended/restarted (snooze, "Send
    /// it around again") so the Lock Screen countdown reflects the new
    /// `decisionAt` instead of drifting out of sync with GhostCartStore.
    static func updateEndTime(for item: AlmostBuy) {
        guard let endTime = item.decisionAt else { return }
        Task {
            for activity in activities(for: item.id) {
                let state = GhostLiveActivityAttributes.ContentState(
                    status: .active,
                    endTime: endTime,
                    statusMessage: statusMessageActive
                )
                await activity.update(ActivityContent(state: state, staleDate: endTime))
                LiveActivityDebugLog.log("UPDATE id=\(activity.id) newEnd=\(endTime)")
            }
        }
    }

    /// Ends the activity with final "Ghosted successfully" / "Bought from
    /// source" content, then auto-dismisses shortly after so a resolved
    /// item doesn't linger on the Lock Screen (Apple's supported dismissal
    /// window, not a background task keeping it alive).
    static func resolve(itemID: UUID, outcome: AlmostBuyState, amount: Double) {
        guard outcome == .resolvedSkipped || outcome == .resolvedBought else { return }
        let status: GhostLiveActivityStatus = outcome == .resolvedSkipped ? .completed : .bought
        let message = outcome == .resolvedSkipped
            ? "Ghosted successfully · \(AmountFormatter.string(amount)) AED avoided"
            : "Bought from source"
        Task {
            await endActivities(for: itemID, status: status, message: message, dismissalPolicy: .after(Date().addingTimeInterval(20)))
        }
    }

    /// Ends the activity immediately without a resolution (item deleted,
    /// cooldown cancelled some other way).
    static func cancel(itemID: UUID) {
        Task {
            await endActivities(for: itemID, status: .cancelled, message: "Cooling-off ended", dismissalPolicy: .immediate)
        }
    }

    private static func endActivities(
        for itemID: UUID,
        status: GhostLiveActivityStatus,
        message: String,
        dismissalPolicy: ActivityUIDismissalPolicy
    ) async {
        for activity in activities(for: itemID) {
            let state = GhostLiveActivityAttributes.ContentState(
                status: status,
                endTime: activity.content.state.endTime,
                statusMessage: message
            )
            await activity.end(ActivityContent(state: state, staleDate: nil), dismissalPolicy: dismissalPolicy)
            LiveActivityDebugLog.log("END id=\(activity.id) status=\(status.rawValue)")
        }
    }

    private static func activities(for itemID: UUID) -> [Activity<GhostLiveActivityAttributes>] {
        Activity<GhostLiveActivityAttributes>.activities.filter { $0.attributes.itemID == itemID.uuidString }
    }

    private static func observePushToken(for activity: Activity<GhostLiveActivityAttributes>) {
        Task {
            for await tokenData in activity.pushTokenUpdates {
                let redacted = tokenData.map { String(format: "%02x", $0) }.joined().prefix(8) + "…"
                LiveActivityDebugLog.log("PUSH TOKEN CREATED id=\(activity.id) token=\(redacted) (not uploaded - no remote update backend yet)")
            }
        }
    }
}
