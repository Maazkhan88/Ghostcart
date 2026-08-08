import SwiftUI

// In-context tutorial spotlight - visual design ported from Android's
// TutorialGuideOverlay.kt (dimmed scrim everywhere except a rounded cutout
// around the real production control, green border on the cutout, a
// floating "teacher" bubble with a step label + message that flips
// top/bottom depending on where the cutout sits). Production UI stays fully
// mounted and interactive underneath; only the spotlit rect is tappable,
// everywhere else in the scrim blocks touches - same contract as Android's
// TutorialTouchBlocker four-quadrant layout.
struct TutorialGuideSpec: Equatable {
    let message: String
    let stepLabel: String
}

// SwiftUI's equivalent of Android's onGloballyPositioned/Rect reporting:
// any production view tagged with .tutorialTarget(id) reports its frame up
// through this preference key, and TutorialGuideOverlay reads it back by id.
struct TutorialTargetFrame: Equatable {
    let id: String
    let frame: CGRect
}

struct TutorialTargetFramesKey: PreferenceKey {
    static var defaultValue: [TutorialTargetFrame] = []
    static func reduce(value: inout [TutorialTargetFrame], nextValue: () -> [TutorialTargetFrame]) {
        value.append(contentsOf: nextValue())
    }
}

extension View {
    /// Tags a real production control as a tutorial spotlight target. Purely
    /// additive - has zero effect outside an active tutorial, since nothing
    /// reads TutorialTargetFramesKey unless TutorialGuideOverlay is mounted.
    func tutorialTarget(_ id: String) -> some View {
        background(
            GeometryReader { proxy in
                Color.clear.preference(
                    key: TutorialTargetFramesKey.self,
                    value: [TutorialTargetFrame(id: id, frame: proxy.frame(in: .global))]
                )
            }
        )
    }
}

struct TutorialGuideOverlay: View {
    let targetFrame: CGRect?
    let guide: TutorialGuideSpec
    let onSkip: () -> Void

    var body: some View {
        GeometryReader { proxy in
            let size = proxy.size

            ZStack(alignment: .topLeading) {
                if let targetFrame {
                    let padding: CGFloat = 9
                    let target = CGRect(
                        x: max(0, targetFrame.minX - padding),
                        y: max(0, targetFrame.minY - padding),
                        width: min(size.width, targetFrame.width + padding * 2),
                        height: min(size.height, targetFrame.height + padding * 2)
                    )

                    // Scrim with a cutout - drawn as four rects around the
                    // target rather than a mask, matching Android's four-quadrant
                    // Canvas.drawRect approach (simpler to reason about than
                    // blend-mode masking, and avoids compositing quirks). Each
                    // rect blocks touches simply by being an opaque view on
                    // top - there is deliberately no blanket tap-catcher
                    // behind them, so the untouched cutout area lets real
                    // taps fall straight through to the production control
                    // underneath (a `.contentShape` + `.onTapGesture` here
                    // previously covered the *entire* frame regardless of the
                    // visual cutout, silently swallowing every tap on the
                    // whole screen including the spotlighted button itself -
                    // do not reintroduce that).
                    Color.black.opacity(0.70)
                        .frame(width: size.width, height: target.minY)
                        .position(x: size.width / 2, y: target.minY / 2)
                    Color.black.opacity(0.70)
                        .frame(width: size.width, height: max(0, size.height - target.maxY))
                        .position(x: size.width / 2, y: target.maxY + max(0, size.height - target.maxY) / 2)
                    Color.black.opacity(0.70)
                        .frame(width: target.minX, height: target.height)
                        .position(x: target.minX / 2, y: target.midY)
                    Color.black.opacity(0.70)
                        .frame(width: max(0, size.width - target.maxX), height: target.height)
                        .position(x: target.maxX + max(0, size.width - target.maxX) / 2, y: target.midY)

                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color.ghostGreenColor, lineWidth: 3)
                        .frame(width: target.width, height: target.height)
                        .position(x: target.midX, y: target.midY)
                        .allowsHitTesting(false)

                    guideBubble(target: target, containerSize: size)
                        .allowsHitTesting(false)
                } else {
                    // The target hasn't reported a frame yet (e.g. this
                    // frame hasn't laid out, or the spotlighted screen isn't
                    // currently visible) - dim lightly rather than leaving a
                    // fully invisible full-screen view, and always keep the
                    // skip escape hatch below reachable so the tutorial can
                    // never soft-lock the app.
                    Color.black.opacity(0.35)
                        .frame(width: size.width, height: size.height)
                        .allowsHitTesting(false)
                }

                Button("Skip tutorial", action: onSkip)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.black.opacity(0.6), in: Capsule())
                    .padding(.top, 8)
                    .padding(.trailing, 16)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .ignoresSafeArea()
        }
    }

    private func guideBubble(target: CGRect, containerSize: CGSize) -> some View {
        let showBelow = target.midY <= containerSize.height * 0.55
        return VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .top, spacing: 12) {
                TutorialTeacherBadge()
                VStack(alignment: .leading, spacing: 4) {
                    Text(guide.stepLabel)
                        .font(.caption2.weight(.black))
                        .foregroundStyle(Color.ghostGreenColor)
                    Text(guide.message)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.white)
                }
            }
            .padding(16)
        }
        .background(Color.black, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color.white.opacity(0.12), lineWidth: 1)
        }
        .frame(maxWidth: 440)
        .padding(.horizontal, 20)
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity, alignment: .center)
        .position(
            x: containerSize.width / 2,
            y: showBelow ? min(containerSize.height - 90, target.maxY + 90) : max(90, target.minY - 90)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

// Floating, gently bobbing ghost mascot badge - simpler than Android's
// custom teacher illustration set (tutorial_teacher_pointer/board/checklist
// drawables), reusing this app's existing GhostMascotView asset system
// instead of introducing a parallel illustration set for one feature.
private struct TutorialTeacherBadge: View {
    @State private var floatUp = false

    var body: some View {
        GhostMascotView(poseName: "wave")
            .frame(width: 44, height: 44)
            .padding(10)
            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .offset(y: floatUp ? -4 : 4)
            .animation(.easeInOut(duration: 1.3).repeatForever(autoreverses: true), value: floatUp)
            .onAppear { floatUp = true }
            .accessibilityHidden(true)
    }
}
