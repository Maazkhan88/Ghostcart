import Foundation

// Shared between the GhostCart app target and the GhostCartShare extension.
// The Share Extension writes a pending import into the App Group container;
// the app picks it up the next time it becomes active and seeds the capture
// flow. No auto-foregrounding hack and no custom URL scheme are required,
// which keeps the extension App Review-safe.
enum SharedImportBridge {
    // Must match the App Group entitlement on both targets.
    static let appGroupID = "group.com.ghostcart.app"
    private static let pendingKey = "ghostcart.pending-shared-import"

    static var defaults: UserDefaults? {
        UserDefaults(suiteName: appGroupID)
    }

    static func save(_ pending: PendingSharedImport) {
        guard let defaults, let data = try? JSONEncoder().encode(pending) else { return }
        defaults.set(data, forKey: pendingKey)
    }

    // Reads and clears the pending import so it is consumed exactly once.
    static func takePending() -> PendingSharedImport? {
        guard let defaults, let data = defaults.data(forKey: pendingKey) else { return nil }
        defaults.removeObject(forKey: pendingKey)
        return try? JSONDecoder().decode(PendingSharedImport.self, from: data)
    }
}

struct PendingSharedImport: Codable, Equatable {
    var sourceURL: String
    var sharedTitle: String?
    var sharedImageURL: String?
    var createdAt: Date

    init(sourceURL: String, sharedTitle: String? = nil, sharedImageURL: String? = nil, createdAt: Date = Date()) {
        self.sourceURL = sourceURL
        self.sharedTitle = sharedTitle
        self.sharedImageURL = sharedImageURL
        self.createdAt = createdAt
    }
}
