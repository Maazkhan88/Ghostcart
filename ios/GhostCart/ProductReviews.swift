import SwiftUI

// Cross-device reviews via GET/POST/DELETE /api/products/:id/reviews(+/:reviewId)
// when signed in - the backend + table didn't exist before this pass (see
// docs/claude-ios-ghost-delivery-parity.md's known-gaps list). Guests keep
// the original on-device-only behavior (ProductReviewStore, unchanged)
// since posting requires an account server-side and the rest of the app
// already draws that same guest/signed-in line for account-gated features
// (e.g. favorites).
struct ProductReview: Identifiable, Codable, Equatable {
    let id: String
    var productID: String
    var rating: Int
    var text: String
    var createdAt: Date
    var authorName: String?
    var isOwn: Bool

    init(id: String = UUID().uuidString, productID: String, rating: Int, text: String, createdAt: Date = Date(), authorName: String? = nil, isOwn: Bool = true) {
        self.id = id
        self.productID = productID
        self.rating = rating
        self.text = text
        self.createdAt = createdAt
        self.authorName = authorName
        self.isOwn = isOwn
    }
}

enum ProductReviewStore {
    private static let key = "ghostcart.v2.local-product-reviews"

    static func all() -> [ProductReview] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([ProductReview].self, from: data) else { return [] }
        return decoded
    }

    static func reviews(for productID: String) -> [ProductReview] {
        all().filter { $0.productID == productID }.sorted { $0.createdAt > $1.createdAt }
    }

    static func add(_ review: ProductReview) {
        var current = all()
        current.append(review)
        save(current)
    }

    static func delete(id: String) {
        save(all().filter { $0.id != id })
    }

    private static func save(_ reviews: [ProductReview]) {
        guard let data = try? JSONEncoder().encode(reviews) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

enum ProductReviewService {
    private static let iso: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static func fetchReviews(productID: String) async -> [ProductReview]? {
        // Called only when signed in (see ProductReviewsSheet.loadReviews) -
        // authorizedGet attaches the bearer token so the backend can mark
        // isOwn on the caller's own reviews; the route itself is public
        // (works signed-out too, just without isOwn) if this is ever called
        // from a guest context in the future.
        guard let response = try? await AuthService.shared.authorizedGet(path: "/api/products/\(productID)/reviews"),
              let rows = response["reviews"] as? [[String: Any]] else { return nil }
        return rows.compactMap { row -> ProductReview? in
            guard let id = row["id"] as? String,
                  let rating = row["rating"] as? Int,
                  let text = row["text"] as? String else { return nil }
            let createdAtString = row["createdAt"] as? String ?? ""
            let createdAt = iso.date(from: createdAtString)
                ?? ISO8601DateFormatter().date(from: createdAtString)
                ?? sqliteDateFormatter.date(from: createdAtString)
                ?? Date()
            return ProductReview(
                id: id,
                productID: productID,
                rating: rating,
                text: text,
                createdAt: createdAt,
                authorName: row["authorName"] as? String,
                isOwn: (row["isOwn"] as? Bool) ?? false
            )
        }
    }

    @discardableResult
    static func postReview(productID: String, rating: Int, text: String) async -> Bool {
        (try? await AuthService.shared.authorizedPost(
            path: "/api/products/\(productID)/reviews",
            body: ["rating": rating, "text": text]
        )) != nil
    }

    @discardableResult
    static func deleteReview(productID: String, reviewID: String) async -> Bool {
        (try? await AuthService.shared.authorizedDelete(
            path: "/api/products/\(productID)/reviews/\(reviewID)"
        )) != nil
    }

    private static let sqliteDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter
    }()
}

// The "rating/review/comment action" required on every marketplace card.
// Never fabricates content - shows a real empty state when there's nothing
// to show yet, per spec ("No reviews yet" rather than fake content).
struct ProductReviewsSheet: View {
    let product: MarketplaceProduct
    @EnvironmentObject private var auth: AuthService
    @Environment(\.dismiss) private var dismiss

    @State private var reviews: [ProductReview] = []
    @State private var draftRating = 5
    @State private var draftText = ""
    @State private var deleteTarget: ProductReview?
    @State private var isPosting = false
    @State private var isLoading = false

    private var isSignedIn: Bool { auth.isSignedIn }

