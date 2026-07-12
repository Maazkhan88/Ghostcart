import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GhostCartViewModel()
    @State private var isCartOpen = false

    var body: some View {
        ZStack {
            // Main Tab Container
            VStack(spacing: 0) {
                // Tab Content
                ZStack {
                    if viewModel.activeTab == "Catalog" {
                        CatalogView(
                            cartIds: viewModel.cartIds,
                            cooledIds: viewModel.cooledIds,
                            ghostedIds: viewModel.ghostedIds,
                            onAddToCart: { viewModel.addToCart($0) },
                            onRemoveFromCart: { viewModel.removeFromCart($0) },
                            onCoolDown: { viewModel.startCooling($0) },
                            onResetState: { viewModel.resetProductState($0) }
                        )
                    } else if viewModel.activeTab == "Dashboard" {
                        DashboardView(
                            ghostedCount: viewModel.ghostedIds.count,
                            cooledCount: viewModel.cooledIds.count
                        )
                    } else if viewModel.activeTab == "Waitlist" {
                        WaitlistView(
                            submitted: viewModel.waitlistSubmitted,
                            onSubmit: { viewModel.submitWaitlist($0) }
                        )
                    }
                }

                // Bottom Tab Bar
                BottomTabBar(
                    activeTab: viewModel.activeTab,
                    cartCount: viewModel.cartIds.count,
                    onTabSelected: { tab in
                        if tab == "Cart" {
                            isCartOpen = true
                        } else {
                            viewModel.changeTab(tab)
                        }
                    }
                )
            }
            .background(Color.inkColor.ignoresSafeArea())

            // Overlay: Cart screen
            if isCartOpen {
                CartView(
                    cartIds: viewModel.cartIds,
                    onRemoveFromCart: { viewModel.removeFromCart($0) },
                    onCheckout: {
                        isCartOpen = false
                        viewModel.triggerFakeCheckout()
                    },
                    onDismiss: { isCartOpen = false }
                )
                .transition(.move(edge: .trailing))
                .zIndex(1)
            }

            // Overlay: Delivery timeline
            if viewModel.deliveryStep >= 0 {
                CheckoutView(
                    deliveryStep: viewModel.deliveryStep,
                    onViewReceipt: { viewModel.showReceipt() }
                )
                .transition(.opacity)
                .zIndex(2)
            }

            // Overlay: Receipt
            if viewModel.receiptVisible {
                ReceiptView(
                    ghostedCount: viewModel.ghostedIds.count,
                    onDismiss: { viewModel.dismissReceipt() }
                )
                .transition(.opacity)
                .zIndex(3)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: isCartOpen)
        .animation(.easeInOut(duration: 0.25), value: viewModel.deliveryStep >= 0)
        .animation(.easeInOut(duration: 0.25), value: viewModel.receiptVisible)
    }
}

struct BottomTabBar: View {
    let activeTab: String
    let cartCount: Int
    let onTabSelected: (String) -> Void

    let tabs: [(String, String)] = [
        ("Catalog", "leaf"),
        ("Dashboard", "chart"),
        ("Cart", "wallet"),
        ("Waitlist", "lock")
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs, id: \.0) { (label, icon) in
                let isSelected = activeTab == label
                Button(action: { onTabSelected(label) }) {
                    VStack(spacing: 4) {
                        ZStack(alignment: .topTrailing) {
                            ProductIconView(name: icon, color: isSelected ? .ghostGreenColor : .gray)
                                .frame(width: 22, height: 22)

                            if label == "Cart" && cartCount > 0 {
                                ZStack {
                                    Circle()
                                        .fill(Color.ghostGreenColor)
                                        .frame(width: 14, height: 14)
                                    Text("\(cartCount)")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundColor(.inkColor)
                                }
                                .offset(x: 8, y: -8)
                            }
                        }

                        Text(label)
                            .font(.system(size: 9, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? .white : .gray)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
            }
        }
        .background(
            Rectangle()
                .fill(Color.inkColor)
                .shadow(color: Color.black.opacity(0.4), radius: 8, y: -4)
                .overlay(
                    Rectangle()
                        .fill(Color(white: 0.12))
                        .frame(height: 1),
                    alignment: .top
                )
        )
    }
}

#Preview {
    ContentView()
}
