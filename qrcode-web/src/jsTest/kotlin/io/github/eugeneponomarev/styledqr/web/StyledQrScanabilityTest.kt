package io.github.eugeneponomarev.styledqr.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyledQrScanabilityTest {
    @Test
    fun resultExposesMeasurementsAndWarnings() {
        val options = StyledQrScanabilityOptions().apply {
            style.foregroundHex = "#B4B4B4"
        }
        val result = analyzeStyledQrScanability("x", options)
        assertTrue(result.isSuccess)
        val report = assertNotNull(result.report)
        assertTrue(report.isRenderable)
        assertEquals("LOW_CONTRAST", report.warnings.single().code)
        assertNull(report.measurements.pixelsPerModule)
        assertNull(report.measurements.logoAffectedCodewordsInMostLoadedBlock)
    }

    @Test
    fun transparentAndInvalidStyleDoNotProduceFalseMeasurements() {
        val options = StyledQrScanabilityOptions().apply { style.foregroundAlpha = 0 }
        val transparent = assertNotNull(analyzeStyledQrScanability("x", options).report)
        assertNull(transparent.measurements.minContrastRatio)
        assertEquals("CONTRAST_INDETERMINATE_TRANSPARENCY", transparent.warnings.single().code)
        options.style.moduleScale = 0.1
        val invalid = analyzeStyledQrScanability("x", options)
        assertNull(invalid.report)
        assertEquals("InvalidStyle", invalid.error?.code)
        assertFalse(invalid.isSuccess)
    }

    @Test
    fun reportShowsGeometryAndLogoErrorsInOrder() {
        val options = StyledQrScanabilityOptions().apply {
            style.minVersion = 7
            style.maxVersion = 7
            style.logo = StyledQrLogoOptions()
            output = StyledQrExportOptions(0)
            logoIsRendered = true
        }
        val report = assertNotNull(analyzeStyledQrScanability("x", options).report)
        assertFalse(report.isRenderable)
        assertEquals(listOf("INVALID_RENDER_PARAMETERS", "LOGO_OVERLAPS_FUNCTION_PATTERN"),
            report.errors.map { it.code })
        assertNull(report.measurements.pixelsPerModule)
        assertNull(report.measurements.logoAllowedCodewordsPerBlock)
    }

    @Test
    fun blankAndEncodingFailureAreExplicit() {
        assertTrue(analyzeStyledQrScanability("").isEmpty)
        val options = StyledQrScanabilityOptions().apply {
            style.minVersion = 1
            style.maxVersion = 1
        }
        val result = analyzeStyledQrScanability("x".repeat(100), options)
        assertEquals("PayloadTooLong", result.error?.code)
        assertNull(result.report)
    }
}
