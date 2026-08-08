import Foundation
import Security

extension Notification.Name {
    // Fired after any successful sign-in (password, Google, or Apple) or a
    // restored session on launch - GhostCartStore observes this to pull
    // server-side almost-buy history down (AlmostBuySyncService.fetchRemote).
    static let ghostCartDidSignIn = Notification.Name("ghostcart.didSignIn")
}

// Mirrors Android's AuthRepository: opaque bearer tokens against
// /api/auth/{signup,signin,session,signout} (docs/backend-v2.md). Token is
// kept in the Keychain (a credential, unlike the rest of the app's local
// state which lives in UserDefaults via GhostCartStore).
struct AuthUser: Codable, Equatable {
    let id: String
    let email: String
    let displayName: String?
}

enum AuthState: Equatable {
    case guest
    case signedIn(AuthUser)
}

@MainActor
final class AuthService: ObservableObject {
    static let shared = AuthService()

    @Published private(set) var state: AuthState = .guest
    private var accessToken: String? {
        didSet { KeychainToken.save(accessToken) }
    }

    private init() {
        accessToken = KeychainToken.load()
    }

    var isSignedIn: Bool { if case .signedIn = state { return true }; return false }

    func signUp(email: String, password: String, displayName: String?) async throws {
        var body: [String: Any] = ["email": email, "password": password]
        if let displayName, !displayName.trimmingCharacters(in: .whitespaces).isEmpty {
            body["displayName"] = displayName
        }
        try await authenticate(path: "/api/auth/signup", body: body)
    }

    func signIn(email: String, password: String) async throws {
        try await authenticate(path: "/api/auth/signin", body: ["email": email, "password": password])
    }

    func signInWithGoogle(idToken: String) async throws {
        try await authenticate(
            path: "/api/auth/google",
            body: ["idToken": idToken],
            analyticsMethod: "google"
        )
    }

    func signInWithApple(identityToken: String, nonce: String, displayName: String?) async throws {
        var body: [String: Any] = ["identityToken": identityToken, "nonce": nonce]
        if let displayName, !displayName.isEmpty { body["displayName"] = displayName }
        try await authenticate(
            path: "/api/auth/apple",
            body: body,
            analyticsMethod: "apple"
        )
    }

    func continueAsGuest() {
        accessToken = nil
        state = .guest
    }

    func signOut() {
        let token = accessToken
        accessToken = nil
        state = .guest
        guard let token else { return }
        Task {
            _ = try? await ApiClient.shared.postJSON(path: "/api/auth/signout", body: [:], bearerToken: token)
        }
    }

    /// Restores a session on launch by validating any saved token.
    func restoreSession() async {
        guard let token = accessToken else { return }
        do {
            let object = try await ApiClient.shared.getJSON(path: "/api/auth/session", bearerToken: token)
            if let user = Self.parseUser(object["user"] as? [String: Any]) {
                state = .signedIn(user)
                await FirebaseService.shared.registerStoredTokenIfSignedIn()
                NotificationCenter.default.post(name: .ghostCartDidSignIn, object: nil)
            } else {
                accessToken = nil
            }
        } catch {
            accessToken = nil
        }
    }

    /// Attaches the bearer token to an arbitrary authenticated call.
    func authorizedGet(path: String) async throws -> [String: Any] {
        guard let accessToken else { throw ApiError(message: "Sign in to continue.") }
        return try await ApiClient.shared.getJSON(path: path, bearerToken: accessToken)
    }

    func authorizedPost(path: String, body: [String: Any]) async throws -> [String: Any] {
        guard let accessToken else { throw ApiError(message: "Sign in to continue.") }
        return try await ApiClient.shared.postJSON(path: path, body: body, bearerToken: accessToken)
    }

    func authorizedPatch(path: String, body: [String: Any]) async throws -> [String: Any] {
        guard let accessToken else { throw ApiError(message: "Sign in to continue.") }
        return try await ApiClient.shared.patchJSON(path: path, body: body, bearerToken: accessToken)
    }

    func authorizedDelete(path: String) async throws -> [String: Any] {
        guard let accessToken else { throw ApiError(message: "Sign in to continue.") }
        return try await ApiClient.shared.deleteJSON(path: path, bearerToken: accessToken)
    }

