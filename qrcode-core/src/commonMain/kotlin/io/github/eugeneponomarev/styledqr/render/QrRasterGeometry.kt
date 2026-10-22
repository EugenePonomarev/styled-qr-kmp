package io.github.eugeneponomarev.styledqr.render

/**
 * Controls QR module geometry inside a raster image.
 *
 * The output size is measured in physical pixels.
 */
public enum class QrRasterSizeMode {
    /** Fills the image side. Module boundaries may fall between physical pixels. */
    Fit,

    /**
     * Uses an integer number of physical pixels per module.
     *
     * The remaining space surrounds the complete QR grid, including its quiet zone.
     * An odd remaining pixel is placed on the right and bottom.
     */
    IntegerModules,
}

/**
 * Raster output parameters, separate from the visual [QrStyle].
 *
 * Construction does not validate or throw. Renderers validate these parameters
 * during rendering so that safe APIs can return InvalidRenderParameters.
 */
public class QrRasterRenderOptions(
    public val sizePx: Int,
    public val sizeMode: QrRasterSizeMode = QrRasterSizeMode.IntegerModules,
)

/**
 * Geometry in physical pixels.
 *
 * Offsets surround the complete grid, including its quiet zone.
 * Renderers must fill the entire output background before drawing this grid.
 */
internal class QrRasterGeometry(
    val sizePx: Int,
    val totalModules: Int,
    val moduleSizePx: Double,
    val offsetPx: Double,
    val trailingOffsetPx: Double,
)

/**
 * Calculates geometry after matrix size and quiet zone have been combined.
 *
 * Both modes require at least one physical pixel per module.
 */
internal fun calculateQrRasterGeometry(
    options: QrRasterRenderOptions,
    totalModules: Int,
): QrRasterGeometry {
    requireRasterParameter(
        totalModules > 0,
        "totalModules must be greater than zero",
    )
    requireRasterParameter(
        options.sizePx > 0,
        "sizePx must be greater than zero",
    )
    requireRasterParameter(
        options.sizePx >= totalModules,
        "sizePx must be at least $totalModules to represent every QR module",
    )

    return when (options.sizeMode) {
        QrRasterSizeMode.Fit -> QrRasterGeometry(
            sizePx = options.sizePx,
            totalModules = totalModules,
            moduleSizePx = options.sizePx.toDouble() / totalModules,
            offsetPx = 0.0,
            trailingOffsetPx = 0.0,
        )

        QrRasterSizeMode.IntegerModules -> {
            // Reuse the existing Android layout and its rounding convention.
            val layout = calculateRasterModuleLayout(
                sizePx = options.sizePx,
                totalModules = totalModules,
            )
            val gridSizePx = layout.moduleSizePx * totalModules
            val trailingOffsetPx =
                options.sizePx - gridSizePx - layout.offsetPx

            QrRasterGeometry(
                sizePx = options.sizePx,
                totalModules = totalModules,
                moduleSizePx = layout.moduleSizePx.toDouble(),
                offsetPx = layout.offsetPx.toDouble(),
                trailingOffsetPx = trailingOffsetPx.toDouble(),
            )
        }
    }
}

private fun requireRasterParameter(condition: Boolean, message: String) {
    if (!condition) {
        throw QrRenderException(
            QrRenderErrorCode.InvalidRenderParameters,
            message,
        )
    }
}

/**
 * UIKit drawing coordinates derived from physical pixel geometry.
 *
 * Fractional points are expected when one physical pixel occupies
 * a fraction of a point.
 */
internal class QrRasterPointGeometry(
    val sizePoints: Double,
    val moduleSizePoints: Double,
    val offsetPoints: Double,
)

internal fun QrRasterGeometry.toPointGeometry(
    effectiveScale: Double,
): QrRasterPointGeometry {
    if (!effectiveScale.isFinite() || effectiveScale <= 0.0) {
        throw QrRenderException(
            QrRenderErrorCode.InvalidRenderParameters,
            "Resolved image scale must be finite and greater than zero",
        )
    }

    val sizePoints = sizePx.toDouble() / effectiveScale
    validateQrImagePixelSide(sizePoints, effectiveScale)

    return QrRasterPointGeometry(
        sizePoints = sizePoints,
        moduleSizePoints = moduleSizePx / effectiveScale,
        offsetPoints = offsetPx / effectiveScale,
    )
}
