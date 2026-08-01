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
        guard let object, let id = object["id"] as? String, let email = object["email"] as? String else { return nil }
        return AuthUser(id: id, email: email, displayName: object["displayName"] as? String)
    }
}

// Minimal Keychain wrapper - just enough for a single opaque session token.
private enum KeychainToken {
    private static let service = "com.ghostcart.auth"
    private static let account = "accessToken"

    static func save(_ token: String?) {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account]
        SecItemDelete(query as CFDictionary)
        guard let token, let data = token.data(using: .utf8) else { return }
        var addQuery = query
        addQuery[kSecValueData as String] = data
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    static func load() -> String? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                     kSecAttrService as String: service,
                                     kSecAttrAccount as String: account,
                                     kSecReturnData as String: true,
                                     kSecMatchLimit as String: kSecMatchLimitOne]
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
