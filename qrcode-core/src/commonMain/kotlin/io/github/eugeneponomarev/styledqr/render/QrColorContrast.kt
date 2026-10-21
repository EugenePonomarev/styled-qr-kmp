package io.github.eugeneponomarev.styledqr.render

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal const val MIN_QR_FOREGROUND_CONTRAST_RATIO: Double = 4.5

internal fun QrColor.requireOpaqueDarkForegroundAgainst(
    background: QrColor,
    description: String,
) {
    require(alpha == 255) {
        "$description must be opaque (alpha 255)"
    }
    require(background.alpha == 255) {
        "$description requires an opaque background (alpha 255)"
    }

    val foregroundLuminance = relativeLuminance()
    val backgroundLuminance = background.relativeLuminance()
    val contrast = contrastRatioAgainst(background)

    require(
        foregroundLuminance < backgroundLuminance &&
                contrast >= MIN_QR_FOREGROUND_CONTRAST_RATIO,
    ) {
        "Unsafe $description: contrast $contrast:1 against background " +
                "(minimum $MIN_QR_FOREGROUND_CONTRAST_RATIO:1; foreground must be darker)"
    }
}

internal fun QrColor.relativeLuminance(): Double =
    0.2126 * LINEARIZED_SRGB_CHANNELS[red] +
            0.7152 * LINEARIZED_SRGB_CHANNELS[green] +
            0.0722 * LINEARIZED_SRGB_CHANNELS[blue]

internal fun QrColor.contrastRatioAgainst(other: QrColor): Double {
    val first = relativeLuminance()
    val second = other.relativeLuminance()
    return (max(first, second) + 0.05) / (min(first, second) + 0.05)
}

private val LINEARIZED_SRGB_CHANNELS = DoubleArray(256) { channel ->
    val value = channel / 255.0
    if (value <= 0.04045) {
        value / 12.92
    } else {
        ((value + 0.055) / 1.055).pow(2.4)
    }
}
