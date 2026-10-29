package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrScanabilityReportTest {
    private fun qr(version: Int = 3) = QrCodeGenerator.encodeText(
        "x", QrErrorCorrectionLevel.H, minVersion = version, maxVersion = version,
    )

    @Test
    fun defaultsAndSolidWarnings() {
        val code = qr()
        val normal = code.analyzeScanability(QrStyle())
        assertTrue(normal.isRenderable)
        assertTrue(normal.errors.isEmpty())
        assertTrue(normal.warnings.isEmpty())
        assertEquals(3, normal.measurements.version)
        assertEquals(29, normal.measurements.matrixModules)
        assertEquals(QrErrorCorrectionLevel.H, normal.measurements.errorCorrection)
        assertEquals(4, normal.measurements.quietZoneModules)
        assertNull(normal.measurements.pixelsPerModule)
        assertEquals(21.0, normal.measurements.minContrastRatio!!, 1e-10)
        assertEquals(16.5, normal.measurements.contrastMargin!!, 1e-10)
        assertNull(normal.measurements.logoAffectedCodewordsInMostLoadedBlock)
        val grey = code.analyzeScanability(QrStyle(foreground = QrColor(180, 180, 180)))
        assertTrue(grey.isRenderable)
        assertEquals(listOf(QrScanabilityIssueCode.LOW_CONTRAST), grey.warnings.map { it.code })
        val inverted = code.analyzeScanability(QrStyle(foreground = QrColor.White, background = QrColor.Black))
        assertEquals(21.0, inverted.measurements.minContrastRatio!!, 1e-10)
        assertTrue(inverted.warnings.isEmpty())
    }

    @Test
    fun transparencyMakesBothContrastMeasurementsUnavailable() {
        val styles = listOf(
            QrStyle(foreground = QrColor(0, 0, 0, 0)),
            QrStyle(background = QrColor(255, 255, 255, 0)),
            QrStyle(logo = QrLogoOptions(background = QrColor(255, 255, 255, 0))),
        )
        for (style in styles) {
            val report = qr().analyzeScanability(style, logoIsRendered = true)
            assertTrue(report.isRenderable)
            assertNull(report.measurements.minContrastRatio)
            assertNull(report.measurements.contrastMargin)
            assertEquals(QrScanabilityIssueCode.CONTRAST_INDETERMINATE_TRANSPARENCY, report.warnings.single().code)
        }
    }

    @Test
    fun contrastIncludesGradientAndFinderOverride() {
        val override = QrColor(90, 90, 90)
        val style = QrStyle(
            foregroundGradient = QrLinearGradient(QrColor.Black, QrColor(25, 25, 25)),
            finderPatternAppearance = QrFinderPatternAppearance(centerColor = override),
        )
        val report = qr().analyzeScanability(style)
        assertEquals(override.contrastRatioAgainst(QrColor.White), report.measurements.minContrastRatio!!, 1e-10)
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun geometryUsesExistingFitAndIntegerRules() {
        val code = qr()
        val fit = code.analyzeScanability(QrStyle(), QrRasterRenderOptions(300, QrRasterSizeMode.Fit))
        assertEquals(300.0 / 37, fit.measurements.pixelsPerModule!!, 1e-10)
        val integer = code.analyzeScanability(QrStyle(), QrRasterRenderOptions(300))
        assertEquals(8.0, integer.measurements.pixelsPerModule!!, 1e-10)
        val invalid = code.analyzeScanability(QrStyle(), QrRasterRenderOptions(0))
        assertFalse(invalid.isRenderable)
        assertNull(invalid.measurements.pixelsPerModule)
        assertEquals(QrScanabilityIssueCode.INVALID_RENDER_PARAMETERS, invalid.errors.single().code)
        assertEquals("sizePx must be greater than zero", invalid.errors.single().message)
    }

    @Test
    fun logoMeasurementsAndFailureCodes() {
        val code = qr()
        val style = QrStyle(logo = QrLogoOptions(sizeFraction = 0.16))
        assertNull(code.analyzeScanability(style).measurements.logoAllowedCodewordsPerBlock)
        val valid = code.analyzeScanability(style, logoIsRendered = true)
        assertTrue(valid.isRenderable)
        assertEquals(6, valid.measurements.logoAffectedCodewordsInMostLoadedBlock)
        assertEquals(10, valid.measurements.logoAllowedCodewordsPerBlock)
        assertEquals(4, valid.measurements.remainingLogoCodewordHeadroom)
        val over = code.analyzeScanability(
            QrStyle(logo = QrLogoOptions(sizeFraction = 0.25, paddingFraction = 0.06)),
            logoIsRendered = true,
        )
        assertFalse(over.isRenderable)
        assertEquals(QrScanabilityIssueCode.LOGO_EXCEEDS_RS_BUDGET, over.errors.single().code)
        assertEquals(12, over.measurements.logoAffectedCodewordsInMostLoadedBlock)
        assertEquals(10, over.measurements.logoAllowedCodewordsPerBlock)
        assertEquals(-2, over.measurements.remainingLogoCodewordHeadroom)
    }

    @Test
    fun geometryErrorPrecedesOverlapAndUnavailableLogoMetricsStayNull() {
        val code = qr(7)
        val report = code.analyzeScanability(
            QrStyle(logo = QrLogoOptions(sizeFraction = 0.16)),
            QrRasterRenderOptions(0),
            logoIsRendered = true,
        )
        assertEquals(
            listOf(QrScanabilityIssueCode.INVALID_RENDER_PARAMETERS, QrScanabilityIssueCode.LOGO_OVERLAPS_FUNCTION_PATTERN),
            report.errors.map { it.code },
        )
        assertFalse(report.isRenderable)
        assertNull(report.measurements.logoAffectedCodewordsInMostLoadedBlock)
        assertNull(report.measurements.logoAllowedCodewordsPerBlock)
        assertNull(report.measurements.remainingLogoCodewordHeadroom)
    }
}
