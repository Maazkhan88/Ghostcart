import SwiftUI

// Local-only reviews/comments. There is no backend reviews table or API
// route yet (confirmed by inspection: no /api/*review*/*comment* endpoint
// exists anywhere in this repo). Rather than fabricate content or invent
// unreviewed backend schema/endpoints on the fly, this stores each device's
// own reviews locally, per product, and is honest about that scope in its
// own UI copy. Real cross-device reviews need the backend work documented
// in the project's build summary (see: PENDING_BACKEND_REVIEWS.md-equivalent
// notes in the final implementation summary) before this can sync.
struct ProductReview: Identifiable, Codable, Equatable {
    let id: UUID
    var productID: String
    var rating: Int
    var text: String
    var createdAt: Date
    var isOwn: Bool

    init(id: UUID = UUID(), productID: String, rating: Int, text: String, createdAt: Date = Date(), isOwn: Bool = true) {
        self.id = id
        self.productID = productID
        self.rating = rating
        self.text = text
        self.createdAt = createdAt
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

    static func delete(id: UUID) {
        save(all().filter { $0.id != id })
    }

    private static func save(_ reviews: [ProductReview]) {
        guard let data = try? JSONEncoder().encode(reviews) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

// The "rating/review/comment action" required on every marketplace card.
// Never fabricates content - shows a real empty state when there's nothing
// to show yet, per spec ("No reviews yet" rather than fake content).
struct ProductReviewsSheet: View {
    let product: MarketplaceProduct
    @Environment(\.dismiss) private var dismiss

    @State private var reviews: [ProductReview] = []
    @State private var draftRating = 5
    @State private var draftText = ""
    @State private var deleteTarget: ProductReview?

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
                        Text("Reviews are stored on this device only.")
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
                            Button("Post review") { addReview() }
                                .buttonStyle(GhostPrimaryButtonStyle())
                                .disabled(draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
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
            .onAppear {
                reviews = ProductReviewStore.reviews(for: product.id)
                GhostAnalytics.productReviewsOpened(product.id)
            }
            .confirmationDialog("Delete this review?", isPresented: Binding(
                get: { deleteTarget != nil },
                set: { if !$0 { deleteTarget = nil } }
            ), titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    if let deleteTarget { ProductReviewStore.delete(id: deleteTarget.id) }
                    reviews = ProductReviewStore.reviews(for: product.id)
                    deleteTarget = nil
                }
                Button("Cancel", role: .cancel) {}
            }
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

    private func addReview() {
        let trimmed = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        ProductReviewStore.add(ProductReview(productID: product.id, rating: draftRating, text: trimmed))
        reviews = ProductReviewStore.reviews(for: product.id)
        draftText = ""
        draftRating = 5
    }
}
