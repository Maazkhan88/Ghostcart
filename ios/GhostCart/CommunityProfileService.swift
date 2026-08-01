import Foundation

// Mirrors Android's CommunityProfileRepository + ProfileCommunitySection
// (GhostCartV2Screens.kt) against GET/PATCH /api/me/profile. Two independent
// things live on this one server record: displayName (always editable), and
// opting in/out of the public leaderboard via username + communityConsent.
// Opting out leaves the username reserved server-side so re-opting in later
// keeps the same identity - never delete it client-side either.
struct CommunityProfile: Equatable {
    var email: String
    var displayName: String?
    var username: String?
    var avatarUrl: String?
    var communityConsent: Bool
    var gender: String?
    var avatarPresetId: String?
    var showRecentActivityPublicly: Bool
}

enum CommunityProfileService {
    static func fetch() async -> CommunityProfile? {
        guard let response = try? await AuthService.shared.authorizedGet(path: "/api/me/profile"),
              let profile = response["profile"] as? [String: Any] else { return nil }
        return parse(profile)
    }

    /// Sends only the fields that changed. Throws the server's ApiError message
    /// (rename-cooldown 429, duplicate-username 409, validation 400) so the
    /// caller can show it directly - these are all meant to be user-readable.
    static func update(
        displayName: String? = nil,
        username: String? = nil,
        communityConsent: Bool? = nil,
        showRecentActivityPublicly: Bool? = nil
    ) async throws -> CommunityProfile {
        var body: [String: Any] = [:]
        if let displayName { body["displayName"] = displayName }
        if let username { body["username"] = username }
        if let communityConsent { body["communityConsent"] = communityConsent }
        if let showRecentActivityPublicly { body["showRecentActivityPublicly"] = showRecentActivityPublicly }
        let response = try await AuthService.shared.authorizedPatch(path: "/api/me/profile", body: body)
        guard let profile = response["profile"] as? [String: Any], let parsed = parse(profile) else {
            throw ApiError(message: "Could not update profile.")
        }
        return parsed
    }

    private static func parse(_ object: [String: Any]) -> CommunityProfile? {
        guard let email = object["email"] as? String else { return nil }
        return CommunityProfile(
            email: email,
            displayName: object["displayName"] as? String,
            username: object["username"] as? String,
            avatarUrl: object["avatarUrl"] as? String,
            communityConsent: (object["communityConsent"] as? Bool) ?? false,
            gender: object["gender"] as? String,
            avatarPresetId: object["avatarPresetId"] as? String,
            showRecentActivityPublicly: (object["showRecentActivityPublicly"] as? Bool) ?? false
        )
    }
}
