import SwiftUI

struct GhostMascotView: View {
    let poseName: String

    private var assetName: String {
        switch poseName {
        case "cart": return "MascotCart"
        case "wallet": return "MascotWallet"
        case "cooldown": return "MascotCooldown"
        case "thumbsup": return "MascotThumbsup"
        case "combo": return "MascotCombo"
        default: return "MascotWave"
        }
    }

    var body: some View {
        Image(assetName)
            .resizable()
            .scaledToFit()
    }
}

struct GhostCartLogoView: View {
    var body: some View {
        Image("GhostCartLogo")
            .resizable()
            .scaledToFit()
            .accessibilityLabel("Ghost Cart")
    }
}
