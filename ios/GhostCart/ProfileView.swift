import SwiftUI
import UIKit

struct ProfileView: View {
    @EnvironmentObject private var store: GhostCartStore
    @EnvironmentObject private var auth: AuthService
    @EnvironmentObject private var onboarding: OnboardingState
    @State private var showMembership = false
    @State private var showLeaderboard = false
    @State private var showTutorial = false
    @State private var showSignIn = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                ScreenHeader(
                    eyebrow: "Your controls",
                    title: "Profile",
                    subtitle: accountSubtitle
                )

                accountCard

                Button { showMembership = true } label: {
                    MembershipCardPreview(profile: store.membership)
                }
                .buttonStyle(.plain)

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(title: "Preferences")
                    NavigationLink {
                        ReminderSettingsView()
                    } label: {
                        SettingsRow(
                            image: "bell.badge",
                            title: "Reminders",
                            subtitle: "Cooldown, meal and salary-day controls"
                        )
                    }
                    .buttonStyle(.plain)

                    GhostCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Appearance", systemImage: "circle.lefthalf.filled")
                                .font(.headline.weight(.bold))
                            Picker("Appearance", selection: appearanceBinding) {
                                ForEach(AppearancePreference.allCases) { preference in
                                    Text(preference.title).tag(preference)
                                }
                            }
                            .pickerStyle(.segmented)

                            Toggle("Reduce decorative motion", isOn: reduceMotionBinding)
                                .font(.subheadline.weight(.semibold))
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(title: "Community & learning")
                    Button { showLeaderboard = true } label: {
                        SettingsRow(image: "trophy", title: "Community Leaderboard", subtitle: "See opted-in members and their Ghost Cart progress")
                    }
                    .buttonStyle(.plain)
                    Button {
                        TutorialView.resetSavedSession()
                        showTutorial = true
                    } label: {
                        SettingsRow(image: "play.rectangle", title: "Replay app tutorial", subtitle: "Practice the complete cooling and decision journey")
                    }
                    .buttonStyle(.plain)
                }

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeading(title: "Privacy & trust")
                    GhostCard {
                        VStack(alignment: .leading, spacing: 12) {
                            TrustRow(image: "internaldrive", text: "Almost-buys are stored locally in this scaffold.")
                            TrustRow(image: "chart.bar.xaxis", text: "Free-text names and links are not sent to analytics.")
                            TrustRow(image: "checkmark.shield", text: "Ghost membership is not a financial product.")
                        }
                    }
                }

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 112)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .sheet(isPresented: $showMembership) {
            NavigationStack {
                MembershipCardEditorView()
            }
        }
        .sheet(isPresented: $showLeaderboard) {
            NavigationStack { LeaderboardView() }
        }
        .fullScreenCover(isPresented: $showTutorial) {
            TutorialView(onFinish: {
                onboarding.progress.tutorialComplete = true
                showTutorial = false
            })
            .environmentObject(store)
        }
        .sheet(isPresented: $showSignIn) {
            NavigationStack {
                AuthView(
                    onAuthSuccess: { showSignIn = false },
                    onGuest: { showSignIn = false }
                )
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Close") { showSignIn = false }
                    }
                }
            }
            // AuthView's card is always Color.paperColor (fixed white, not
            // dark-mode-adaptive) by design, matching the onboarding
            // presentation - force the sheet chrome (nav bar/Close button)
            // to match instead of following the system Dark preference,
            // which otherwise clashed with the always-white content below it.
            .preferredColorScheme(.light)
            .environmentObject(auth)
        }
    }

    private var accountSubtitle: String {
        if case .signedIn(let user) = auth.state { return user.email }
        return "Guest profile · sign in from the welcome flow to sync account features."
    }

    @ViewBuilder
    private var accountCard: some View {
        if case .signedIn(let user) = auth.state {
            GhostCard {
                HStack(spacing: 12) {
                    Image(systemName: "person.crop.circle.fill")
                        .font(.largeTitle)
                        .foregroundStyle(Color.ghostGreenColor)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(user.displayName ?? store.membership.displayName).font(.headline.weight(.bold))
                        Text(user.email).font(.caption).foregroundStyle(Color.secondary)
                    }
                    Spacer()
                    Button("Sign out") { auth.signOut() }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.red)
                }
            }
        } else {
            GhostCard {
                VStack(alignment: .leading, spacing: 12) {
                    Label {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Using Ghost Cart as a guest").font(.headline.weight(.bold))
                            Text("Your local cooldown and wallet data remains on this device.").font(.caption).foregroundStyle(Color.secondary)
                        }
                    } icon: {
                        Image(systemName: "person.crop.circle.badge.questionmark").foregroundStyle(Color.ghostGreenColor)
                    }
                    Button("Sign in or create account") { showSignIn = true }
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.ghostGreenColor)
                }
            }
        }
    }

    private var appearanceBinding: Binding<AppearancePreference> {
        Binding(
            get: { store.preferences.appearance },
            set: { store.preferences.appearance = $0 }
        )
    }

    private var reduceMotionBinding: Binding<Bool> {
        Binding(
            get: { store.preferences.reduceDecorativeMotion },
            set: { store.preferences.reduceDecorativeMotion = $0 }
        )
    }
}

