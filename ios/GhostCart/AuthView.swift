import SwiftUI

// Mirrors Android's AuthScreen: tabbed Sign In / Sign Up over email+password,
// plus a Guest bypass. Google Sign-In needs the native SDK and Apple
// Sign-In needs a verified Services ID/callback domain configured server
// side - both out of scope here, so (like Android) the buttons are present
// but explain themselves rather than silently doing nothing.
struct AuthView: View {
    @EnvironmentObject private var auth: AuthService
    let onAuthSuccess: () -> Void
    let onGuest: () -> Void

    private enum Mode: String, CaseIterable { case signIn = "Sign In", signUp = "Sign Up" }

    @State private var mode: Mode = .signIn
    @State private var email = ""
    @State private var password = ""
    @State private var displayName = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var unavailableProviderMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                GhostMascotView(poseName: "wave")
                    .frame(width: 72, height: 72)

                Picker("Mode", selection: $mode) {
                    ForEach(Mode.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)

                VStack(spacing: 12) {
                    if mode == .signUp {
                        TextField("Display name (optional)", text: $displayName)
                            .ghostTextField()
                    }
                    TextField("Email", text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .ghostTextField()
                    SecureField("Password", text: $password)
                        .ghostTextField()
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button(isSubmitting ? "Please wait…" : mode.rawValue, action: submit)
                    .buttonStyle(GhostPrimaryButtonStyle())
                    .disabled(isSubmitting || email.trimmingCharacters(in: .whitespaces).isEmpty || password.isEmpty)

                VStack(spacing: 10) {
                    Button {
                        unavailableProviderMessage = "Google Sign-In isn't wired up on iOS yet."
                    } label: {
                        Label("Continue with Google", systemImage: "g.circle")
                    }
                    .buttonStyle(GhostSecondaryButtonStyle())

                    Button {
                        unavailableProviderMessage = "Apple Sign-In requires a verified Services ID and callback domain, not configured yet."
                    } label: {
                        Label("Continue with Apple", systemImage: "apple.logo")
                    }
                    .buttonStyle(GhostSecondaryButtonStyle())
                }

                Button("Continue as Guest", action: onGuest)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.secondary)
                    .padding(.top, 4)
            }
            .padding(24)
        }
        .background(Color.paperColor.ignoresSafeArea())
        .alert("Not available yet", isPresented: Binding(
            get: { unavailableProviderMessage != nil },
            set: { if !$0 { unavailableProviderMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(unavailableProviderMessage ?? "")
        }
    }

    private func submit() {
        errorMessage = nil
        isSubmitting = true
        Task {
            defer { isSubmitting = false }
            do {
                switch mode {
                case .signIn:
                    try await auth.signIn(email: email, password: password)
                case .signUp:
                    try await auth.signUp(email: email, password: password, displayName: displayName)
                }
                onAuthSuccess()
            } catch {
                errorMessage = (error as? ApiError)?.message ?? "Something went wrong. Try again."
            }
        }
    }
}

#Preview {
    AuthView(onAuthSuccess: {}, onGuest: {})
        .environmentObject(AuthService.shared)
}
