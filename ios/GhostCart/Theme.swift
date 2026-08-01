import Foundation
import SwiftUI

extension Color {
    static let inkColor = Color(red: 0.02, green: 0.02, blue: 0.02)
    static let paperColor = Color.white
    static let softGrayColor = Color(red: 0.96, green: 0.96, blue: 0.96)
    static let shadowGrayColor = Color(red: 0.85, green: 0.85, blue: 0.85)
    static let darkGrayColor = Color(red: 0.09, green: 0.09, blue: 0.09)
    static let ghostGreenColor = Color(red: 0.39, green: 0.84, blue: 0.29)
}

extension AppearancePreference {
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

struct GhostPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.weight(.bold))
            .foregroundStyle(Color.inkColor)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(Color.ghostGreenColor.opacity(configuration.isPressed ? 0.78 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
    }
}

struct GhostSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.bold))
            .foregroundStyle(Color.primary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            .background(Color.primary.opacity(configuration.isPressed ? 0.12 : 0.06))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct GhostCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(16)
            .background(Color.primary.opacity(0.045))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color.primary.opacity(0.09), lineWidth: 1)
            }
    }
}

struct ScreenHeader: View {
    let eyebrow: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(eyebrow.uppercased())
                .font(.caption2.weight(.black))
                .tracking(1.2)
                .foregroundStyle(Color.ghostGreenColor)
            Text(title)
                .font(.largeTitle.weight(.black))
                .foregroundStyle(Color.primary)
            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(Color.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct SectionHeading: View {
    let title: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.title3.weight(.bold))
            Spacer()
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.ghostGreenColor)
            }
        }
    }
}

struct SimulationDisclosure: View {
    var body: some View {
        // Exact copy from Android's safety_disclosure string resource
        // (strings.xml), verified against a live device.
        Label("Simulation only. No payment is collected and no order is placed.", systemImage: "checkmark.shield")
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.secondary)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.ghostGreenColor.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

// Mirrors Android's ProductPhoto direct-asset table (Icons.kt:441-479) -
// bundled marketplace-catalog illustrations, matched by name substring, used
// as a base layer under any remote imageUrl so catalog items missing a
// photo (e.g. Coffee & Drinks items with imageUrl = null server-side) still
// show real art instead of a generic glyph, exactly like Android does.
func bundledProductPhotoName(for productName: String) -> String? {
    let name = productName.lowercased().trimmingCharacters(in: .whitespaces)
    let table: [(String, String)] = [
        ("spanish latte", "product_marketplace_spanish_latte"),
        ("midnight burger", "product_marketplace_midnight_burger"),
        ("blackout burger combo", "product_marketplace_blackout_burger_combo"),
        ("coffee & croissant combo", "product_marketplace_croissant_coffee"),
        ("pizza combo", "product_marketplace_pizza_combo"),
        ("ghost cappuccino", "product_marketplace_ghost_cappuccino"),
        ("noise cancelling headphones", "product_marketplace_noise_cancelling_headphones"),
        ("acai bowl", "product_marketplace_acai_bowl"),
        ("white sneaker", "product_marketplace_white_sneakers"),
        ("gaming tablet", "product_marketplace_gaming_tablet"),
        ("minimal smartwatch", "product_marketplace_minimal_smartwatch"),
        ("wireless earbuds", "product_marketplace_wireless_earbuds"),
        ("luxury perfume", "product_marketplace_luxury_perfume"),
        ("smartwatch pro", "product_marketplace_smartwatch_pro"),
        ("ghost cart phone case", "product_merch_phone_case"),
        ("ghost cart laptop sleeve", "product_merch_laptop_sleeve"),
        ("ghost cart travel tumbler", "product_merch_travel_tumbler"),
        ("ghost cart classic cap", "product_merch_classic_cap"),
        ("ghost cart tote bag", "product_merch_tote_bag"),
        ("ghost cart steel bottle", "product_merch_steel_bottle"),
        ("ghost cart notebook", "product_merch_notebook"),
        ("ghost cart pen", "product_merch_pen"),
        ("ghost cart ceramic mug", "product_merch_ceramic_mug"),
        ("ghost cart lunch box", "product_merch_lunch_box"),
        ("ghost cart running sneakers", "product_merch_running_sneakers"),
        ("ghost cart logo t-shirt", "product_merch_logo_tshirt"),
        ("ghost cart protein shaker", "product_merch_protein_shaker"),
        ("ghost cart backpack", "product_merch_backpack"),
        ("ghost cart bucket hat", "product_merch_bucket_hat"),
        ("ghost cart minimal watch", "product_merch_minimal_watch"),
        ("ghost cart gym duffel", "product_merch_gym_duffel"),
        ("ghost cart supplement jar", "product_merch_supplement_jar"),
        ("ghost cart logo hoodie", "product_merch_logo_hoodie"),
        ("ghost cart beanie", "product_merch_beanie"),
        ("headphone", "product_marketplace_headphones"),
        ("tablet", "product_marketplace_tablet"),
    ]
    if let match = table.first(where: { name.contains($0.0) }) { return match.1 }
    if name.contains("perfume") || name.contains("cologne") { return "product_perfume" }
    return nil
}

// Shared product-thumbnail: real remote image when the item has one (Coil's
// AsyncImage equivalent), category glyph fallback otherwise. Used anywhere
// an AlmostBuy/CommunityProduct thumbnail appears (Home, Cooldowns).
// `productName`, when supplied, additionally checks the bundled marketplace
// illustration table below before falling back to the category glyph -
// only Marketplace call sites pass this (see MarketplaceSection.swift).
struct ProductThumbnail: View {
    let imageURL: String?
    let systemImage: String
    var productName: String? = nil
    var width: CGFloat = 44
    var height: CGFloat = 44
    var cornerRadius: CGFloat = 14
    var fillWidth: Bool = false

