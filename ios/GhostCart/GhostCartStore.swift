import Foundation
import SwiftUI
import UserNotifications

final class GhostCartStore: ObservableObject {
    @Published private(set) var items: [AlmostBuy] = []
    @Published private(set) var cartItems: [GhostCartItem] = []
    @Published private(set) var activeOrder: SimulatedOrder?
    // Transient (not persisted): a community tap or a shared link stages a
    // pre-filled capture that the Ghost + screen picks up on appear.
    @Published var captureSeed: CaptureSeed?
    // Persisted separately (see ShareQueue.swift) - mirrors Android's
    // "share_queue" SharedPreferences entry. Populated once a second share
    // arrives while an earlier one hasn't been confirmed yet.
    @Published var shareQueue: [ShareQueueItem] = []
    @Published var membership: MembershipProfile {
        didSet { save() }
    }
    @Published var preferences: GhostCartPreferences {
        didSet {
            guard !isLoading else { return }
            save()
            NotificationService.shared.applyRoutinePreferences(preferences.reminders)
        }
    }

    private let persistenceKey = "ghostcart.v2.local-state"
    private let notificationService: NotificationService
    private var isLoading = true

    init(notificationService: NotificationService = .shared) {
        self.notificationService = notificationService
        self.membership = .fresh()
        self.preferences = GhostCartPreferences()
        restore()
        loadShareQueue()
        expireAbandonedDecisions()
        isLoading = false
        notificationService.applyRoutinePreferences(preferences.reminders)
        refreshCoolingNotifications()
        NotificationCenter.default.addObserver(forName: .ghostCartDidSignIn, object: nil, queue: .main) { [weak self] _ in
            Task { await self?.syncFromServer() }
        }
    }

    var progress: ProgressSnapshot {
        ProgressSnapshot(
            almostSpent: items.reduce(0) { $0 + $1.amount },
            cooling: items.filter { $0.state.isWaiting }.reduce(0) { $0 + $1.amount },
            moneyKept: items.filter { $0.state == .resolvedSkipped }.reduce(0) { $0 + $1.amount },
            boughtIntentionally: items.filter { $0.state == .resolvedBought }.reduce(0) { $0 + $1.amount }
        )
    }

    var readyItems: [AlmostBuy] {
        items.filter { $0.isReady() }.sorted { ($0.decisionAt ?? .distantPast) < ($1.decisionAt ?? .distantPast) }
    }

    var coolingItems: [AlmostBuy] {
        items.filter { $0.state.isWaiting && !$0.isReady() }.sorted { ($0.decisionAt ?? .distantFuture) < ($1.decisionAt ?? .distantFuture) }
    }

    var capturedItems: [AlmostBuy] {
        items.filter { $0.state == .captured }.sorted { $0.capturedAt > $1.capturedAt }
    }

    var recentDecisions: [AlmostBuy] {
        items.filter { $0.state.isResolved }.sorted { ($0.resolvedAt ?? .distantPast) > ($1.resolvedAt ?? .distantPast) }
    }

    var cartQuantity: Int { cartItems.reduce(0) { $0 + $1.quantity } }
    var cartSubtotalCents: Int { cartItems.reduce(0) { $0 + $1.priceCents * $1.quantity } }

    @discardableResult
    func capture(
        name: String,
        amount: Double,
        category: AlmostBuyCategory,
        trigger: SpendingTrigger,
        source: CaptureSource,
        sourceURL: String?,
        imageURL: String? = nil,
        ghostOrderId: String? = nil,
        onServerId: ((String) -> Void)? = nil
    ) -> UUID {
        let item = AlmostBuy(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            amount: max(0, amount),
            category: category,
            trigger: trigger,
            source: source,
            sourceURL: sourceURL?.trimmingCharacters(in: .whitespacesAndNewlines),
            imageURL: imageURL,
            ghostOrderId: ghostOrderId
        )
        items.insert(item, at: 0)
        save()
        syncCreateInBackground(item, onServerId: onServerId)
        return item.id
    }

    func stageCapture(_ seed: CaptureSeed) {
        captureSeed = seed
    }

    func consumeCaptureSeed() -> CaptureSeed? {
        defer { captureSeed = nil }
        return captureSeed
    }

