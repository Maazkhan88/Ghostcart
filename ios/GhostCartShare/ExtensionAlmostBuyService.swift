import Foundation

// Extension-local counterpart to AlmostBuySyncService.syncCreate. Not a
// shared source file with that one on purpose: AlmostBuySyncService calls
// through AuthService.shared for its token, and AuthService is coupled to
// FirebaseService (see ExtensionAuth.swift's doc comment) - this duplicates
// just the request-shape logic (a few lines) against the same
// /api/almost-buys endpoint, using ApiClient directly with an explicit
// bearer token read from the shared Keychain instead.
enum ExtensionAlmostBuyService {
    private static let iso: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    /// Creates a real, permanent, server-side almost-buy directly from the
    /// extension - the main app picks it up automatically next launch via
    /// its existing sync/hydration path, no local storage write needed here.
    static func create(
        title: String,
        category: AlmostBuyCategory,
        amount: Double,
        sourceURL: String?,
        imageURL: String?,
        cooldownMinutes: Int,
        bearerToken: String
    ) async -> Bool {
        var body: [String: Any] = [
            "title": title,
            "category": category.title,
            "almostSpentCents": Int((amount * 100).rounded()),
            "sourceKind": sourceURL != nil ? "share" : "manual",
            "trigger": SpendingTrigger.other.rawValue,
            "coolOffUntil": iso.string(from: Date().addingTimeInterval(TimeInterval(cooldownMinutes * 60))),
        ]
        if let sourceURL { body["sourceUrl"] = sourceURL }
        if let imageURL { body["imageUrl"] = imageURL }
        guard let response = try? await ApiClient.shared.postJSON(
            path: "/api/almost-buys",
            body: body,
            bearerToken: bearerToken
        ), let almostBuy = response["almostBuy"] as? [String: Any],
            (almostBuy["id"] as? String)?.isEmpty == false else {
            return false
        }
        return true
    }
}
