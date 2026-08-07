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
        guard !Self.isConfigured else {
            PushDebugLog.log("FirebaseApp.configure() skipped - already configured")
            return true
        }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let options = FirebaseOptions(contentsOfFile: path) else {
            PushDebugLog.log("FirebaseApp.configure() FAILED - GoogleService-Info.plist missing or unreadable")
            return false
        }

        FirebaseApp.configure(options: options)
        Self.isConfigured = true
        Messaging.messaging().delegate = FirebaseMessagingDelegate.shared
        PushDebugLog.log("FirebaseApp.configure() SUCCESS - bundle=\(options.bundleID) gcmSenderID=\(options.gcmSenderID) projectID=\(options.projectID ?? "nil")")
        return true
    }

    func receivedFCMToken(_ token: String) {
        let previous = UserDefaults.standard.string(forKey: tokenKey)
        PushDebugLog.log(previous == token ? "FCM token unchanged: \(token)" : "FCM token REFRESHED. New: \(token) Previous: \(previous ?? "none")")
        UserDefaults.standard.set(token, forKey: tokenKey)
        Task { await registerStoredTokenIfSignedIn() }
    }

    func registerStoredTokenIfSignedIn() async {
        guard AuthService.shared.isSignedIn else {
            PushDebugLog.log("Backend token upload SKIPPED - not signed in")
            return
        }
        guard let token = UserDefaults.standard.string(forKey: tokenKey), !token.isEmpty else {
            PushDebugLog.log("Backend token upload SKIPPED - no FCM token stored yet")
            return
        }

        do {
            _ = try await AuthService.shared.authorizedPost(
                path: "/api/me/device-tokens",
                body: ["token": token, "platform": "ios"]
            )
            PushDebugLog.log("Backend token upload SUCCESS: \(token)")
        } catch {
            PushDebugLog.log("Backend token upload FAILED: \(error.localizedDescription)")
        }
    }

    func registerForRemoteNotificationsIfAuthorized() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        let status = Self.describe(settings.authorizationStatus)
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
            PushDebugLog.log("registerForRemoteNotifications() SKIPPED - authorization status: \(status)")
            return
        }
        PushDebugLog.log("Calling UIApplication.registerForRemoteNotifications() - authorization status: \(status)")
        UIApplication.shared.registerForRemoteNotifications()
    }

    private static func describe(_ status: UNAuthorizationStatus) -> String {
        switch status {
        case .authorized: return "authorized"
        case .denied: return "denied"
        case .notDetermined: return "notDetermined"
        case .provisional: return "provisional"
        case .ephemeral: return "ephemeral"
        @unknown default: return "unknown(\(status.rawValue))"
        }
    }
}

final class FirebaseMessagingDelegate: NSObject, MessagingDelegate {
    static let shared = FirebaseMessagingDelegate()

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else {
            PushDebugLog.log("didReceiveRegistrationToken called with nil token")
            return
        }
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

    // Ghost Delivery / tutorial event set (product spec's required minimum).
    static func tutorialStarted() { event("tutorial_started") }
    static func tutorialStepViewed(_ step: String) { event("tutorial_step_viewed", parameters: ["step": step]) }
    static func tutorialCompleted() { event("tutorial_completed") }
    static func tutorialSkipped(atStep step: String) { event("tutorial_skipped", parameters: ["step": step]) }
    static func tutorialReplayed() { event("tutorial_replayed") }
    static func productFavorited(_ productID: String, favorited: Bool) {
        event("product_favorited", parameters: ["product_id": productID, "favorited": favorited])
    }
    static func productShared(_ productID: String) { event("product_shared", parameters: ["product_id": productID]) }
    static func productReviewsOpened(_ productID: String) { event("product_reviews_opened", parameters: ["product_id": productID]) }
    static func ghostItTapped(category: String) { event("ghost_it_tapped", parameters: ["category": category]) }
    static func deliveryDurationSelected(minutes: Int) { event("delivery_duration_selected", parameters: ["minutes": minutes]) }
    static func ghostOrderPlaced(category: String, minutes: Int) {
        event("ghost_order_placed", parameters: ["category": category, "minutes": minutes])
    }
    static func ghostStageReached(_ stage: String) { event("ghost_stage_reached", parameters: ["stage": stage]) }
    static func ghostNotificationOpened(_ stage: String) { event("ghost_notification_opened", parameters: ["stage": stage]) }
    static func trackerOpened() { event("tracker_opened") }
    static func sourceOpenedDuringCooling() { event("source_opened_during_cooling") }
    static func ghostOrderDelivered() { event("ghost_order_delivered") }
    static func decisionSkipped() { event("decision_skipped") }
    static func decisionRestarted() { event("decision_restarted") }
    static func decisionBuyFromSource() { event("decision_buy_from_source") }
    static func decisionBoughtAlready() { event("decision_bought_already") }
    static func askFriendStarted() { event("ask_friend_started") }
    static func ghostReceiptShared() { event("ghost_receipt_shared") }
    static func walletOpened() { event("wallet_opened") }
    static func leaderboardOpened() { event("leaderboard_opened") }

    static func storyViewed(index: Int) {
        event("story_viewed", parameters: ["index": index])
    }

    static func shared(contentType: String) {
        event(AnalyticsEventShare, parameters: [AnalyticsParameterContentType: contentType])
    }
}
