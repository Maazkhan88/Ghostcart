import SwiftUI
import MapKit

// UIViewRepresentable MKMapView wrapper: draws the deterministic simulated
// route as a polyline and a moving Ghost Rider annotation. Uses MKMapView
// directly (rather than SwiftUI's iOS 17+ Map/MapContentBuilder) so the app
// stays on its iOS 16 deployment target. This never touches the device's
// real location - no CLLocationManager, no location permission requested.
struct GhostRouteMapView: UIViewRepresentable {
    let origin: CLLocationCoordinate2D
    let destination: CLLocationCoordinate2D
    let waypoints: [CLLocationCoordinate2D]
    let riderPosition: CLLocationCoordinate2D
    let reduceMotion: Bool

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.isUserInteractionEnabled = true
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
            edgePadding: UIEdgeInsets(top: 40, left: 40, bottom: 40, right: 40),
            animated: false
        )
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        guard let rider = mapView.annotations.compactMap({ $0 as? GhostRiderAnnotation }).first else { return }
        if reduceMotion {
            rider.coordinate = riderPosition
        } else {
            UIView.animate(withDuration: 0.6) {
                rider.coordinate = riderPosition
            }
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            guard let polyline = overlay as? MKPolyline else { return MKOverlayRenderer(overlay: overlay) }
            let renderer = MKPolylineRenderer(polyline: polyline)
            renderer.strokeColor = UIColor(red: 0.39, green: 0.84, blue: 0.29, alpha: 1)
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
                view.accessibilityLabel = "Ghost Rider (simulated)"
                return view
            }
            guard let point = annotation as? MKPointAnnotation else { return nil }
            let identifier = point.title == "origin" ? "origin" : "destination"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKMarkerAnnotationView(annotation: point, reuseIdentifier: identifier)
            let marker = view as? MKMarkerAnnotationView
            marker?.markerTintColor = point.title == "origin" ? .darkGray : UIColor(red: 0.39, green: 0.84, blue: 0.29, alpha: 1)
            marker?.glyphImage = UIImage(systemName: point.title == "origin" ? "shippingbox" : "flag.checkered")
            marker?.canShowCallout = false
            return view
        }

        private static let riderImage: UIImage = {
            let config = UIImage.SymbolConfiguration(pointSize: 26, weight: .bold)
            let symbol = UIImage(systemName: "figure.wave.circle.fill", withConfiguration: config)?
                .withTintColor(UIColor(red: 0.39, green: 0.84, blue: 0.29, alpha: 1), renderingMode: .alwaysOriginal)
            return symbol ?? UIImage()
        }()
    }
}

private final class GhostRiderAnnotation: NSObject, MKAnnotation {
    @objc dynamic var coordinate: CLLocationCoordinate2D = CLLocationCoordinate2D()
}
