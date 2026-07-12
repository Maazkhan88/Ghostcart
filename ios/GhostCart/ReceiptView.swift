import SwiftUI

struct ReceiptView: View {
    let ghostedCount: Int
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.inkColor.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                VStack(alignment: .center, spacing: 0) {
                    Text("GHOST RECEIPT")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.gray)
                        .tracking(1)
                        .padding(.bottom, 20)

                    GhostMascotView(poseName: "thumbsup")
                        .frame(width: 92, height: 92)
                        .padding(.bottom, 24)

                    Text("Nothing purchased.\nCraving completed.")
                        .font(.system(size: 28, weight: .black))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                        .padding(.bottom, 24)

                    // Divider
                    Rectangle()
                        .fill(Color(white: 0.17))
                        .frame(height: 1)
                        .padding(.bottom, 16)

                    Text("\(ghostedCount) almost-buy\(ghostedCount == 1 ? "" : "s") ghosted in this demo.")
                        .font(.system(size: 13))
                        .foregroundColor(Color(white: 0.72))
                        .multilineTextAlignment(.center)
                        .padding(.bottom, 12)

                    Text("Real amount charged: Zero")
                        .font(.system(size: 15, weight: .black))
                        .foregroundColor(.ghostGreenColor)
                        .padding(.bottom, 8)
                }
                .padding(.horizontal, 24)

                Spacer()

                VStack(spacing: 16) {
                    Button(action: onDismiss) {
                        Text("Ghost another cart")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color.clear)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.white, lineWidth: 1)
                            )
                    }

                    Text("Not an invoice or proof of purchase.")
                        .font(.system(size: 8))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 48)
            }
        }
    }
}
