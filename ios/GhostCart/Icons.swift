import SwiftUI

struct ProductIconView: View {
    let name: String
    var color: Color = .white
    
    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let strokeWidth = w * 0.06
            
            Group {
                switch name {
                case "sneaker":
                    Image("ProductSneaker")
                        .resizable()
                        .scaledToFit()
                    
                case "perfume":
                    Image("ProductPerfume")
                        .resizable()
                        .scaledToFit()
                    
                case "burger":
                    Image("MascotCombo")
                        .resizable()
                        .scaledToFit()
                    
                case "headphones":
                    Path { path in
                        // Headband
                        path.move(to: CGPoint(x: w * 0.15, y: h * 0.5))
                        path.addArc(center: CGPoint(x: w * 0.5, y: h * 0.5), radius: w * 0.35, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                        
                        // Left cup
                        path.addRect(CGRect(x: w * 0.12, y: h * 0.5, width: w * 0.14, height: h * 0.3))
                        // Right cup
                        path.addRect(CGRect(x: w * 0.74, y: h * 0.5, width: w * 0.14, height: h * 0.3))
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                case "leaf":
                    Path { path in
                        path.move(to: CGPoint(x: w * 0.15, y: h * 0.85))
                        path.addQuadCurve(to: CGPoint(x: w * 0.55, y: h * 0.15), control: CGPoint(x: w * 0.15, y: h * 0.45))
                        path.addQuadCurve(to: CGPoint(x: w * 0.85, y: h * 0.85), control: CGPoint(x: w * 0.85, y: h * 0.45))
                        path.addQuadCurve(to: CGPoint(x: w * 0.15, y: h * 0.85), control: CGPoint(x: w * 0.55, y: h * 0.85))
                        path.move(to: CGPoint(x: w * 0.15, y: h * 0.85))
                        path.addLine(to: CGPoint(x: w * 0.55, y: h * 0.45))
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                case "chart":
                    Path { path in
                        path.move(to: CGPoint(x: w * 0.2, y: h * 0.8))
                        path.addLine(to: CGPoint(x: w * 0.2, y: h * 0.4))
                        
                        path.move(to: CGPoint(x: w * 0.5, y: h * 0.8))
                        path.addLine(to: CGPoint(x: w * 0.5, y: h * 0.2))
                        
                        path.move(to: CGPoint(x: w * 0.8, y: h * 0.8))
                        path.addLine(to: CGPoint(x: w * 0.8, y: h * 0.5))
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                case "wallet":
                    Path { path in
                        path.addRoundedRect(in: CGRect(x: w * 0.15, y: h * 0.25, width: w * 0.7, height: h * 0.55), cornerSize: CGSize(width: w * 0.06, height: w * 0.06))
                        path.addRoundedRect(in: CGRect(x: w * 0.55, y: h * 0.4, width: w * 0.32, height: h * 0.25), cornerSize: CGSize(width: w * 0.03, height: w * 0.03))
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                case "lock":
                    Path { path in
                        path.addRoundedRect(in: CGRect(x: w * 0.2, y: h * 0.45, width: w * 0.6, height: h * 0.45), cornerSize: CGSize(width: w * 0.08, height: w * 0.08))
                        path.move(to: CGPoint(x: w * 0.3, y: h * 0.45))
                        path.addArc(center: CGPoint(x: w * 0.5, y: h * 0.45), radius: w * 0.2, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                default:
                    Circle()
                        .stroke(color, lineWidth: strokeWidth)
                }
            }
        }
    }
}

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
