import UIKit
import SwiftUI
import UniformTypeIdentifiers

// Share Extension entry point. Hosts a "mini Ghost Cart" - a rich,
// WhatsApp-style capture experience directly inside the extension, instead
// of either the plain system "Cancel/Post" compose template or (previously
// tried and reverted) attempting to auto-foreground the main app, which
// iOS does not reliably support for share extensions and which risks App
// Review rejection via undocumented private-API workarounds.
//
// Signed-in + successful extraction: saves a real, permanent, server-side
// almost-buy directly (ExtensionAlmostBuyService) - no need to open the
// main app at all for it to "count". Signed-out, extraction failure, or a
// multi-item listing page: falls back to the existing pending-import App
// Group mechanism (SharedImportBridge) unchanged - the main app picks it
// up next time it's opened, exactly as it did before this change.
final class ShareViewController: UIViewController {
    private let model = MiniCaptureModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        let hosting = UIHostingController(rootView: MiniCaptureView(model: model, onDone: { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
        }))
        addChild(hosting)
        view.addSubview(hosting.view)
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            hosting.view.topAnchor.constraint(equalTo: view.topAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            hosting.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])
        hosting.didMove(toParent: self)

        resolveSharedURL { [weak self] resolvedURL in
            Task { @MainActor in
                await self?.model.start(sourceURL: resolvedURL)
            }
        }
    }

    // MARK: - URL extraction

    private func resolveSharedURL(completion: @escaping (String?) -> Void) {
        let providers = (extensionContext?.inputItems as? [NSExtensionItem])?
            .flatMap { $0.attachments ?? [] } ?? []
        findURL(in: providers, index: 0, completion: completion)
    }

    private func findURL(in providers: [NSItemProvider], index: Int, completion: @escaping (String?) -> Void) {
        guard index < providers.count else {
            completion(nil)
            return
        }
        let provider = providers[index]
        let advance = { [weak self] in self?.findURL(in: providers, index: index + 1, completion: completion) }

        if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { item, _ in
                if let url = item as? URL, isShareableHttpsURL(url.absoluteString) {
                    completion(url.absoluteString)
                } else {
                    advance()
                }
            }
        } else if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { item, _ in
                if let text = item as? String, let url = firstHttpsURL(in: text) {
                    completion(url)
                } else {
                    advance()
                }
            }
        } else {
            advance()
        }
    }
}

// Lightweight, self-contained validation so the extension does not need to
// pull in the app's networking layer. The app re-validates every link with
// the full safety rules (and the server validates again) before previewing.
private func isShareableHttpsURL(_ value: String) -> Bool {
    guard let url = URL(string: value.trimmingCharacters(in: .whitespacesAndNewlines)),
          url.scheme?.lowercased() == "https",
          let host = url.host, host.contains(".")
    else { return false }
    return true
}

private func firstHttpsURL(in text: String) -> String? {
    guard let regex = try? NSRegularExpression(pattern: #"https://[^\s]+"#, options: [.caseInsensitive]) else {
        return nil
    }
    let range = NSRange(text.startIndex..., in: text)
    for match in regex.matches(in: text, options: [], range: range) {
        guard let matchRange = Range(match.range, in: text) else { continue }
        let candidate = String(text[matchRange])
            .trimmingCharacters(in: CharacterSet(charactersIn: ".,)]}>\""))
        if isShareableHttpsURL(candidate) { return candidate }
    }
    return nil
}
