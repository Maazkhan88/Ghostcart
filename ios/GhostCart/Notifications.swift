import SwiftUI

// The backend-synced notifications feed shown from the Home header's bell
// icon. Mirrors Android's NotificationsRepository/NotificationsScreen
// against the same shared backend (db/schema.ts's `notifications` table,
// GET/POST /api/me/notifications) - no separate iOS backend work, per the
// 2026-08-08 handoff. There is deliberately no server-side read-state:
// unread/read is derived purely from a locally stored "last seen" cursor
// (the newest createdAt seen), same as Android.
enum NotificationKind: String, Codable {
    case delivery
    case gift
    case announcement

    var systemImage: String {
        switch self {
        case .delivery: return "shippingbox.fill"
        case .gift: return "gift.fill"
        case .announcement: return "megaphone.fill"
        }
    }
}

struct NotificationItem: Identifiable, Equatable {
    let id: String
    let kind: NotificationKind
    let title: String
    let body: String
    let link: String?
    let createdAt: Date
}

enum NotificationsService {
    private static let sqliteDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter
    }()

    private static func parseDate(_ string: String) -> Date {
        ISO8601DateFormatter().date(from: string)
            ?? sqliteDateFormatter.date(from: string)
            ?? Date()
    }

    static func fetch() async -> [NotificationItem]? {
        guard let response = try? await AuthService.shared.authorizedGet(path: "/api/me/notifications"),
              let rows = response["notifications"] as? [[String: Any]] else { return nil }
        return rows.compactMap { row -> NotificationItem? in
            guard let id = row["id"] as? String,
                  let rawType = row["type"] as? String,
                  let kind = NotificationKind(rawValue: rawType),
                  let title = row["title"] as? String,
                  let body = row["body"] as? String else { return nil }
            return NotificationItem(
                id: id,
                kind: kind,
                title: title,
                body: body,
                link: row["link"] as? String,
                createdAt: parseDate(row["createdAt"] as? String ?? "")
            )
        }
    }

    /// Mirrors a Ghost Delivery stage update into the feed right after the
    /// local notification for it fires. Best-effort, fire-and-forget - same
    /// "never blocks, silently no-ops when signed out" philosophy as
    /// AlmostBuySyncService. The backend only accepts `type: "delivery"`
    /// from clients; gift/announcement rows are always server-authored.
    static func postDeliveryUpdate(title: String, body: String, link: String? = nil) {
        Task {
            _ = try? await AuthService.shared.authorizedPost(
                path: "/api/me/notifications",
                body: {
                    var payload: [String: Any] = ["type": "delivery", "title": title, "body": body]
                    if let link { payload["link"] = link }
                    return payload
                }()
            )
        }
    }
}

// The "last seen" unread cursor - purely local, never synced. Matches
// Android's equivalent local-only read tracking (no server read-state
// column exists to sync against).
enum NotificationsSeenCursor {
    private static let key = "ghostcart.notifications.last-seen-created-at"

    static func lastSeen() -> Date? {
        UserDefaults.standard.object(forKey: key) as? Date
    }

    static func markSeen(through createdAt: Date) {
        UserDefaults.standard.set(createdAt, forKey: key)
    }

    /// True when `newest` (the feed's first/newest row - the backend
    /// already returns newest-first) is newer than the locally stored
    /// cursor, or no cursor exists yet and there's at least one item.
    static func hasUnread(newest: Date?) -> Bool {
        guard let newest else { return false }
        guard let seen = lastSeen() else { return true }
        return newest > seen
    }
}

struct NotificationsView: View {
    @State private var items: [NotificationItem] = []
    @State private var loading = true
    let onMarkedSeen: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 10) {
                if loading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 80)
                } else if items.isEmpty {
                    EmptyStateView(
                        image: "bell",
                        title: "No notifications yet",
                        message: "Ghost Delivery updates, gifts, and announcements will show up here."
                    )
                    .padding(.top, 40)
                } else {
                    ForEach(items) { item in
                        NotificationRow(item: item)
                    }
                }
            }
            .padding(20)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .refreshable { await load() }
        .onDisappear {
            guard let newest = items.first?.createdAt else { return }
            NotificationsSeenCursor.markSeen(through: newest)
            onMarkedSeen()
        }
    }

    private func load() async {
        loading = true
        if let fetched = await NotificationsService.fetch() {
            items = fetched
        }
        loading = false
    }
}

private struct NotificationRow: View {
    let item: NotificationItem

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: item.kind.systemImage)
                .font(.headline)
                .foregroundStyle(Color.ghostGreenColor)
                .frame(width: 38, height: 38)
                .background(Color.primary.opacity(0.05))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title).font(.subheadline.weight(.bold))
                Text(item.body).font(.caption).foregroundStyle(Color.secondary)
                Text(item.createdAt.compactDayAndTime)
                    .font(.caption2)
                    .foregroundStyle(Color.secondary.opacity(0.7))
            }
            Spacer()
        }
        .padding(14)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}
