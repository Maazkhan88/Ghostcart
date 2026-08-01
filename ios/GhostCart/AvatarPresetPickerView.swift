import SwiftUI

// Mirrors Android's AvatarPresetPickerDialog (GhostCartV2Screens.kt:1097-1137):
// a 4-column grid of every AvatarPreset.all entry, "Done" closes.
struct AvatarPresetPickerView: View {
    let selectedPresetId: String?
    let onSelect: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 4)

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 10) {
                    ForEach(AvatarPreset.all) { preset in
                        let selected = preset.id == selectedPresetId
                        Button {
                            onSelect(preset.id)
                        } label: {
                            Image(preset.imageName)
                                .resizable()
                                .scaledToFit()
                                .padding(8)
                                .frame(width: 64, height: 64)
                                .background(Color.ghostGreenColor.opacity(0.12))
                                .clipShape(Circle())
                                .overlay {
                                    Circle().stroke(
                                        selected ? Color.ghostGreenColor : Color.primary.opacity(0.09),
                                        lineWidth: selected ? 2 : 1
                                    )
                                }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(20)
            }
            .navigationTitle("Choose a Ghost avatar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(Color.ghostGreenColor)
                }
            }
        }
    }
}