    func addToCart(_ product: MarketplaceProduct) {
        if let index = cartItems.firstIndex(where: { $0.id == product.id }) {
            cartItems[index].quantity += 1
        } else {
            let mappedCategory = AlmostBuyCategory(serverName: product.category)
            cartItems.append(GhostCartItem(
                id: product.id,
                name: product.name,
                category: product.category,
                priceCents: max(0, product.priceCents),
                imageURL: product.imageUrl,
                sourceURL: product.sourceURL,
                quantity: 1,
                cooldownMinutes: mappedCategory.recommendedCooldownMinutes
            ))
        }
        save()
    }

    func incrementCartItem(id: String) {
        guard let index = cartItems.firstIndex(where: { $0.id == id }) else { return }
        cartItems[index].quantity += 1
        save()
    }

    func decrementCartItem(id: String) {
        guard let index = cartItems.firstIndex(where: { $0.id == id }) else { return }
        if cartItems[index].quantity > 1 {
            cartItems[index].quantity -= 1
        } else {
            cartItems.remove(at: index)
        }
        save()
    }

    func clearCart() {
        cartItems.removeAll()
        save()
    }

    // giftCartItemId/giftRecipientName/giftRecipientEmail mirror Android's
    // "Send as a gift" checkbox inside Fake Checkout (CheckoutFlowScreens.kt):
    // the chosen cart product is captured like any other, and only once its
    // server sync round-trip returns a serverId does the gift POST fire -
    // there's no separate "pick an existing item" gifting flow.
    //
    // ghostDeliveryMinutes is the single duration chosen once at checkout
    // (the "When should your Ghost Order be delivered?" prompt) and applies
    // to every item in the order under one shared ghostOrderId - "Ghost it"
    // itself never asks for a duration, only adds to cart. Mirrors the
    // Android product contract in docs/claude-ios-handoff-ghost-delivery.md.
    @discardableResult
    func completeSimulatedCheckout(
        ghostDeliveryMinutes: Int,
        giftCartItemId: String? = nil,
        giftRecipientName: String? = nil,
        giftRecipientEmail: String? = nil
    ) -> SimulatedOrder? {
        guard !cartItems.isEmpty else { return nil }
        let subtotal = cartSubtotalCents
        let promo = Int((Double(subtotal) * 0.10).rounded(.down))
        let discounted = subtotal - promo
        let fee = Int((Double(discounted) * 0.05).rounded(.down))
        let vat = Int((Double(discounted) * 0.05).rounded(.down))
        let order = SimulatedOrder(
            id: "GC-\(String(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(8)).uppercased())",
            items: cartItems,
            placedAt: Date(),
            subtotalCents: subtotal,
            promoDiscountCents: promo,
            serviceFeeCents: fee,
            vatCents: vat,
            totalCents: discounted + fee + vat,
            deliveryStep: 0
        )

        let ghostOrderId = "GO-\(UUID().uuidString)"
        var giftAlreadyAttached = false
        for cartItem in cartItems {
            for _ in 0..<cartItem.quantity {
                let attachGift = !giftAlreadyAttached && cartItem.id == giftCartItemId
                    && giftRecipientName != nil && giftRecipientEmail != nil
                if attachGift { giftAlreadyAttached = true }
                let recipientName = giftRecipientName
                let recipientEmail = giftRecipientEmail
                let id = capture(
                    name: cartItem.name,
                    amount: Double(cartItem.priceCents) / 100,
                    category: AlmostBuyCategory(serverName: cartItem.category),
                    trigger: .other,
                    source: .manual,
                    sourceURL: cartItem.sourceURL,
                    imageURL: cartItem.imageURL,
                    ghostOrderId: ghostOrderId,
                    onServerId: attachGift ? { serverId in
                        guard let recipientName, let recipientEmail else { return }
                        Task {
                            try? await GhostGiftService.create(
                                almostBuyId: serverId,
                                recipientName: recipientName,
                                recipientEmail: recipientEmail
                            )
                        }
                    } : nil
                )
                startCooling(id: id, minutes: ghostDeliveryMinutes)
            }
        }
        cartItems.removeAll()
        activeOrder = order
        save()
        return order
    }

    func advanceDelivery() {
        guard var order = activeOrder else { return }
        order.deliveryStep = min(order.deliveryStep + 1, 4)
        activeOrder = order
        save()
    }

    func dismissDelivery() {
        activeOrder = nil
        save()
    }