    init(imageURL: String?, systemImage: String, productName: String? = nil, size: CGFloat = 44, cornerRadius: CGFloat = 14) {
        self.imageURL = imageURL
        self.systemImage = systemImage
        self.productName = productName
        self.width = size
        self.height = size
        self.cornerRadius = cornerRadius
    }

    init(imageURL: String?, systemImage: String, productName: String? = nil, width: CGFloat, height: CGFloat, cornerRadius: CGFloat = 14) {
        self.imageURL = imageURL
        self.systemImage = systemImage
        self.productName = productName
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }

    // Grid cards (LazyVGrid with flexible columns) need the thumbnail - and
    // therefore the whole card, since the card's VStack has no width of its
    // own - to stretch to the column's actual (device-dependent) width
    // instead of hugging a hardcoded pixel width. Without this, the card
    // sat at its fixed intrinsic width inside a wider flexible column,
    // leaving a dead gap on the right ("everything stuck to the left").
    init(imageURL: String?, systemImage: String, productName: String? = nil, fillWidthHeight height: CGFloat, cornerRadius: CGFloat = 14) {
        self.imageURL = imageURL
        self.systemImage = systemImage
        self.productName = productName
        self.height = height
        self.cornerRadius = cornerRadius
        self.fillWidth = true
    }

    private var bundledPhotoName: String? {
        productName.flatMap(bundledProductPhotoName(for:))
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                // Product photography always sits on a neutral white stage,
                // matching Android's DiscoveryProductCard in both themes.
                // The former green tint remained visible around contained
                // images and made the product art look color-cast.
                .fill(Color.white)
            // Bundled illustration as a base layer under any remote image,
            // matching Android's ProductPhoto z-order (Icons.kt) - catalog
            // items with no imageUrl still show real art, not just a glyph.
            if let bundledPhotoName, UIImage(named: bundledPhotoName) != nil {
                Image(bundledPhotoName).resizable().scaledToFit().padding(min(width, height) * 0.08)
            } else {
                Image(systemName: systemImage)
                    .foregroundStyle(Color.ghostGreenColor)
            }
            if let imageURL, let url = URL(string: imageURL) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        // Contained, not cropped - matches Android's
                        // DiscoveryProductCard (ContentScale.Fit against a
                        // white box), not a cover/fill crop.
                        image.resizable().scaledToFit().padding(min(width, height) * 0.08)
                            .background(Color.white)
                    default:
                        Color.clear
                    }
                }
            }
        }
        .font(.headline)
        .modifier(ProductThumbnailFrame(fillWidth: fillWidth, width: width, height: height))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}

private struct ProductThumbnailFrame: ViewModifier {
    let fillWidth: Bool
    let width: CGFloat
    let height: CGFloat

    func body(content: Content) -> some View {
        if fillWidth {
            content.frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
        } else {
            content.frame(width: width, height: height)
        }
    }
}

struct EmptyStateView: View {
    let image: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: image)
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(Color.ghostGreenColor)
                .accessibilityHidden(true)
            Text(title)
                .font(.headline.weight(.bold))
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(28)
        .frame(maxWidth: .infinity)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

enum AmountFormatter {
    static let number: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "en_AE")
        formatter.numberStyle = .decimal
        // Always 2 decimals ("8,399.00", not "8,399") - matches Android
        // exactly, verified on a live device (Progress strip, product
        // cards, cooldown amounts all show trailing .00).
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.groupingSeparator = ","
        return formatter
    }()

    static func string(_ value: Double) -> String {
        number.string(from: NSNumber(value: value)) ?? "0"
    }
}

// Real dirham glyph icon + amount, matching Android's currency_dirham.png
// usage (e.g. Progress strip, product prices) - copied directly from
// android/app/src/main/res/drawable-nodpi/currency_dirham.png, not
// recreated.
struct DirhamAmount: View {
    let value: Double
    var font: Font = .body
    var iconSize: CGFloat = 14
    var color: Color = .primary

    var body: some View {
        HStack(spacing: 3) {
            Image("DirhamGlyph")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: iconSize, height: iconSize)
            Text(AmountFormatter.string(value))
                .font(font)
        }
        .foregroundStyle(color)
    }
}

extension Date {
    var compactDayAndTime: String {
        formatted(date: .abbreviated, time: .shortened)
    }

    func relativeDescription(from referenceDate: Date = Date()) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: self, relativeTo: referenceDate)
    }
}
