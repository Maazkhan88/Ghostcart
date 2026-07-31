import SwiftUI

enum AppTab: Hashable {
    case home
    case cooldowns
    case cart
    case capture
    case progress
    case profile
}

struct ContentView: View {
    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab: AppTab = .home

    init() {
        #if DEBUG
        let requested = ProcessInfo.processInfo.environment["GHOSTCART_INITIAL_TAB"]
        let initial: AppTab
        switch requested {
        case "orders": initial = .cooldowns
        case "cart": initial = .cart
        case "wallet": initial = .progress
        case "profile": initial = .profile
        default: initial = .home
        }
        _selectedTab = State(initialValue: initial)
        #endif
    }

    var body: some View {
        screen
            .safeAreaInset(edge: .bottom, spacing: 0) {
                GhostBottomNav(selectedTab: $selectedTab)
            }
        .onReceive(NotificationCenter.default.publisher(for: .ghostCartNotificationDestination)) { notification in
            guard let destination = notification.userInfo?["destination"] as? String else { return }
            switch destination {
            case "capture": selectedTab = .capture
            case "profile": selectedTab = .profile
            case "home": selectedTab = .home
            default: selectedTab = .cooldowns
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .ghostCartNotificationAction)) { _ in
            handleNotificationAction()
        }
        .onAppear {
            handleSharedImport()
            handleNotificationAction()
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active { handleSharedImport() }
        }
    }

    private func handleNotificationAction() {
        guard let pending = NotificationActionBridge.take() else { return }
        switch pending.actionIdentifier {
        case NotificationService.skippedActionIdentifier:
            store.resolve(id: pending.almostBuyID, outcome: .resolvedSkipped)
            selectedTab = .cooldowns
        case NotificationService.boughtActionIdentifier:
            store.resolve(id: pending.almostBuyID, outcome: .resolvedBought)
            selectedTab = .cooldowns
        case NotificationService.moreTimeActionIdentifier:
            selectedTab = .cooldowns
        default:
            break
        }
    }

    @ViewBuilder
    private var screen: some View {
        switch selectedTab {
        case .home:
            NavigationStack {
                HomeView(
                    onGhostSomething: { selectedTab = .capture },
                    onOpenCart: { selectedTab = .cart },
                    onViewCooldowns: { selectedTab = .cooldowns },
                    onOpenProfile: { selectedTab = .profile }
                )
            }
        case .cooldowns:
            NavigationStack { CooldownsView() }
        case .cart:
            NavigationStack { CartFlowView(onViewProgress: { selectedTab = .progress }) }
        case .capture:
            NavigationStack { CaptureView(onComplete: { selectedTab = .cooldowns }) }
        case .progress:
            NavigationStack { ProgressView() }
        case .profile:
            NavigationStack { ProfileView() }
        }
    }

    // A link shared into the GhostCartShare extension is written to the App
    // Group container. When the app becomes active we consume it once, preview
    // it, and stage a pre-filled capture on the Ghost + tab.
    private func handleSharedImport() {
        guard let pending = SharedImportBridge.takePending() else { return }
        Task {
            let result = await ProductImportService.previewLink(
                sourceURL: pending.sourceURL,
                sharedTitle: pending.sharedTitle,
                sharedImageURL: pending.sharedImageURL
            )
            let seed = Self.seed(from: pending, result: result)
            await MainActor.run {
                store.stageCapture(seed)
                selectedTab = .capture
            }
        }
    }

    private static func seed(from pending: PendingSharedImport, result: Result<LinkImportResult, ApiError>) -> CaptureSeed {
        let sharedName = pending.sharedTitle.flatMap { titleLooksLikeFallback($0) ? nil : $0 } ?? ""
        if case .success(.product(let product)) = result {
            return CaptureSeed(
                name: titleLooksLikeFallback(product.title) ? sharedName : product.title,
                amount: product.amount,
                category: AlmostBuyCategory(serverName: product.category),
                sourceURL: product.sourceURL.isEmpty ? pending.sourceURL : product.sourceURL,
                imageURL: product.imageURL ?? pending.sharedImageURL,
                sourceDomain: product.sourceDomain,
                retailer: product.retailer,
                note: product.note,
                offerCommunityShare: true
            )
        }
        // Listing pages and failed previews still open the capture form with the
        // link filled in, so the user can pick or enter details manually.
        return CaptureSeed(
            name: sharedName,
            amount: nil,
            category: .other,
            sourceURL: pending.sourceURL,
            imageURL: pending.sharedImageURL,
            sourceDomain: nil,
            retailer: nil,
            note: nil,
            offerCommunityShare: true
        )
    }
}

