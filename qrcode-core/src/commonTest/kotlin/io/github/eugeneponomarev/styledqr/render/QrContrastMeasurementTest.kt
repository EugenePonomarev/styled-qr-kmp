package io.github.eugeneponomarev.styledqr.render

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrContrastMeasurementTest {
    @Test
    fun solidContrastIsSymmetricAndDoesNotRejectLowContrast() {
        assertEquals(21.0, QrStyle().minimumRenderedContrast(false)!!, 1e-10)
        assertEquals(21.0, QrStyle(foreground = QrColor.White, background = QrColor.Black)
            .minimumRenderedContrast(false)!!, 1e-10)
        val grey = QrStyle(foreground = QrColor(180, 180, 180))
        assertTrue(grey.minimumRenderedContrast(false)!! < 4.5)
    }

    @Test
    fun transparencyMakesMeasurementIndeterminateWhenLayerIsUsed() {
        assertNull(QrStyle(foreground = QrColor(0, 0, 0, 0)).minimumRenderedContrast(false))
        assertNull(QrStyle(background = QrColor(255, 255, 255, 128)).minimumRenderedContrast(false))
        val withLogo = QrStyle(logo = QrLogoOptions(background = QrColor(255, 255, 255, 0)))
        assertEquals(21.0, withLogo.minimumRenderedContrast(false)!!, 1e-10)
        assertNull(withLogo.minimumRenderedContrast(true))
    }

    @Test
    fun actualGradientReplacesUnusedTransparentForeground() {
        val gradient = QrLinearGradient(QrColor.Black, QrColor(25, 25, 25))
        val style = QrStyle(foreground = QrColor(0, 0, 0, 0), foregroundGradient = gradient)
        assertEquals(QrColor(25, 25, 25).contrastRatioAgainst(QrColor.White),
            style.minimumRenderedContrast(false)!!, 1e-10)
    }

    @Test
    fun finderOverridesAreIncluded() {
        val outer = QrColor(90, 90, 90)
        val style = QrStyle(
            foreground = QrColor.Black,
            finderPatternAppearance = QrFinderPatternAppearance(outerColor = outer),
        )
        assertEquals(outer.contrastRatioAgainst(QrColor.White), style.minimumRenderedContrast(false)!!, 1e-10)
    }

    @Test
    fun gradientMinimumMatchesIndependentDenseReferenceInFastPathCase() {
        val gradient = QrLinearGradient(QrColor(0, 0, 0), QrColor(90, 70, 60))
        val style = QrStyle(foregroundGradient = gradient)
        var reference = Double.POSITIVE_INFINITY
        for (index in 0..20000) {
            reference = min(reference, gradient.colorAt(index / 20000.0).contrastRatioAgainst(QrColor.White))
        }
        assertEquals(reference, style.minimumRenderedContrast(false)!!, 1e-10)
    }

    @Test
    fun exactMinimumIncludesInteriorColoursForGeneralGradient() {
        // Direct helper exercises the general algorithm; QrStyle deliberately rejects this gradient.
        val gradient = QrLinearGradient(QrColor.Black, QrColor.White)
        val background = QrColor(128, 128, 128)
        val minimum = gradient.minimumContrastAgainst(background)
        assertEquals(1.0, minimum, 1e-10)
        assertTrue(minimum < gradient.startColor.contrastRatioAgainst(background))
        assertTrue(minimum < gradient.endColor.contrastRatioAgainst(background))
    }
}
