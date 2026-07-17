import Foundation
import UserNotifications

final class NotificationService {
    static let shared = NotificationService()

    private let center = UNUserNotificationCenter.current()

    private init() {}

    func requestAuthorizationIfNeeded() async -> Bool {
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        default:
            return false
        }
    }

    func scheduleCoolingComplete(for item: AlmostBuy, enabled: Bool) {
        let identifier = coolingIdentifier(for: item.id)
        center.removePendingNotificationRequests(withIdentifiers: [identifier])

        guard enabled, let decisionAt = item.decisionAt, decisionAt > Date() else { return }

        Task {
            guard await requestAuthorizationIfNeeded() else { return }

            let content = UNMutableNotificationContent()
            content.title = "Ready to decide?"
            content.body = "Your cooling period for \(item.name) is complete. Skip it, buy intentionally, or take more time."
            content.sound = .default
            content.userInfo = [
                "destination": "cooldowns",
                "almostBuyID": item.id.uuidString
            ]

            let interval = max(1, decisionAt.timeIntervalSinceNow)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
            try? await center.add(request)
        }
    }

    func cancelCoolingComplete(for id: UUID) {
        center.removePendingNotificationRequests(withIdentifiers: [coolingIdentifier(for: id)])
    }

    func applyRoutinePreferences(_ preferences: ReminderPreferences) {
        let identifiers = ["ghostcart.lunch", "ghostcart.dinner", "ghostcart.late-night", "ghostcart.salary-day"]
        center.removePendingNotificationRequests(withIdentifiers: identifiers)

        guard preferences.pausedUntil.map({ $0 <= Date() }) ?? true else { return }
        guard preferences.lunchEnabled || preferences.dinnerEnabled || preferences.lateNightEnabled || preferences.salaryDayEnabled else { return }

        Task {
            guard await requestAuthorizationIfNeeded() else { return }

            if preferences.lunchEnabled && !isQuietHour(preferences.lunchHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.lunch",
                    hour: preferences.lunchHour,
                    title: "Ghost lunch before ordering?",
                    body: "Capture the craving, cool it for 15 minutes, then decide intentionally."
                )
            }

            if preferences.dinnerEnabled && !isQuietHour(preferences.dinnerHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.dinner",
                    hour: preferences.dinnerHour,
                    title: "Dinner temptation check",
                    body: "Put the order in Ghost Cart first. No real payment. No real delivery."
                )
            }

            if preferences.lateNightEnabled && !isQuietHour(preferences.lateNightHour, preferences: preferences) {
                await scheduleDaily(
                    identifier: "ghostcart.late-night",
                    hour: preferences.lateNightHour,
                    title: "Late-night cart?",
                    body: "Capture it now and give tomorrow-you the decision."
                )
            }

            if preferences.salaryDayEnabled {
                await scheduleMonthlySalaryDay(day: preferences.salaryDay)
            }
        }
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
