@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrStyle
import platform.UIKit.UIImage

/**
 * Swift-importable iOS QR renderer.
 *
 * Kotlin/Native cannot export a Kotlin subclass of [platform.UIKit.UIImageView] for direct Swift
 * consumption. This class deliberately owns only QR state and produces a native [UIImage], which
 * Swift/UIKit can place in any UIImageView and SwiftUI can display with Image(uiImage:).
 */
public class StyledQrImageRenderer {
    /** Text or URL encoded into the QR code. */
    public var content: String = ""

    /** QR Reed–Solomon error-correction level. High correction is the default for logo support. */
    public var errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H

    /** Shared visual configuration used by Android, SVG, and iOS renderers. */
    public var qrStyle: QrStyle = QrStyle()

    /** Optional centre logo. Set [qrStyle.logo] to customise its safe reserved area. */
    public var logo: UIImage? = null

    /** A description of the last encoding or logo-safety error, or `null` after a successful draw. */
    public var lastRenderError: String? = null
        private set

    /** Updates all mutable QR inputs together. */
    public fun setQrCode(
        content: String,
        errorCorrection: QrErrorCorrectionLevel,
        style: QrStyle,
        logo: UIImage? = null,
    ) {
        this.content = content
        this.errorCorrection = errorCorrection
        this.qrStyle = style
        this.logo = logo
    }

    /**
     * Renders the current QR configuration into a UIImage.
     *
     * A blank [content] returns `null` without an error. A non-positive [sizePoints], invalid
     * payload, or unsafe logo returns `null` and populates [lastRenderError].
     */
    public fun render(sizePoints: Double, scale: Double = 0.0): UIImage? {
        lastRenderError = null
        if (sizePoints <= 0.0) {
            lastRenderError = "sizePoints must be greater than zero"
            return null
        }
        if (content.isBlank()) return null

        return try {
            val code = QrCodeGenerator.encodeText(content, errorCorrection)
            val minimumRenderSize = code.size + qrStyle.quietZoneModules * 2
            code.toUIImage(
                sizePoints = maxOf(sizePoints, minimumRenderSize.toDouble()),
                style = qrStyle,
                scale = scale,
                logo = logo,
            )
        } catch (error: IllegalArgumentException) {
            lastRenderError = error.message
            null
        }
    }
}
