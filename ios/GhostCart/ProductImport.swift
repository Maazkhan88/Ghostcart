import Foundation
import Combine

// A product resolved from a shared/pasted public link via /api/link-preview.
struct ImportedProduct: Equatable {
    var title: String
    var priceCents: Int?
    var currencyCode: String?
    var category: String
    var imageURL: String?
    var sourceURL: String
    var sourceDomain: String
    var retailer: String
    var status: String
    var note: String?

    var amount: Double? { priceCents.map { Double($0) / 100 } }
}

// One item within a detected listing/collection page.
struct ListingProductStub: Identifiable, Equatable {
    var id: String { sourceURL }
    var title: String
    var priceCents: Int?
    var category: String
    var imageURL: String?
    var sourceURL: String
    var sourceDomain: String
    var retailer: String
}

enum LinkImportResult: Equatable {
    case product(ImportedProduct)
    case listing(sourceDomain: String, retailer: String, items: [ListingProductStub])
}

// A privacy-safe community "User Ghosted" card from /api/community-products.
struct CommunityProduct: Identifiable, Equatable {
    let id: String
    var title: String
    var priceCents: Int
    var currencyCode: String
    var category: String
    var imageURL: String?
    var sourceDomain: String
    var sourceURL: String?
    var ghostCount: Int
    var activityTag: String

    var amount: Double { Double(priceCents) / 100 }
}

// UI state for the capture screen's link import flow.
enum ProductImportState: Equatable {
    case idle
    case loading
    case ready(ImportedProduct)
    case listingDetected(sourceDomain: String, retailer: String, items: [ListingProductStub])
    case error(String)
}

// Cross-tab handoff: a community tap or a shared link seeds the capture form.
struct CaptureSeed: Equatable {
    var name: String
    var amount: Double?
    var category: AlmostBuyCategory
    var sourceURL: String?
    var imageURL: String?
    var sourceDomain: String?
    var retailer: String?
    var note: String?
    // Whether to preselect the anonymous community opt-in. Community-origin
    // items are already public, so re-sharing them defaults off.
    var offerCommunityShare: Bool
    // Marks a seed built from the OS share sheet (mirrors Android's
    // AlmostBuyDraft.sourceKind == "share"). Used to decide whether a second
    // incoming share should migrate this seed into the share queue instead
    // of silently overwriting it.
    var isFromShare: Bool = false
}

// MARK: - Shared-metadata merge (ported from Android)

// Allows one optional word between "this/it" and "out" - Amazon's actual iOS
// share-sheet default caption is "Check this deal out on Amazon", which the
// old ^check (this|it) out\b pattern missed (the word "deal" breaks the
// match), letting that promo text slip through as if it were a real product
// title. Confirmed against a real share. Mirrored in Android's
// GENERIC_SHARE_CAPTION_REGEX - keep both in sync.
private let genericShareCaptionPattern = #"^check (this|it)(\s+\w+)? out\b"#

// Recognizes titles that are share-sheet captions or SKUs rather than a real
// product name. Mirrors Android's titleLooksLikeFallback, including the
// "Check this out at Amazon" native-share caption fix.
func titleLooksLikeFallback(_ value: String) -> Bool {
    let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
    if value == "Shared product" { return true }
    if value.lowercased().hasPrefix("shared item from ") { return true }
    if trimmed.range(of: genericShareCaptionPattern, options: [.regularExpression, .caseInsensitive]) != nil {
        return true
    }
    if trimmed.range(of: #"^[A-Z0-9_-]{10,}$"#, options: [.regularExpression, .caseInsensitive]) != nil {
        return true
    }
    return false
}

// Fills a preview with a title/image supplied by the OS share sheet when the
// server could only return a fallback. Mirrors Android's mergeSharedMetadata.
func mergeSharedMetadata(_ product: ImportedProduct, sharedTitle: String?, sharedImageURL: String?) -> ImportedProduct {
    var merged = product
    let cleanTitle = sharedTitle?.trimmingCharacters(in: .whitespacesAndNewlines)
        .prefixString(160).nonEmpty
    let cleanImage = sharedImageURL?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty

    if product.status == "needs_input" || titleLooksLikeFallback(product.title) {
        merged.title = cleanTitle ?? product.title
    }
    merged.imageURL = product.imageURL ?? cleanImage

    let complete = !titleLooksLikeFallback(merged.title) && merged.priceCents != nil && merged.imageURL != nil
    if complete {
        merged.status = "complete"
        merged.note = nil
    } else if !titleLooksLikeFallback(merged.title) || merged.imageURL != nil {
        merged.status = "partial"
    }
    return merged
}

private extension String {
    var nonEmpty: String? { isEmpty ? nil : self }
    func prefixString(_ maxCount: Int) -> String { String(prefix(maxCount)) }
}

// MARK: - Service