private struct MembershipCardPreview: View {
    let profile: MembershipProfile

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("GHOST MEMBERSHIP")
                    .font(.caption2.weight(.black))
                    .tracking(1.1)
                Spacer()
                Image(systemName: "arrow.up.right")
            }
            .foregroundStyle(Color.white.opacity(0.65))
            Text(profile.displayName)
                .font(.title2.weight(.black))
                .foregroundStyle(Color.white)
            Text(profile.ghostID)
                .font(.caption.monospaced().weight(.semibold))
                .foregroundStyle(Color.ghostGreenColor)
            Text("A membership artifact for your Ghost Cart journey. Not a financial product.")
                .font(.caption)
                .foregroundStyle(Color.white.opacity(0.62))
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.inkColor)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct SettingsRow: View {
    let image: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 13) {
            Image(systemName: image)
                .font(.headline)
                .foregroundStyle(Color.ghostGreenColor)
                .frame(width: 42, height: 42)
                .background(Color.ghostGreenColor.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.headline.weight(.bold)).foregroundStyle(Color.primary)
                Text(subtitle).font(.caption).foregroundStyle(Color.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.secondary)
        }
        .padding(15)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct TrustRow: View {
    let image: String
    let text: String

    var body: some View {
        Label {
            Text(text).font(.subheadline).foregroundStyle(Color.secondary)
        } icon: {
            Image(systemName: image).foregroundStyle(Color.ghostGreenColor)
        }
    }
}

