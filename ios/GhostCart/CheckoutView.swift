import SwiftUI

struct CheckoutView: View {
    let deliveryStep: Int
    let onViewReceipt: () -> Void

    private let timelineSteps = [
        "Placed order",
        "Order accepted",
        "Order getting prepared",
        "Ghost rider picking up the order",
        "Ghost rider on the way to deliver",
        "Ghost rider has delivered your ghost order"
    ]

    private var currentMascotPose: String {
        switch deliveryStep {
        case 0: return "cart"
        case 1: return "wave"
        case 2: return "cooldown"
        case 3: return "cooldown"
        case 4: return "cooldown"
        default: return "thumbsup"
        }
    }

    var body: some View {
        ZStack {
            Color.inkColor.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                // Header
                VStack(alignment: .leading, spacing: 4) {
                    Text("Simulated Ghost Delivery".uppercased())
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.ghostGreenColor)
                        .tracking(1)
                        .padding(.top, 56)

                    Text(deliveryStep == 5 ? "Ghost order\nhas arrived!" : "Following your\nghost order...")
                        .font(.system(size: 26, weight: .black))
                        .foregroundColor(.white)
                        .lineSpacing(4)
                        .padding(.bottom, 20)
                }
                .padding(.horizontal, 16)

                // Mascot
                GhostMascotView(poseName: currentMascotPose)
                    .frame(width: 92, height: 92)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)

                // Progress bar
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color(white: 0.17))
                            .frame(height: 6)
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.ghostGreenColor)
                            .frame(width: geo.size.width * CGFloat(deliveryStep.clamped(to: 0...5)) / 5.0, height: 6)
                            .animation(.easeInOut(duration: 0.4), value: deliveryStep)
                    }
                }
                .frame(height: 6)
                .padding(.horizontal, 16)
                .padding(.bottom, 24)

                // Steps list
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(timelineSteps.enumerated()), id: \.offset) { index, stepText in
                        let isCompleted = deliveryStep > index
                        let isActive = deliveryStep == index

                        HStack(spacing: 10) {
                            Text(isCompleted ? "✓" : (isActive ? "●" : "○"))
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(isCompleted ? .ghostGreenColor : (isActive ? .white : .gray))
                                .frame(width: 16)

                            Text(stepText)
                                .font(.system(size: 11, weight: isActive ? .bold : .regular))
                                .foregroundColor(isCompleted ? .ghostGreenColor : (isActive ? .white : .gray))
                        }
                    }
                }
                .padding(.horizontal, 16)

                Spacer()

                // Footer
                VStack(spacing: 12) {
                    if deliveryStep == 5 {
                        Button(action: onViewReceipt) {
                            Text("View Ghost Receipt")
                                .font(.system(size: 14, weight: .black))
                                .foregroundColor(.inkColor)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color.ghostGreenColor)
                                .cornerRadius(12)
                        }
                    }

                    Text("Simulation only · No real courier is dispatched.")
                        .font(.system(size: 8))
                        .foregroundColor(.gray)
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                        .padding(.bottom, 32)
                }
                .padding(.horizontal, 16)
            }
        }
    }
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}
