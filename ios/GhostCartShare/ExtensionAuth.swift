import Foundation
import Security

// Read-only counterpart to AuthService's KeychainToken, for the share
// extension. Deliberately NOT a shared source file with AuthService.swift:
// AuthService.restoreSession() calls FirebaseService.shared, which would
// pull FirebaseMessaging/FirebaseCore into this extension for zero benefit
// (the extension only ever needs to read the token, never sign in/out or
// restore a session). Reads the same Keychain item AuthService writes, via
// the keychain-access-groups entitlement both targets now declare - Apple's
// supported Keychain Sharing mechanism, not a new/insecure storage path.
enum ExtensionAuth {
    private static let service = "com.ghostcart.auth"
    private static let account = "accessToken"
    private static let accessGroup = "2A5Q764W66.com.ghostcart.app.shared"

    static func readToken() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessGroup as String: accessGroup,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
