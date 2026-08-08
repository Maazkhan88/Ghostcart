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

    // Link-import state
    @State private var importState: ProductImportState = .idle
    @State private var importedImageURL: String?
    @State private var importedSourceDomain: String?
    @State private var importedRetailer: String?
    @State private var shareWithCommunity = false

    private var parsedAmount: Double? {
        Double(amountText.replacingOccurrences(of: ",", with: "."))
    }

    private var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && (parsedAmount ?? -1) >= 0
    }

    private var trimmedSourceURL: String {
        sourceURL.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var hasShareableLink: Bool {
        source == .url && isSafePublicHttpsURL(trimmedSourceURL)
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

                if source == .url {
                    linkImportSection
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
                    CaptureFieldLabel(title: "Ghost Delivery time", required: true)
                    Text("Your Ghost Delivery time is your cooling period. Nothing will be purchased or physically delivered.")
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

                if hasShareableLink {
                    communityShareToggle
                }

                VStack(spacing: 11) {
                    Button("Ghost it") { submit(startCooling: true) }
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
        .onAppear { consumeSeedIfNeeded() }
        .onChange(of: store.captureSeed) { _ in consumeSeedIfNeeded() }
        .alert("Almost-buy captured", isPresented: $showCapturedConfirmation) {
            Button("View cooldowns", action: onComplete)
            Button("Capture another", role: .cancel) { resetForm() }
        } message: {
            Text("You can resolve it later as skipped, bought intentionally, or needing more time.")
        }
    }

    // MARK: - Link import UI

    private var linkImportSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            CaptureFieldLabel(title: "Product link", required: false)
            TextField("https://", text: $sourceURL)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .ghostTextField()
                .onChange(of: sourceURL) { _ in
                    if case .error = importState { importState = .idle }
                }

            Button {
                previewLink()
            } label: {
                if case .loading = importState {
                    HStack(spacing: 8) {
                        SwiftUI.ProgressView().tint(Color.primary)
                        Text("Reading link...")
                    }
                } else {
                    Label("Fill details from link", systemImage: "sparkles")
                }
            }
            .buttonStyle(GhostSecondaryButtonStyle())
            .disabled(!isSafePublicHttpsURL(trimmedSourceURL) || isLoadingImport)

            Text("Ghost Cart reads only public product pages. It never signs in, buys, or opens a real cart.")
                .font(.caption)
                .foregroundStyle(Color.secondary)

            importResultView
        }
    }

    private var isLoadingImport: Bool {
        if case .loading = importState { return true }
        return false
    }

    @ViewBuilder
    private var importResultView: some View {
        switch importState {
        case .idle, .loading:
            EmptyView()
        case .ready(let product):
            GhostCard {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: AlmostBuyCategory(serverName: product.category).systemImage)
                        .font(.title3)
                        .foregroundStyle(Color.ghostGreenColor)
                        .frame(width: 34)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(readabilityTitle(for: product))
                            .font(.subheadline.weight(.bold))
                        Text(captureStatusMessage(product.status))
                            .font(.caption)
                            .foregroundStyle(Color.secondary)
                        if let retailer = importedRetailer, !retailer.isEmpty {
                            Text(retailer)
                                .font(.caption2)
                                .foregroundStyle(Color.secondary)
                        }
                    }
                    Spacer(minLength: 0)
                }
            }
        case .listingDetected(_, let retailer, let items):
            VStack(alignment: .leading, spacing: 8) {
                Text("This looks like a \(retailer.isEmpty ? "collection" : retailer) page. Pick the item you almost bought:")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                ForEach(items.prefix(6)) { item in
                    Button {
                        applyListingStub(item)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: AlmostBuyCategory(serverName: item.category).systemImage)
                                .foregroundStyle(Color.ghostGreenColor)
                                .frame(width: 30)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title).font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Color.primary)
                                if let cents = item.priceCents {
                                    Text(AmountFormatter.string(Double(cents) / 100) + " UAE dirham")
                                        .font(.caption2)
                                        .foregroundStyle(Color.secondary)
                                }
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color.secondary)
                        }
                        .padding(12)
                        .background(Color.primary.opacity(0.04))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        case .error(let message):
            Text(message)
                .font(.caption)
                .foregroundStyle(Color.red)
        }
    }

    private var communityShareToggle: some View {
        GhostCard {
            Toggle(isOn: $shareWithCommunity) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Share anonymously to the community shelf")
                        .font(.subheadline.weight(.bold))
                    Text("Adds a privacy-safe \"User Ghosted\" card. No account, name, or identity is attached, and it never changes your Money Kept.")
                        .font(.caption)
                        .foregroundStyle(Color.secondary)
                }
            }
            .tint(Color.ghostGreenColor)
        }
    }

    private func captureStatusMessage(_ status: String) -> String {
        switch status {
        case "complete": return "Title, price, and category filled from the link. Edit anything before ghosting."
        case "partial": return "Some details were read. Check the title and price before ghosting."
        default: return "Ghost Cart could not read everything. Enter the missing details manually."
        }
    }

    private func readabilityTitle(for product: ImportedProduct) -> String {
        titleLooksLikeFallback(product.title) ? (name.isEmpty ? product.title : name) : product.title
    }

    // MARK: - Actions

    private func previewLink() {
        let trimmed = trimmedSourceURL
        guard isSafePublicHttpsURL(trimmed) else {
            importState = .error("Enter a public https product link.")
            return
        }
        importState = .loading
        Task {
            let result = await ProductImportService.previewLink(sourceURL: trimmed)
            await MainActor.run { handlePreview(result) }
        }
    }

    private func handlePreview(_ result: Result<LinkImportResult, ApiError>) {
        switch result {
        case .success(.product(let product)):
            applyImportedProduct(product)
            importState = .ready(product)
        case .success(.listing(let domain, let retailer, let items)):
            importState = .listingDetected(sourceDomain: domain, retailer: retailer, items: items)
        case .failure(let error):
            importState = .error(error.message)
        }
    }

    private func applyImportedProduct(_ product: ImportedProduct) {
        if !titleLooksLikeFallback(product.title) {
            name = product.title
        }
        if let amount = product.amount {
            amountText = amountString(amount)
        }
        category = AlmostBuyCategory(serverName: product.category)
        cooldownMinutes = category.recommendedCooldownMinutes
        importedImageURL = product.imageURL
        importedSourceDomain = product.sourceDomain
        importedRetailer = product.retailer
        if !product.sourceURL.isEmpty {
            sourceURL = product.sourceURL
        }
    }

    private func applyListingStub(_ stub: ListingProductStub) {
        name = stub.title
        if let cents = stub.priceCents {
            amountText = amountString(Double(cents) / 100)
        }
        category = AlmostBuyCategory(serverName: stub.category)
        cooldownMinutes = category.recommendedCooldownMinutes
        importedImageURL = stub.imageURL
        importedSourceDomain = stub.sourceDomain
        importedRetailer = stub.retailer
        sourceURL = stub.sourceURL
        importState = .ready(ImportedProduct(
            title: stub.title,
            priceCents: stub.priceCents,
            currencyCode: nil,
            category: stub.category,
            imageURL: stub.imageURL,
            sourceURL: stub.sourceURL,
            sourceDomain: stub.sourceDomain,
            retailer: stub.retailer,
            status: stub.priceCents != nil ? "partial" : "needs_input",
            note: nil
        ))
    }

    private func consumeSeedIfNeeded() {
        guard let seed = store.consumeCaptureSeed() else { return }
        name = seed.name
        if let amount = seed.amount {
            amountText = amountString(amount)
        }
        category = seed.category
        cooldownMinutes = seed.category.recommendedCooldownMinutes
        importedImageURL = seed.imageURL
        importedSourceDomain = seed.sourceDomain
        importedRetailer = seed.retailer
        // Defaults on when this capture was seeded from a link the user
        // already shared into Ghost Cart (they already made the product
        // public by sharing it externally, so the in-app community toggle
        // matching that is reasonable) - matches Android's
        // `mutableStateOf(seed?.sourceUrl != null)` in GhostCartV2Screens.kt.
        // Manually-typed captures (no seed, or a seed with no source URL)
        // keep defaulting off; the user can still uncheck this before
        // confirming either way - the consent gate itself is unchanged.
        let hasSharedLink = seed.sourceURL?.isEmpty == false
        shareWithCommunity = hasSharedLink
        if let url = seed.sourceURL, !url.isEmpty {
            source = .url
            sourceURL = url
        }
        importState = .idle
    }

    private func amountString(_ amount: Double) -> String {
        amount == amount.rounded() ? String(Int(amount)) : String(amount)
    }

    // Category-sensitive Ghost Delivery time presets: food leads with short
    // durations (15/30 min), everything else leads with longer ones.
    private var cooldownOptions: [Int] {
        let presetMinutes = GhostDeliveryDuration.presets(for: category).compactMap { $0.totalMinutes }
        return Array(Set(presetMinutes + [category.recommendedCooldownMinutes])).sorted()
    }

    private func cooldownLabel(_ minutes: Int) -> String {
        if minutes < 60 { return "\(minutes) min" }
        if minutes < 24 * 60 { return "\(minutes / 60) hr" }
        return "\(minutes / (24 * 60)) day\(minutes == 24 * 60 ? "" : "s")"
    }

    private func submit(startCooling: Bool) {
        guard let amount = parsedAmount, canSubmit else { return }
        GhostAnalytics.ghostItTapped(category: category.rawValue)
        if startCooling { GhostAnalytics.deliveryDurationSelected(minutes: cooldownMinutes) }
        let capturedURL = source == .url && !trimmedSourceURL.isEmpty ? trimmedSourceURL : nil
        let id = store.capture(
            name: name,
            amount: amount,
            category: category,
            trigger: trigger,
            source: source,
            sourceURL: capturedURL,
            imageURL: importedImageURL
        )
        if startCooling {
            store.startCooling(id: id, minutes: cooldownMinutes)
        }
        if shareWithCommunity, let capturedURL {
            let publishName = name
            let publishCategory = category
            let publishImage = importedImageURL
            Task {
                // Consent-gated, best-effort, and independent of Money Kept.
                await ProductImportService.publish(
                    title: publishName,
                    category: publishCategory,
                    amount: amount,
                    sourceURL: capturedURL,
                    imageURL: publishImage
                )
            }
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
        importState = .idle
        importedImageURL = nil
        importedSourceDomain = nil
        importedRetailer = nil
        shareWithCommunity = false
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

extension View {
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
