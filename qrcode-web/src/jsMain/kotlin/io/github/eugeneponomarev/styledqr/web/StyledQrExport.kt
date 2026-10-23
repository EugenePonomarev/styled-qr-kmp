@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import kotlin.js.JsExport
import kotlin.js.jsTypeOf

/**
 * Output dimensions, separate from the QR's visual style.
 *
 * Supported size modes: "integer-modules" and "fit".
 */
@JsExport
public class StyledQrExportOptions(
    public var sizePx: Int,
    public var sizeMode: String = "integer-modules",
)

/**
 * Safely generates an SVG with explicit output dimensions.
 *
 * Valid blank content returns an empty result.
 */
@JsExport
public fun renderStyledQrSvgForExport(
    content: String,
    output: StyledQrExportOptions,
    options: StyledQrOptions = StyledQrOptions(),
): StyledQrSvgResult {
    return try {
        val request = prepareStyledQrRequest(options)
        val rasterOptions = prepareStyledQrExport(output)

        if (content.isBlank()) {
            StyledQrSvgResult(null, null)
        } else {
            StyledQrSvgResult(
                svg = renderPreparedStyledQrSvg(
                    content = content,
                    request = request,
                    output = rasterOptions,
                ),
                error = null,
            )
        }
    } catch (error: StyledQrWebException) {
        exportFailure(
            error.code,
            error.message ?: "Unable to generate QR export",
        )
    } catch (error: Exception) {
        exportFailure(
            QrRenderErrorCode.RenderFailed,
            error.message ?: "Unable to generate QR export",
        )
    }
}

internal fun prepareStyledQrExport(
    output: StyledQrExportOptions,
): QrRasterRenderOptions {
    return try {
        val sizePx = checkedWebInteger(output.sizePx, "sizePx")
        require(sizePx > 0) {
            "sizePx must be greater than zero"
        }
        require(jsTypeOf(output.sizeMode) == "string") {
            "sizeMode must be a string"
        }

        QrRasterRenderOptions(
            sizePx = sizePx,
            sizeMode = webEnum<QrRasterSizeMode>(
                output.sizeMode,
                "sizeMode",
            ),
        )
    } catch (error: IllegalArgumentException) {
        throw StyledQrWebException(
            QrRenderErrorCode.InvalidRenderParameters,
            error.message ?: "Invalid export parameters",
            error,
        )
    }
}

private fun exportFailure(
    code: QrRenderErrorCode,
    message: String,
): StyledQrSvgResult = StyledQrSvgResult(
    svg = null,
    error = StyledQrWebError(code.name, message),
)
