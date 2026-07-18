import UIKit
import Social
import UniformTypeIdentifiers

// Share Extension entry point. Ghost Cart registers as a share target for
// public web links (and plain text containing one). It does NOT open a real
// cart, sign in, or buy anything: it only writes the shared link into the
// App Group container so the main app can turn it into a cooling-off capture
// the next time it is opened.
final class ShareViewController: SLComposeServiceViewController {
    override func isContentValid() -> Bool { true }

    override func configurationItems() -> [Any]! { [] }

    override func presentationAnimationDidFinish() {
        placeholder = "Add a note (optional)"
        textView.text = ""
    }

    override func didSelectPost() {
        let caption = contentText?.trimmingCharacters(in: .whitespacesAndNewlines)
        resolveSharedURL { [weak self] resolvedURL in
            if let resolvedURL {
                SharedImportBridge.save(
                    PendingSharedImport(
                        sourceURL: resolvedURL,
                        sharedTitle: (caption?.isEmpty == false) ? caption : nil,
                        sharedImageURL: nil
                    )
                )
            }
            self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
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
