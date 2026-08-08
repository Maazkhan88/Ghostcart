import Foundation

enum MiniCaptureStage: Equatable {
    case resolving
    case ready
    case signedOut
    case error(String)
    case saving
    case saved
}

// Drives the extension's mini Product Capture UI. Reuses the same
// ProductImportService/ApiClient the main app's CaptureView uses (shared
// source files, see GhostCartShare's Compile Sources) - this is
// deliberately not a second, parallel product-import implementation.
@MainActor
final class MiniCaptureModel: ObservableObject {
    @Published private(set) var stage: MiniCaptureStage = .resolving
    @Published private(set) var product: ImportedProduct?
    @Published var manualAmountText: String = ""
    @Published var cooldownMinutes: Int = AlmostBuyCategory.other.recommendedCooldownMinutes

    private var resolvedSourceURL: String?
    private var token: String?

    var category: AlmostBuyCategory {
        AlmostBuyCategory(serverName: product?.category)
    }

    var displayTitle: String {
        guard let product else { return "" }
        return titleLooksLikeFallback(product.title) ? "Shared product" : product.title
    }

    private var resolvedAmount: Double? {
        product?.amount ?? Double(manualAmountText.replacingOccurrences(of: ",", with: "."))
    }

    var canConfirm: Bool {
        guard case .ready = stage else { return false }
        return (resolvedAmount ?? -1) >= 0 && !displayTitle.isEmpty
    }

    /// Called once the share extension has extracted a URL from the shared
    /// item (or nil if none was found in the share payload at all).
    func start(sourceURL: String?) async {
        guard let sourceURL else {
            stage = .error("Ghost Cart couldn't find a product link in this share.")
            return
        }
        resolvedSourceURL = sourceURL
        token = ExtensionAuth.readToken()
        guard token != nil else {
            stage = .signedOut
            return
        }

        let result = await ProductImportService.previewLink(sourceURL: sourceURL)
        switch result {
        case .success(.product(let imported)):
            product = imported
            cooldownMinutes = AlmostBuyCategory(serverName: imported.category).recommendedCooldownMinutes
            stage = .ready
        case .success(.listing):
            // A listing needs the full item-picker CaptureView already has
            // (ProductListingPickerView-equivalent) - out of scope for a
            // "keep it extremely simple and fast" extension UI. Fall back
            // to the existing safe pending-import path instead of building
            // a second listing picker here.
            saveForLater()
            stage = .error("This page has several items - saved the link. Pick the right one in Ghost Cart.")
        case .failure(let error):
            stage = .error(error.message)
        }
    }

    /// The signed-out and extraction-failure fallback: identical to what
    /// ContentView.handleSharedImport already consumes today, unchanged.
    func saveForLater() {
        guard let resolvedSourceURL else { return }
        SharedImportBridge.save(PendingSharedImport(sourceURL: resolvedSourceURL, sharedTitle: nil, sharedImageURL: nil))
    }

    /// Creates a real, permanent, server-side almost-buy directly - no
    /// local write, no need to foreground the main app for this to "count".
    /// Falls back to the pending-import path (never silently loses the
    /// share) if the direct save fails for any reason.
    func confirmGhostIt() async {
        guard canConfirm, let token, let amount = resolvedAmount else { return }
        stage = .saving
        let ok = await ExtensionAlmostBuyService.create(
            title: displayTitle,
            category: category,
            amount: amount,
            sourceURL: resolvedSourceURL,
            imageURL: product?.imageURL,
            cooldownMinutes: cooldownMinutes,
            bearerToken: token
        )
        if ok {
            stage = .saved
        } else {
            saveForLater()
            stage = .error("Couldn't save directly - saved the link instead. Finish it in Ghost Cart.")
        }
    }
}