// Custom bottom nav, not SwiftUI's native TabView chrome. Two separate bugs
// (an oversized mascot image, then solid-colored-square icons) appeared when
// custom template images were used inside .tabItem - that API path seems to
// mishandle custom images on this SwiftUI/OS version, while the exact same
// images render correctly as normal views everywhere else in this app
// (headers, product thumbnails). Building the bar directly sidesteps that
// and matches Android's actual GhostBottomNav (Navigation.kt:997-1065),
// including the raised circular Cart button, more faithfully than tabItem
// ever could anyway.
private struct GhostBottomNav: View {
    @EnvironmentObject private var store: GhostCartStore
    @Binding var selectedTab: AppTab

    @ViewBuilder
    var body: some View {
        VStack(spacing: 0) {
            if #available(iOS 26.0, *) {
                barContent
                    .glassEffect(.regular, in: .capsule)
            } else {
                barContent
                    .background(.regularMaterial, in: Capsule())
            }
        }
        // The center cart mascot is raised 10pt above the capsule. Reserve
        // that space in the safe-area inset so the final screen row never
        // scrolls underneath the visible navigation control.
        .padding(.top, 12)
        .padding(.horizontal, 14)
        .padding(.bottom, 4)
    }

    private var barContent: some View {
        HStack(spacing: 0) {
            navItem(.home, icon: "HomeIcon", label: "Home")
            // No badge here - mirror plan slice 4 fix. This tab previously
            // carried a "ready to decide" count badge, but Android's badge
            // on this row is cart-item count, on the CENTER tab, not this
            // one (GhostBottomNav, Navigation.kt:1039-1055; confirmed via
            // deep audit docs/handoffs/2026-07-31-android-ios-deep-audit-
            // and-plan.md §F). iOS's label mismatch the same audit also
            // flagged ("Orders" vs expected "Cooldowns") was independently
            // re-verified against the live strings.xml value and a real
            // device screenshot - Android's actual displayed string is
            // "Orders" (R.string.nav_cooldowns = "Orders", not "Cooldowns"
            // despite the key name), so no label change was made here.
            navItem(.cooldowns, icon: "OrdersIcon", label: "Orders")
            cartItem
            navItem(.progress, icon: "WalletIcon", label: "Wallet")
            navItem(.profile, icon: "ProfileIcon", label: "Profile")
        }
        .padding(.horizontal, 8)
        // Keep the capsule compact. The previous 10pt top + 26pt bottom
        // padding made the navigation chrome dominate the screen and left
        // the labels floating too far above the home indicator.
        .padding(.vertical, 7)
    }

    private func navItem(_ tab: AppTab, icon: String, label: String, badge: Int = 0) -> some View {
        let selected = selectedTab == tab
        return Button(action: { selectedTab = tab }) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    Image(icon)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 25, height: 25)
                    if badge > 0 {
                        Text("\(badge)")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(3)
                            .background(Color.red)
                            .clipShape(Circle())
                            .offset(x: 8, y: -6)
                    }
                }
                Text(label).font(.system(size: 10, weight: .semibold))
            }
            .foregroundStyle(selected ? Color.ghostGreenColor : Color.secondary)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    // Android's central Cart tab: raised 48dp circular Ghost-green button
    // with the cart mascot, no text label (GhostBottomNav,
    // Navigation.kt:1014-1038). Real Android badge here is cart-item
    // count, capped "9+" - not added yet because there is no real cart
    // model on iOS to count (this tab still opens the capture form; a real
    // cart is mirror-plan slice 11). Deliberately not faking a number here
    // - no badge is more honest than a wrong one.
    private var cartItem: some View {
        Button(action: { selectedTab = .cart }) {
            ZStack(alignment: .topTrailing) {
                Image("MascotCart")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 32, height: 32)
                    .frame(width: 48, height: 48)
                    .background(Color.ghostGreenColor)
                    .clipShape(Circle())
                    .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
                if store.cartQuantity > 0 {
                    Text(store.cartQuantity > 9 ? "9+" : "\(store.cartQuantity)")
                        .font(.system(size: 9, weight: .black))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 5)
                        .frame(minWidth: 18, minHeight: 18)
                        .background(Color.red, in: Capsule())
                        .offset(x: 6, y: -5)
                }
            }
            .offset(y: -10)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityLabel("Cart")
    }
}

