package io.github.eugeneponomarev.styledqr.render

import kotlin.test.*

class QrImageRenderParametersTest {
    @Test
    fun acceptsFinitePositivePointsAndTheAutomaticScaleSentinel() {
        for (scale in listOf(0.0, 1.0, 2.0, 3.0, 0.5)) {
            validateQrImageRenderArguments(240.5, scale)
        }
    }

    @Test
    fun rejectsInvalidPointsBeforeUIKit() {
        for (points in listOf(
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
        )) {
            invalid { validateQrImageRenderArguments(points, 1.0) }
        }
    }

    @Test
    fun rejectsInvalidScaleBeforeUIKit() {
        for (scale in listOf(
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
        )) {
            invalid { validateQrImageRenderArguments(240.0, scale) }
        }
    }

    @Test
    fun moduleCountCannotOverflowAnInt() {
        assertEquals(29, qrImageTotalModules(21, 4))
        assertEquals(
            Int.MAX_VALUE,
            qrImageTotalModules(21, (Int.MAX_VALUE - 21) / 2),
        )
        invalid { qrImageTotalModules(21, Int.MAX_VALUE) }
    }

    @Test
    fun validatesTheResolvedPixelSizeWithoutImposingAnImageQualityLimit() {
        validateQrImagePixelSide(240.5, 3.0)
        validateQrImagePixelSide(4096.0, 1.0)
        validateQrImagePixelSide(1.0, 1.0)

        invalid { validateQrImagePixelSide(240.0, 0.0) }
        invalid { validateQrImagePixelSide(240.0, Double.NaN) }
        invalid { validateQrImagePixelSide(240.0, Double.MAX_VALUE) }
        invalid { validateQrImagePixelSide(Double.MAX_VALUE, 1.0) }
        invalid { validateQrImagePixelSide(240.0, 0.00001) }
    }

    private fun invalid(block: () -> Unit) {
        val error = assertFailsWith<QrRenderException>(block = block)
        assertEquals(QrRenderErrorCode.InvalidRenderParameters, error.code)
        assertTrue(error.message.orEmpty().isNotBlank())
    }
}
