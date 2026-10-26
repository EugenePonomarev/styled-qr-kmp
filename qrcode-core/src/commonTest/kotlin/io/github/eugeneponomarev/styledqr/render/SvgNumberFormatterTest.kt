package io.github.eugeneponomarev.styledqr.render

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SvgNumberFormatterTest {

    @Test
    fun normalizesZeroAndSerializesIntegersWithoutDecimalSuffixes() {
        assertEquals("0", 0.0.toSvgNumber())
        assertEquals("0", (-0.0).toSvgNumber())
        assertEquals("0", (-0.0000004).toSvgNumber())
        assertEquals("4", 4.0.toSvgNumber())
        assertEquals("-4", (-4.0).toSvgNumber())
        assertEquals("10", 10.0.toSvgNumber())
    }

    @Test
    fun serializesFractionsWithAtMostSixDigitsAndTrimsTrailingZeroes() {
        val serialized = listOf(
            12.5.toSvgNumber() to "12.5",
            0.77.toSvgNumber() to "0.77",
            (300.0 / 29.0).toSvgNumber() to "10.344828",
            1.2345676.toSvgNumber() to "1.234568",
            (-1.2345676).toSvgNumber() to "-1.234568",
            0.0000005.toSvgNumber() to "0.000001",
            (-0.0000005).toSvgNumber() to "-0.000001",
        )

        val grammar = Regex(
            """^(?:0|-?(?:[1-9][0-9]*(?:\.[0-9]{1,6})?|0\.[0-9]{1,6}))$""",
        )
        serialized.forEach { (actual, expected) ->
            assertEquals(expected, actual)
            assertTrue(grammar.matches(actual), "Expected canonical SVG grammar for $actual")
        }
    }

    @Test
    fun cssAlphaFractionsUseTheSameCanonicalFormatter() {
        assertEquals("rgba(1, 2, 3, 0)", QrColor(1, 2, 3, alpha = 0).toCss())
        assertEquals("rgba(1, 2, 3, 0.003922)", QrColor(1, 2, 3, alpha = 1).toCss())
        assertEquals("rgba(1, 2, 3, 0.501961)", QrColor(1, 2, 3, alpha = 128).toCss())
        assertEquals("#010203", QrColor(1, 2, 3, alpha = 255).toCss())
    }

    @Test
    fun canonicalValueStaysWithinTheFixedPrecisionErrorBound() {
        val values = listOf(
            -10.344827586206897,
            -0.77,
            -0.0000005,
            0.0000005,
            0.77,
            10.344827586206897,
            123456.7890123,
        )

        values.forEach { value ->
            val parsed = value.toSvgNumber().toDouble()

            assertTrue(
                abs(parsed - value) <= 0.0000005 + 0.000000000001,
                "Expected $parsed to stay within the canonical precision bound for $value",
            )
        }
    }

    @Test
    fun rejectsNonFiniteAndOutOfRangeValues() {
        listOf(
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.MAX_VALUE,
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                value.toSvgNumber()
            }
        }
    }
}
