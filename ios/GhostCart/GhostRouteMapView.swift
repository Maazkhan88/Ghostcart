import SwiftUI
import MapKit
#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

// MKMapView wrapper (UIViewRepresentable on iOS, NSViewRepresentable on
// macOS - MKMapView/MKPolyline/MKAnnotation etc. are themselves identical
// cross-platform MapKit API, only the SwiftUI representable protocol and a
// handful of UIColor/NSColor, UIImage/NSImage calls differ) that draws the
// deterministic simulated route as a polyline and a moving Ghost Rider
// annotation. Uses MKMapView directly (rather than SwiftUI's iOS 17+/
// macOS 14+ Map/MapContentBuilder) so the app stays on its iOS 16
// deployment target. This never touches the device's real location - no
// CLLocationManager, no location permission requested.
#if os(iOS)
private typealias PlatformViewRepresentable = UIViewRepresentable
private typealias PlatformColor = UIColor
private typealias PlatformEdgeInsets = UIEdgeInsets
#elseif os(macOS)
private typealias PlatformViewRepresentable = NSViewRepresentable
private typealias PlatformColor = NSColor
private typealias PlatformEdgeInsets = NSEdgeInsets
#endif

private let ghostGreen = PlatformColor(red: 0.39, green: 0.84, blue: 0.29, alpha: 1)

struct GhostRouteMapView: PlatformViewRepresentable {
    let origin: CLLocationCoordinate2D
    let destination: CLLocationCoordinate2D
    let waypoints: [CLLocationCoordinate2D]
    let riderPosition: CLLocationCoordinate2D
    let reduceMotion: Bool

    private func makeMapView() -> MKMapView {
        let mapView = MKMapView()
        #if os(iOS)
        mapView.isUserInteractionEnabled = true
        #endif
        mapView.showsUserLocation = false
        mapView.pointOfInterestFilter = .excludingAll
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false

        let polyline = MKPolyline(coordinates: waypoints, count: waypoints.count)
        mapView.addOverlay(polyline)

        let originAnnotation = MKPointAnnotation()
        originAnnotation.coordinate = origin
        originAnnotation.title = "origin"
        mapView.addAnnotation(originAnnotation)

        let destinationAnnotation = MKPointAnnotation()
        destinationAnnotation.coordinate = destination
        destinationAnnotation.title = "destination"
        mapView.addAnnotation(destinationAnnotation)

        let riderAnnotation = GhostRiderAnnotation()
        riderAnnotation.coordinate = riderPosition
        mapView.addAnnotation(riderAnnotation)

        let rect = polyline.boundingMapRect
        mapView.setVisibleMapRect(
            rect,
            edgePadding: PlatformEdgeInsets(top: 40, left: 40, bottom: 40, right: 40),
            animated: false
        )
        return mapView
    }

    private func updateMapView(_ mapView: MKMapView) {
        guard let rider = mapView.annotations.compactMap({ $0 as? GhostRiderAnnotation }).first else { return }
        if reduceMotion {
            rider.coordinate = riderPosition
            return
        }
        #if os(iOS)
        UIView.animate(withDuration: 0.6) {
            rider.coordinate = riderPosition
        }
        #elseif os(macOS)
        NSAnimationContext.runAnimationGroup { context in
            context.duration = 0.6
            rider.animator().coordinate = riderPosition
        }
        #endif
    }

    #if os(iOS)
    func makeUIView(context: Context) -> MKMapView { makeMapView() }
    func updateUIView(_ mapView: MKMapView, context: Context) { updateMapView(mapView) }
    #elseif os(macOS)
    func makeNSView(context: Context) -> MKMapView { makeMapView() }
    func updateNSView(_ mapView: MKMapView, context: Context) { updateMapView(mapView) }
    #endif

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            guard let polyline = overlay as? MKPolyline else { return MKOverlayRenderer(overlay: overlay) }
            let renderer = MKPolylineRenderer(polyline: polyline)
            renderer.strokeColor = ghostGreen
            renderer.lineWidth = 4
            renderer.lineDashPattern = [0, 10]
            renderer.lineCap = .round
            return renderer
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if let rider = annotation as? GhostRiderAnnotation {
                let identifier = "ghostRider"
                let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                    ?? MKAnnotationView(annotation: rider, reuseIdentifier: identifier)
                view.annotation = rider
                view.image = Self.riderImage
                view.centerOffset = CGPoint(x: 0, y: -14)
                #if os(iOS)
                view.accessibilityLabel = "Ghost Rider (simulated)"
                #elseif os(macOS)
                view.setAccessibilityLabel("Ghost Rider (simulated)")
                #endif
                return view
            }
            guard let point = annotation as? MKPointAnnotation else { return nil }
            let identifier = point.title == "origin" ? "origin" : "destination"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKMarkerAnnotationView(annotation: point, reuseIdentifier: identifier)
            let marker = view as? MKMarkerAnnotationView
            marker?.markerTintColor = point.title == "origin" ? .darkGray : ghostGreen
            marker?.glyphImage = Self.glyphImage(point.title == "origin" ? "shippingbox" : "flag.checkered")
            marker?.canShowCallout = false
            return view
        }

        #if os(iOS)
        private static func glyphImage(_ systemName: String) -> UIImage? {
            UIImage(systemName: systemName)
        }

        private static let riderImage: UIImage = {
            let config = UIImage.SymbolConfiguration(pointSize: 26, weight: .bold)
            let symbol = UIImage(systemName: "figure.wave.circle.fill", withConfiguration: config)?
                .withTintColor(ghostGreen, renderingMode: .alwaysOriginal)
            return symbol ?? UIImage()
        }()
        #elseif os(macOS)
        private static func glyphImage(_ systemName: String) -> NSImage? {
            NSImage(systemSymbolName: systemName, accessibilityDescription: nil)
        }

        private static let riderImage: NSImage = {
            // .withTintColor(_:renderingMode:) is iOS-only; macOS bakes a
            // color directly into the symbol via a palette configuration
            // instead (merged with the size/weight config), same visual
            // result (a solid green rider glyph) without needing an
            // NSImageView content-tint context, since this image is
            // consumed directly by MKAnnotationView.image.
            let sizeConfig = NSImage.SymbolConfiguration(pointSize: 26, weight: .bold)
            let colorConfig = NSImage.SymbolConfiguration(paletteColors: [ghostGreen])
            let symbol = NSImage(systemSymbolName: "figure.wave.circle.fill", accessibilityDescription: nil)?
                .withSymbolConfiguration(sizeConfig.applying(colorConfig))
            symbol?.isTemplate = false
            return symbol ?? NSImage()
        }()
        #endif
    }
}

private final class GhostRiderAnnotation: NSObject, MKAnnotation {
    @objc dynamic var coordinate: CLLocationCoordinate2D = CLLocationCoordinate2D()
}
