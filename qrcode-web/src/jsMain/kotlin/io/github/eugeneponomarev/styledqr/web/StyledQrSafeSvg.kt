@file:OptIn(ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.toSvg
import kotlin.js.JsExport

/** Error codes match the names of the shared QrRenderErrorCode entries. */
@JsExport
public class StyledQrWebError internal constructor(
    public val code: String,
    public val message: String,
)

@JsExport
public class StyledQrSvgResult internal constructor(
    public val svg: String?,
    public val error: StyledQrWebError?,
) {
    public val isSuccess: Boolean get() = svg != null && error == null
    public val isEmpty: Boolean get() = svg == null && error == null
}

/**
 * Safe, synchronous SVG generation.
 *
 * Style and generation parameters are validated before checking blank content.
 * Valid blank input returns an empty result without encoding a matrix.
 *
 * For nonblank input, validates logo data-URI syntax and shared placement safety.
 * Image decoding is performed by the browser when displaying the SVG.
 */
@JsExport
public fun renderStyledQrSvg(
    content: String,
    options: StyledQrOptions = StyledQrOptions(),
): StyledQrSvgResult {
    return try {
        val request = prepareStyledQrRequest(options)

        if (content.isBlank()) {
            StyledQrSvgResult(null, null)
        } else {
            StyledQrSvgResult(
                svg = renderPreparedStyledQrSvg(content, request),
                error = null,
            )
        }
    } catch (error: StyledQrWebException) {
        svgFailure(
            error.code,
            error.message ?: "Unable to generate QR SVG",
        )
    } catch (error: Exception) {
        svgFailure(
            QrRenderErrorCode.RenderFailed,
            error.message ?: "Unable to generate QR SVG",
        )
    }
}

internal class PreparedStyledQrRequest(
    val style: QrStyle,
    val errorCorrection: QrErrorCorrectionLevel,
    val minVersion: Int,
    val maxVersion: Int,
    val logoDataUri: String?,
)

internal class StyledQrWebException(
    val code: QrRenderErrorCode,
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

internal fun prepareStyledQrRequest(
    options: StyledQrOptions,
): PreparedStyledQrRequest {
    val style = webSvgStep(QrRenderErrorCode.InvalidStyle) {
        resolveStyledQrStyle(options)
    }

    return webSvgStep(QrRenderErrorCode.InvalidRenderParameters) {
        val minimum = checkedWebInteger(options.minVersion, "minVersion")
        val maximum = checkedWebInteger(options.maxVersion, "maxVersion")

        require(minimum in 1..40) {
            "minVersion must be in 1..40"
        }
        require(maximum in minimum..40) {
            "maxVersion must be in $minimum..40"
        }

        PreparedStyledQrRequest(
            style = style,
            errorCorrection = webEnum<QrErrorCorrectionLevel>(
                options.errorCorrection,
                "errorCorrection",
            ),
            minVersion = minimum,
            maxVersion = maximum,
            logoDataUri = options.logoDataUri,
        )
    }
}

internal fun renderPreparedStyledQrSvg(
    content: String,
    request: PreparedStyledQrRequest,
    output: QrRasterRenderOptions? = null,
): String {
    val code = webSvgStep(QrRenderErrorCode.PayloadTooLong) {
        QrCodeGenerator.encodeText(
            text = content,
            errorCorrection = request.errorCorrection,
            minVersion = request.minVersion,
            maxVersion = request.maxVersion,
        )
    }

    val svgWithoutLogo = if (output != null) {
        webSvgStep(QrRenderErrorCode.InvalidRenderParameters) {
            // The common renderer uses qrImageTotalModules and
            // calculateQrRasterGeometry before considering any logo.
            // These helpers are internal to qrcode-core; use its public
            // rendering path instead of duplicating their calculations.
            SvgQrRenderer.renderWithRasterOptions(
                qrCode = code,
                options = output,
                style = request.style,
            )
        }
    } else {
        // Preserve the unsized SVG validation and its existing diagnostics.
        webSvgStep(QrRenderErrorCode.InvalidRenderParameters) {
            val totalModules =
                code.size.toLong() + request.style.quietZoneModules.toLong() * 2L

            require(totalModules in 1..Int.MAX_VALUE.toLong()) {
                "QR SVG module count is outside the supported integer range"
            }
        }
        null
    }

    val logo = webSvgStep(QrRenderErrorCode.LogoValidationFailed) {
        val uri = request.logoDataUri?.let(::validatedWebLogoDataUri)

        if (uri != null) {
            code.calculateLogoLayout(
                requireNotNull(request.style.logo) {
                    "Logo options are missing"
                },
            )
        }

        uri
    }

    return if (output == null) {
        code.toSvg(request.style, logo)
    } else if (logo == null) {
        requireNotNull(svgWithoutLogo)
    } else {
        webSvgStep(QrRenderErrorCode.InvalidRenderParameters) {
            SvgQrRenderer.renderWithRasterOptions(
                qrCode = code,
                options = output,
                style = request.style,
                logoDataUri = logo,
            )
        }
    }
}

private inline fun <T> webSvgStep(
    code: QrRenderErrorCode,
    block: () -> T,
): T = try {
    block()
} catch (error: StyledQrWebException) {
    throw error
} catch (error: IllegalArgumentException) {
    throw StyledQrWebException(
        code = code,
        message = error.message ?: code.name,
        cause = error,
    )
}

private fun svgFailure(
    code: QrRenderErrorCode,
    message: String,
): StyledQrSvgResult = StyledQrSvgResult(
    svg = null,
    error = StyledQrWebError(
        code = code.name,
        message = message,
    ),
)
