package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRenderError
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrRenderException
import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.calculateQrRasterGeometry
import io.github.eugeneponomarev.styledqr.render.qrImageTotalModules

/** Stateless high-level Android renderer with explicit result states. */
public class StyledQrBitmapRenderer {
    /**
     * Validates style, version bounds and output parameters before checking
     * for blank content. Valid blank input returns an empty result.
     *
     * Never enlarges an insufficient output size.
     * Does not modify the supplied configuration or logo.
     * The caller owns the returned bitmap and the supplied logo.
     */
    public fun renderWithOptions(
        content: String,
        options: QrStyleConfiguration,
        output: QrRasterRenderOptions,
        errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
        logo: Bitmap? = null,
        minVersion: Int = 1,
        maxVersion: Int = 40,
    ): QrBitmapRenderResult {
        val built = options.build()
        built.error?.let { error ->
            return QrBitmapRenderResult(null, error)
        }

        return try {
            if (output.sizePx <= 0) {
                return failure(
                    QrRenderErrorCode.InvalidRenderParameters,
                    "sizePx must be greater than zero",
                )
            }
            if (minVersion !in 1..40) {
                return failure(
                    QrRenderErrorCode.InvalidRenderParameters,
                    "minVersion must be in 1..40",
                )
            }
            if (maxVersion !in minVersion..40) {
                return failure(
                    QrRenderErrorCode.InvalidRenderParameters,
                    "maxVersion must be in $minVersion..40",
                )
            }

            if (content.isBlank()) {
                return QrBitmapRenderResult(null, null)
            }

            val style = requireNotNull(built.style)
            val code = try {
                QrCodeGenerator.encodeText(
                    text = content,
                    errorCorrection = errorCorrection,
                    minVersion = minVersion,
                    maxVersion = maxVersion,
                )
            } catch (error: IllegalArgumentException) {
                return failure(
                    QrRenderErrorCode.PayloadTooLong,
                    error.message ?: "Payload is too long",
                )
            }

            // Validate output before logo placement, matching the iOS order.
            calculateQrRasterGeometry(
                options = output,
                totalModules = qrImageTotalModules(
                    matrixSize = code.size,
                    quietZoneModules = style.quietZoneModules,
                ),
            )

            if (logo != null) {
                if (logo.isRecycled) {
                    return failure(
                        QrRenderErrorCode.LogoValidationFailed,
                        "Logo bitmap has been recycled",
                    )
                }

                // Use shared safety validation to classify placement failures.
                try {
                    code.calculateLogoLayout(style.logo ?: QrLogoOptions())
                } catch (error: IllegalArgumentException) {
                    return failure(
                        QrRenderErrorCode.LogoValidationFailed,
                        error.message ?: "Invalid QR logo placement",
                    )
                }
            }

            QrBitmapRenderResult(
                bitmap = code.toBitmapWithOptions(
                    options = output,
                    style = style,
                    logo = logo,
                ),
                error = null,
            )
        } catch (error: QrRenderException) {
            failure(
                error.code,
                error.message ?: "Unable to render QR bitmap",
            )
        } catch (error: Exception) {
            failure(
                QrRenderErrorCode.RenderFailed,
                error.message ?: "Unable to render QR bitmap",
            )
        }
    }

    private fun failure(
        code: QrRenderErrorCode,
        message: String,
    ): QrBitmapRenderResult = QrBitmapRenderResult(
        bitmap = null,
        error = QrRenderError(code, message),
    )
}

public class QrBitmapRenderResult internal constructor(
    public val bitmap: Bitmap?,
    public val error: QrRenderError?,
) {
    public val isSuccess: Boolean get() = bitmap != null && error == null
    public val isEmpty: Boolean get() = bitmap == null && error == null
}
