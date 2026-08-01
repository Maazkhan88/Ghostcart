import Foundation

// Mirrors Android's AlmostBuySync.kt exactly: best-effort background sync of
// on-device AlmostBuy items to /api/almost-buys(+/resolve), purely so the
// server-side cooldown-expiry cron and cross-device history have something
// to find. Local storage (GhostCartStore/UserDefaults) stays the source of
// truth for the UI - every method here silently returns nil/false on any
// failure (offline, signed out, server error) rather than throwing, and
// GhostCartStore never blocks a capture/resolve/snooze action waiting on
// these calls, same as Android's stated design ("This never blocks capture
// or resolution, and silently no-ops when signed out").
enum AlmostBuySyncService {
    private static let iso: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    @discardableResult
    static func syncCreate(_ item: AlmostBuy) async -> String? {
        guard await AuthService.shared.isSignedIn else { return nil }
        var body: [String: Any] = [
            "title": item.name,
            "category": item.category.title,
            "almostSpentCents": Int((item.amount * 100).rounded()),
            "sourceKind": item.sourceURL != nil ? "share" : "manual",
        ]
        body["trigger"] = item.trigger.rawValue
        if let sourceURL = item.sourceURL { body["sourceUrl"] = sourceURL }
        if let imageURL = item.imageURL { body["imageUrl"] = imageURL }
        if let decisionAt = item.decisionAt, item.state.isWaiting {
            body["coolOffUntil"] = iso.string(from: decisionAt)
        }
        guard let response = try? await AuthService.shared.authorizedPost(path: "/api/almost-buys", body: body),
              let almostBuy = response["almostBuy"] as? [String: Any],
              let serverId = almostBuy["id"] as? String, !serverId.isEmpty else { return nil }
        return serverId
    }

    @discardableResult
    static func syncResolve(serverId: String, outcome: AlmostBuyState) async -> Bool {
        guard await AuthService.shared.isSignedIn else { return false }
        guard let outcomeString = resolveOutcome(outcome) else { return false }
        let result = try? await AuthService.shared.authorizedPost(
            path: "/api/almost-buys/\(serverId)/resolve",
            body: ["outcome": outcomeString]
        )
        return result != nil
    }

    @discardableResult
    static func syncExtend(serverId: String, decisionAt: Date) async -> Bool {
        guard await AuthService.shared.isSignedIn else { return false }
        let body: [String: Any] = ["state": "cooling", "coolOffUntil": iso.string(from: decisionAt)]
        let result = try? await AuthService.shared.authorizedPost(path: "/api/almost-buys/\(serverId)", body: body)
        return result != nil
    }

    // One-time backfill for items resolved locally before this account ever
    // had a session (or before sync existed) - recreates the item, then
    // immediately resolves it with the same outcome it already has locally.
    // Never invents an outcome; only called for items already
    // .resolvedSkipped/.resolvedBought.
    @discardableResult
    static func syncResolvedBackfill(_ item: AlmostBuy) async -> String? {
        guard let serverId = await syncCreate(item) else { return nil }
        _ = await syncResolve(serverId: serverId, outcome: item.state)
        return serverId
    }

    // Pulls this account's server-side history down - the counterpart to
    // the push-only methods above. Returns nil on any failure so callers
    // can treat this as best-effort; never mutates local state itself,
    // GhostCartStore decides how to merge.
    static func fetchRemote() async -> [AlmostBuy]? {
        guard await AuthService.shared.isSignedIn else { return nil }
        guard let response = try? await AuthService.shared.authorizedGet(path: "/api/almost-buys?limit=100"),
              let rows = response["almostBuys"] as? [[String: Any]] else { return nil }
        return rows.compactMap(parseRemoteItem)
    }

    private static func resolveOutcome(_ state: AlmostBuyState) -> String? {
        switch state {
        case .resolvedSkipped: return "skipped"
        case .resolvedBought: return "bought"
        default: return nil
        }
    }

    private static func parseRemoteItem(_ row: [String: Any]) -> AlmostBuy? {
        guard let serverId = row["id"] as? String, let title = row["title"] as? String else { return nil }
        let category = AlmostBuyCategory(serverName: row["category"] as? String)
        let trigger = SpendingTrigger(rawValue: row["trigger"] as? String ?? "") ?? .other
        let cents = (row["almostSpentCents"] as? Int) ?? Int(row["almostSpentCents"] as? Double ?? 0)
        let stateString = row["state"] as? String ?? "captured"
        let state: AlmostBuyState
        switch stateString {
        case "resolved_skipped": state = .resolvedSkipped
        case "resolved_bought": state = .resolvedBought
        case "cooling", "snoozed": state = .cooling
        default: state = .captured
        }
        let capturedAt = parseDate(row["capturedAt"]) ?? Date()
        return AlmostBuy(
            name: title,
            amount: Double(cents) / 100,
            category: category,
            trigger: trigger,
            source: (row["sourceUrl"] as? String) != nil ? .url : .manual,
            sourceURL: row["sourceUrl"] as? String,
            imageURL: row["imageUrl"] as? String,
            capturedAt: capturedAt,
            decisionAt: parseDate(row["coolOffUntil"]),
            resolvedAt: parseDate(row["resolvedAt"]),
            state: state,
            serverId: serverId
        )
    }

    private static func parseDate(_ value: Any?) -> Date? {
        guard let string = value as? String, !string.isEmpty else { return nil }
        return iso.date(from: string) ?? ISO8601DateFormatter().date(from: string)
    }
}
