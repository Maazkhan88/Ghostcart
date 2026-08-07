import Foundation

// Mirrors Android's GhostGiftRepository (data/GhostGiftRepository.kt) against
// /api/ghost-gifts. A gift always originates from an existing AlmostBuy that
// already has a serverId (captured/cooling/snoozed, with an image) - there is
// no gift-only draft on the server side.
struct GiftListItem: Identifiable, Equatable {
    var id: String
    var status: String
    var createdAt: Date
    var expiresAt: Date
    var revealedAt: Date?
    var itemTitle: String
    var category: String
    var imageURL: String?
    var amountCents: Int
    // Present only on items in receivedGifts.
    var senderName: String?

    var amount: Double { Double(amountCents) / 100 }
}

// What GET /api/ghost-gifts/reveal returns after acceptedPrivacy is posted -
// the actual product details are never sent before that point (only the
// blurred teaser image, embedded server-side in the email).
struct RevealedGhostGift: Equatable {
    var id: String
    var senderName: String
    var title: String
    var category: String
    var imageURL: String?
    var amountCents: Int
    var currencyCode: String
    var disclosure: String

    var amount: Double { Double(amountCents) / 100 }
}

enum GhostGiftService {
    @discardableResult
    static func create(almostBuyId: String, recipientName: String, recipientEmail: String) async throws -> Bool {
        let body: [String: Any] = [
            "almostBuyId": almostBuyId,
            "recipientName": recipientName.trimmingCharacters(in: .whitespacesAndNewlines),
            "recipientEmail": recipientEmail.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "recipientConsentConfirmed": true,
        ]
        let response = try await AuthService.shared.authorizedPost(path: "/api/ghost-gifts", body: body)
        guard let gift = response["ghostGift"] as? [String: Any] else {
            throw ApiError(message: "Could not send this gift.")
        }
        return (gift["emailSent"] as? Bool) ?? false
    }

    static func list() async throws -> (sent: [GiftListItem], received: [GiftListItem]) {
        let response = try await AuthService.shared.authorizedGet(path: "/api/ghost-gifts")
        let sent = (response["sentGifts"] as? [[String: Any]] ?? []).compactMap(parseListItem)
        let received = (response["receivedGifts"] as? [[String: Any]] ?? []).compactMap(parseListItem)
        return (sent, received)
    }

    // Works signed-out; a signed-in session with a matching email gets
    // silently linked as the recipient server-side.
    static func reveal(token: String) async throws -> RevealedGhostGift {
        let response = try await AuthService.shared.optionallyAuthorizedPost(
            path: "/api/ghost-gifts/reveal",
            body: ["token": token, "acceptedPrivacy": true]
        )
        guard let gift = response["ghostGift"] as? [String: Any] else {
            throw ApiError(message: "Could not reveal this gift.")
        }
        return RevealedGhostGift(
            id: gift["id"] as? String ?? "",
            senderName: gift["senderName"] as? String ?? "A Ghost Cart member",
            title: gift["title"] as? String ?? "Gift",
            category: gift["category"] as? String ?? "Other",
            imageURL: gift["imageUrl"] as? String,
            amountCents: intValue(gift["amountCents"]) ?? 0,
            currencyCode: gift["currencyCode"] as? String ?? "AED",
            disclosure: gift["disclosure"] as? String
                ?? "Simulation only. No gift was purchased or sent. No real payment or delivery occurred."
        )
    }

    static func withdraw(id: String) async throws {
        _ = try await AuthService.shared.authorizedPost(path: "/api/ghost-gifts/\(id)/withdraw", body: [:])
    }

    private static func intValue(_ value: Any?) -> Int? {
        if let number = value as? NSNumber { return number.intValue }
        if let string = value as? String { return Int(string) }
        return nil
    }

    private static func parseListItem(_ object: [String: Any]) -> GiftListItem? {
        guard let id = object["id"] as? String,
              let status = object["status"] as? String,
              let title = object["itemTitle"] as? String else { return nil }
        let formatter = ISO8601DateFormatter()
        return GiftListItem(
            id: id,
            status: status,
            createdAt: (object["createdAt"] as? String).flatMap(formatter.date) ?? Date(),
            expiresAt: (object["expiresAt"] as? String).flatMap(formatter.date) ?? Date(),
            revealedAt: (object["revealedAt"] as? String).flatMap(formatter.date),
            itemTitle: title,
            category: object["category"] as? String ?? "Other",
            imageURL: object["imageUrl"] as? String,
            amountCents: intValue(object["amountCents"]) ?? 0,
            senderName: object["senderName"] as? String
        )
    }
}

extension GhostCartStore {
    // "Ghost it too" on the reveal screen: opts the revealed item into the
    // recipient's own 24-hour cooldown simulation as a fresh AlmostBuy,
    // entirely independent of the sender's original item. Mirrors Android's
    // AppViewModel.ghostRevealedGift.
    func ghostRevealedGift(_ gift: RevealedGhostGift) {
        let id = capture(
            name: gift.title,
            amount: gift.amount,
            category: AlmostBuyCategory(serverName: gift.category),
            trigger: .gift,
            source: .manual,
            sourceURL: nil,
            imageURL: gift.imageURL
        )
        startCooling(id: id, minutes: 24 * 60)
    }
}

// A public https://theghostcart.com/gift/{token} Universal Link. Mirrors
// Android's MainActivity.captureGhostGiftLink token validation exactly.
enum GiftDeepLink {
    private static let tokenPattern = #"^[A-Za-z0-9_-]{43}$"#

    static func token(from url: URL) -> String? {
        guard url.host?.lowercased() == "theghostcart.com" else { return nil }
        let segments = url.pathComponents.filter { $0 != "/" }
        guard segments.count == 2, segments[0] == "gift" else { return nil }
        let token = segments[1]
        guard token.range(of: tokenPattern, options: .regularExpression) != nil else { return nil }
        return token
    }
}

// Holds an incoming gift-link token until the app is ready to present the
// reveal screen (mirrors Android's ghostGiftRequest app state).
@MainActor
final class DeepLinkRouter: ObservableObject {
    @Published var pendingGiftToken: String?

    func handle(_ url: URL) {
        guard let token = GiftDeepLink.token(from: url) else { return }
        pendingGiftToken = token
    }
}
