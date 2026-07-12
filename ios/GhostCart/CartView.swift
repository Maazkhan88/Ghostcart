import SwiftUI

struct CartView: View {
    let cartIds: Set<String>
    let onRemoveFromCart: (String) -> Void
    let onCheckout: () -> Void
    let onDismiss: () -> Void

    var cartProducts: [Product] {
        DemoCatalog.products.filter { cartIds.contains($0.id) }
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.inkColor.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top bar
                HStack {
                    Text("GHOST CART")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Text("\(cartProducts.count) item\(cartProducts.count == 1 ? "" : "s")")
                        .font(.system(size: 11))
                        .foregroundColor(.gray)
                }
                .padding(.horizontal, 16)
                .padding(.top, 56)
                .padding(.bottom, 16)

                if cartProducts.isEmpty {
                    Spacer()
                    VStack(spacing: 20) {
                        ZStack {
                            Circle()
                                .stroke(Color.ghostGreenColor.opacity(0.2), lineWidth: 1)
                                .frame(width: 160, height: 160)
                            GhostMascotView(poseName: "cart")
                                .frame(width: 80, height: 80)
                        }
                        Text("Your cart is ghost empty.")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                        Text("Browse items and select \"Ghost it\" to fill your cart.")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(cartProducts) { product in
                                CartItemRow(product: product, onRemove: {
                                    onRemoveFromCart(product.id)
                                })
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                    }

                    Spacer()

                    VStack(spacing: 8) {
                        HStack {
                            Text("Real amount charged")
                                .font(.system(size: 12))
                                .foregroundColor(.gray)
                            Spacer()
                            Text("Zero")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.ghostGreenColor)
                        }

                        Button(action: onCheckout) {
                            Text("Complete Fake Checkout")
                                .font(.system(size: 14, weight: .black))
                                .foregroundColor(.inkColor)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color.ghostGreenColor)
                                .cornerRadius(12)
                        }

                        Text("Close the loop without entering checkout details")
                            .font(.system(size: 8))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 8)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }

            // Back button
            Button(action: onDismiss) {
                Text("← Back")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.ghostGreenColor)
            }
            .padding(.leading, 16)
            .padding(.top, 56)
        }
    }
}

struct CartItemRow: View {
    let product: Product
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Color(hex: product.toneColorHex).opacity(0.15)
                ProductIconView(name: product.vectorIconName)
                    .frame(width: 24, height: 24)
            }
            .frame(width: 40, height: 40)
            .cornerRadius(8)

            VStack(alignment: .leading, spacing: 2) {
                Text(product.name)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                Text("Almost-buy")
                    .font(.system(size: 9))
                    .foregroundColor(.gray)
            }
            Spacer()

            Button(action: onRemove) {
                Text("×")
                    .font(.system(size: 20))
                    .foregroundColor(.gray)
            }
            .padding(.horizontal, 8)
        }
        .padding(12)
        .background(Color.darkGrayColor)
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(white: 0.17), lineWidth: 1))
    }
}
