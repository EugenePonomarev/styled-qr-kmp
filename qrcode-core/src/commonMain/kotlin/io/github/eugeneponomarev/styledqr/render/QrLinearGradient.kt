package io.github.eugeneponomarev.styledqr.render

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val GRADIENT_VALIDATION_STEPS = 64
private const val MIN_GRADIENT_CONTRAST_RATIO = 4.5

/** Position in the QR matrix, excluding its quiet zone. Both coordinates are in 0.0..1.0. */
public data class QrGradientPoint(
    val x: Double,
    val y: Double,
) {
    init {
        require(x in 0.0..1.0) { "QrGradientPoint.x must be in 0.0..1.0" }
        require(y in 0.0..1.0) { "QrGradientPoint.y must be in 0.0..1.0" }
    }
}

/** One global foreground gradient shared by data modules and the dark finder layers. */
public data class QrLinearGradient(
    val startColor: QrColor,
    val endColor: QrColor,
    val start: QrGradientPoint = QrGradientPoint(0.0, 0.0),
    val end: QrGradientPoint = QrGradientPoint(1.0, 1.0),
) {
    init {
        // Numeric comparison also rejects +0.0 → -0.0, whose coordinates are identical.
        require(start.x != end.x || start.y != end.y) {
            "QrLinearGradient start and end points must be different"
        }
    }
}

/** Samples the sRGB interpolation used by the renderers, including both endpoints. */
internal fun QrLinearGradient.validateAgainst(background: QrColor) {
    require(background.alpha == 255) { "Gradient background must be opaque (alpha 255)" }
    require(startColor.alpha == 255 && endColor.alpha == 255) {
        "Gradient startColor and endColor must be opaque (alpha 255)"
    }

    val backgroundLuminance = background.relativeLuminance()
    for (sample in 0..GRADIENT_VALIDATION_STEPS) {
        val progress = sample / GRADIENT_VALIDATION_STEPS.toDouble()
        val color = colorAt(progress)
        val luminance = color.relativeLuminance()
        val contrast = color.contrastRatioAgainst(background)
        require(luminance < backgroundLuminance && contrast >= MIN_GRADIENT_CONTRAST_RATIO ) {
            "Unsafe gradient at progress $progress: " +
                    "contrast $contrast:1 against background " +
                    "(minimum $MIN_GRADIENT_CONTRAST_RATIO:1; " +
                    "foreground must be darker)"
        }
    }
}

internal fun QrLinearGradient.colorAt(progress: Double): QrColor {
    require(progress in 0.0..1.0) { "Gradient progress must be in 0.0..1.0" }
    fun mix(from: Int, to: Int): Int = (from + (to - from) * progress).roundToInt()
    return QrColor(
        red = mix(startColor.red, endColor.red),
        green = mix(startColor.green, endColor.green),
        blue = mix(startColor.blue, endColor.blue),
    )
}

internal fun QrColor.relativeLuminance(): Double {
    fun linearize(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(red) +
        0.7152 * linearize(green) +
        0.0722 * linearize(blue)
}

internal fun QrColor.contrastRatioAgainst(other: QrColor): Double {
    val first = relativeLuminance()
    val second = other.relativeLuminance()
    return (max(first, second) + 0.05) / (min(first, second) + 0.05)
}
