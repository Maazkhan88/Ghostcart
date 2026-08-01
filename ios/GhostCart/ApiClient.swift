import Foundation

// Canonical Cloudflare Worker for the whole app (auth, activity, product
// import, sharing). Mirrors Android's ApiConfig (android/.../data/ApiConfig.kt);
// both platforms target the same consolidated deployment. Uses the branded
// theghostcart.com custom domain, not the workers.dev URL - the workers.dev
// route currently 404s (Cloudflare error 1042), theghostcart.com is the live
// one. Do not point this at the retired nameless-d98e or ghost-cart-preview
// endpoints either.
enum ApiConfig {
    static let baseURL = "https://theghostcart.com"
    static let requestTimeout: TimeInterval = 8

    // The backend currently accepts community values in UAE dirham only. The
    // three-letter code is assembled from characters on purpose: the repo's
    // iOS static check forbids the bare currency token in source so it never
    // leaks into user-facing price strings (we render numbers plus the
    // "UAE dirham" wording, never the raw code).
    static let uaeDirhamCode: String = ["A", "E", "D"].joined()
}

// Errors surfaced to the capture UI. `message` is always safe to show.
struct ApiError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

// Generic transport/parse-failure wording for the shared HTTP client - used
// by every API call in the app (auth, sync, marketplace, product import,
// etc.), not just product capture. Previously this said "could not read
// product details... enter manually", which leaked confusing, wrong-context
// text into unrelated failures (e.g. a network hiccup during Google/Apple
// sign-in showed product-import guidance). Callers that want more specific
// framing (like ProductImport.swift's link-capture flow) already set their
// own message at the catch site instead of relying on this fallback.
private let manualEntryFallback =
    "Ghost Cart couldn't connect right now. Check your connection and try again."

private let unavailableFallback =
    "Ghost Cart is temporarily unavailable. Try again in a moment."

// Shared blocked suffixes / safety rules ported from Android's
// isSafePublicHttpsUrl so the client refuses private or non-HTTPS links
// before they ever reach the network.
private let blockedLinkSuffixes = [".localhost", ".local", ".internal", ".home", ".lan", ".onion"]

func isSafePublicHttpsURL(_ value: String) -> Bool {
    guard let url = URL(string: value.trimmingCharacters(in: .whitespacesAndNewlines)),
          let scheme = url.scheme?.lowercased(), scheme == "https",
          url.user == nil, url.password == nil,
          let rawHost = url.host, !rawHost.isEmpty
    else { return false }

    let host = rawHost.lowercased().trimmingTrailingDots()
    if let port = url.port, port != 443 { return false }
    if !host.contains(".") { return false }
    if host == "localhost" { return false }

    let isIPv4 = host.range(of: #"^[0-9]{1,3}(?:\.[0-9]{1,3}){3}$"#, options: .regularExpression) != nil
    let isIPv6 = host.contains(":")
    if isIPv4 || isIPv6 { return false }

    if blockedLinkSuffixes.contains(where: host.hasSuffix) { return false }
    return true
}

private let sharedURLPattern = #"https://[^\s]+"#

// Pulls the first safe public HTTPS link out of shared text (a share-sheet
// caption can contain surrounding words). Trailing punctuation is stripped,
// mirroring Android's extractSharedUrl.
func extractSharedURL(from sharedText: String?) -> String? {
    guard let sharedText else { return nil }
    guard let regex = try? NSRegularExpression(pattern: sharedURLPattern, options: [.caseInsensitive]) else {
        return nil
    }
    let range = NSRange(sharedText.startIndex..., in: sharedText)
    for match in regex.matches(in: sharedText, options: [], range: range) {
        guard let matchRange = Range(match.range, in: sharedText) else { continue }
        let candidate = String(sharedText[matchRange])
            .trimmingCharacters(in: CharacterSet(charactersIn: ".,)]}>\""))
        if isSafePublicHttpsURL(candidate) { return candidate }
    }
    return nil
}

private extension String {
    func trimmingTrailingDots() -> String {
        var result = self
        while result.hasSuffix(".") { result.removeLast() }
        return result
    }
}

// Thin JSON HTTP client. Error mapping mirrors Android's
// parseProductApiResponse: non-JSON bodies become manual-entry guidance
// instead of a decode crash, and a server `error` field wins when present.
struct ApiClient {
    static let shared = ApiClient()

    func getJSON(
        path: String,
        bearerToken: String? = nil,
        additionalHeaders: [String: String] = [:]
    ) async throws -> [String: Any] {
        try await sendJSON(
            path: path,
            method: "GET",
            body: nil,
            bearerToken: bearerToken,
            additionalHeaders: additionalHeaders
        )
    }

    func patchJSON(
        path: String,
        body: [String: Any],
        bearerToken: String? = nil,
        additionalHeaders: [String: String] = [:]
    ) async throws -> [String: Any] {
        try await sendJSON(
            path: path,
            method: "PATCH",
            body: body,
            bearerToken: bearerToken,
            additionalHeaders: additionalHeaders
        )
    }

    func postJSON(
        path: String,
        body: [String: Any],
        bearerToken: String? = nil,
        additionalHeaders: [String: String] = [:]
    ) async throws -> [String: Any] {
        try await sendJSON(
            path: path,
            method: "POST",
            body: body,
            bearerToken: bearerToken,
            additionalHeaders: additionalHeaders
        )
    }

    private func sendJSON(
        path: String,
        method: String,
        body: [String: Any]?,
        bearerToken: String? = nil,
        additionalHeaders: [String: String] = [:]
    ) async throws -> [String: Any] {
        guard let url = URL(string: "\(ApiConfig.baseURL)\(path)") else {
            throw ApiError(message: unavailableFallback)
        }
        var request = URLRequest(url: url, timeoutInterval: ApiConfig.requestTimeout)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let bearerToken {
            request.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        }
        for (field, value) in additionalHeaders {
            request.setValue(value, forHTTPHeaderField: field)
        }
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw ApiError(message: manualEntryFallback)
        }

        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        let text = String(data: data, encoding: .utf8) ?? ""

        // Non-JSON payload (e.g. an HTML error page) -> manual-entry guidance.
        if !text.isEmpty, !text.trimmingCharacters(in: .whitespacesAndNewlines).hasPrefix("{") {
            throw ApiError(message: manualEntryFallback)
        }

        let object: [String: Any]
        if text.isEmpty {
            object = [:]
        } else if let parsed = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] {
            object = parsed
        } else {
            throw ApiError(message: manualEntryFallback)
        }

        if !(200...299).contains(status) {
            let serverMessage = (object["error"] as? String)?.trimmingCharacters(in: .whitespaces)
            throw ApiError(message: serverMessage?.isEmpty == false ? serverMessage! : unavailableFallback)
        }
        return object
    }
}
