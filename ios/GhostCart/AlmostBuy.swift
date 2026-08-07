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
    case gift = "Gift"
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
        case .gift: return "Gift"
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
    // When cooling actually began (distinct from capturedAt, which can predate
    // cooling if the item sat "captured" first). Powers the timestamp-based
    // Ghost Delivery stage machine (GhostDelivery.swift) - nil on records
    // captured before this field existed, which safely falls back to
    // capturedAt for stage math. Reset on "Send it around again" so a
    // restarted cycle gets its own fresh six-stage delivery.
    var coolingStartedAt: Date?
    var state: AlmostBuyState
    // Set once this item has a counterpart on the backend (mirrors
    // Android's AlmostBuy.serverId in AlmostBuySync.kt). nil means either
    // never synced (guest, offline, or sync just hasn't run yet) - local
    // storage stays the source of truth for the UI either way.
    var serverId: String?
    // Groups items Ghosted together from one share-queue bulk confirm, so
    // Orders/Cooldowns can later resolve a multi-item batch item-by-item.
    // Mirrors Android's AlmostBuy.ghostOrderId (AlmostBuyModels.kt).
    var ghostOrderId: String?

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
        coolingStartedAt: Date? = nil,
        state: AlmostBuyState = .captured,
        serverId: String? = nil,
        ghostOrderId: String? = nil
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
        self.coolingStartedAt = coolingStartedAt
        self.resolvedAt = resolvedAt
        self.state = state
        self.serverId = serverId
        self.ghostOrderId = ghostOrderId
    }

    func isReady(at date: Date = Date()) -> Bool {
        state.isWaiting && (decisionAt ?? .distantFuture) <= date
    }
}

// A cart product is a snapshot rather than a reference to the remote catalog,
// so a guest can finish the simulated checkout offline and catalog changes do
// not silently alter an in-progress cart.
struct GhostCartItem: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var category: String
    var priceCents: Int
    var imageURL: String?
    var quantity: Int
    var cooldownMinutes: Int

    var amount: Double { Double(priceCents * quantity) / 100 }
}

struct SimulatedOrder: Identifiable, Codable, Hashable {
    let id: String
    let items: [GhostCartItem]
    let placedAt: Date
    let subtotalCents: Int
    let promoDiscountCents: Int
    let serviceFeeCents: Int
    let vatCents: Int
    let totalCents: Int
    var deliveryStep: Int

    var totalAmount: Double { Double(totalCents) / 100 }
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
    // Ghost Delivery stage notifications: the five in-progress stages
    // (placed/preparing/picking up/out for delivery/nearby) toggle together;
    // the "delivered" notification stays separately controllable per spec.
    var deliveryStagesEnabled = true
    var deliveredEnabled = true
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
