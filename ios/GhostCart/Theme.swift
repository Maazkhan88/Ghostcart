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

// Shared product-thumbnail: real remote image when the item has one (Coil's
// AsyncImage equivalent), category glyph fallback otherwise. Used anywhere
// an AlmostBuy/CommunityProduct thumbnail appears (Home, Cooldowns).
struct ProductThumbnail: View {
    let imageURL: String?
    let systemImage: String
    var width: CGFloat = 44
    var height: CGFloat = 44
    var cornerRadius: CGFloat = 14

    init(imageURL: String?, systemImage: String, size: CGFloat = 44, cornerRadius: CGFloat = 14) {
        self.imageURL = imageURL
        self.systemImage = systemImage
        self.width = size
        self.height = size
        self.cornerRadius = cornerRadius
    }

    init(imageURL: String?, systemImage: String, width: CGFloat, height: CGFloat, cornerRadius: CGFloat = 14) {
        self.imageURL = imageURL
        self.systemImage = systemImage
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                // Product photography always sits on a neutral white stage,
                // matching Android's DiscoveryProductCard in both themes.
                // The former green tint remained visible around contained
                // images and made the product art look color-cast.
                .fill(Color.white)
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
                        Image(systemName: systemImage)
                            .foregroundStyle(Color.ghostGreenColor)
                    }
                }
            } else {
                Image(systemName: systemImage)
                    .foregroundStyle(Color.ghostGreenColor)
            }
        }
        .font(.headline)
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
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
