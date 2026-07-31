import Foundation
import UIKit
import UserNotifications
import FirebaseAnalytics
import FirebaseCore
import FirebaseMessaging

/// Owns the shared Firebase lifecycle and keeps the FCM token in sync with
/// Ghost Cart's existing bearer-token backend. Firebase is intentionally not
/// configured until the Apple-specific GoogleService-Info.plist is present.
@MainActor
final class FirebaseService {
    static let shared = FirebaseService()
    private(set) nonisolated(unsafe) static var isConfigured = false

    private let tokenKey = "ghostcart.firebase.fcm-token"

    private init() {}

    @discardableResult
    func configure() -> Bool {
        guard !Self.isConfigured else { return true }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let options = FirebaseOptions(contentsOfFile: path) else {
            #if DEBUG
            print("[Firebase] GoogleService-Info.plist is missing; Firebase is not configured.")
            #endif
            return false
        }

        FirebaseApp.configure(options: options)
        Self.isConfigured = true
        Messaging.messaging().delegate = FirebaseMessagingDelegate.shared
        return true
    }

    func receivedFCMToken(_ token: String) {
        UserDefaults.standard.set(token, forKey: tokenKey)
        Task { await registerStoredTokenIfSignedIn() }
    }

    func registerStoredTokenIfSignedIn() async {
        guard AuthService.shared.isSignedIn,
              let token = UserDefaults.standard.string(forKey: tokenKey),
              !token.isEmpty else { return }

        _ = try? await AuthService.shared.authorizedPost(
            path: "/api/me/device-tokens",
            body: ["token": token, "platform": "ios"]
        )
    }

    func registerForRemoteNotificationsIfAuthorized() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }
        UIApplication.shared.registerForRemoteNotifications()
    }
}

final class FirebaseMessagingDelegate: NSObject, MessagingDelegate {
    static let shared = FirebaseMessagingDelegate()

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Task { @MainActor in
            FirebaseService.shared.receivedFCMToken(fcmToken)
        }
    }
}

/// iOS counterpart of Android's typed Analytics wrapper. Keeping the event
/// names identical preserves the existing GA4 funnels across both apps.
enum GhostAnalytics {
    static func event(_ name: String, parameters: [String: Any]? = nil) {
        guard FirebaseService.isConfigured else { return }
        Analytics.logEvent(name, parameters: parameters)
    }

    static func signIn(method: String = "app") {
        event(AnalyticsEventLogin, parameters: [AnalyticsParameterMethod: method])
    }

    static func screen(_ name: String) {
        event(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: name,
            AnalyticsParameterScreenClass: name,
        ])
    }

    static func notificationReceived(type: String) {
        event("notification_received", parameters: ["type": type])
    }

    static func notificationOpened(type: String) {
        event("notification_opened", parameters: ["type": type])
    }

    static func storyViewed(index: Int) {
        event("story_viewed", parameters: ["index": index])
    }

    static func shared(contentType: String) {
        event(AnalyticsEventShare, parameters: [AnalyticsParameterContentType: contentType])
    }
}