    private var averageRating: Double? {
        guard !reviews.isEmpty else { return nil }
        return Double(reviews.reduce(0) { $0 + $1.rating }) / Double(reviews.count)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(product.name).font(.headline.weight(.bold))
                        if let averageRating {
                            HStack(spacing: 6) {
                                Image(systemName: "star.fill").foregroundStyle(Color.ghostGreenColor)
                                Text(String(format: "%.1f", averageRating))
                                    .font(.subheadline.weight(.bold))
                                Text("(\(reviews.count) review\(reviews.count == 1 ? "" : "s"))")
                                    .font(.caption)
                                    .foregroundStyle(Color.secondary)
                            }
                        } else {
                            Text("No reviews yet").font(.subheadline).foregroundStyle(Color.secondary)
                        }
                        Text(isSignedIn ? "Reviews are visible to everyone on Ghost Cart." : "Sign in to post a review others can see. Showing this device's local reviews only.")
                            .font(.caption2)
                            .foregroundStyle(Color.secondary)
                    }

                    GhostCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Add your review").font(.subheadline.weight(.bold))
                            HStack(spacing: 4) {
                                ForEach(1...5, id: \.self) { star in
                                    Button {
                                        draftRating = star
                                    } label: {
                                        Image(systemName: star <= draftRating ? "star.fill" : "star")
                                            .foregroundStyle(Color.ghostGreenColor)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .accessibilityElement(children: .ignore)
                            .accessibilityLabel("Rating")
                            .accessibilityValue("\(draftRating) out of 5 stars")
                            .accessibilityAdjustableAction { direction in
                                switch direction {
                                case .increment: draftRating = min(5, draftRating + 1)
                                case .decrement: draftRating = max(1, draftRating - 1)
                                @unknown default: break
                                }
                            }
                            TextField("What did you think?", text: $draftText, axis: .vertical)
                                .lineLimit(3...5)
                                .ghostTextField()
                            Button(isPosting ? "Posting..." : "Post review") { Task { await addReview() } }
                                .buttonStyle(GhostPrimaryButtonStyle())
                                .disabled(draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isPosting)
                        }
                    }

                    if !reviews.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Reviews").font(.subheadline.weight(.bold))
                            ForEach(reviews) { review in
                                reviewRow(review)
                            }
                        }
                    }
                }
                .padding(20)
                .padding(.bottom, 24)
            }
            .navigationTitle("Reviews & comments")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                await loadReviews()
                GhostAnalytics.productReviewsOpened(product.id)
            }
            .confirmationDialog("Delete this review?", isPresented: Binding(
                get: { deleteTarget != nil },
                set: { if !$0 { deleteTarget = nil } }
            ), titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    if let deleteTarget { Task { await removeReview(deleteTarget) } }
                    deleteTarget = nil
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private func loadReviews() async {
        if isSignedIn {
            isLoading = true
            reviews = await ProductReviewService.fetchReviews(productID: product.id) ?? []
            isLoading = false
        } else {
            reviews = ProductReviewStore.reviews(for: product.id)
        }
    }

    private func reviewRow(_ review: ProductReview) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                HStack(spacing: 2) {
                    ForEach(1...5, id: \.self) { star in
                        Image(systemName: star <= review.rating ? "star.fill" : "star")
                            .font(.caption2)
                            .foregroundStyle(Color.ghostGreenColor)
                    }
                }
                Spacer()
                Text(review.createdAt.formatted(date: .abbreviated, time: .omitted))
                    .font(.caption2)
                    .foregroundStyle(Color.secondary)
            }
            if let authorName = review.authorName, !review.isOwn {
                Text(authorName).font(.caption2.weight(.bold)).foregroundStyle(Color.secondary)
            }
            Text(review.text).font(.caption)
            if review.isOwn {
                Button("Delete", role: .destructive) { deleteTarget = review }
                    .font(.caption2.weight(.bold))
            }
        }
        .padding(12)
        .background(Color.primary.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    private func addReview() async {
        let trimmed = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        if isSignedIn {
            isPosting = true
            let ok = await ProductReviewService.postReview(productID: product.id, rating: draftRating, text: trimmed)
            isPosting = false
            guard ok else { return }
            await loadReviews()
        } else {
            ProductReviewStore.add(ProductReview(productID: product.id, rating: draftRating, text: trimmed))
            reviews = ProductReviewStore.reviews(for: product.id)
        }
        draftText = ""
        draftRating = 5
    }

    private func removeReview(_ review: ProductReview) async {
        if isSignedIn {
            guard await ProductReviewService.deleteReview(productID: product.id, reviewID: review.id) else { return }
            await loadReviews()
        } else {
            ProductReviewStore.delete(id: review.id)
            reviews = ProductReviewStore.reviews(for: product.id)
        }
    }
}