    func startCooling(id: UUID, minutes: Int) {
        let now = Date()
        updateItem(id: id) { item in
            item.state = .cooling
            item.coolingStartedAt = now
            item.decisionAt = Calendar.current.date(byAdding: .minute, value: max(1, minutes), to: now)
            item.resolvedAt = nil
        }
        guard let item = item(id: id) else { return }
        notificationService.scheduleCoolingComplete(for: item, enabled: preferences.reminders.coolingCompleteEnabled)
        notificationService.scheduleDeliveryStages(
            for: item,
            stagesEnabled: preferences.reminders.deliveryStagesEnabled,
            deliveredEnabled: preferences.reminders.deliveredEnabled
        )
        GhostAnalytics.ghostOrderPlaced(category: item.category.rawValue, minutes: minutes)
        syncExtendOrCreateInBackground(item)
    }

    // "Send it around again": a fresh Ghost Delivery cycle for the same item.
    // The prior cycle's resolution (if any) is not touched here - callers
    // that want history preserve it themselves before calling this. Never
    // adds Money Kept.
    func restartGhostDelivery(id: UUID, minutes: Int) {
        notificationService.cancelDeliveryStages(for: id)
        startCooling(id: id, minutes: minutes)
    }

    func resolve(id: UUID, outcome: AlmostBuyState) {
        guard outcome == .resolvedSkipped || outcome == .resolvedBought else { return }
        updateItem(id: id) { item in
            item.state = outcome
            item.resolvedAt = Date()
        }
        notificationService.cancelCoolingComplete(for: id)
        notificationService.cancelDeliveryStages(for: id)
        guard let item = item(id: id) else { return }
        syncResolveInBackground(item)
    }

    func snooze(id: UUID, minutes: Int) {
        updateItem(id: id) { item in
            item.state = .snoozed
            item.decisionAt = Calendar.current.date(byAdding: .minute, value: max(1, minutes), to: Date())
            item.resolvedAt = nil
        }
        guard let item = item(id: id) else { return }
        notificationService.scheduleCoolingComplete(for: item, enabled: preferences.reminders.coolingCompleteEnabled)
        syncExtendOrCreateInBackground(item)
    }

    // MARK: - Server sync (best-effort, never blocks the caller)

    private func syncCreateInBackground(_ item: AlmostBuy, onServerId: ((String) -> Void)? = nil) {
        Task { @MainActor [weak self] in
            guard let serverId = await AlmostBuySyncService.syncCreate(item) else { return }
            self?.attachServerId(serverId, to: item.id)
            onServerId?(serverId)
        }
    }

    private func syncExtendOrCreateInBackground(_ item: AlmostBuy) {
        Task { @MainActor [weak self] in
            if let serverId = item.serverId {
                await AlmostBuySyncService.syncExtend(serverId: serverId, decisionAt: item.decisionAt ?? Date())
            } else if let serverId = await AlmostBuySyncService.syncCreate(item) {
                self?.attachServerId(serverId, to: item.id)
            }
        }
    }

    private func syncResolveInBackground(_ item: AlmostBuy) {
        Task { @MainActor [weak self] in
            if let serverId = item.serverId {
                await AlmostBuySyncService.syncResolve(serverId: serverId, outcome: item.state)
            } else if let serverId = await AlmostBuySyncService.syncResolvedBackfill(item) {
                self?.attachServerId(serverId, to: item.id)
            }
        }
    }

    private func attachServerId(_ serverId: String, to id: UUID) {
        updateItem(id: id) { $0.serverId = serverId }
    }

    // Called after sign-in (see AuthService's .ghostCartDidSignIn
    // notification, observed in init below). Purely additive: pulls the
    // account's server history and adds any item this install doesn't
    // already have (matched by serverId) - never deletes or overwrites a
    // local item, so a failed/partial fetch can never lose data. Also
    // best-effort backfills local resolved items that predate this
    // account/session having a serverId, mirroring Android's
    // syncResolvedBackfill call site.
    func syncFromServer() async {
        if let remoteItems = await AlmostBuySyncService.fetchRemote() {
            await MainActor.run {
                let merged = Self.mergeRemoteItems(local: items, remote: remoteItems)
                guard merged.count != items.count else { return }
                items = merged
                save()
            }
        }
        let unsyncedResolved = await MainActor.run {
            items.filter { $0.serverId == nil && $0.state.isResolved }
        }
        for item in unsyncedResolved {
            syncResolveInBackground(item)
        }
    }

