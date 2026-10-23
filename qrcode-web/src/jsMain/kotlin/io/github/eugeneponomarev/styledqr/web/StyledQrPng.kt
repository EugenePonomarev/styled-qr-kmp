@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.js.JsExport
import kotlin.js.Promise

@JsExport
public class StyledQrPngResult internal constructor(
    public val dataUri: String?,
    public val error: StyledQrWebError?,
) {
    public val isSuccess: Boolean get() = dataUri != null && error == null
    public val isEmpty: Boolean get() = dataUri == null && error == null
}

/**
 * Safely exports a PNG using the requested physical pixel dimensions.
 *
 * Successful rendering requires a browser with Canvas 2D support.
 * Expected failures resolve to a result containing an error.
 *
 * Logo images are decoded before rendering. QR styling, encoding and logo
 * placement validation use the same path as the sized SVG API.
 */
@JsExport
public fun renderStyledQrPng(
    content: String,
    output: StyledQrExportOptions,
    options: StyledQrOptions = StyledQrOptions(),
): Promise<StyledQrPngResult> {
    val svgResult = renderStyledQrSvgForExport(content, output, options)
    val svg = svgResult.svg

    if (svg == null) {
        return Promise.resolve(
            StyledQrPngResult(
                dataUri = null,
                error = svgResult.error,
            ),
        )
    }

    // Capture mutable options before any asynchronous callbacks.
    val sizePx = output.sizePx
    val logoDataUri = options.logoDataUri?.trim()

    return Promise { resolve, _ ->
        fun rasterizeSvg() {
            val source = try {
                // Generated SVG contains only ASCII markup and image base64.
                "data:image/svg+xml;base64," + window.btoa(svg)
            } catch (error: Exception) {
                resolve(
                    pngFailure(
                        QrRenderErrorCode.RenderFailed,
                        error.message ?: "Unable to prepare the generated SVG",
                    ),
                )
                return
            }

            loadPngExportImage(
                source = source,
                onLoaded = { image ->
                    resolve(createPng(image, sizePx))
                },
                onFailure = { message ->
                    resolve(
                        pngFailure(
                            QrRenderErrorCode.RenderFailed,
                            "Unable to load the generated SVG: $message",
                        ),
                    )
                },
            )
        }

        if (logoDataUri == null) {
            rasterizeSvg()
        } else {
            loadPngExportImage(
                source = logoDataUri,
                onLoaded = {
                    rasterizeSvg()
                },
                onFailure = { message ->
                    resolve(
                        pngFailure(
                            QrRenderErrorCode.LogoValidationFailed,
                            "Unable to decode the logo image: $message",
                        ),
                    )
                },
            )
        }
    }
}

private fun loadPngExportImage(
    source: String,
    onLoaded: (HTMLImageElement) -> Unit,
    onFailure: (String) -> Unit,
) {
    try {
        val image = document.createElement("img") as HTMLImageElement

        image.onload = {
            try {
                if (image.naturalWidth > 0 && image.naturalHeight > 0) {
                    onLoaded(image)
                } else {
                    onFailure("The decoded image has no pixels")
                }
            } catch (error: Exception) {
                onFailure(error.message ?: "Unable to process the decoded image")
            }
            null
        }

        image.onerror = { _, _, _, _, _ ->
            onFailure("The browser rejected the image data")
            null
        }

        image.src = source
    } catch (error: Exception) {
        onFailure(error.message ?: "Unable to load image data")
    }
}

private fun createPng(
    image: HTMLImageElement,
    sizePx: Int,
): StyledQrPngResult {
    return try {
        if (image.naturalWidth != sizePx || image.naturalHeight != sizePx) {
            return pngFailure(
                QrRenderErrorCode.RenderFailed,
                "The SVG image dimensions do not match the requested pixel size",
            )
        }

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = sizePx
        canvas.height = sizePx

        if (canvas.width != sizePx || canvas.height != sizePx) {
            return pngFailure(
                QrRenderErrorCode.InvalidRenderParameters,
                "The browser cannot create a canvas of the requested size",
            )
        }

        val renderingContext = canvas.getContext("2d")
            ?: return pngFailure(
                QrRenderErrorCode.RenderFailed,
                "Canvas 2D is unavailable",
            )

        val context = renderingContext as CanvasRenderingContext2D

        // The SVG already contains the complete geometry and background.
        context.drawImage(image, 0.0, 0.0)

        val dataUri = canvas.toDataURL("image/png")

        if (!dataUri.startsWith("data:image/png;base64,")) {
            return pngFailure(
                QrRenderErrorCode.InvalidRenderParameters,
                "The browser cannot export a PNG of the requested size",
            )
        }

        StyledQrPngResult(dataUri, null)
    } catch (error: Exception) {
        pngFailure(
            QrRenderErrorCode.RenderFailed,
            error.message ?: "Unable to export QR PNG",
        )
    }
}

private fun pngFailure(
    code: QrRenderErrorCode,
    message: String,
): StyledQrPngResult = StyledQrPngResult(
    dataUri = null,
    error = StyledQrWebError(code.name, message),
)
