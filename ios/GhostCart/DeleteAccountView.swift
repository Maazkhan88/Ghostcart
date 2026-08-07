import SwiftUI

// Mirrors Android's delete-account confirmation dialog (GhostCartV2Screens.kt
// ~1296-1327) for copy/layout, but - unlike Android, which is still local-data
// only - actually deletes the account server-side via DELETE /api/me/account
// first. Apple Guideline 5.1.1(v) is explicit that "only offering to
// temporarily deactivate or disable an account is insufficient"; a purely
// local wipe left the account (and every server-side row) fully intact and
// reusable, which doesn't satisfy that. The server delete cascades to every
// user-owned table (db/schema.ts onDelete: "cascade", enforced - D1 runs
// with PRAGMA foreign_keys = ON), then the local wipe + sign-out proceed the
// same as before so the device state matches immediately either way.
struct DeleteAccountButton: View {
    @EnvironmentObject private var store: GhostCartStore
    @EnvironmentObject private var auth: AuthService
    @State private var confirming = false
    @State private var isDeleting = false
    @State private var errorMessage: String?

    var body: some View {
        Button {
            confirming = true
        } label: {
            if isDeleting {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            } else {
                Text("Delete account")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.red)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
        }
        .disabled(isDeleting)
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.red.opacity(0.4), lineWidth: 1)
        }
        .alert("Delete Ghost Cart account?", isPresented: $confirming) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                Task { await deleteAccount() }
            }
        } message: {
            Text("This permanently deletes your account and all associated data from Ghost Cart's servers, and removes this device's local profile, favorites, cooldowns, simulated wallet and activity history. This cannot be undone.")
        }
        .alert("Couldn't delete account", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func deleteAccount() async {
        isDeleting = true
        defer { isDeleting = false }
        do {
            _ = try await auth.authorizedDelete(path: "/api/me/account")
            store.resetAllLocalData()
            auth.signOut()
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Check your connection and try again."
        }
    }
}
