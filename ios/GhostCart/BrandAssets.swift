import SwiftUI

struct GhostMascotView: View {
    let poseName: String

    // Matches Android's GhostMascotPose (ui/Icons.kt) pose->drawable
    // mapping exactly, per the asset manifest (§2). "peek" reuses waveAlt
    // on Android too, not a dedicated asset.
    private var assetName: String {
        switch poseName {
        case "cart": return "MascotCart"
        case "wallet": return "MascotWallet"
        case "cooldown": return "MascotCooldown"
        case "thumbsup": return "MascotThumbsup"
        case "combo": return "MascotCombo"
        case "waveAlt", "peek": return "MascotWaveAlt"
        case "trio": return "MascotTrio"
        case "phoneList": return "MascotPhoneList"
        case "checkoutPhone": return "MascotCheckoutPhone"
        case "male": return "MascotMale"
        case "female": return "MascotFemale"
        default: return "MascotWave"
        }
    }

    var body: some View {
        Image(assetName)
            .resizable()
            .scaledToFit()
    }
}

// The bundled artwork is a fixed black-on-transparent PNG (icon badge +
// wordmark), so it disappears on a dark background unless recolored.
// Rendered as a template and tinted, it follows whatever color is passed in
// (defaults to `.primary`, which flips white/black with light/dark mode) -
// no hardcoded white backing needed.
struct GhostCartLogoView: View {
    var tint: Color = .primary

    var body: some View {
        Image("GhostCartLogo")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundStyle(tint)
            .accessibilityLabel("Ghost Cart")
    }
}