enum ProductImportService {
    static func previewLink(
        sourceURL: String,
        sharedTitle: String? = nil,
        sharedImageURL: String? = nil
    ) async -> Result<LinkImportResult, ApiError> {
        do {
            let response = try await ApiClient.shared.postJSON(path: "/api/link-preview", body: ["url": sourceURL])
            if let listing = response["listing"] as? [String: Any] {
                let rawItems = listing["items"] as? [[String: Any]] ?? []
                let stubs = rawItems.compactMap(parseListingItem)
                return .success(.listing(
                    sourceDomain: listing["sourceDomain"] as? String ?? "",
                    retailer: listing["retailer"] as? String ?? "",
                    items: stubs
                ))
            }
            guard let productObject = response["product"] as? [String: Any] else {
                return .failure(ApiError(message: "Ghost Cart could not read this link. Enter the details manually."))
            }
            let imported = parseImportedProduct(productObject)
            return .success(.product(mergeSharedMetadata(imported, sharedTitle: sharedTitle, sharedImageURL: sharedImageURL)))
        } catch let error as ApiError {
            return .failure(error)
        } catch {
            return .failure(ApiError(message: "Ghost Cart could not read this link. Enter the details manually."))
        }
    }

    // Optional, consent-gated anonymous publish. Never changes Money Kept.
    @discardableResult
    static func publish(
        title: String,
        category: AlmostBuyCategory,
        amount: Double,
        sourceURL: String?,
        imageURL: String?
    ) async -> Result<CommunityProduct?, ApiError> {
        guard let sourceURL, !sourceURL.isEmpty, isSafePublicHttpsURL(sourceURL) else {
            return .success(nil)
        }
        do {
            let body: [String: Any] = [
                "shareWithCommunity": true,
                "sourceUrl": sourceURL,
                "title": title,
                "category": category.title,
                "priceCents": Int((amount * 100).rounded()),
                "currencyCode": ApiConfig.uaeDirhamCode,
                "imageUrl": imageURL ?? NSNull(),
                "source": "ios",
                "eventId": UUID().uuidString,
            ]
            let response = try await ApiClient.shared.postJSON(path: "/api/community-products", body: body)
            if let productObject = response["product"] as? [String: Any] {
                return .success(parseCommunityProduct(productObject))
            }
            return .success(nil)
        } catch let error as ApiError {
            return .failure(error)
        } catch {
            return .failure(ApiError(message: "Ghost Cart could not share this item right now."))
        }
    }

    // MARK: - Parsing helpers

    private static func nullableString(_ object: [String: Any], _ key: String) -> String? {
        guard let value = object[key] as? String else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || trimmed.lowercased() == "null" { return nil }
        return trimmed
    }

    private static func intValue(_ object: [String: Any], _ key: String) -> Int? {
        if object[key] is NSNull { return nil }
        if let number = object[key] as? NSNumber { return number.intValue }
        if let string = object[key] as? String { return Int(string) }
        return nil
    }

    private static func parseImportedProduct(_ object: [String: Any]) -> ImportedProduct {
        ImportedProduct(
            title: object["title"] as? String ?? "Shared product",
            priceCents: intValue(object, "priceCents"),
            currencyCode: nullableString(object, "currencyCode"),
            category: object["category"] as? String ?? "Other",
            imageURL: nullableString(object, "imageUrl"),
            sourceURL: object["canonicalUrl"] as? String ?? "",
            sourceDomain: object["sourceDomain"] as? String ?? "",
            retailer: object["retailer"] as? String ?? "",
            status: object["status"] as? String ?? "needs_input",
            note: nullableString(object, "note")
        )
    }

    private static func parseListingItem(_ object: [String: Any]) -> ListingProductStub? {
        guard let sourceURL = object["canonicalUrl"] as? String else { return nil }
        return ListingProductStub(
            title: object["title"] as? String ?? "Shared product",
            priceCents: intValue(object, "priceCents"),
            category: object["category"] as? String ?? "Other",
            imageURL: nullableString(object, "imageUrl"),
            sourceURL: sourceURL,
            sourceDomain: object["sourceDomain"] as? String ?? "",
            retailer: object["retailer"] as? String ?? ""
        )
    }

    private static func parseCommunityProduct(_ object: [String: Any]) -> CommunityProduct? {
        guard let id = object["id"] as? String, let title = object["title"] as? String else { return nil }
        return CommunityProduct(
            id: id,
            title: title,
            priceCents: intValue(object, "priceCents") ?? 0,
            currencyCode: object["currencyCode"] as? String ?? ApiConfig.uaeDirhamCode,
            category: object["category"] as? String ?? "Other",
            imageURL: nullableString(object, "imageUrl"),
            sourceDomain: object["sourceDomain"] as? String ?? "",
            sourceURL: nullableString(object, "sourceUrl"),
            ghostCount: intValue(object, "ghostCount") ?? 1,
            activityTag: object["activityTag"] as? String ?? "User Ghosted"
        )
    }
}

// MARK: - Category mapping from server strings

extension AlmostBuyCategory {
    init(serverName raw: String?) {
        let value = (raw ?? "").lowercased()
        switch true {
        case value.contains("food"), value.contains("grocery"), value.contains("dining"):
            self = .food
        case value.contains("fashion"), value.contains("beauty"), value.contains("apparel"), value.contains("clothing"):
            self = .fashionBeauty
        case value.contains("electronic"), value.contains("tech"), value.contains("gadget"):
            self = .electronics
        case value.contains("luxury"), value.contains("premium"):
            self = .luxury
        case value.contains("home"), value.contains("furniture"), value.contains("kitchen"):
            self = .home
        case value.contains("gaming"), value.contains("game"):
            self = .gaming
        case value.contains("music"), value.contains("audio"), value.contains("instrument"):
            self = .music
        default:
            self = .other
        }
    }
}
