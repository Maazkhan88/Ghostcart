import UIKit
import UniformTypeIdentifiers

// Share Extension entry point. Ghost Cart registers as a share target for
// public web links (and plain text containing one). It does NOT open a real
// cart, sign in, or buy anything: it only writes the shared link into the
// App Group container so the main app can turn it into a cooling-off capture
// the next time it is opened.
//
// A plain UIViewController rather than SLComposeServiceViewController on
// purpose: the system-provided "Cancel/Post" compose template requires an
// explicit user tap to complete, which put a confusing, unbranded extra
// step (indistinguishable from e.g. X's own "Post" share sheet) between
// sharing and Ghost Cart's own capture screen. Android's equivalent
// (MainActivity.captureSharedProduct, an ACTION_SEND intent filter) has no
// such intermediate step - it resolves the link and hands off immediately.
// This mirrors that: resolve the shared URL and complete automatically,
// with only a brief branded "Saving to Ghost Cart" spinner while that
// resolution (typically well under a second) runs.
final class ShareViewController: UIViewController {
    private static let ghostGreen = UIColor(red: 0.39, green: 0.84, blue: 0.29, alpha: 1)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        setUpSavingUI()

        resolveSharedURL { [weak self] resolvedURL in
            if let resolvedURL {
                SharedImportBridge.save(
                    PendingSharedImport(sourceURL: resolvedURL, sharedTitle: nil, sharedImageURL: nil)
                )
            }
            DispatchQueue.main.async {
                self?.finish()
            }
        }
    }

    // Hands off to Ghost Cart itself via its own custom URL scheme
    // (registered in GhostCart/Info.plist) rather than just completing the
    // extension and leaving the user in whatever app they shared from -
    // Android's equivalent (an ACTION_SEND intent filter) opens the app
    // directly, this is the iOS mechanism for the same result.
    // NSExtensionContext.open(_:completionHandler:) both dismisses the
    // extension and foregrounds the target app in one call when it
    // succeeds; completeRequest is only needed as a fallback if opening the
    // app URL itself fails for some reason.
    private func finish() {
        guard let openURL = URL(string: "ghostcart://share") else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }
        extensionContext?.open(openURL) { [weak self] success in
            guard !success else { return }
            self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
        }
    }

    private func setUpSavingUI() {
        let icon = UILabel()
        icon.text = "👻"
        icon.font = .systemFont(ofSize: 40)
        icon.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = "Saving to Ghost Cart…"
        label.font = .systemFont(ofSize: 15, weight: .semibold)
        label.textColor = .label
        label.translatesAutoresizingMaskIntoConstraints = false

        let spinner = UIActivityIndicatorView(style: .medium)
        spinner.color = Self.ghostGreen
        spinner.startAnimating()
        spinner.translatesAutoresizingMaskIntoConstraints = false

        let stack = UIStackView(arrangedSubviews: [icon, spinner, label])
        stack.axis = .vertical
        stack.alignment = .center
        stack.spacing = 14
        stack.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
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
