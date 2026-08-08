import SwiftUI

// The extension's "mini Ghost Cart" - a WhatsApp-style rich share
// experience instead of the plain system compose sheet. Reuses the exact
// duration picker (GhostDeliveryDuration/CustomDurationSheet) and product
// thumbnail (Theme.swift's ProductThumbnail) the main app's CaptureView
// already uses - same components, shared source files, not a rebuild.
struct MiniCaptureView: View {
    @ObservedObject var model: MiniCaptureModel
    let onDone: () -> Void
    @State private var showCustomDurationSheet = false
    @State private var isCustomCooldown = false

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color(.systemBackground))
        .task { }
    }

    private var header: some View {
        HStack(spacing: 8) {
            Text("👻").font(.title3)
            Text("Ghost Cart").font(.headline.weight(.black))
            Spacer()
            Button("Close", action: onDone)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.secondary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    @ViewBuilder
    private var content: some View {
        switch model.stage {
        case .resolving:
            statusView(spinner: true, message: "Ghosting product…")
        case .signedOut:
            signedOutView
        case .error(let message):
            errorView(message)
        case .ready:
            readyView
        case .saving:
            statusView(spinner: true, message: "Saving to Ghost Cart…")
        case .saved:
            statusView(spinner: false, message: "Saved to your Ghost Cart", symbol: "checkmark.circle.fill")
                .task {
                    try? await Task.sleep(nanoseconds: 900_000_000)
                    onDone()
                }
        }
    }

    private func statusView(spinner: Bool, message: String, symbol: String? = nil) -> some View {
        VStack(spacing: 14) {
            if spinner {
                ProgressView().tint(Color.ghostGreenColor)
            } else if let symbol {
                Image(systemName: symbol)
                    .font(.system(size: 40))
                    .foregroundStyle(Color.ghostGreenColor)
            }
            Text(message)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(32)
    }

    private var signedOutView: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle.badge.exclamationmark")
                .font(.system(size: 40))
                .foregroundStyle(Color.secondary)
            Text("Sign in to Ghost Cart to save items shared here.")
                .font(.subheadline.weight(.semibold))
                .multilineTextAlignment(.center)
            Button("Save link for now") {
                model.saveForLater()
                onDone()
            }
            .buttonStyle(GhostSecondaryButtonStyle())
        }
        .padding(32)
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 36))
                .foregroundStyle(Color.secondary)
            Text(message)
                .font(.subheadline.weight(.semibold))
                .multilineTextAlignment(.center)
            Button("Done", action: onDone)
                .buttonStyle(GhostPrimaryButtonStyle())
        }
        .padding(32)
    }

    private var readyView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                ProductThumbnail(
                    imageURL: model.product?.imageURL,
                    systemImage: model.category.systemImage,
                    productName: model.displayTitle,
                    fillWidthHeight: 140,
                    cornerRadius: 16
                )

                Text(model.displayTitle)
                    .font(.headline.weight(.bold))
                    .lineLimit(2)

                if model.product?.amount != nil {
                    DirhamAmount(value: model.product?.amount ?? 0, font: .title3.weight(.black))
                } else {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Amount").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                        TextField("0", text: $model.manualAmountText)
                            .keyboardType(.decimalPad)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(Color.primary.opacity(0.055))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }

                if let retailer = model.product?.retailer, !retailer.isEmpty {
                    Text(retailer)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.ghostGreenColor)
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Ghost Delivery time").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 8)], spacing: 8) {
                        ForEach(GhostDeliveryDuration.presets(for: model.category)) { duration in
                            durationPill(duration)
                        }
                    }
                }

                Button {
                    Task { await model.confirmGhostIt() }
                } label: {
                    Text("Ghost it")
                }
                .buttonStyle(GhostPrimaryButtonStyle())
                .disabled(!model.canConfirm)
            }
            .padding(20)
        }
        .sheet(isPresented: $showCustomDurationSheet) {
            CustomDurationSheet(initialMinutes: model.cooldownMinutes) { minutes in
                model.cooldownMinutes = minutes
                isCustomCooldown = true
                showCustomDurationSheet = false
            }
        }
    }

    @ViewBuilder
    private func durationPill(_ duration: GhostDeliveryDuration) -> some View {
        if let minutes = duration.totalMinutes {
            Button {
                isCustomCooldown = false
                model.cooldownMinutes = minutes
            } label: {
                Text(duration.label)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(model.cooldownMinutes == minutes && !isCustomCooldown ? Color.inkColor : Color.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(model.cooldownMinutes == minutes && !isCustomCooldown ? Color.ghostGreenColor : Color.primary.opacity(0.055))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        } else {
            Button {
                showCustomDurationSheet = true
            } label: {
                Text(isCustomCooldown ? GhostDeliveryDuration.formattedCustomLabel(minutes: model.cooldownMinutes) : duration.label)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(isCustomCooldown ? Color.inkColor : Color.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(isCustomCooldown ? Color.ghostGreenColor : Color.primary.opacity(0.055))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }
}
