import SwiftUI
import AuthenticationServices
import CryptoKit
import Security
import UIKit
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

// Mirrors Android's AuthScreen: tabbed Sign In / Sign Up over email+password,
// native provider authentication, plus a Guest bypass.
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
    @State private var appleNonce: String?

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
                    Button(action: signInWithGoogle) {
                        HStack(spacing: 10) {
                            Image("GoogleGLogo")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 20, height: 20)
                            Text("Continue with Google")
                        }
                    }
                    .buttonStyle(GhostSecondaryButtonStyle())
                    .disabled(isSubmitting)

                    SignInWithAppleButton(.continue) { request in
                        let nonce = Self.randomNonce()
                        appleNonce = nonce
                        request.requestedScopes = [.fullName, .email]
                        request.nonce = Self.sha256(nonce)
                    } onCompletion: { result in
                        handleAppleResult(result)
                    }
                    .signInWithAppleButtonStyle(.black)
                    .frame(height: 52)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .disabled(isSubmitting)
                }

                Button("Continue as Guest", action: onGuest)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.secondary)
                    .padding(.top, 4)
            }
            .padding(24)
        }
        .background(Color.paperColor.ignoresSafeArea())
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

    private func signInWithGoogle() {
        errorMessage = nil
        #if canImport(GoogleSignIn)
        guard let clientID = Self.googlePlistValue("CLIENT_ID"),
              let serverClientID = Bundle.main.object(forInfoDictionaryKey: "GIDServerClientID") as? String,
              !clientID.isEmpty, !serverClientID.isEmpty else {
            errorMessage = "Google Sign-In setup is incomplete. Add the iOS OAuth client to Firebase, then replace GoogleService-Info.plist."
            return
        }
        guard let presenter = Self.presentingViewController else {
            errorMessage = "Google Sign-In could not open. Try again."
            return
        }

        isSubmitting = true
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID,
            serverClientID: serverClientID
        )
        Task {
            defer { isSubmitting = false }
            do {
                let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
                guard let token = result.user.idToken?.tokenString else {
                    throw ProviderSignInError("Google did not return an identity token.")
                }
                try await auth.signInWithGoogle(idToken: token)
                onAuthSuccess()
            } catch let error as GIDSignInError where error.code == .canceled {
                // Cancellation is an intentional user action, not a failure.
            } catch {
                errorMessage = Self.message(for: error)
            }
        }
        #else
        errorMessage = "Google Sign-In is not included in this build."
        #endif
    }

    private func handleAppleResult(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .failure(let error as ASAuthorizationError) where error.code == .canceled:
            appleNonce = nil
        case .failure(let error):
            appleNonce = nil
            errorMessage = Self.message(for: error)
        case .success(let authorization):
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = credential.identityToken,
                  let identityToken = String(data: tokenData, encoding: .utf8),
                  let nonce = appleNonce else {
                appleNonce = nil
                errorMessage = "Apple did not return a valid identity token. Try again."
                return
            }
            appleNonce = nil
            let name = credential.fullName.map { PersonNameComponentsFormatter().string(from: $0) }
            isSubmitting = true
            Task {
                defer { isSubmitting = false }
                do {
                    try await auth.signInWithApple(
                        identityToken: identityToken,
                        nonce: nonce,
                        displayName: name
                    )
                    onAuthSuccess()
                } catch {
                    errorMessage = Self.message(for: error)
                }
            }
        }
    }

    private static func message(for error: Error) -> String {
        (error as? ApiError)?.message ?? (error as? LocalizedError)?.errorDescription ?? "Sign-in failed. Try again."
    }

    private static func googlePlistValue(_ key: String) -> String? {
        guard let url = Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist"),
              let data = try? Data(contentsOf: url),
              let values = try? PropertyListSerialization.propertyList(from: data, format: nil) as? [String: Any]
        else { return nil }
        return values[key] as? String
    }

    private static var presentingViewController: UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first,
              var controller = scene.windows.first(where: \.isKeyWindow)?.rootViewController else { return nil }
        while let presented = controller.presentedViewController { controller = presented }
        return controller
    }

    private static func randomNonce(length: Int = 32) -> String {
        precondition(length > 0)
        let characters = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var randomBytes = [UInt8](repeating: 0, count: 16)
        while result.count < length {
            guard SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes) == errSecSuccess else {
                fatalError("Unable to generate a secure Apple Sign-In nonce.")
            }
            for byte in randomBytes where result.count < length && Int(byte) < characters.count {
                result.append(characters[Int(byte)])
            }
        }
        return result
    }

    private static func sha256(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

private struct ProviderSignInError: LocalizedError {
    let message: String
    init(_ message: String) { self.message = message }
    var errorDescription: String? { message }
}

#Preview {
    AuthView(onAuthSuccess: {}, onGuest: {})
        .environmentObject(AuthService.shared)
}
