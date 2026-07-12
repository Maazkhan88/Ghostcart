import SwiftUI

struct CatalogView: View {
    let cartIds: Set<String>
    let cooledIds: Set<String>
    let ghostedIds: Set<String>
    let onAddToCart: (String) -> Void
    let onRemoveFromCart: (String) -> Void
    let onCoolDown: (String) -> Void
    let onResetState: (String) -> Void
    
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 4) {
                GhostCartLogoView()
                    .frame(width: 126, height: 48)
                    .padding(.horizontal, 10)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.bottom, 14)

                Text("Browse Temptation".uppercased())
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.ghostGreenColor)
                    .tracking(1)
                
                Text("Choose an almost-buy.")
                    .font(.system(size: 24, weight: .black))
                    .foregroundColor(.white)
                    .padding(.bottom, 16)
                
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(DemoCatalog.products) { product in
                        let inCart = cartIds.contains(product.id)
                        let cooled = cooledIds.contains(product.id)
                        let ghosted = ghostedIds.contains(product.id)
                        
                        ProductCardView(
                            product: product,
                            inCart: inCart,
                            cooled: cooled,
                            ghosted: ghosted,
                            onAddToCart: { onAddToCart(product.id) },
                            onRemoveFromCart: { onRemoveFromCart(product.id) },
                            onCoolDown: { onCoolDown(product.id) },
                            onResetState: { onResetState(product.id) }
                        )
                    }
                }
                .padding(.bottom, 80)
            }
            .padding(16)
        }
        .background(Color.inkColor.ignoresSafeArea())
    }
}

struct ProductCardView: View {
    let product: Product
    let inCart: Bool
    let cooled: Bool
    let ghosted: Bool
    let onAddToCart: () -> Void
    let onRemoveFromCart: () -> Void
    let onCoolDown: () -> Void
    let onResetState: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Graphic Area
            ZStack {
                Color(hex: product.toneColorHex).opacity(0.08)
                
                ProductIconView(name: product.vectorIconName)
                    .frame(width: 56, height: 56)
            }
            .frame(height: 110)
            .cornerRadius(12)
            .padding(12)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(product.category.uppercased())
                    .font(.system(size: 8, weight: .bold))
                    .foregroundColor(.gray)
                
                Text(product.name)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                
                Text(product.note)
                    .font(.system(size: 10))
                    .foregroundColor(Color(white: 0.72))
                    .lineLimit(2)
                    .frame(height: 28)
                    .padding(.bottom, 8)
                
                // Status line
                HStack(spacing: 6) {
                    let statusText = cooled ? "Cooling active" : (ghosted ? "Ghosted" : (inCart ? "In Ghost Cart" : "Ready to ghost"))
                    Text(statusText)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor((cooled || ghosted) ? Color.ghostGreenColor : Color.gray)
                    
                    if cooled || ghosted {
                        Button(action: onResetState) {
                            Text("RESET")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.ghostGreenColor)
                                .underline()
                        }
                    }
                }
                .padding(.bottom, 8)
                
                // Action Buttons
                HStack(spacing: 6) {
                    if inCart {
                        Button(action: onRemoveFromCart) {
                            Text("Undo")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.inkColor)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .background(Color.white)
                                .cornerRadius(9)
                        }
                        .frame(height: 34)
                    } else {
                        Button(action: onAddToCart) {
                            Text("Ghost it")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.inkColor)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .background(Color.ghostGreenColor)
                                .cornerRadius(9)
                        }
                        .frame(height: 34)
                    }
                    
                    HoldToCoolButton(onCooled: onCoolDown)
                        .frame(height: 34)
                }
            }
            .padding([.horizontal, .bottom], 12)
        }
        .background(Color.darkGrayColor)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(white: 0.17), lineWidth: 1)
        )
    }
}

struct HoldToCoolButton: View {
    let onCooled: () -> Void
    @State private var progress: CGFloat = 0.0
    @State private var isHolding = false
    @State private var timer: Timer? = nil

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                if progress > 0 {
                    Color.ghostGreenColor.opacity(0.28)
                        .frame(width: geo.size.width * progress)
                }
                
                Text(isHolding ? "Cooling..." : "Hold to cool")
                    .font(.system(size: 9, weight: .black))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .background(Color.clear)
            .cornerRadius(9)
            .overlay(
                RoundedRectangle(cornerRadius: 9)
                    .stroke(Color(white: 0.17), lineWidth: 1)
            )
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        if !isHolding {
                            isHolding = true
                            progress = 0
                            timer = Timer.scheduledTimer(withTimeInterval: 0.016, repeats: true) { t in
                                progress += 0.016 / 0.9
                                if progress >= 1.0 {
                                    t.invalidate()
                                    onCooled()
                                    isHolding = false
                                    progress = 0
                                }
                            }
                        }
                    }
                    .onEnded { _ in
                        isHolding = false
                        timer?.invalidate()
                        progress = 0
                    }
            )
        }
    }
}