private enum CartFlowStage {
    case cart
    case checkout
    case success
    case delivery
}

private struct CartFlowView: View {
    @EnvironmentObject private var store: GhostCartStore
    let onViewProgress: () -> Void
    @State private var stage: CartFlowStage = .cart
    @State private var cooldownItem: GhostCartItem?

    var body: some View {
        Group {
            switch stage {
            case .cart:
                cartView
            case .checkout:
                GhostCheckoutView(
                    onBack: { stage = .cart },
                    onPlaced: { stage = .success }
                )
            case .success:
                if let order = store.activeOrder {
                    OrderGhostedSuccessView(
                        order: order,
                        onTrack: { stage = .delivery },
                        onProgress: onViewProgress
                    )
                } else {
                    cartView
                }
            case .delivery:
                if let order = store.activeOrder {
                    FakeDeliveryTrackingView(order: order, onReceipt: { stage = .success })
                } else {
                    cartView
                }
            }
        }
        .sheet(item: $cooldownItem) { item in
            CartCooldownPicker(item: item) { minutes in
                store.setCartCooldown(id: item.id, minutes: minutes)
                cooldownItem = nil
            }
            .presentationDetents([.medium])
        }
    }

    private var cartView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack {
                    Text("Ghost Cart").font(.title2.weight(.black))
                    Spacer()
                    if !store.cartItems.isEmpty {
                        Button(role: .destructive, action: store.clearCart) {
                            Image(systemName: "trash")
                        }
                    }
                }

                GhostMascotView(poseName: "wave").frame(width: 58, height: 58)
                Text("The craving disappeared.\nThe money stayed.")
                    .font(.title2.weight(.black))
                Text("Nothing here is charged or delivered.")
                    .font(.caption).foregroundStyle(Color.secondary)
                Label {
                    HStack(spacing: 4) {
                        Text("You almost spent")
                        DirhamAmount(value: Double(store.cartSubtotalCents) / 100, font: .caption.weight(.bold), iconSize: 10, color: Color.ghostGreenColor)
                    }
                } icon: {
                    Image(systemName: "checkmark.circle.fill")
                }
                .font(.caption.weight(.bold)).foregroundStyle(Color.ghostGreenColor)