    // For endpoints that work signed-out but attach the current session when
    // one exists (e.g. gift reveal, which links the gift to the recipient's
    // account only if they happen to be signed in with a matching email).
    func optionallyAuthorizedPost(path: String, body: [String: Any]) async throws -> [String: Any] {
        try await ApiClient.shared.postJSON(path: path, body: body, bearerToken: accessToken)
    }

    private func authenticate(
        path: String,
        body: [String: Any],
        analyticsMethod: String = "password"
    ) async throws {
        let object = try await ApiClient.shared.postJSON(path: path, body: body)
        guard let token = object["accessToken"] as? String,
              let user = Self.parseUser(object["user"] as? [String: Any]) else {
            throw ApiError(message: "Sign-in succeeded but the response was unreadable. Try again.")
        }
        accessToken = token
        state = .signedIn(user)
        GhostAnalytics.signIn(method: analyticsMethod)
        await FirebaseService.shared.registerStoredTokenIfSignedIn()
        NotificationCenter.default.post(name: .ghostCartDidSignIn, object: nil)
    }

    private static func parseUser(_ object: [String: Any]?) -> AuthUser? {
        // users.id is an integer primary key server-side (db/schema.ts), so
        // JSONSerialization hands this back as an NSNumber, not a String -
        // "id" as? String always failed here, silently breaking every
        // sign-in method (not just Google/Apple) with a generic "response
        // was unreadable" error after the server had already succeeded.
        guard let object, let email = object["email"] as? String else { return nil }
        let id: String
        if let stringId = object["id"] as? String {
            id = stringId
        } else if let numberId = object["id"] as? NSNumber {
            id = numberId.stringValue
        } else {
            return nil
        }
        return AuthUser(id: id, email: email, displayName: object["displayName"] as? String)
    }
}

// Minimal Keychain wrapper - just enough for a single opaque session token.
// Shared with GhostCartShare via Keychain Sharing (the
// keychain-access-groups entitlement on both targets) so the share
// extension's mini-capture UI can read the same session token without any
// new, separately-secured storage - Apple's supported mechanism for this,
// not a workaround. Without an explicit access group, a Keychain item
// resolves to the app's own private group (TEAMID.bundleID) and is
// invisible to any other target, extensions included.
private enum KeychainToken {
    private static let service = "com.ghostcart.auth"
    private static let account = "accessToken"
    static let sharedAccessGroup = "2A5Q764W66.com.ghostcart.app.shared"

    static func save(_ token: String?) {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account,
                                     kSecAttrAccessGroup as String: sharedAccessGroup]
        SecItemDelete(query as CFDictionary)
        guard let token, let data = token.data(using: .utf8) else { return }
        var addQuery = query
        addQuery[kSecValueData as String] = data
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    static func load() -> String? {
        if let token = load(accessGroup: sharedAccessGroup) { return token }
        // Migration path: anyone who signed in before this build has their
        // token stored under the implicit, per-target default access group
        // (no kSecAttrAccessGroup at all) - a Keychain item's access group
        // is part of its identity, so simply changing the query above would
        // otherwise make every existing session invisible (silently
        // signing everyone out, not just in the new share extension) rather
        // than actually finding nothing. Re-save under the new shared
        // group and clean up the old entry so this only ever runs once per
        // install.
        guard let legacyToken = load(accessGroup: nil) else { return nil }
        // Delete the legacy (no-access-group) entry before writing the new
        // one, not after - deleting without an explicit access group could
        // otherwise ambiguously match the just-written shared-group item
        // too, undoing the migration it was meant to complete.
        deleteLegacy()
        save(legacyToken)
        return legacyToken
    }

    private static func load(accessGroup: String?) -> String? {
        var query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account,
                                     kSecReturnData as String: true,
                                     kSecMatchLimit as String: kSecMatchLimitOne]
        if let accessGroup { query[kSecAttrAccessGroup as String] = accessGroup }
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func deleteLegacy() {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account]
        SecItemDelete(query as CFDictionary)
    }
}
