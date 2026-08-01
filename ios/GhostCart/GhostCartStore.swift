import Foundation
import SwiftUI

final class GhostCartStore: ObservableObject {
    @Published private(set) var items: [AlmostBuy] = []
    @Published private(set) var cartItems: [GhostCartItem] = []
    @Published private(set) var activeOrder: SimulatedOrder?
    // Transient (not persisted): a community tap or a shared link stages a
    // pre-filled capture that the Ghost + screen picks up on appear.
    @Published var captureSeed: CaptureSeed?
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
        imageURL: String? = nil
    ) -> UUID {
        let item = AlmostBuy(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            amount: max(0, amount),
            category: category,
            trigger: trigger,
            source: source,
            sourceURL: sourceURL?.trimmingCharacters(in: .whitespacesAndNewlines),
            imageURL: imageURL
        )
        items.insert(item, at: 0)
        save()
        syncCreateInBackground(item)
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

    func setCartCooldown(id: String, minutes: Int) {
        guard let index = cartItems.firstIndex(where: { $0.id == id }) else { return }
        cartItems[index].cooldownMinutes = max(1, minutes)
        save()
    }

    func clearCart() {
        cartItems.removeAll()
        save()
    }

    @discardableResult
    func completeSimulatedCheckout() -> SimulatedOrder? {
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

        for cartItem in cartItems {
            for _ in 0..<cartItem.quantity {
                let id = capture(
                    name: cartItem.name,
                    amount: Double(cartItem.priceCents) / 100,
                    category: AlmostBuyCategory(serverName: cartItem.category),
                    trigger: .other,
                    source: .manual,
                    sourceURL: nil,
                    imageURL: cartItem.imageURL
                )
                startCooling(id: id, minutes: cartItem.cooldownMinutes)
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
        updateItem(id: id) { item in
            item.state = .cooling
            item.decisionAt = Calendar.current.date(byAdding: .minute, value: max(1, minutes), to: Date())
            item.resolvedAt = nil
        }
        guard let item = item(id: id) else { return }
        notificationService.scheduleCoolingComplete(for: item, enabled: preferences.reminders.coolingCompleteEnabled)
        syncExtendOrCreateInBackground(item)
    }

    func resolve(id: UUID, outcome: AlmostBuyState) {
        guard outcome == .resolvedSkipped || outcome == .resolvedBought else { return }
        updateItem(id: id) { item in
            item.state = outcome
            item.resolvedAt = Date()
        }
        notificationService.cancelCoolingComplete(for: id)
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

    private func syncCreateInBackground(_ item: AlmostBuy) {
        Task { @MainActor [weak self] in
            guard let serverId = await AlmostBuySyncService.syncCreate(item) else { return }
            self?.attachServerId(serverId, to: item.id)
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
                let knownServerIds = Set(items.compactMap(\.serverId))
                let newItems = remoteItems.filter { !knownServerIds.contains($0.serverId ?? "") }
                guard !newItems.isEmpty else { return }
                items.append(contentsOf: newItems)
                items.sort { $0.capturedAt > $1.capturedAt }
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

    func delete(id: UUID) {
        items.removeAll { $0.id == id }
        notificationService.cancelCoolingComplete(for: id)
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
