import UIKit
import StyledQrKmp

func verifyStyledQrKmpSwiftApi() {
    let renderer = StyledQrImageRenderer()
    renderer.content = "https://example.com"

    let defaultImage: UIImage? = renderer.render(
        sizePoints: 240.0,
        scale: 0.0
    )
    _ = defaultImage

    for theme in QrThemes.shared.all {
        renderer.qrStyle = theme.createStyle(logo: nil)

        let themedImage: UIImage? = renderer.render(
            sizePoints: 240.0,
            scale: 0.0
        )
        _ = themedImage

        let options = theme.createLogoOptions()
        _ = theme.createStyle(logo: options)
    }
}