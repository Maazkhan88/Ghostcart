import SwiftUI

struct WaitlistView: View {
    let submitted: Bool
    let onSubmit: (String) -> Void

    @State private var emailInput: String = ""

    var body: some View {
        ZStack {
            Color.inkColor.ignoresSafeArea()

            if !submitted {
                VStack(spacing: 16) {
                    Text("COMING SOON")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.ghostGreenColor)
                        .tracking(1)

                    Text("Get early access.")
                        .font(.system(size: 28, weight: .black))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)

                    Text("Join our waitlist to know when the native app launches. No real payments, ever.")
                        .font(.system(size: 13))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12)
                        .lineSpacing(4)

                    TextField("Email address", text: $emailInput)
                        .padding(14)
                        .background(Color.darkGrayColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(emailInput.isEmpty ? Color(white: 0.17) : Color.ghostGreenColor, lineWidth: 1)
                        )
                        .autocapitalization(.none)
                        .keyboardType(.emailAddress)
                        .padding(.top, 8)

                    Button(action: {
                        if !emailInput.isEmpty { onSubmit(emailInput) }
                    }) {
                        Text("Join Waitlist")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.inkColor)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color.ghostGreenColor)
                            .cornerRadius(12)
                    }
                }
                .padding(24)
            } else {
                VStack(spacing: 16) {
                    Text("SUCCESS")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.ghostGreenColor)
                        .tracking(1)

                    Text("You are on the list!")
                        .font(.system(size: 28, weight: .black))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)

                    Text("Your submission has been captured locally. We will notify you when we launch.")
                        .font(.system(size: 13))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12)
                        .lineSpacing(4)

                    Text("Sample data · not a user claim")
                        .font(.system(size: 8, weight: .semibold))
                        .foregroundColor(.gray)
                }
                .padding(24)
            }
        }
    }
}
