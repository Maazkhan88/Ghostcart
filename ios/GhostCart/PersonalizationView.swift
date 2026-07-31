import SwiftUI

// Mirrors Android's PersonalizationScreen: multi-select overspend categories
// (reuses AlmostBuyCategory, same set Marketplace.overspendCategories maps
// to) plus a savings-goal preset pick.
struct PersonalizationView: View {
    @Binding var selectedCategories: Set<AlmostBuyCategory>
    @Binding var savingsGoal: Int?
    let onContinue: () -> Void

    private let presets = [500, 1000, 2500]
    private let columns = [GridItem(.adaptive(minimum: 140), spacing: 10)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                ScreenHeader(
                    eyebrow: "Personalize",
                    title: "What tempts you most?",
                    subtitle: "Pick a few categories so Ghost Cart can flag the right moments."
                )

                LazyVGrid(columns: columns, spacing: 10) {
                    ForEach(AlmostBuyCategory.allCases) { category in
                        categoryChip(category)
                    }
                }

                SectionHeading(title: "Savings goal")

                HStack(spacing: 10) {
                    ForEach(presets, id: \.self) { amount in
                        goalChip(amount)
                    }
                }

                Button("Continue", action: onContinue)
                    .buttonStyle(GhostPrimaryButtonStyle())
                    .padding(.top, 8)
            }
            .padding(24)
        }
        .background(Color.paperColor.ignoresSafeArea())
    }

    private func categoryChip(_ category: AlmostBuyCategory) -> some View {
        let isSelected = selectedCategories.contains(category)
        return Button {
            if isSelected { selectedCategories.remove(category) } else { selectedCategories.insert(category) }
        } label: {
            Text(category.title)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity)
                .background(isSelected ? Color.ghostGreenColor : Color.primary.opacity(0.05))
                .foregroundStyle(isSelected ? Color.inkColor : Color.primary)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func goalChip(_ amount: Int) -> some View {
        let isSelected = savingsGoal == amount
        return Button {
            savingsGoal = amount
        } label: {
            Text(AmountFormatter.string(Double(amount)))
                .font(.subheadline.weight(.bold))
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity)
                .background(isSelected ? Color.ghostGreenColor : Color.primary.opacity(0.05))
                .foregroundStyle(isSelected ? Color.inkColor : Color.primary)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    PersonalizationView(selectedCategories: .constant([]), savingsGoal: .constant(nil), onContinue: {})
}