                if store.cartItems.isEmpty {
                    EmptyStateView(
                        image: "cart",
                        title: "Your Ghost Cart is empty",
                        message: "Add something you almost bought from Home."
                    )
                    if store.activeOrder != nil {
                        Button("Track active fake delivery") { stage = .delivery }
                            .buttonStyle(GhostPrimaryButtonStyle())
                    }
                } else {
                    ForEach(store.cartItems) { item in
                        cartRow(item)
                    }
                    GhostCard { orderSummary(subtotalOnly: true) }
                    Button("Clear Ghost Cart", role: .destructive, action: store.clearCart)
                        .frame(maxWidth: .infinity)

                    Button("Proceed to Ghost Checkout") { stage = .checkout }
                        .buttonStyle(GhostPrimaryButtonStyle())
                }
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
    }

    private func cartRow(_ item: GhostCartItem) -> some View {
        HStack(spacing: 12) {
            ProductThumbnail(
                imageURL: item.imageURL,
                systemImage: AlmostBuyCategory(serverName: item.category).systemImage,
                size: 58,
                cornerRadius: 13
            )
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name).font(.subheadline.weight(.bold)).lineLimit(2)
                DirhamAmount(value: Double(item.priceCents) / 100, font: .caption, iconSize: 9, color: Color.secondary)
                Button(cooldownLabel(item.cooldownMinutes)) { cooldownItem = item }
                    .font(.caption2.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
            }
            Spacer()
            HStack(spacing: 10) {
                quantityButton("minus", action: { store.decrementCartItem(id: item.id) })
                Text("\(item.quantity)").font(.caption.weight(.bold)).frame(minWidth: 12)
                quantityButton("plus", action: { store.incrementCartItem(id: item.id) })
            }
        }
        .padding(.vertical, 5)
    }

    private func quantityButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.caption.weight(.bold))
                .frame(width: 28, height: 28)
                .background(Color.primary.opacity(0.06), in: Circle())
        }
        .buttonStyle(.plain)
    }

    private func orderSummary(subtotalOnly: Bool) -> some View {
        VStack(spacing: 10) {
            summaryLine("Subtotal", cents: store.cartSubtotalCents)
            summaryLine("Small Win Fee", cents: 0)
            summaryLine("Real amount charged", cents: 0, accent: true)
            Divider()
            summaryLine("Total you almost spent", cents: store.cartSubtotalCents, bold: true)
        }
    }

    private func summaryLine(_ label: String, cents: Int, accent: Bool = false, bold: Bool = false) -> some View {
        HStack {
            Text(label).font(bold ? .subheadline.weight(.black) : .caption).foregroundStyle(bold ? Color.primary : Color.secondary)
            Spacer()
            DirhamAmount(
                value: Double(cents) / 100,
                font: bold ? .subheadline.weight(.black) : .caption.weight(.bold),
                iconSize: bold ? 13 : 10,
                color: accent ? Color.ghostGreenColor : Color.primary
            )
        }
    }

    private func cooldownLabel(_ minutes: Int) -> String {
        if minutes < 60 { return "Cooldown: \(minutes) min" }
        if minutes < 1440 { return "Cooldown: \(minutes / 60) hr" }
        return "Cooldown: \(minutes / 1440) day\(minutes == 1440 ? "" : "s")"
    }
}

private struct CartCooldownPicker: View {
    let item: GhostCartItem
    let onSelect: (Int) -> Void
    @Environment(\.dismiss) private var dismiss
    private let options = [30, 24 * 60, 48 * 60, 7 * 24 * 60]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Capsule().fill(Color.secondary.opacity(0.3)).frame(width: 38, height: 5).frame(maxWidth: .infinity)
            Text("Choose a cooldown").font(.title2.weight(.black))
            Text("Give \(item.name) time before you decide.").font(.caption).foregroundStyle(Color.secondary)
            ForEach(options, id: \.self) { minutes in
                Button {
                    onSelect(minutes)
                } label: {
                    HStack {
                        Image(systemName: item.cooldownMinutes == minutes ? "largecircle.fill.circle" : "circle")
                        Text(label(minutes)).font(.subheadline.weight(.bold))
                        Spacer()
                    }
                    .padding(14)
                    .foregroundStyle(Color.primary)
                    .background(item.cooldownMinutes == minutes ? Color.ghostGreenColor.opacity(0.18) : Color.primary.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
            }
            Button("Cancel") { dismiss() }.frame(maxWidth: .infinity)
        }
        .padding(20)
    }

    private func label(_ minutes: Int) -> String {
        switch minutes {
        case 30: return "30 minutes"
        case 1440: return "24 hours"
        case 2880: return "2 days"
        default: return "7 days"
        }
    }
}

private struct GhostCheckoutView: View {
    @EnvironmentObject private var store: GhostCartStore
    let onBack: () -> Void
    let onPlaced: () -> Void

