import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var store: GhostCartStore
    @State private var showMembership = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                ScreenHeader(
                    eyebrow: "Your controls",
                    title: "Profile",
                    subtitle: "Choose how Ghost Cart supports you. Promotional nudges remain separate and opt-in."
                )

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
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .sheet(isPresented: $showMembership) {
            NavigationStack {
                MembershipCardEditorView()
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

    var body: some View {
        Form {
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
