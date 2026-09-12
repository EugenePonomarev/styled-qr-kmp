import UIKit
import StyledQrKmp

func verifyStyledQrKmpSwiftApi() {
    let renderer = StyledQrImageRenderer()
    renderer.content = "https://example.com"

    let image: UIImage? = renderer.render(sizePoints: 240.0, scale: 0.0)
    _ = image
}
