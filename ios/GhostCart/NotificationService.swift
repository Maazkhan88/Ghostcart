import Foundation
import UserNotifications

enum NotificationAuthorizationState: Equatable {
    case notDetermined
    case authorized
    case denied
}

final class NotificationService {
    static let shared = NotificationService()
    static let coolingCategoryIdentifier = "ghostcart.cooling-ready"
    static let skippedActionIdentifier = "ghostcart.action.skipped"
    static let boughtActionIdentifier = "ghostcart.action.bought"
    static let moreTimeActionIdentifier = "ghostcart.action.more-time"

    private let center = UNUserNotificationCenter.current()
    private var routineTask: Task<Void, Never>?
    private var coolingTasks: [UUID: Task<Void, Never>] = [:]

    private init() {
        let skipped = UNNotificationAction(
            identifier: Self.skippedActionIdentifier,
            title: "Skipped it",
            options: []
        )
        let bought = UNNotificationAction(
            identifier: Self.boughtActionIdentifier,
            title: "Bought it",
            options: []
        )
        let moreTime = UNNotificationAction(
            identifier: Self.moreTimeActionIdentifier,
            title: "More time",
            options: [.foreground]
        )
        let category = UNNotificationCategory(
            identifier: Self.coolingCategoryIdentifier,
            actions: [skipped, bought, moreTime],
            intentIdentifiers: [],
            options: []
        )
        center.setNotificationCategories([category])
    }

    func requestAuthorizationIfNeeded() async -> Bool {
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            await FirebaseService.shared.registerForRemoteNotificationsIfAuthorized()
            return true
        case .notDetermined:
            let granted = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
            if granted {
                await FirebaseService.shared.registerForRemoteNotificationsIfAuthorized()
            }
            return granted
        default:
            return false
        }
    }

    func authorizationState() async -> NotificationAuthorizationState {
        switch await center.notificationSettings().authorizationStatus {
        case .authorized, .provisional, .ephemeral: return .authorized
        case .notDetermined: return .notDetermined
        default: return .denied
        }
    }

    func scheduleCoolingComplete(for item: AlmostBuy, enabled: Bool) {
        let identifier = coolingIdentifier(for: item.id)
        coolingTasks[item.id]?.cancel()
        coolingTasks[item.id] = nil
        center.removePendingNotificationRequests(withIdentifiers: [identifier])

        guard enabled, let decisionAt = item.decisionAt, decisionAt > Date() else { return }

        coolingTasks[item.id] = Task { [weak self] in
            guard let self else { return }
            guard await requestAuthorizationIfNeeded() else { return }
            guard !Task.isCancelled else { return }

            let content = UNMutableNotificationContent()
            content.title = "Ready to decide?"
            content.body = "Your cooling period for \(item.name) is complete. Skip it, buy intentionally, or take more time."
            content.sound = .default
            content.categoryIdentifier = Self.coolingCategoryIdentifier
            content.userInfo = [
                "destination": "cooldowns",
                "almostBuyID": item.id.uuidString
            ]

            let interval = max(1, decisionAt.timeIntervalSinceNow)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
            try? await center.add(request)
            coolingTasks[item.id] = nil
        }
    }

    func cancelCoolingComplete(for id: UUID) {
        coolingTasks[id]?.cancel()
        coolingTasks[id] = nil
        center.removePendingNotificationRequests(withIdentifiers: [coolingIdentifier(for: id)])
    }

    func applyRoutinePreferences(_ preferences: ReminderPreferences) {
        let identifiers = ["ghostcart.lunch", "ghostcart.dinner", "ghostcart.late-night", "ghostcart.salary-day"]
        routineTask?.cancel()
        center.removePendingNotificationRequests(withIdentifiers: identifiers)

        guard preferences.pausedUntil.map({ $0 <= Date() }) ?? true else { return }
        guard preferences.lunchEnabled || preferences.dinnerEnabled || preferences.lateNightEnabled || preferences.salaryDayEnabled else { return }

        routineTask = Task { [weak self] in
            guard let self else { return }
            guard await requestAuthorizationIfNeeded() else { return }
            guard !Task.isCancelled else { return }

            if preferences.lunchEnabled && !isQuietHour(preferences.lunchHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.lunch",
                    hour: preferences.lunchHour,
                    title: "Ghost lunch before ordering?",
                    body: "Capture the craving, cool it for 15 minutes, then decide intentionally."
                )
            }
            guard !Task.isCancelled else { return }

            if preferences.dinnerEnabled && !isQuietHour(preferences.dinnerHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.dinner",
                    hour: preferences.dinnerHour,
                    title: "Dinner temptation check",
                    body: "Put the order in Ghost Cart first. No real payment. No real delivery."
                )
            }
            guard !Task.isCancelled else { return }

            if preferences.lateNightEnabled && !isQuietHour(preferences.lateNightHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.late-night",
                    hour: preferences.lateNightHour,
                    title: "Late-night cart?",
                    body: "Capture it now and give tomorrow-you the decision."
                )
            }
            guard !Task.isCancelled else { return }

            if preferences.salaryDayEnabled {
                await scheduleMonthlySalaryDay(day: preferences.salaryDay)
            }
        }
    }

    func scheduleTestNotification() async throws {
        guard await requestAuthorizationIfNeeded() else {
            throw NotificationServiceError.permissionDenied
        }
        center.removePendingNotificationRequests(withIdentifiers: ["ghostcart.test"])
        let content = UNMutableNotificationContent()
        content.title = "Ghost Cart reminders are working"
        content.body = "This is a test only. No purchase, payment, or delivery happened."
        content.sound = .default
        content.userInfo = ["destination": "profile", "type": "test"]
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false)
        try await center.add(UNNotificationRequest(identifier: "ghostcart.test", content: content, trigger: trigger))
    }

    private func scheduleDaily(identifier: String, hour: Int, title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = ["destination": "capture"]

        var components = DateComponents()
        components.hour = hour
        components.minute = 0
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        try? await center.add(UNNotificationRequest(identifier: identifier, content: content, trigger: trigger))
    }

    private func scheduleMonthlySalaryDay(day: Int) async {
        let content = UNMutableNotificationContent()
        content.title = "Protect the plan before the impulse"
        content.body = "Capture any tempting purchase and give it time before you spend."
        content.sound = .default
        content.userInfo = ["destination": "capture"]

        var components = DateComponents()
        components.day = min(max(day, 1), 28)
        components.hour = 10
        components.minute = 0
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        try? await center.add(UNNotificationRequest(identifier: "ghostcart.salary-day", content: content, trigger: trigger))
    }

    private func isQuietHour(_ hour: Int, preferences: ReminderPreferences) -> Bool {
        guard preferences.quietHoursEnabled else { return false }
        let start = preferences.quietStartHour
        let end = preferences.quietEndHour
        if start == end { return true }
        if start < end { return hour >= start && hour < end }
        return hour >= start || hour < end
    }

    private func coolingIdentifier(for id: UUID) -> String {
        "ghostcart.cooling.\(id.uuidString)"
    }
}

private enum NotificationServiceError: LocalizedError {
    case permissionDenied

    var errorDescription: String? {
        "Notifications are disabled for Ghost Cart. Enable them in iPhone Settings."
    }
}
