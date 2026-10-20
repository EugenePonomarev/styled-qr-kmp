@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrRenderError
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrRenderException
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration
import io.github.eugeneponomarev.styledqr.render.qrImageTotalModules
import io.github.eugeneponomarev.styledqr.render.validateQrImageRenderArguments
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

    public var lastRenderErrorCode: QrRenderErrorCode? = null
        private set

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

    public fun render(sizePoints: Double, scale: Double = 0.0): UIImage? {
        lastRenderError = null
        lastRenderErrorCode = null

        try {
            validateQrImageRenderArguments(sizePoints, scale)
        } catch (error: QrRenderException) {
            return recordFailure(error.code, error.message)
        }

        if (content.isBlank()) return null

        val code = try {
            QrCodeGenerator.encodeText(content, errorCorrection)
        } catch (error: IllegalArgumentException) {
            return recordFailure(
                QrRenderErrorCode.PayloadTooLong,
                error.message,
            )
        }

        return try {
            val minimumRenderSize =
                qrImageTotalModules(code.size, qrStyle.quietZoneModules)

            code.toUIImage(
                sizePoints = maxOf(sizePoints, minimumRenderSize.toDouble()),
                style = qrStyle,
                scale = scale,
                logo = logo,
            )
        } catch (error: QrRenderException) {
            recordFailure(error.code, error.message)
        } catch (error: IllegalArgumentException) {
            recordFailure(QrRenderErrorCode.RenderFailed, error.message)
        }
    }

    /**
     * Validates the configuration inside Kotlin and returns a result.
     *
     * Does not change stored content/errorCorrection/qrStyle/logo.
     * Updates lastRenderError and lastRenderErrorCode for this call.
     * Configuration is validated even for blank content.
     */
    public fun renderWithOptions(
        content: String,
        options: QrStyleConfiguration,
        sizePoints: Double,
        errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
        scale: Double = 0.0,
        logo: UIImage? = null,
    ): QrImageRenderResult {
        val built = options.build()
        val styleError = built.error

        if (styleError != null) {
            recordFailure(styleError.code, styleError.message)
            return QrImageRenderResult(null, styleError)
        }

        val worker = StyledQrImageRenderer()
        worker.setQrCode(
            content,
            errorCorrection,
            requireNotNull(built.style),
            logo,
        )

        val image = worker.render(sizePoints, scale)
        lastRenderError = worker.lastRenderError
        lastRenderErrorCode = worker.lastRenderErrorCode

        val error = lastRenderErrorCode?.let { code ->
            QrRenderError(
                code,
                lastRenderError ?: "Unable to render QR image",
            )
        }

        return QrImageRenderResult(image, error)
    }

    private fun recordFailure(
        code: QrRenderErrorCode,
        message: String?,
    ): UIImage? {
        lastRenderErrorCode = code
        lastRenderError = message ?: "Unable to render QR image"
        return null
    }
}

public class QrImageRenderResult internal constructor(
    public val image: UIImage?,
    public val error: QrRenderError?,
) {
    public val isSuccess: Boolean get() = image != null && error == null
    public val isEmpty: Boolean get() = image == null && error == null
}
