import UIKit
import StyledQrKmp

func verifyStyledQrKmpSwiftApi() {
    let renderer = StyledQrImageRenderer()
    renderer.content = "https://example.com"

    for theme in QrThemes.shared.all {
        renderer.qrStyle = theme.createStyle(logo: nil)

        let image: UIImage? = renderer.render(
            sizePoints: 240.0,
            scale: 0.0
        )
        _ = image

        let options = theme.createLogoOptions()
        _ = theme.createStyle(logo: options)
    }
}
