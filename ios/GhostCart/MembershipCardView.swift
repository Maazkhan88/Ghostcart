import SwiftUI

struct MembershipCardEditorView: View {
    @EnvironmentObject private var store: GhostCartStore
    @Environment(\.dismiss) private var dismiss
    @State private var displayName = ""
    @State private var theme: MembershipTheme = .ink
    @State private var exportURL: URL?

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                MembershipCardCanvas(
                    profile: MembershipProfile(
                        displayName: displayName.isEmpty ? "Ghost Cart Member" : displayName,
                        ghostID: store.membership.ghostID,
                        memberSince: store.membership.memberSince,
                        theme: theme
                    )
                )
                .frame(height: 218)

                VStack(alignment: .leading, spacing: 14) {
                    Text("Name on membership")
                        .font(.subheadline.weight(.bold))
                    TextField("Ghost Cart Member", text: $displayName)
                        .textInputAutocapitalization(.words)
                        .ghostTextField()

                    Text("Theme")
                        .font(.subheadline.weight(.bold))
                    Picker("Theme", selection: $theme) {
                        ForEach(MembershipTheme.allCases) { option in
                            Text(option.title).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Button("Save membership") {
                    store.updateMembership(displayName: displayName, theme: theme)
                }
                .buttonStyle(GhostPrimaryButtonStyle())

                Button {
                    store.updateMembership(displayName: displayName, theme: theme)
                    exportURL = renderMembershipCard()
                } label: {
                    Label("Prepare high-resolution image", systemImage: "square.and.arrow.up")
                }
                .buttonStyle(GhostSecondaryButtonStyle())

                if let exportURL {
                    ShareLink(item: exportURL) {
                        Label("Share or save PNG", systemImage: "photo")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(GhostSecondaryButtonStyle())
                }

                Text("Ghost membership is an achievement artifact with a non-financial Ghost ID. It cannot be used to purchase anything.")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)

                SimulationDisclosure()
            }
            .padding(20)
        }
        .background(Color(.systemBackground))
        .navigationTitle("Ghost membership")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Done") { dismiss() }
            }
        }
        .onAppear {
            displayName = store.membership.displayName
            theme = store.membership.theme
        }
    }

    private func renderMembershipCard() -> URL? {
        let profile = MembershipProfile(
            displayName: displayName.isEmpty ? "Ghost Cart Member" : displayName,
            ghostID: store.membership.ghostID,
            memberSince: store.membership.memberSince,
            theme: theme
        )
        let renderer = ImageRenderer(
            content: MembershipCardCanvas(profile: profile)
                .frame(width: 1200, height: 756)
        )
        renderer.scale = 1
        guard let data = renderer.uiImage?.pngData() else { return nil }
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("ghost-membership.png")
        do {
            try data.write(to: url, options: .atomic)
            return url
        } catch {
            return nil
        }
    }
}

struct MembershipCardCanvas: View {
    let profile: MembershipProfile

    private var background: Color {
        switch profile.theme {
        case .ink: return .inkColor
        case .paper: return .white
        case .green: return .ghostGreenColor
        }
    }

    private var foreground: Color {
        profile.theme == .ink ? .white : .inkColor
    }

    private var accent: Color {
        profile.theme == .ink ? .ghostGreenColor : .inkColor
    }

    var body: some View {
        GeometryReader { geometry in
            let scale = max(geometry.size.width / 340, 1)
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    GhostCartLogoView(tint: foreground)
                        .frame(width: 112 * scale, height: 38 * scale)
                    Spacer()
                    Text("MEMBERSHIP")
                        .font(.system(size: 8 * scale, weight: .black))
                        .tracking(1.2 * scale)
                        .foregroundStyle(foreground.opacity(0.62))
                }

                Spacer()

                Text(profile.displayName)
                    .font(.system(size: 23 * scale, weight: .black, design: .rounded))
                    .foregroundStyle(foreground)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(profile.ghostID)
                    .font(.system(size: 11 * scale, weight: .bold, design: .monospaced))
                    .foregroundStyle(accent)
                    .padding(.top, 4 * scale)

                Spacer()

                HStack {
                    Text("Member since \(profile.memberSince.formatted(.dateTime.month(.abbreviated).year()))")
                    Spacer()
                    Text("SIMULATION-ONLY")
                }
                .font(.system(size: 8 * scale, weight: .bold))
                .foregroundStyle(foreground.opacity(0.58))
            }
            .padding(18 * scale)
            .frame(width: geometry.size.width, height: geometry.size.height)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: 24 * scale, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 24 * scale, style: .continuous)
                    .stroke(profile.theme == .paper ? Color.inkColor.opacity(0.12) : Color.white.opacity(0.12), lineWidth: 1)
            }
        }
        .aspectRatio(1.586, contentMode: .fit)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Ghost membership for \(profile.displayName), Ghost ID \(profile.ghostID)")
    }
}