    // Pure, side-effect-free merge: purely additive, pulls the account's
    // server history in and adds any item this install doesn't already have
    // (matched by serverId) - never deletes or overwrites a local item, so a
    // failed/partial fetch can never lose data. Extracted from
    // syncFromServer so the actual dedup/sort logic is unit-testable without
    // mocking the network layer.
    static func mergeRemoteItems(local: [AlmostBuy], remote: [AlmostBuy]) -> [AlmostBuy] {
        let knownServerIds = Set(local.compactMap(\.serverId))
        let newItems = remote.filter { !knownServerIds.contains($0.serverId ?? "") }
        guard !newItems.isEmpty else { return local }
        return (local + newItems).sorted { $0.capturedAt > $1.capturedAt }
    }

    func delete(id: UUID) {
        items.removeAll { $0.id == id }
        notificationService.cancelCoolingComplete(for: id)
        notificationService.cancelDeliveryStages(for: id)
        save()
    }

    func updateMembership(displayName: String, theme: MembershipTheme) {
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        membership.displayName = trimmed.isEmpty ? "Ghost Cart Member" : trimmed
        membership.theme = theme
    }

    func setRoutineReminder(_ keyPath: WritableKeyPath<ReminderPreferences, Bool>, enabled: Bool) {
        preferences.reminders[keyPath: keyPath] = enabled
    }

    func pauseRoutineReminders(days: Int) {
        preferences.reminders.pausedUntil = Calendar.current.date(byAdding: .day, value: max(days, 1), to: Date())
    }

    func resumeRoutineReminders() {
        preferences.reminders.pausedUntil = nil
    }

    func refreshCoolingNotifications() {
        for item in items where item.state.isWaiting {
            notificationService.scheduleCoolingComplete(for: item, enabled: preferences.reminders.coolingCompleteEnabled)
        }
    }

    func refreshDeliveryStageNotifications() {
        for item in items where item.state.isWaiting {
            notificationService.scheduleDeliveryStages(
                for: item,
                stagesEnabled: preferences.reminders.deliveryStagesEnabled,
                deliveredEnabled: preferences.reminders.deliveredEnabled
            )
        }
    }

    private func item(id: UUID) -> AlmostBuy? {
        items.first { $0.id == id }
    }

    private func updateItem(id: UUID, mutation: (inout AlmostBuy) -> Void) {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return }
        mutation(&items[index])
        save()
    }

    private func expireAbandonedDecisions(now: Date = Date()) {
        let expiryWindow = 7 * 24 * 60 * 60.0
        var changed = false
        for index in items.indices where items[index].state.isWaiting {
            guard let decisionAt = items[index].decisionAt,
                  now.timeIntervalSince(decisionAt) >= expiryWindow else { continue }
            items[index].state = .expired
            items[index].resolvedAt = now
            notificationService.cancelCoolingComplete(for: items[index].id)
            changed = true
        }
        if changed { save() }
    }

    // Mirrors Android's deleteAccountAndLocalData (AppViewModel.kt): purely a
    // local wipe. Android's own "Delete account" doesn't call a backend
    // delete endpoint either (none exists) - it clears SharedPreferences,
    // cancels scheduled reminders, and resets in-memory state to fresh
    // defaults. Matching that exactly rather than inventing new server
    // behavior here.
    func resetAllLocalData() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        UserDefaults.standard.removeObject(forKey: persistenceKey)
        UserDefaults.standard.removeObject(forKey: "ghostcart.v2.favorite-product-ids")
        clearShareQueue()
        items = []
        cartItems = []
        activeOrder = nil
        membership = .fresh()
        preferences = GhostCartPreferences()
    }

    private func save() {
        guard !isLoading else { return }
        let state = PersistedState(
            items: items,
            membership: membership,
            preferences: preferences,
            cartItems: cartItems,
            activeOrder: activeOrder
        )
        guard let data = try? JSONEncoder().encode(state) else { return }
        UserDefaults.standard.set(data, forKey: persistenceKey)
    }

    private func restore() {
        guard let data = UserDefaults.standard.data(forKey: persistenceKey),
              let state = try? JSONDecoder().decode(PersistedState.self, from: data) else { return }
        items = state.items
        membership = state.membership
        preferences = state.preferences
        cartItems = state.cartItems ?? []
        activeOrder = state.activeOrder
    }
}

private struct PersistedState: Codable {
    var items: [AlmostBuy]
    var membership: MembershipProfile
    var preferences: GhostCartPreferences
    var cartItems: [GhostCartItem]?
    var activeOrder: SimulatedOrder?
}
