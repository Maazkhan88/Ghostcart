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
                    Path { path in
                        path.move(to: CGPoint(x: w * 0.1, y: h * 0.75))
                        path.addLine(to: CGPoint(x: w * 0.9, y: h * 0.75))
                        path.addLine(to: CGPoint(x: w * 0.9, y: h * 0.55))
                        path.addQuadCurve(to: CGPoint(x: w * 0.65, y: h * 0.3), control: CGPoint(x: w * 0.75, y: h * 0.5))
                        path.addLine(to: CGPoint(x: w * 0.45, y: h * 0.3))
                        path.addLine(to: CGPoint(x: w * 0.35, y: h * 0.45))
                        path.addQuadCurve(to: CGPoint(x: w * 0.1, y: h * 0.55), control: CGPoint(x: w * 0.2, y: h * 0.5))
                        path.closeSubpath()
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
                case "perfume":
                    Group {
                        RoundedRectangle(cornerRadius: w * 0.1)
                            .stroke(color, lineWidth: strokeWidth)
                            .frame(width: w * 0.6, height: h * 0.5)
                            .offset(y: h * 0.1)
                        
                        Rectangle()
                            .stroke(color, lineWidth: strokeWidth)
                            .frame(width: w * 0.2, height: h * 0.2)
                            .offset(y: -h * 0.25)
                        
                        Rectangle()
                            .stroke(color, lineWidth: strokeWidth * 0.6)
                            .frame(width: w * 0.3, height: h * 0.24)
                            .offset(y: h * 0.1)
                    }
                    .frame(width: w, height: h)
                    
                case "burger":
                    Path { path in
                        // Bun top
                        path.move(to: CGPoint(x: w * 0.15, y: h * 0.4))
                        path.addQuadCurve(to: CGPoint(x: w * 0.85, y: h * 0.4), control: CGPoint(x: w * 0.5, y: h * 0.1))
                        path.closeSubpath()
                        
                        // Cheese/Meat
                        path.move(to: CGPoint(x: w * 0.12, y: h * 0.46))
                        path.addLine(to: CGPoint(x: w * 0.88, y: h * 0.46))
                        path.addLine(to: CGPoint(x: w * 0.88, y: h * 0.56))
                        path.addLine(to: CGPoint(x: w * 0.12, y: h * 0.56))
                        path.closeSubpath()
                        
                        // Bun bottom
                        path.move(to: CGPoint(x: w * 0.15, y: h * 0.62))
                        path.addLine(to: CGPoint(x: w * 0.85, y: h * 0.62))
                        path.addLine(to: CGPoint(x: w * 0.85, y: h * 0.82))
                        path.addLine(to: CGPoint(x: w * 0.15, y: h * 0.82))
                        path.closeSubpath()
                    }
                    .stroke(color, lineWidth: strokeWidth)
                    
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
    
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24)
                .fill(Color.darkGrayColor)
            
            GeometryReader { geo in
                let w = geo.size.width
                let h = geo.size.height
                
                ZStack {
                    // Mascot Body Outline
                    Path { path in
                        path.move(to: CGPoint(x: w * 0.25, y: h * 0.85))
                        path.addLine(to: CGPoint(x: w * 0.25, y: h * 0.45))
                        path.addQuadCurve(to: CGPoint(x: w * 0.5, y: h * 0.15), control: CGPoint(x: w * 0.25, y: h * 0.15))
                        path.addQuadCurve(to: CGPoint(x: w * 0.75, y: h * 0.45), control: CGPoint(x: w * 0.75, y: h * 0.15))
                        path.addLine(to: CGPoint(x: w * 0.75, y: h * 0.85))
                        
                        path.addQuadCurve(to: CGPoint(x: w * 0.5, y: h * 0.85), control: CGPoint(x: w * 0.62, y: h * 0.75))
                        path.addQuadCurve(to: CGPoint(x: w * 0.25, y: h * 0.85), control: CGPoint(x: w * 0.38, y: h * 0.75))
                    }
                    .fill(Color.white)
                    
                    // Eyes
                    Circle()
                        .fill(Color.black)
                        .frame(width: w * 0.1, height: w * 0.1)
                        .position(x: w * 0.42, y: h * 0.38)
                    
                    Circle()
                        .fill(Color.black)
                        .frame(width: w * 0.1, height: w * 0.1)
                        .position(x: w * 0.58, y: h * 0.38)
                    
                    // Pose additions
                    if poseName == "thumbsup" {
                        Circle()
                            .fill(Color.ghostGreenColor)
                            .frame(width: w * 0.14, height: w * 0.14)
                            .position(x: w * 0.82, y: h * 0.6)
                    } else if poseName == "cooldown" {
                        Circle()
                            .fill(Color(red: 0.5, green: 0.85, blue: 1.0))
                            .frame(width: w * 0.12, height: w * 0.12)
                            .position(x: w * 0.5, y: h * 0.05)
                    } else if poseName == "cart" {
                        Circle()
                            .fill(Color.gray)
                            .frame(width: w * 0.08, height: w * 0.08)
                            .position(x: w * 0.32, y: h * 0.88)
                        
                        Circle()
                            .fill(Color.gray)
                            .frame(width: w * 0.08, height: w * 0.08)
                            .position(x: w * 0.68, y: h * 0.88)
                    }
                }
            }
            .frame(width: 54, height: 54)
        }
    }
}
