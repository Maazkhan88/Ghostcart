import SwiftUI

struct DashboardView: View {
    let ghostedCount: Int
    let cooledCount: Int

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                VStack(alignment: .leading, spacing: 4) {
                    Text("Almost Bought".uppercased())
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.ghostGreenColor)
                        .tracking(1)
                    Text("Insights & metrics.")
                        .font(.system(size: 24, weight: .black))
                        .foregroundColor(.white)
                    Text("Sample data · not a user claim")
                        .font(.system(size: 8, weight: .semibold))
                        .foregroundColor(.gray)
                }

                // Stats row
                HStack(spacing: 12) {
                    MetricCardView(label: "Saved Urges", value: "\(ghostedCount)", disclaimer: "Item count")
                    MetricCardView(label: "Cooled Urges", value: "\(cooledCount)", disclaimer: "Simulated delay")
                }

                // Donut chart card
                VStack(alignment: .leading, spacing: 12) {
                    Text("Cravings by Category")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                    Text("Sample data · not a user claim")
                        .font(.system(size: 8))
                        .foregroundColor(.gray)

                    HStack(spacing: 24) {
                        DonutChartView(slices: [
                            DonutSliceData(color: Color(hex: "D0E5FF"), fraction: 0.35),
                            DonutSliceData(color: Color(hex: "FFF3D6"), fraction: 0.25),
                            DonutSliceData(color: Color(hex: "FFD6D6"), fraction: 0.20),
                            DonutSliceData(color: Color(hex: "D6FFE5"), fraction: 0.20)
                        ])
                        .frame(width: 120, height: 120)

                        VStack(alignment: .leading, spacing: 8) {
                            LegendRow(color: Color(hex: "D0E5FF"), label: "Fashion (35%)")
                            LegendRow(color: Color(hex: "FFF3D6"), label: "Beauty (25%)")
                            LegendRow(color: Color(hex: "FFD6D6"), label: "Delivery (20%)")
                            LegendRow(color: Color(hex: "D6FFE5"), label: "Tech (20%)")
                        }
                    }
                }
                .padding(16)
                .background(Color.darkGrayColor)
                .cornerRadius(16)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(white: 0.17), lineWidth: 1))

                // Line chart card
                VStack(alignment: .leading, spacing: 12) {
                    Text("Your weekly mood & mindset")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                    Text("Sample data · not a user claim")
                        .font(.system(size: 8))
                        .foregroundColor(.gray)

                    LineChartView(points: [4, 6, 3, 8, 5, 9, 6])
                        .frame(height: 130)

                    HStack {
                        ForEach(["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"], id: \.self) { day in
                            Text(day)
                                .font(.system(size: 9))
                                .foregroundColor(.gray)
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
                .padding(16)
                .background(Color.darkGrayColor)
                .cornerRadius(16)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(white: 0.17), lineWidth: 1))
            }
            .padding(16)
            .padding(.bottom, 80)
        }
        .background(Color.inkColor.ignoresSafeArea())
    }
}

struct MetricCardView: View {
    let label: String
    let value: String
    let disclaimer: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(.gray)
            Text(value)
                .font(.system(size: 28, weight: .black))
                .foregroundColor(.ghostGreenColor)
            Text(disclaimer)
                .font(.system(size: 8))
                .foregroundColor(.gray)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.darkGrayColor)
        .cornerRadius(16)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(white: 0.17), lineWidth: 1))
    }
}

struct DonutSliceData {
    let color: Color
    let fraction: CGFloat
}

struct DonutChartView: View {
    let slices: [DonutSliceData]

    var body: some View {
        Canvas { context, size in
            let strokeWidth = size.width * 0.18
            var startAngle = Angle.degrees(-90)
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = (size.width - strokeWidth) / 2

            for slice in slices {
                let endAngle = startAngle + Angle.degrees(Double(slice.fraction) * 360)
                var path = Path()
                path.addArc(center: center, radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: false)

                context.stroke(path, with: .color(slice.color), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .butt))
                startAngle = endAngle
            }
        }
    }
}

struct LegendRow: View {
    let color: Color
    let label: String

    var body: some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 10, height: 10)
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(Color(white: 0.72))
        }
    }
}

struct LineChartView: View {
    let points: [CGFloat]
    private let maxVal: CGFloat = 10

    var body: some View {
        Canvas { context, size in
            let stepX = size.width / CGFloat(points.count - 1)

            // Grid lines
            for i in 0...4 {
                let y = size.height * CGFloat(i) / 4
                var gridPath = Path()
                gridPath.move(to: CGPoint(x: 0, y: y))
                gridPath.addLine(to: CGPoint(x: size.width, y: y))
                context.stroke(gridPath, with: .color(Color(white: 0.13)), style: StrokeStyle(lineWidth: 1))
            }

            // Polyline
            var linePath = Path()
            for (index, point) in points.enumerated() {
                let x = CGFloat(index) * stepX
                let y = size.height - (point / maxVal) * size.height
                if index == 0 {
                    linePath.move(to: CGPoint(x: x, y: y))
                } else {
                    linePath.addLine(to: CGPoint(x: x, y: y))
                }
            }
            context.stroke(linePath, with: .color(Color.ghostGreenColor), style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))

            // Dots
            for (index, point) in points.enumerated() {
                let x = CGFloat(index) * stepX
                let y = size.height - (point / maxVal) * size.height
                let dotRect = CGRect(x: x - 4, y: y - 4, width: 8, height: 8)
                context.fill(Path(ellipseIn: dotRect), with: .color(.white))
            }
        }
    }
}
