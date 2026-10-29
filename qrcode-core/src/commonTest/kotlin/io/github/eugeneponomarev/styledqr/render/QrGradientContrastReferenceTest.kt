package io.github.eugeneponomarev.styledqr.render

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Test reference independently evaluates the RGB formula near mathematical channel crossings. */
class QrGradientContrastReferenceTest {
    @Test
    fun minimumMatchesReferenceForSafeAndAdversarialGradients() {
        val cases = listOf(
            // Validator's RGB-corner fast path succeeds.
            QrLinearGradient(QrColor(0, 0, 0), QrColor(90, 70, 60)),
            // The unreachable RGB corner is unsafe, so validation enumerates transitions.
            QrLinearGradient(QrColor(154, 25, 131), QrColor(16, 137, 17)),
            // Simultaneous opposite-direction rounding, including a transient third colour.
            // This gradient is intentionally measured directly: QrStyle rejects it.
            QrLinearGradient(QrColor(235, 3, 38), QrColor(238, 0, 38)),
            // Contrast's minimum occurs inside the interpolation, not at an endpoint.
            QrLinearGradient(QrColor.Black, QrColor.White),
        )
        val backgrounds = listOf(QrColor.White, QrColor.White, QrColor.White, QrColor(128, 128, 128))
        cases.zip(backgrounds).forEach { (gradient, background) ->
            assertEquals(referenceMinimum(gradient, background), gradient.minimumContrastAgainst(background), 1e-10)
        }
        // Both paths are exercised with styles supported by the renderer.
        cases.take(2).forEach { gradient ->
            val measured = QrStyle(foregroundGradient = gradient).minimumRenderedContrast(false)!!
            assertEquals(referenceMinimum(gradient, QrColor.White), measured, 1e-10)
        }
        val tie = cases[2]
        val tieMinimum = referenceMinimum(tie, QrColor.White)
        assertTrue(tieMinimum < independentRatio(tie.startColor, QrColor.White))
        assertTrue(tieMinimum < independentRatio(tie.endColor, QrColor.White))
    }

    private fun referenceMinimum(gradient: QrLinearGradient, background: QrColor): Double {
        val start = intArrayOf(gradient.startColor.red, gradient.startColor.green, gradient.startColor.blue)
        val end = intArrayOf(gradient.endColor.red, gradient.endColor.green, gradient.endColor.blue)
        var result = Double.POSITIVE_INFINITY
        fun evaluate(progress: Double) {
            if (progress !in 0.0..1.0) return
            val rgb = IntArray(3) { i -> (start[i] + (end[i] - start[i]) * progress).roundToInt() }
            result = min(result, independentRatio(QrColor(rgb[0], rgb[1], rgb[2]), background))
        }
        evaluate(0.0)
        evaluate(1.0)
        // These samples straddle each ideal half-integer crossing and nearby Double values.
        // They do not call quantizationBoundaries(), firstChangedProgressBits() or colorAt().
        for (channel in 0..2) {
            val changes = abs(end[channel] - start[channel])
            for (step in 1..changes) {
                val crossing = (step - 0.5) / changes
                val bits = crossing.toBits()
                for (offset in -128L..128L) {
                    val candidate = bits + offset
                    if (candidate >= 0L && candidate <= 1.0.toBits()) evaluate(Double.fromBits(candidate))
                }
            }
        }
        return result
    }

    private fun independentRatio(first: QrColor, second: QrColor): Double {
        fun luminance(color: QrColor): Double {
            fun linear(channel: Int): Double {
                val srgb = channel / 255.0
                return if (srgb <= 0.04045) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
        }
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}