    private var subtotal: Int { store.cartSubtotalCents }
    private var promo: Int { Int((Double(subtotal) * 0.10).rounded(.down)) }
    private var discounted: Int { subtotal - promo }
    private var fee: Int { Int((Double(discounted) * 0.05).rounded(.down)) }
    private var vat: Int { Int((Double(discounted) * 0.05).rounded(.down)) }
    private var total: Int { discounted + fee + vat }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Button(action: onBack) { Image(systemName: "chevron.left") }
                    Text("Ghost Checkout").font(.title3.weight(.black))
                    Spacer()
                }
                Text("Simulation mode").font(.caption).foregroundStyle(Color.secondary)

                Label {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("This is not a real order.").font(.subheadline.weight(.black))
                        Text("No payment will be charged.\nNo product will be delivered.").font(.caption)
                    }
                } icon: { Image(systemName: "exclamationmark.triangle") }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay { RoundedRectangle(cornerRadius: 14).stroke(Color.primary) }

                infoRow("location.fill", title: "Fake Delivery Address", subtitle: "123 Ghost Street\nAl Wasl, Dubai\nUnited Arab Emirates", action: "Change")
                infoRow("wallet.pass.fill", title: "Ghost Wallet", subtitle: "Simulated Ghost Cart credit", action: "View")
                infoRow("tag.fill", title: "Promo Code", subtitle: "GHOST10 · 10% off applied", action: "Remove")

                GhostCard {
                    VStack(spacing: 10) {
                        checkoutLine("Subtotal", cents: subtotal)
                        checkoutLine("Promo Discount (GHOST10)", cents: -promo, accent: true)
                        checkoutLine("Service Fee", cents: fee)
                        checkoutLine("VAT (5%)", cents: vat)
                        Divider()
                        checkoutLine("Simulated total", cents: total, bold: true)
                        checkoutLine("Real amount charged", cents: 0, accent: true)
                    }
                }

                Button("Place Ghost Order") {
                    if store.completeSimulatedCheckout() != nil { onPlaced() }
                }
                .buttonStyle(GhostPrimaryButtonStyle())
                Text("By continuing, you confirm this is only a spending-decision simulation.")
                    .font(.caption2).foregroundStyle(Color.secondary).multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            .padding(20).padding(.bottom, 24)
        }
        .background(Color(.systemBackground)).navigationBarHidden(true)
    }

    private func infoRow(_ icon: String, title: String, subtitle: String, action: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon).frame(width: 36, height: 36).background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 10))
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.subheadline.weight(.bold))
                Text(subtitle).font(.caption).foregroundStyle(Color.secondary)
            }
            Spacer()
            Text(action).font(.caption.weight(.bold)).foregroundStyle(Color.ghostGreenColor)
        }
        .padding(14).overlay { RoundedRectangle(cornerRadius: 14).stroke(Color.primary.opacity(0.1)) }
    }

    private func checkoutLine(_ label: String, cents: Int, accent: Bool = false, bold: Bool = false) -> some View {
        HStack {
            Text(label).font(bold ? .subheadline.weight(.black) : .caption).foregroundStyle(bold ? Color.primary : Color.secondary)
            Spacer()
            Text(cents < 0 ? "−" : "")
            DirhamAmount(value: abs(Double(cents) / 100), font: bold ? .subheadline.weight(.black) : .caption.weight(.bold), iconSize: bold ? 13 : 10, color: accent ? Color.ghostGreenColor : Color.primary)
        }
    }
}

