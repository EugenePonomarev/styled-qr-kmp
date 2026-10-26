package io.github.eugeneponomarev.styledqr.render

import kotlin.math.abs
import kotlin.math.floor

private const val SVG_NUMBER_FRACTION_DIGITS: Int = 6
private const val SVG_NUMBER_SCALE: Long = 1_000_000L

/**
 * Serializes a finite SVG/CSS scalar into the canonical decimal form shared by every KMP target.
 *
 * The result follows the grammar:
 *
 * 0 | -?(?:[1-9][0-9]*(?:\.[0-9]{1,6})?|0\.[0-9]{1,6})
 *
 * It has at most six fractional digits, trims insignificant trailing zeroes, normalizes both
 * signed zeroes to 0, and rounds
 * halfway values away from zero. It intentionally does not use platform [Double.toString]
 * implementations because their textual representation is not a cross-target contract.
 */
internal fun Double.toSvgNumber(): String {
    require(isFinite()) { "SVG number must be finite" }
    if (this == 0.0) return "0"

    val negative = this < 0.0
    val scaledMagnitude = floor(abs(this) * SVG_NUMBER_SCALE.toDouble() + 0.5)
    require(scaledMagnitude < Long.MAX_VALUE.toDouble()) {
        "SVG number is outside the supported range"
    }

    val units = scaledMagnitude.toLong()
    if (units == 0L) return "0"

    val whole = units / SVG_NUMBER_SCALE
    val fraction = units % SVG_NUMBER_SCALE

    return buildString {
        if (negative) append('-')
        append(whole)

        if (fraction != 0L) {
            append('.')
            append(
                fraction
                    .toString()
                    .padStart(SVG_NUMBER_FRACTION_DIGITS, '0')
                    .trimEnd('0'),
            )
        }
    }
}

/** Appends a canonical SVG/CSS scalar without delegating [StringBuilder] to [Double.toString]. */
internal fun StringBuilder.appendSvgNumber(value: Double): StringBuilder {
    append(value.toSvgNumber())
    return this
}
