package io.github.eugeneponomarev.styledqr.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrLinearGradientTest {

    private val blue = QrColor.fromHex("#075985")
    private val violet = QrColor.fromHex("#6D28D9")

    @Test
    fun defaultsDescribeFullDiagonalWithoutChangingLegacyStyle() {
        val gradient = QrLinearGradient(
            startColor = blue,
            endColor = violet,
        )

        assertEquals(
            QrGradientPoint(0.0, 0.0),
            gradient.start,
        )
        assertEquals(
            QrGradientPoint(1.0, 1.0),
            gradient.end,
        )

        assertNull(QrStyle().foregroundGradient)
        assertEquals(QrColor.Black, QrStyle().foreground)
        assertEquals(
            QrStyle(),
            QrStyle(foregroundGradient = null),
        )
    }

    @Test
    fun rejectsInvalidGradientPoints() {
        val invalidCoordinates = listOf(
            -0.01,
            1.01,
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
        )

        invalidCoordinates.forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                QrGradientPoint(invalid, 0.0)
            }

            assertFailsWith<IllegalArgumentException> {
                QrGradientPoint(0.0, invalid)
            }
        }
    }

    @Test
    fun rejectsCoincidentGradientPoints() {
        assertFailsWith<IllegalArgumentException> {
            QrLinearGradient(
                startColor = blue,
                endColor = violet,
                start = QrGradientPoint(0.5, 0.5),
                end = QrGradientPoint(0.5, 0.5),
            )
        }

        // +0.0 and -0.0 describe exactly the same coordinate.
        assertFailsWith<IllegalArgumentException> {
            QrLinearGradient(
                startColor = blue,
                endColor = violet,
                start = QrGradientPoint(0.0, 0.0),
                end = QrGradientPoint(-0.0, 0.0),
            )
        }
    }

    @Test
    fun rejectsTransparentBackgroundAndGradientColors() {
        val translucent = QrColor(
            red = 0,
            green = 0,
            blue = 0,
            alpha = 254,
        )

        assertFailsWith<IllegalArgumentException> {
            QrStyle(
                background = QrColor(
                    red = 255,
                    green = 255,
                    blue = 255,
                    alpha = 254,
                ),
                foregroundGradient = QrLinearGradient(
                    startColor = blue,
                    endColor = violet,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            QrStyle(
                foregroundGradient = QrLinearGradient(
                    startColor = translucent,
                    endColor = violet,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            QrStyle(
                foregroundGradient = QrLinearGradient(
                    startColor = blue,
                    endColor = translucent,
                ),
            )
        }
    }

    @Test
    fun rejectsUnsafeContrastWithDiagnostic() {
        val weak = assertFailsWith<IllegalArgumentException> {
            QrStyle(
                foregroundGradient = QrLinearGradient(
                    startColor = QrColor.White,
                    endColor = violet,
                ),
            )
        }

        assertTrue(
            weak.message.orEmpty().contains("Unsafe gradient at progress 0"),
        )
        assertTrue(
            weak.message.orEmpty().contains("contrast"),
        )
        assertTrue(
            weak.message.orEmpty().contains("minimum 4.5:1"),
        )
    }

    @Test
    fun rejectsForegroundThatIsLighterThanBackground() {
        val inverted = assertFailsWith<IllegalArgumentException> {
            QrStyle(
                background = QrColor.Black,
                foregroundGradient = QrLinearGradient(
                    startColor = blue,
                    endColor = violet,
                ),
            )
        }

        assertTrue(
            inverted.message.orEmpty().contains(
                "foreground must be darker",
            ),
        )
    }

    @Test
    fun interpolatesChannelsInSrgb() {
        val gradient = QrLinearGradient(
            startColor = blue,
            endColor = violet,
        )

        assertEquals(
            blue,
            gradient.colorAt(0.0),
        )

        assertEquals(
            QrColor(
                red = 58,
                green = 65,
                blue = 175,
            ),
            gradient.colorAt(0.5),
        )

        assertEquals(
            violet,
            gradient.colorAt(1.0),
        )
    }

    @Test
    fun rejectsInvalidInterpolationProgress() {
        listOf(
            -0.01,
            1.01,
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                QrLinearGradient(
                    startColor = blue,
                    endColor = violet,
                ).colorAt(invalid)
            }
        }
    }

    @Test
    fun safeEndpointsProduceSafeSrgbTransition() {
        val gradient = QrLinearGradient(
            startColor = blue,
            endColor = violet,
        )
        val background = QrColor.White

        (0..64).forEach { sample ->
            val progress = sample / 64.0
            val color = gradient.colorAt(progress)

            assertTrue(
                color.relativeLuminance() <
                        background.relativeLuminance(),
            )

            assertTrue(
                color.contrastRatioAgainst(background) >= 4.5,
            )
        }

        val style = QrStyle(
            background = background,
            foregroundGradient = gradient,
        )

        assertEquals(
            gradient,
            style.foregroundGradient,
        )
    }
}
