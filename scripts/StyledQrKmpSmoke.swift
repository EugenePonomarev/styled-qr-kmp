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

    let start = QrGradientPoint(x: 0.0, y: 0.0)
    let end = QrGradientPoint(x: 1.0, y: 1.0)
    let gradient = QrLinearGradient(
        startColor: QrColor(red: 7, green: 89, blue: 133, alpha: 255),
        endColor: QrColor(red: 109, green: 40, blue: 217, alpha: 255),
        start: start,
        end: end
    )
    let base = QrThemes.shared.all[0].createStyle(logo: nil)
    renderer.qrStyle = QrStyle(
        foreground: base.foreground,
        background: base.background,
        quietZoneModules: base.quietZoneModules,
        moduleShape: base.moduleShape,
        moduleScale: base.moduleScale,
        roundedModuleRadiusFraction: base.roundedModuleRadiusFraction,
        preserveFunctionPatterns: base.preserveFunctionPatterns,
        functionPatternStyle: base.functionPatternStyle,
        finderPatternShape: base.finderPatternShape,
        logo: nil,
        foregroundGradient: gradient
    )
    _ = renderer.render(sizePoints: 240.0, scale: 0.0)
}
