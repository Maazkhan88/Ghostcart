import UIKit
import UserNotifications
import FirebaseMessaging
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

extension Notification.Name {
    static let ghostCartNotificationDestination = Notification.Name("ghostcart.notification.destination")
    static let ghostCartNotificationAction = Notification.Name("ghostcart.notification.action")
}

struct PendingNotificationAction: Codable {
    let actionIdentifier: String
    let almostBuyID: UUID
}

enum NotificationActionBridge {
    private static let key = "ghostcart.pending-notification-action"

    static func stage(_ action: PendingNotificationAction) {
        if let data = try? JSONEncoder().encode(action) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    static func take() -> PendingNotificationAction? {
        defer { UserDefaults.standard.removeObject(forKey: key) }
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(PendingNotificationAction.self, from: data)
    }
}

final class GhostCartAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        FirebaseService.shared.configure()
        Task { await FirebaseService.shared.registerForRemoteNotificationsIfAuthorized() }
        #if DEBUG
        if ProcessInfo.processInfo.environment["GHOSTCART_SCHEDULE_TEST_NOTIFICATION"] == "1" {
            Task { try? await NotificationService.shared.scheduleTestNotification() }
        }
        #endif
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02x", $0) }.joined()
        PushDebugLog.log("APNs registration SUCCESS. Device token: \(tokenHex)")
        Messaging.messaging().apnsToken = deviceToken
    }

    // Previously unimplemented - a silent APNs registration failure (wrong
    // aps-environment, missing capability, provisioning mismatch, etc.) had
    // zero visibility with no handler here. FCM can still mint a token via
    // its own Instance ID system even when this fires, so "we have an FCM
    // token" alone never proved APNs registration actually succeeded.
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        PushDebugLog.log("APNs registration FAILED: \(error.localizedDescription)")
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        #if canImport(GoogleSignIn)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let isRemote = notification.request.trigger is UNPushNotificationTrigger
        PushDebugLog.log("Notification RECEIVED (foreground, \(isRemote ? "remote" : "local")): \(notification.request.content.title) / \(notification.request.content.userInfo)")
        completionHandler([.banner, .sound])
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        PushDebugLog.log("Notification RECEIVED (background/silent): \(userInfo)")
        completionHandler(.newData)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushDebugLog.log("Notification TAPPED, action=\(response.actionIdentifier): \(response.notification.request.content.userInfo)")
        let userInfo = response.notification.request.content.userInfo
        if response.actionIdentifier != UNNotificationDefaultActionIdentifier,
           response.actionIdentifier != UNNotificationDismissActionIdentifier,
           let rawID = userInfo["almostBuyID"] as? String,
           let id = UUID(uuidString: rawID) {
            let pending = PendingNotificationAction(actionIdentifier: response.actionIdentifier, almostBuyID: id)
            NotificationActionBridge.stage(pending)
            NotificationCenter.default.post(name: .ghostCartNotificationAction, object: nil)
        }
        GhostAnalytics.notificationOpened(type: userInfo["type"] as? String ?? "cooldown_resolved")
        NotificationCenter.default.post(
            name: .ghostCartNotificationDestination,
            object: nil,
            userInfo: userInfo
        )
        completionHandler()
    }
}
