package io.github.eugeneponomarev.styledqr.render

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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

/** Checks every colour reachable by [colorAt] for progress values in 0.0..1.0. */
internal fun QrLinearGradient.validateAgainst(background: QrColor) {
    require(background.alpha == 255) { "Gradient background must be opaque (alpha 255)" }
    require(startColor.alpha == 255 && endColor.alpha == 255) {
        "Gradient startColor and endColor must be opaque (alpha 255)"
    }

    val backgroundLuminance = background.relativeLuminance()
    // Every interpolated channel is at most its brighter endpoint. If that
    // (possibly unreachable) RGB corner is safe, all reachable colours are safe.
    val corner = QrColor(
        red = max(startColor.red, endColor.red),
        green = max(startColor.green, endColor.green),
        blue = max(startColor.blue, endColor.blue),
    )
    val cornerLuminance = corner.relativeLuminance()
    if (
        cornerLuminance < backgroundLuminance &&
        (backgroundLuminance + 0.05) / (cornerLuminance + 0.05) >=
        MIN_QR_FOREGROUND_CONTRAST_RATIO
    ) return

    fun check(progress: Double) {
        val color = colorAt(progress)
        val luminance = color.relativeLuminance()
        val contrast = color.contrastRatioAgainst(background)

        require(
            luminance < backgroundLuminance &&
                    contrast >= MIN_QR_FOREGROUND_CONTRAST_RATIO,
        ) {
            "Unsafe gradient at progress $progress: " +
                    "contrast $contrast:1 against background " +
                    "(minimum $MIN_QR_FOREGROUND_CONTRAST_RATIO:1; " +
                    "foreground must be darker)"
        }
    }

    check(0.0)
    check(1.0)
    var previousBits = 0L
    // A rounded channel is monotone in progress. Check the first representable
    // Double at which each channel changes; between consecutive events the
    // rounded RGB colour is constant. This includes isolated simultaneous ties
    // and colours caused by floating-point rounding near a mathematical tie.
    for (bits in quantizationBoundaries()) {
        if (bits == previousBits) continue
        check(Double.fromBits(bits))
        previousBits = bits
    }
}

private fun QrLinearGradient.quantizationBoundaries(): List<Long> = buildList {
    fun addChannel(from: Int, to: Int) {
        val changes = abs(to - from)
        for (step in 1..changes) {
            add(firstChangedProgressBits(from, to, step))
        }
    }

    addChannel(startColor.red, endColor.red)
    addChannel(startColor.green, endColor.green)
    addChannel(startColor.blue, endColor.blue)
}.sorted()

private fun firstChangedProgressBits(from: Int, to: Int, step: Int): Long {
    fun changed(bits: Long): Boolean {
        val rounded = (from + (to - from) * Double.fromBits(bits)).roundToInt()
        return if (to > from) rounded >= from + step else rounded <= from - step
    }

    // Bits of nonnegative finite Doubles increase with their numeric value.
    // The ideal half-integer crossing gives a small search window in the usual
    // case; the full-range fallback keeps the result exact on every target.
    val estimate = ((step - 0.5) / abs(to - from)).toBits()
    val endBits = 1.0.toBits()
    var low = (estimate - 64L).coerceAtLeast(0L)
    var high = (estimate + 64L).coerceAtMost(endBits)
    if (changed(low) || !changed(high)) {
        low = 0L
        high = endBits
    }
    while (low + 1 < high) {
        val middle = low + (high - low) / 2
        if (changed(middle)) high = middle else low = middle
    }
    return high
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
