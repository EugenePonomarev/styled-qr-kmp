import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import java.io.File

fun main() {
    val qrCode = QrCodeGenerator.encodeText(
        text = "https://eugeneponomarev.com",
        errorCorrection = QrErrorCorrectionLevel.H,
    )

    File("styled-qr.svg").writeText(
        qrCode.toSvg(
            style = QrStyle(
                foreground = QrColor.fromHex("#143A5A"),
                background = QrColor.White,
                moduleShape = QrModuleShape.RoundedSquare,
                moduleScale = 0.94,
            ),
        ),
    )
}
