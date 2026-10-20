import UIKit
import StyledQrKmp

public func makeSmokeImage() -> UIImage? {
    let renderer = StyledQrImageRenderer()
    renderer.content = "https://example.com"
    return renderer.render(sizePoints: 240, scale: 1)
}
