import ActivityKit
import WidgetKit
import SwiftUI

// Ghost Cart's brand palette (Theme.swift, app target only) isn't shared
// into this extension target - these two constants are the only colors the
// Live Activity actually needs, copied by value rather than pulling the
// whole app-side design system into a widget process.
private let ghostGreen = Color(red: 0.39, green: 0.84, blue: 0.29)

// en_AE, always 2 decimals - matches AmountFormatter's app-side output
// ("1,299.00") without depending on the app target's Theme.swift.
private func priceString(cents: Int) -> String {
    let formatter = NumberFormatter()
    formatter.locale = Locale(identifier: "en_AE")
    formatter.numberStyle = .decimal
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    formatter.groupingSeparator = ","
    return formatter.string(from: NSNumber(value: Double(cents) / 100)) ?? "0.00"
}

@available(iOS 16.2, *)
struct CoolingOffLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: GhostLiveActivityAttributes.self) { context in
            LockScreenView(attributes: context.attributes, state: context.state)
                .activityBackgroundTint(Color.black)
                .activitySystemActionForegroundColor(Color.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    HStack(spacing: 5) {
                        Text("👻").font(.title3)
                        Text("Ghosting")
                            .font(.caption2.weight(.black))
                            .foregroundStyle(ghostGreen)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    CountdownText(state: context.state, font: .title3.weight(.bold))
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.attributes.productName)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Text("AED \(priceString(cents: context.attributes.priceCents))")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                        Spacer()
                        Text(context.state.statusMessage)
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.6))
                            .lineLimit(1)
                    }
                }
            } compactLeading: {
                Text("👻")
            } compactTrailing: {
                CountdownText(state: context.state, font: .caption2.weight(.bold))
                    .frame(maxWidth: 44)
            } minimal: {
                Text("👻")
            }
            .widgetURL(URL(string: "https://theghostcart.com/\(context.attributes.deepLinkPath)"))
            .keylineTint(ghostGreen)
        }
    }
}

// Shared by the expanded/compact Dynamic Island regions: a live system-
// rendered countdown while active, or a small glyph once resolved. Never
// recomputed by the app - Text(timerInterval:) ticks on its own.
@available(iOS 16.2, *)
private struct CountdownText: View {
    let state: GhostLiveActivityAttributes.ContentState
    let font: Font

    var body: some View {
        Group {
            if state.status == .active {
                Text(timerInterval: Date()...max(state.endTime, Date().addingTimeInterval(1)), countsDown: true)
                    .monospacedDigit()
            } else {
                Text(state.status == .completed ? "✓" : (state.status == .bought ? "🛍" : "—"))
            }
        }
        .font(font)
        .foregroundStyle(.white)
        .minimumScaleFactor(0.7)
        .lineLimit(1)
    }
}

// The Lock Screen presentation. A TimelineView ticking every 30s is what
// lets "Ghosting" -> "Almost free" flip purely from how close `endTime` is
// to now, with zero extra ActivityKit updates from the app for that
// threshold - only the countdown numerals themselves (via
// Text/ProgressView(timerInterval:)) update more often than that, and the
// system drives those, not this view.
@available(iOS 16.2, *)
private struct LockScreenView: View {
    let attributes: GhostLiveActivityAttributes
    let state: GhostLiveActivityAttributes.ContentState

    var body: some View {
        TimelineView(.periodic(from: attributes.startTime, by: 30)) { timeline in
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline) {
                    HStack(spacing: 6) {
                        Text("👻")
                        Text(headline(at: timeline.date))
                            .font(.caption.weight(.black))
                            .foregroundStyle(ghostGreen)
                    }
                    Spacer()
                    CountdownText(state: state, font: .callout.weight(.bold))
                }
                Text(attributes.productName)
                    .font(.title3.weight(.black))
                    .foregroundStyle(.white)
                    .lineLimit(1)

                if state.status == .active {
                    ProgressView(
                        timerInterval: attributes.startTime...max(state.endTime, attributes.startTime.addingTimeInterval(1)),
                        countsDown: false
                    )
                    .tint(ghostGreen)
                }

                HStack {
                    Text("AED \(priceString(cents: attributes.priceCents))")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white.opacity(0.85))
                    Spacer()
                    Text(state.statusMessage)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
            }
            .padding(16)
        }
    }

    private func headline(at date: Date) -> String {
        guard state.status == .active else {
            switch state.status {
            case .completed: return "Ghosted successfully"
            case .bought: return "Bought from source"
            default: return "Cooling-off ended"
            }
        }
        let remaining = state.endTime.timeIntervalSince(date)
        if remaining <= 0 { return "Ready to decide" }
        if remaining <= 600 { return "Almost free" }
        return "Ghosting"
    }
}
