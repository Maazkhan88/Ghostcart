import SwiftUI

// Mirrors Android's delete-account confirmation dialog (GhostCartV2Screens.kt
// ~1296-1327) verbatim, including the copy. Android's "Delete account" is
// local-data-only (no backend delete endpoint exists) - see
// GhostCartStore.resetAllLocalData for why iOS matches that exactly rather
// than inventing new server behavior.
struct DeleteAccountButton: View {
    @EnvironmentObject private var store: GhostCartStore
    @EnvironmentObject private var auth: AuthService
    @State private var confirming = false

    var body: some View {
        Button {
            confirming = true
        } label: {
            Text("Delete account")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.red.opacity(0.4), lineWidth: 1)
        }
        .alert("Delete Ghost Cart account?", isPresented: $confirming) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                store.resetAllLocalData()
                auth.signOut()
            }
        } message: {
            Text("This permanently removes this device's profile, favorites, cooldowns, simulated wallet and activity history.")
        }
    }
}
