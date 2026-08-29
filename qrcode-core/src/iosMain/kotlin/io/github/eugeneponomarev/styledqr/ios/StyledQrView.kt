package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIColor
import platform.UIKit.UIViewContentModeScaleAspectFit
import kotlin.math.min

/**
 * Native UIKit QR-code view. It extends [UIImageView], so it works directly in UIKit and can be
 * wrapped by `UIViewRepresentable` in SwiftUI.
 */
public class StyledQrView(
    frame: CValue<CGRect>,
) : UIImageView(frame = frame) {
    /** Convenience initializer for UIKit code that configures constraints after creation. */
    public constructor() : this(CGRectMake(0.0, 0.0, 0.0, 0.0))

    /** Text or URL encoded into the QR code. */
    public var content: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidateQrImage()
        }

    /** QR Reed–Solomon error-correction level. High correction is the default for logo support. */
    public var errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H
        set(value) {
            if (field == value) return
            field = value
            invalidateQrImage()
        }

    /** Shared visual configuration used by every platform renderer. */
    public var qrStyle: QrStyle = QrStyle()
        set(value) {
            if (field == value) return
            field = value
            backgroundColor = value.background.toUiColor()
            invalidateQrImage()
        }

    /** Optional centre logo. Set [qrStyle.logo] to customise its safe reserved area. */
    public var logo: UIImage? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateQrImage()
        }

    /** A description of the last encoding or logo-safety error, or `null` after a successful draw. */
    public var lastRenderError: String? = null
        private set

    private var renderedSidePoints: Double = Double.NaN

    init {
        contentMode = UIViewContentModeScaleAspectFit
        clipsToBounds = true
        backgroundColor = qrStyle.background.toUiColor()
    }

    /** Updates all mutable QR inputs together. */
    public fun setQrCode(
        content: String,
        errorCorrection: QrErrorCorrectionLevel,
        style: QrStyle,
        logo: UIImage? = null,
    ) {
        this.content = content
        this.errorCorrection = errorCorrection
        qrStyle = style
        this.logo = logo
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        renderIfNeeded()
    }

    private fun renderIfNeeded() {
        val sidePoints = bounds.useContents { min(size.width, size.height) }
        if (sidePoints <= 0.0 || renderedSidePoints == sidePoints) return

        renderedSidePoints = sidePoints
        lastRenderError = null
        if (content.isBlank()) {
            image = null
            return
        }

        try {
            val code = QrCodeGenerator.encodeText(content, errorCorrection)
            val minimumRenderSize = code.size + qrStyle.quietZoneModules * 2
            image = code.toUIImage(
                sizePoints = maxOf(sidePoints, minimumRenderSize.toDouble()),
                style = qrStyle,
                logo = logo,
            )
        } catch (error: IllegalArgumentException) {
            image = null
            lastRenderError = error.message
        }
    }

    private fun invalidateQrImage() {
        renderedSidePoints = Double.NaN
        lastRenderError = null
        image = null
        setNeedsLayout()
    }
}

private fun QrColor.toUiColor(): UIColor = UIColor(
    red = red / 255.0,
    green = green / 255.0,
    blue = blue / 255.0,
    alpha = alpha / 255.0,
)
