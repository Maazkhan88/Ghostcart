import SwiftUI

extension Color {
    static let inkColor = Color(red: 0.02, green: 0.02, blue: 0.02) // #050505
    static let paperColor = Color.white // #FFFFFF
    static let softGrayColor = Color(red: 0.96, green: 0.96, blue: 0.96) // #F4F4F4
    static let darkGrayColor = Color(red: 0.09, green: 0.09, blue: 0.09) // #161616
    static let ghostGreenColor = Color(red: 0.39, green: 0.84, blue: 0.29) // #64D64A
    
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}
