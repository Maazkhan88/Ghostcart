import Foundation

enum AlmostBuyState: String, Codable, CaseIterable {
    case captured
    case cooling
    case snoozed
    case resolvedSkipped = "resolved_skipped"
    case resolvedBought = "resolved_bought"
    case expired

    var title: String {
        switch self {
        case .captured: return "Captured"
        case .cooling: return "Cooling"
        case .snoozed: return "More time"
        case .resolvedSkipped: return "Skipped"
        case .resolvedBought: return "Bought intentionally"
        case .expired: return "Expired"
        }
    }

    var isWaiting: Bool {
        self == .cooling || self == .snoozed
    }

    var isResolved: Bool {
        self == .resolvedSkipped || self == .resolvedBought
    }
}

enum AlmostBuyCategory: String, Codable, CaseIterable, Identifiable {
    case food
    case fashionBeauty = "fashion_beauty"
    case electronics
    case luxury
    case home
    case gaming
    case music
    case other

    var id: String { rawValue }

    var title: String {
        switch self {
        case .food: return "Food"
        case .fashionBeauty: return "Fashion & beauty"
        case .electronics: return "Electronics"
        case .luxury: return "Luxury"
        case .home: return "Home"
        case .gaming: return "Gaming"
        case .music: return "Music"
        case .other: return "Other"
        }
    }

    var systemImage: String {
        switch self {
        case .food: return "fork.knife"
        case .fashionBeauty: return "tshirt"
        case .electronics: return "laptopcomputer"
        case .luxury: return "sparkles"
        case .home: return "house"
        case .gaming: return "gamecontroller"
        case .music: return "guitars"
        case .other: return "shippingbox"
        }
    }

    var recommendedCooldownMinutes: Int {
        switch self {
        case .food: return 30
        case .fashionBeauty: return 24 * 60
        case .electronics: return 48 * 60
        case .luxury: return 7 * 24 * 60
        case .home, .gaming, .music, .other: return 24 * 60
        }
    }
}

enum SpendingTrigger: String, Codable, CaseIterable, Identifiable {
    case boredom
    case stress
    case fomo
    case hunger
    case reward
    case lateNight = "late_night"
    case practicalNeed = "practical_need"
    case other

    var id: String { rawValue }

    var title: String {
        switch self {
        case .boredom: return "Boredom"
        case .stress: return "Stress"
        case .fomo: return "FOMO"
        case .hunger: return "Hunger"
        case .reward: return "Reward"
        case .lateNight: return "Late-night scrolling"
        case .practicalNeed: return "Practical need"
        case .other: return "Other"
        }
    }
}

enum CaptureSource: String, Codable, CaseIterable, Identifiable {
    case manual
    case url
    case screenshot

    var id: String { rawValue }

    var title: String {
        switch self {
        case .manual: return "Manual"
        case .url: return "Product link"
        case .screenshot: return "Screenshot"
        }
    }
}

struct AlmostBuy: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var amount: Double
    var category: AlmostBuyCategory
    var trigger: SpendingTrigger
    var source: CaptureSource
    var sourceURL: String?
    var imageURL: String?
    var capturedAt: Date
    var decisionAt: Date?
    var resolvedAt: Date?
    var state: AlmostBuyState

    init(
        id: UUID = UUID(),
        name: String,
        amount: Double,
        category: AlmostBuyCategory,
        trigger: SpendingTrigger,
        source: CaptureSource,
        sourceURL: String? = nil,
        imageURL: String? = nil,
        capturedAt: Date = Date(),
        decisionAt: Date? = nil,
        resolvedAt: Date? = nil,
        state: AlmostBuyState = .captured
    ) {
        self.id = id
        self.name = name
        self.amount = amount
        self.category = category
        self.trigger = trigger
        self.source = source
        self.sourceURL = sourceURL
        self.imageURL = imageURL
        self.capturedAt = capturedAt
        self.decisionAt = decisionAt
        self.resolvedAt = resolvedAt
        self.state = state
    }

    func isReady(at date: Date = Date()) -> Bool {
        state.isWaiting && (decisionAt ?? .distantFuture) <= date
    }
}

struct ProgressSnapshot {
    let almostSpent: Double
    let cooling: Double
    let moneyKept: Double
    let boughtIntentionally: Double
}

enum MembershipTheme: String, Codable, CaseIterable, Identifiable {
    case ink
    case paper
    case green

    var id: String { rawValue }
    var title: String { rawValue.capitalized }
}

struct MembershipProfile: Codable, Equatable {
    var displayName: String
    var ghostID: String
    var memberSince: Date
    var theme: MembershipTheme

    static func fresh() -> MembershipProfile {
        let token = UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(8)
        return MembershipProfile(
            displayName: "Ghost Cart Member",
            ghostID: "GC-\(token)",
            memberSince: Date(),
            theme: .ink
        )
    }
}

enum AppearancePreference: String, Codable, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }
    var title: String { rawValue.capitalized }
}

struct ReminderPreferences: Codable, Equatable {
    var coolingCompleteEnabled = true
    var lunchEnabled = false
    var dinnerEnabled = false
    var lateNightEnabled = false
    var salaryDayEnabled = false
    var lunchHour = 13
    var dinnerHour = 20
    var lateNightHour = 22
    var salaryDay = 1
    var quietHoursEnabled = true
    var quietStartHour = 22
    var quietEndHour = 8
    var pausedUntil: Date?
}

struct GhostCartPreferences: Codable, Equatable {
    var reminders = ReminderPreferences()
    var appearance: AppearancePreference = .system
    var reduceDecorativeMotion = false
}