struct ReminderSettingsView: View {
    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.openURL) private var openURL
    @State private var authorizationState: NotificationAuthorizationState = .notDetermined
    @State private var feedbackMessage: String?

    var body: some View {
        Form {
            Section {
                HStack {
                    Label("Notification permission", systemImage: authorizationIcon)
                    Spacer()
                    Text(authorizationLabel)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(authorizationState == .authorized ? Color.ghostGreenColor : Color.secondary)
                }
                if authorizationState == .denied {
                    Button("Open iPhone Settings") {
                        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                        openURL(url)
                    }
                } else {
                    Button("Send test notification in 3 seconds") {
                        Task { await sendTestNotification() }
                    }
                }
                if let feedbackMessage {
                    Text(feedbackMessage).font(.caption).foregroundStyle(Color.secondary)
                }
            } header: {
                Text("Permission & test")
            }

            Section {
                Toggle("Cooling complete", isOn: coolingBinding)
            } header: {
                Text("Decision reminders")
            } footer: {
                Text("This reminder opens Cooldowns when an almost-buy is ready for your decision.")
            }

            Section {
                ReminderTimeRow(title: "Lunch", isOn: lunchBinding, hour: lunchHourBinding)
                ReminderTimeRow(title: "Dinner", isOn: dinnerBinding, hour: dinnerHourBinding)
                ReminderTimeRow(title: "Late night", isOn: lateNightBinding, hour: lateNightHourBinding)

                Toggle("Salary-day nudge", isOn: salaryBinding)
                if store.preferences.reminders.salaryDayEnabled {
                    Stepper("Day \(store.preferences.reminders.salaryDay) of each month", value: salaryDayBinding, in: 1...28)
                }
            } header: {
                Text("Optional nudges")
            } footer: {
                Text("Every nudge is off by default and can be controlled independently.")
            }

            Section {
                Toggle("Quiet hours", isOn: quietHoursBinding)
                if store.preferences.reminders.quietHoursEnabled {
                    DatePicker("Start", selection: quietStartBinding, displayedComponents: .hourAndMinute)
                    DatePicker("End", selection: quietEndBinding, displayedComponents: .hourAndMinute)
                    Text("Optional nudges scheduled inside quiet hours are not delivered.")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                }
            } header: {
                Text("Quiet hours")
            }

            Section {
                if let pausedUntil = store.preferences.reminders.pausedUntil, pausedUntil > Date() {
                    Text("Paused until \(pausedUntil.formatted(date: .abbreviated, time: .shortened))")
                    Button("Resume optional nudges") { store.resumeRoutineReminders() }
                } else {
                    Button("Pause optional nudges for 7 days") { store.pauseRoutineReminders(days: 7) }
                }
            }

            Section {
                Text("Simulation only. Reminder text never confirms a real purchase or delivery.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
            }
        }
        .navigationTitle("Reminders")
        .navigationBarTitleDisplayMode(.inline)
        .task { await refreshAuthorizationState() }
        .onChange(of: store.preferences.reminders) { _ in
            Task { await refreshAuthorizationState() }
        }
    }

    private var authorizationLabel: String {
        switch authorizationState {
        case .authorized: return "Allowed"
        case .denied: return "Off in Settings"
        case .notDetermined: return "Not requested"
        }
    }

    private var authorizationIcon: String {
        switch authorizationState {
        case .authorized: return "checkmark.circle.fill"
        case .denied: return "exclamationmark.triangle.fill"
        case .notDetermined: return "bell"
        }
    }

    @MainActor
    private func refreshAuthorizationState() async {
        authorizationState = await NotificationService.shared.authorizationState()
    }

    @MainActor
    private func sendTestNotification() async {
        do {
            try await NotificationService.shared.scheduleTestNotification()
            feedbackMessage = "Test scheduled. Background the app to see the notification banner."
        } catch {
            feedbackMessage = error.localizedDescription
        }
        await refreshAuthorizationState()
    }

    private var coolingBinding: Binding<Bool> {
        boolBinding(\.coolingCompleteEnabled) { _ in
            store.refreshCoolingNotifications()
        }
    }
    private var lunchBinding: Binding<Bool> { boolBinding(\.lunchEnabled) }
    private var dinnerBinding: Binding<Bool> { boolBinding(\.dinnerEnabled) }
    private var lateNightBinding: Binding<Bool> { boolBinding(\.lateNightEnabled) }
    private var salaryBinding: Binding<Bool> { boolBinding(\.salaryDayEnabled) }
    private var quietHoursBinding: Binding<Bool> { boolBinding(\.quietHoursEnabled) }

    private var lunchHourBinding: Binding<Date> { hourBinding(\.lunchHour) }
    private var dinnerHourBinding: Binding<Date> { hourBinding(\.dinnerHour) }
    private var lateNightHourBinding: Binding<Date> { hourBinding(\.lateNightHour) }
    private var quietStartBinding: Binding<Date> { hourBinding(\.quietStartHour) }
    private var quietEndBinding: Binding<Date> { hourBinding(\.quietEndHour) }

    private var salaryDayBinding: Binding<Int> {
        Binding(
            get: { store.preferences.reminders.salaryDay },
            set: { store.preferences.reminders.salaryDay = $0 }
        )
    }

    private func boolBinding(
        _ keyPath: WritableKeyPath<ReminderPreferences, Bool>,
        afterSet: ((Bool) -> Void)? = nil
    ) -> Binding<Bool> {
        Binding(
            get: { store.preferences.reminders[keyPath: keyPath] },
            set: { value in
                store.preferences.reminders[keyPath: keyPath] = value
                afterSet?(value)
            }
        )
    }

    private func hourBinding(_ keyPath: WritableKeyPath<ReminderPreferences, Int>) -> Binding<Date> {
        Binding(
            get: {
                Calendar.current.date(bySettingHour: store.preferences.reminders[keyPath: keyPath], minute: 0, second: 0, of: Date()) ?? Date()
            },
            set: { date in
                store.preferences.reminders[keyPath: keyPath] = Calendar.current.component(.hour, from: date)
            }
        )
    }
}

private struct ReminderTimeRow: View {
    let title: String
    @Binding var isOn: Bool
    @Binding var hour: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle(title, isOn: $isOn)
            if isOn {
                DatePicker("Time", selection: $hour, displayedComponents: .hourAndMinute)
            }
        }
    }
}