private struct OrderGhostedSuccessView: View {
    let order: SimulatedOrder
    let onTrack: () -> Void
    let onProgress: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                HStack(spacing: 8) {
                    GhostMascotView(poseName: "wave").frame(width: 32, height: 32)
                    Text("Ghost Cart").font(.headline.weight(.black))
                }
                GhostMascotView(poseName: "thumbsup").frame(width: 145, height: 145)
                Text("Order Ghosted\nSuccessfully").font(.title.weight(.black)).multilineTextAlignment(.center)
                HStack(spacing: 4) {
                    Text("Simulated checkout total:").foregroundStyle(Color.secondary)
                    DirhamAmount(value: order.totalAmount, font: .subheadline.weight(.black), iconSize: 12, color: Color.ghostGreenColor)
                }.font(.subheadline)
                HStack {
                    Text("Ghost Order #").font(.caption).foregroundStyle(Color.secondary)
                    Spacer(); Text(order.id).font(.subheadline.weight(.black))
                }
                .padding(14).background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 14))
                HStack(spacing: 10) {
                    successTile("Real amount charged", value: "0")
                    successTile("Money Kept updates only if you later skip", value: "")
                }
                invoiceCard
                Button("Track Fake Delivery", action: onTrack).buttonStyle(GhostPrimaryButtonStyle())
                Button("View Progress", action: onProgress).frame(maxWidth: .infinity)
            }
            .padding(24).padding(.bottom, 20)
        }
        .background(Color(.systemBackground)).navigationBarHidden(true)
    }

    private func successTile(_ label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: "checkmark.circle.fill").foregroundStyle(Color.ghostGreenColor)
            Text(label).font(.caption2).foregroundStyle(Color.secondary)
            if !value.isEmpty { Text(value).font(.headline.weight(.black)) }
        }
        .frame(maxWidth: .infinity, minHeight: 82, alignment: .topLeading)
        .padding(12).overlay { RoundedRectangle(cornerRadius: 14).stroke(Color.primary.opacity(0.1)) }
    }

    private var invoiceCard: some View {
        GhostCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("Order placed").font(.headline.weight(.black))
                Text(order.placedAt.formatted(date: .abbreviated, time: .shortened)).font(.caption).foregroundStyle(Color.secondary)
                Divider()
                ForEach(order.items) { item in
                    HStack {
                        Text("\(item.name) (x\(item.quantity))").font(.caption)
                        Spacer()
                        DirhamAmount(value: item.amount, font: .caption.weight(.bold), iconSize: 9)
                    }
                }
                Divider()
                HStack {
                    Text("Total").font(.subheadline.weight(.black)); Spacer()
                    DirhamAmount(value: order.totalAmount, font: .subheadline.weight(.black), iconSize: 13)
                }
                Text("Payment method · Simulation only").font(.caption).foregroundStyle(Color.secondary)
            }
        }
    }
}

private struct FakeDeliveryTrackingView: View {
    @EnvironmentObject private var store: GhostCartStore
    let order: SimulatedOrder
    let onReceipt: () -> Void
    private let steps = [
        ("Order placed", "We've received your imaginary order."),
        ("Preparing imaginary order", "Our team is carefully doing nothing."),
        ("Ghost Rider is on the way", "Zooming through the void."),
        ("Rider left absolutely nothing at your doorstep", "Yep, nothing's there."),
        ("Fake delivery complete", "Thanks for choosing smart savings.")
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack {
                    GhostMascotView(poseName: "wave").frame(width: 28, height: 28)
                    Text("Ghost Cart").font(.headline.weight(.black)); Spacer(); Text("Help").font(.caption).foregroundStyle(Color.secondary)
                }
                Text("Fake Delivery Tracking").font(.title2.weight(.black))
                Text("Order \(order.id)").font(.caption).foregroundStyle(Color.secondary)
                HStack {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(Color.ghostGreenColor)
                    VStack(alignment: .leading) {
                        Text("No real charge was made").font(.caption.weight(.bold))
                        Text("This stays an almost-spent amount until you decide.").font(.caption2).foregroundStyle(Color.secondary)
                    }
                    Spacer()
                    DirhamAmount(value: order.totalAmount, font: .subheadline.weight(.black), iconSize: 12, color: Color.ghostGreenColor)
                }
                .padding(14).background(Color.ghostGreenColor.opacity(0.14), in: RoundedRectangle(cornerRadius: 16))
                Text("✦ Please prepare to receive absolutely nothing. ✦").font(.subheadline.weight(.bold)).frame(maxWidth: .infinity)

                ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 13) {
                        Image(systemName: index <= order.deliveryStep ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(index <= order.deliveryStep ? Color.ghostGreenColor : Color.secondary.opacity(0.4))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(step.0).font(.subheadline.weight(.bold))
                            Text(step.1).font(.caption).foregroundStyle(Color.secondary)
                        }
                    }
                    .padding(.vertical, 6)
                }
                if order.deliveryStep < 4 {
                    Button("Advance fake delivery") { store.advanceDelivery() }
                        .buttonStyle(GhostPrimaryButtonStyle())
                }
                Button("View receipt", action: onReceipt).frame(maxWidth: .infinity)
            }
            .padding(20).padding(.bottom, 24)
        }
        .background(Color(.systemBackground)).navigationBarHidden(true)
    }
}

#Preview {
    ContentView()
        .environmentObject(GhostCartStore())
}
