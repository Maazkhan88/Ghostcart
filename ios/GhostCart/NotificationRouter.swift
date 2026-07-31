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
        Messaging.messaging().apnsToken = deviceToken
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
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
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
