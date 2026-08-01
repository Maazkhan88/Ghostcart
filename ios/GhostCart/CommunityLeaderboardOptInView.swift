import SwiftUI

// Mirrors Android's ProfileCommunitySection (GhostCartV2Screens.kt:916-1092):
// display name + the opt-in Community Leaderboard toggle. Opting in requires
// a username (validated server-side: format, reserved names, a blocklist,
// uniqueness); opting out hides the user from the leaderboard immediately
// without losing the username, in case they opt back in later. Completely
// separate from - and never weakens - the community-products feed's
// existing anonymous-by-default guarantee.
struct CommunityLeaderboardOptInView: View {
    @Binding var profile: CommunityProfile?
    let onOpenLeaderboard: () -> Void

    @State private var displayNameDraft = ""
    @State private var usernameDraft = ""
    @State private var editingUsername = false
    @State private var saving = false
    @State private var errorMessage: String?

    var body: some View {
        GhostCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Display name").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                TextField("Display name", text: $displayNameDraft)
                    .ghostTextField()
                Button(saving ? "Saving…" : "Save name") {
                    Task { await saveDisplayName() }
                }
                .font(.caption.weight(.bold))
                .foregroundStyle(displayNameCanSave ? Color.ghostGreenColor : Color.secondary)
                .disabled(!displayNameCanSave)

                Divider()

                Text("Community Leaderboard").font(.subheadline.weight(.black)).foregroundStyle(Color.primary)
                Text("Opt in to show a username on the public leaderboard (ranked by money kept). Your email is never shown.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)

                if profile?.communityConsent == true && !editingUsername {
                    optedInContent
                } else {
                    optInForm
                }

                if let errorMessage {
                    Text(errorMessage).font(.caption.weight(.bold)).foregroundStyle(Color.red)
                }
            }
        }
        .onAppear { syncDrafts() }
        .onChange(of: profile) { _ in syncDrafts() }
    }

    private var optedInContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("You're on the leaderboard as @\(profile?.username ?? "")")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.primary)
                Spacer()
            }
            HStack(spacing: 16) {
                Button("View", action: onOpenLeaderboard)
                Button("Edit") {
                    usernameDraft = profile?.username ?? ""
                    editingUsername = true
                }
                Button("Opt out") {
                    Task { await setOptIn(username: nil, consent: false) }
                }
                .foregroundStyle(Color.red)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color.ghostGreenColor)
            .disabled(saving)

            Divider()

            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Show recent activity to other members").font(.caption.weight(.bold)).foregroundStyle(Color.primary)
                    Text("Lets others see your recent ghosted items and activity feed when they open your leaderboard entry. Off by default.")
                        .font(.caption2)
                        .foregroundStyle(Color.secondary)
                }
                Spacer()
                Toggle("", isOn: showRecentActivityBinding)
                    .labelsHidden()
                    .disabled(saving)
            }
        }
    }

    private var optInForm: some View {
        VStack(alignment: .leading, spacing: 10) {
            TextField("e.g. ghost_saver_22", text: $usernameDraft)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .ghostTextField()
            HStack {
                Button(saving ? "Saving…" : (editingUsername ? "Save username" : "Join the leaderboard")) {
                    Task { await setOptIn(username: usernameDraft.trimmingCharacters(in: .whitespaces), consent: true) }
                }
                .foregroundStyle(usernameValid ? Color.ghostGreenColor : Color.secondary)
                .disabled(!usernameValid || saving)
                if editingUsername {
                    Button("Cancel") { editingUsername = false }
                        .foregroundStyle(Color.secondary)
                }
            }
            .font(.caption.weight(.bold))
        }
    }

    private var displayNameCanSave: Bool {
        !saving && displayNameDraft != (profile?.displayName ?? "")
    }

    private var usernameValid: Bool {
        usernameDraft.trimmingCharacters(in: .whitespaces).count >= 3
    }

    private var showRecentActivityBinding: Binding<Bool> {
        Binding(
            get: { profile?.showRecentActivityPublicly ?? false },
            set: { newValue in Task { await setShowRecentActivity(newValue) } }
        )
    }

    private func syncDrafts() {
        displayNameDraft = profile?.displayName ?? ""
        if !editingUsername { usernameDraft = profile?.username ?? "" }
    }

    @MainActor
    private func saveDisplayName() async {
        saving = true; errorMessage = nil
        defer { saving = false }
        do {
            profile = try await CommunityProfileService.update(displayName: displayNameDraft)
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Could not save display name."
        }
    }

    @MainActor
    private func setOptIn(username: String?, consent: Bool) async {
        saving = true; errorMessage = nil
        defer { saving = false }
        do {
            profile = try await CommunityProfileService.update(username: username, communityConsent: consent)
            editingUsername = false
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Could not update the leaderboard."
        }
    }

    @MainActor
    private func setShowRecentActivity(_ value: Bool) async {
        saving = true; errorMessage = nil
        defer { saving = false }
        do {
            profile = try await CommunityProfileService.update(showRecentActivityPublicly: value)
        } catch {
            errorMessage = (error as? ApiError)?.message ?? "Could not update this setting."
        }
    }
}
