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
        Label("Simulation only. No real payment. No real delivery.", systemImage: "checkmark.shield")
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.secondary)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.ghostGreenColor.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        formatter.groupingSeparator = ","
        return formatter
    }()

    static func string(_ value: Double) -> String {
        number.string(from: NSNumber(value: value)) ?? "0"
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
