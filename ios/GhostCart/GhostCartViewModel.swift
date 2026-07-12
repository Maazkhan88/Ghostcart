import SwiftUI

class GhostCartViewModel: ObservableObject {
    @Published var cartIds: Set<String> = []
    @Published var cooledIds: Set<String> = []
    @Published var ghostedIds: Set<String> = []
    @Published var deliveryStep: Int = -1
    @Published var receiptVisible: Bool = false
    @Published var waitlistSubmitted: Bool = false
    @Published var activeTab: String = "Catalog"

    private var deliveryTimer: Timer?
    private let stepDurations: [TimeInterval] = [2.0, 2.0, 2.5, 2.5, 3.0]

    func addToCart(_ id: String) {
        cartIds.insert(id)
        receiptVisible = false
    }

    func removeFromCart(_ id: String) {
        cartIds.remove(id)
    }

    func startCooling(_ id: String) {
        cooledIds.insert(id)
        cartIds.remove(id)
    }

    func resetProductState(_ id: String) {
        cartIds.remove(id)
        cooledIds.remove(id)
        ghostedIds.remove(id)
    }

    func triggerFakeCheckout() {
        guard !cartIds.isEmpty else { return }
        ghostedIds.formUnion(cartIds)
        cartIds.removeAll()
        receiptVisible = false
        deliveryStep = 0
        scheduleNextStep()
    }

    private func scheduleNextStep() {
        guard deliveryStep >= 0 && deliveryStep < 5 else { return }
        let duration = stepDurations[deliveryStep]
        deliveryTimer?.invalidate()
        deliveryTimer = Timer.scheduledTimer(withTimeInterval: duration, repeats: false) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.deliveryStep += 1
                self.scheduleNextStep()
            }
        }
    }

    func showReceipt() {
        deliveryStep = -1
        receiptVisible = true
    }

    func dismissReceipt() {
        receiptVisible = false
    }

    func submitWaitlist(_ email: String) {
        waitlistSubmitted = true
    }

    func changeTab(_ tab: String) {
        activeTab = tab
    }
}
