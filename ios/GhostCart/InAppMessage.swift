import SwiftUI

// Admin-composed ad-hoc message shown on launch, mirrors Android's
// InAppMessageRepository + AppViewModel.refreshInAppMessages/dismissInAppMessage
// (data/InAppMessageRepository.kt, ui/app/AppViewModel.kt). GET /api/in-app-messages
// is public and Android-only today - this brings iOS to parity. Dismissal is
// remembered locally per-install (UserDefaults), same as Android's SharedPreferences
// "dismissed_in_app_message_ids", so a message isn't shown again once seen.
struct InAppMessage: Identifiable, Equatable {
    let id: String
    let title: String
    let body: String
    let imageUrl: String?
    let linkUrl: String?
}

enum InAppMessageService {
    private static let dismissedKey = "ghostcart.dismissed-in-app-message-ids"

    static func fetchActive() async -> [InAppMessage] {
        guard let object = try? await ApiClient.shared.getJSON(path: "/api/in-app-messages"),
              let rows = object["messages"] as? [[String: Any]] else {
            return []
        }
        return rows.compactMap { row -> InAppMessage? in
            guard let id = idString(row["id"]),
                  let title = (row["title"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !title.isEmpty,
                  let body = (row["body"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !body.isEmpty
            else { return nil }
            return InAppMessage(
                id: id,
                title: title,
                body: body,
                imageUrl: nonEmptyString(row["imageUrl"]),
                linkUrl: nonEmptyString(row["linkUrl"])
            )
        }
    }

    /// The first active message not already dismissed on this device.
    static func nextToShow() async -> InAppMessage? {
        let dismissed = dismissedIds()
        let messages = await fetchActive()
        return messages.first { !dismissed.contains($0.id) }
    }

    static func dismiss(_ id: String) {
        var ids = dismissedIds()
        ids.insert(id)
        UserDefaults.standard.set(Array(ids), forKey: dismissedKey)
    }

    private static func dismissedIds() -> Set<String> {
        Set(UserDefaults.standard.stringArray(forKey: dismissedKey) ?? [])
    }

    private static func idString(_ value: Any?) -> String? {
        if let intValue = value as? Int { return String(intValue) }
        if let stringValue = value as? String, !stringValue.isEmpty { return stringValue }
        return nil
    }

    private static func nonEmptyString(_ value: Any?) -> String? {
        guard let stringValue = value as? String else { return nil }
        let trimmed = stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty || trimmed == "null" ? nil : trimmed
    }
}

struct InAppMessageAlertView: View {
    let message: InAppMessage
    let onDismiss: () -> Void
    let onOpenLink: (URL) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if let imageUrl = message.imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    if case .success(let image) = phase {
                        image.resizable().scaledToFill()
                    } else {
                        Color.primary.opacity(0.05)
                    }
                }
                .frame(height: 140)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .clipped()
            }
            Text(message.title).font(.headline.weight(.black))
            Text(message.body).font(.subheadline).foregroundStyle(Color.secondary)

            HStack {
                if let linkUrlString = message.linkUrl, let url = URL(string: linkUrlString) {
                    Button("Dismiss", action: onDismiss)
                        .buttonStyle(GhostSecondaryButtonStyle())
                    Button("View") { onOpenLink(url) }
                        .buttonStyle(GhostPrimaryButtonStyle())
                } else {
                    Button("OK", action: onDismiss)
                        .buttonStyle(GhostPrimaryButtonStyle())
                }
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .padding(28)
    }
}
