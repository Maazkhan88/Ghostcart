import SwiftUI

struct CaptureView: View {
    @EnvironmentObject private var store: GhostCartStore
    let onComplete: () -> Void

    @State private var name = ""
    @State private var amountText = ""
    @State private var category: AlmostBuyCategory = .other
    @State private var trigger: SpendingTrigger = .boredom
    @State private var source: CaptureSource = .manual
    @State private var sourceURL = ""
    @State private var cooldownMinutes = AlmostBuyCategory.other.recommendedCooldownMinutes
    @State private var showCapturedConfirmation = false

    private var parsedAmount: Double? {
        Double(amountText.replacingOccurrences(of: ",", with: "."))
    }

    private var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && (parsedAmount ?? -1) >= 0
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                ScreenHeader(
                    eyebrow: "Ghost +",
                    title: "Capture an almost-buy",
                    subtitle: "Put the temptation somewhere safe before money leaves your account."
                )

                Picker("Capture source", selection: $source) {
                    ForEach(CaptureSource.allCases) { source in
                        Text(source.title).tag(source)
                    }
                }
                .pickerStyle(.segmented)

                if source == .screenshot {
                    GhostCard {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "photo.badge.plus")
                                .font(.title2)
                                .foregroundStyle(Color.ghostGreenColor)
                            VStack(alignment: .leading, spacing: 5) {
                                Text("Screenshot import is coming next")
                                    .font(.subheadline.weight(.bold))
                                Text("You can still enter the item below. Ghost Cart will not upload or analyze a screenshot in this version.")
                                    .font(.caption)
                                    .foregroundStyle(Color.secondary)
                            }
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 16) {
                    CaptureFieldLabel(title: "What are you tempted by?", required: true)
                    TextField("Example: noise-cancelling headphones", text: $name)
                        .textInputAutocapitalization(.sentences)
                        .ghostTextField()

                    CaptureFieldLabel(title: "Amount", required: true)
                    TextField("0", text: $amountText)
                        .keyboardType(.decimalPad)
                        .ghostTextField()
                    Text("Enter the price in UAE dirhams. It will count as Almost Spent, not Money Kept.")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)

                    if source == .url {
                        CaptureFieldLabel(title: "Product link", required: false)
                        TextField("https://", text: $sourceURL)
                            .keyboardType(.URL)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .ghostTextField()
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    CaptureFieldLabel(title: "Category", required: true)
                    Picker("Category", selection: $category) {
                        ForEach(AlmostBuyCategory.allCases) { option in
                            Label(option.title, systemImage: option.systemImage).tag(option)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.primary.opacity(0.055))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .onChange(of: category) { newCategory in
                        cooldownMinutes = newCategory.recommendedCooldownMinutes
                    }

                    CaptureFieldLabel(title: "What triggered it?", required: true)
                    Picker("Trigger", selection: $trigger) {
                        ForEach(SpendingTrigger.allCases) { option in
                            Text(option.title).tag(option)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.primary.opacity(0.055))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }

                VStack(alignment: .leading, spacing: 12) {
                    CaptureFieldLabel(title: "Cooling period", required: true)
                    Text("Recommended for \(category.title.lowercased()): \(cooldownLabel(category.recommendedCooldownMinutes))")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 112), spacing: 10)], spacing: 10) {
                        ForEach(cooldownOptions, id: \.self) { minutes in
                            Button {
                                cooldownMinutes = minutes
                            } label: {
                                Text(cooldownLabel(minutes))
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(cooldownMinutes == minutes ? Color.inkColor : Color.primary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(cooldownMinutes == minutes ? Color.ghostGreenColor : Color.primary.opacity(0.055))
                                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                VStack(spacing: 11) {
                    Button("Capture and start cooling") { submit(startCooling: true) }
                        .buttonStyle(GhostPrimaryButtonStyle())
                        .disabled(!canSubmit)
                        .opacity(canSubmit ? 1 : 0.45)

                    Button("Save without cooling") { submit(startCooling: false) }
                        .buttonStyle(GhostSecondaryButtonStyle())
                        .disabled(!canSubmit)
                        .opacity(canSubmit ? 1 : 0.45)
                }

                SimulationDisclosure()
            }
            .padding(20)
            .padding(.bottom, 24)
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
        .alert("Almost-buy captured", isPresented: $showCapturedConfirmation) {
            Button("View cooldowns", action: onComplete)
            Button("Capture another", role: .cancel) { resetForm() }
        } message: {
            Text("You can resolve it later as skipped, bought intentionally, or needing more time.")
        }
    }

    private var cooldownOptions: [Int] {
        Array(Set([category.recommendedCooldownMinutes, 30, 24 * 60, 48 * 60, 7 * 24 * 60])).sorted()
    }

    private func cooldownLabel(_ minutes: Int) -> String {
        if minutes < 60 { return "\(minutes) min" }
        if minutes < 24 * 60 { return "\(minutes / 60) hr" }
        return "\(minutes / (24 * 60)) day\(minutes == 24 * 60 ? "" : "s")"
    }

    private func submit(startCooling: Bool) {
        guard let amount = parsedAmount, canSubmit else { return }
        let id = store.capture(
            name: name,
            amount: amount,
            category: category,
            trigger: trigger,
            source: source,
            sourceURL: source == .url && !sourceURL.isEmpty ? sourceURL : nil
        )
        if startCooling {
            store.startCooling(id: id, minutes: cooldownMinutes)
        }
        showCapturedConfirmation = true
    }

    private func resetForm() {
        name = ""
        amountText = ""
        sourceURL = ""
        source = .manual
        category = .other
        trigger = .boredom
        cooldownMinutes = AlmostBuyCategory.other.recommendedCooldownMinutes
    }
}

private struct CaptureFieldLabel: View {
    let title: String
    let required: Bool

    var body: some View {
        HStack(spacing: 4) {
            Text(title).font(.subheadline.weight(.bold))
            if required {
                Text("Required")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Color.secondary)
            }
        }
    }
}

private extension View {
    func ghostTextField() -> some View {
        self
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(Color.primary.opacity(0.055))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color.primary.opacity(0.09), lineWidth: 1)
            }
    }
}
