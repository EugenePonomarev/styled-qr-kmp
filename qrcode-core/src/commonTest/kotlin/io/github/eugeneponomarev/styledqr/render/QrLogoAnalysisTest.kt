package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrLogoAnalysisTest {
    @Test
    fun realQrReturnsOriginalLayoutAndBudget() {
        val qr = realQr(3)
        val options = QrLogoOptions(sizeFraction = 0.16)
        val result = assertIs<QrLogoAnalysisResult.Success>(qr.analyzeLogoLayout(options))
        val layout = result.layout
        assertEquals(11, layout.leftModule)
        assertEquals(11, layout.topModule)
        assertEquals(7, layout.sizeModules)
        assertEquals(1, layout.paddingModules)
        assertEquals(5, layout.logoSizeModules)
        assertEquals(6, result.affectedCodewordsInMostLoadedBlock)
        assertEquals(10, result.allowedCodewordsPerBlock)
        val publicLayout = qr.calculateLogoLayout(options)
        assertEquals(layout.leftModule, publicLayout.leftModule)
        assertEquals(layout.topModule, publicLayout.topModule)
        assertEquals(layout.sizeModules, publicLayout.sizeModules)
        assertEquals(layout.paddingModules, publicLayout.paddingModules)
    }

    @Test
    fun realAlignmentPatternIsRejectedBeforeBudgetAnalysis() {
        val qr = realQr(7)
        val options = QrLogoOptions(sizeFraction = 0.16)
        assertTrue(qr.isLocatorModule(22, 22))
        val failure = assertIs<QrLogoAnalysisResult.Failure>(qr.analyzeLogoLayout(options))
        assertEquals(QrLogoAnalysisFailure.OverlapsFunctionPattern, failure.reason)
        assertNull(failure.affectedCodewordsInMostLoadedBlock)
        assertNull(failure.allowedCodewordsPerBlock)
        val message = "Centre logo overlaps a mandatory QR pattern in version 7. " +
            "Use a smaller logo or a shorter payload."
        assertEquals(message, failure.message)
        assertEquals(message, assertFailsWith<IllegalArgumentException> { qr.calculateLogoLayout(options) }.message)
    }

    @Test
    fun realQrReportsExceededBudgetAndOriginalMessage() {
        val qr = realQr(3)
        val options = QrLogoOptions(sizeFraction = 0.25, paddingFraction = 0.06)
        val failure = assertIs<QrLogoAnalysisResult.Failure>(qr.analyzeLogoLayout(options))
        assertEquals(QrLogoAnalysisFailure.ExceedsCorrectionBudget, failure.reason)
        assertEquals(12, failure.affectedCodewordsInMostLoadedBlock)
        assertEquals(10, failure.allowedCodewordsPerBlock)
        val message = "Centre logo can alter up to 12 codewords in one correction block, " +
            "but QR version 3 at H correction safely allows only 10. Reduce the logo or padding."
        assertEquals(message, failure.message)
        assertEquals(message, assertFailsWith<IllegalArgumentException> { qr.calculateLogoLayout(options) }.message)
    }

    @Test
    fun countsDistinctCodewordsPerBlockAtAndAboveBudget() {
        val options = QrLogoOptions(sizeFraction = 0.25, paddingFraction = 0.06)
        val atLimit = syntheticQr(firstBlockCount = 10).analyzeLogoLayout(options)
        val success = assertIs<QrLogoAnalysisResult.Success>(atLimit)
        // Block 0 has 10 distinct words, block 1 has 9; repeated modules must not count twice.
        assertEquals(10, success.affectedCodewordsInMostLoadedBlock)
        assertEquals(10, success.allowedCodewordsPerBlock)
        val overLimit = syntheticQr(firstBlockCount = 11).analyzeLogoLayout(options)
        val failure = assertIs<QrLogoAnalysisResult.Failure>(overLimit)
        assertEquals(QrLogoAnalysisFailure.ExceedsCorrectionBudget, failure.reason)
        assertEquals(11, failure.affectedCodewordsInMostLoadedBlock)
        assertEquals(10, failure.allowedCodewordsPerBlock)
    }

    @Test
    fun rasterGeometryErrorPrecedesLogoOverlap() {
        val qr = realQr(7)
        val style = QrStyle(logo = QrLogoOptions(sizeFraction = 0.16))
        val error = assertFailsWith<QrRenderException> {
            SvgQrRenderer.renderWithRasterOptions(
                qrCode = qr,
                options = QrRasterRenderOptions(sizePx = 0),
                style = style,
                logoDataUri = "data:image/png;base64,AA==",
            )
        }
        assertEquals(QrRenderErrorCode.InvalidRenderParameters, error.code)
        assertEquals("sizePx must be greater than zero", error.message)
    }

    private fun realQr(version: Int): QrCode = QrCodeGenerator.encodeText(
        text = "x",
        errorCorrection = QrErrorCorrectionLevel.H,
        minVersion = version,
        maxVersion = version,
    )

    // Isolates accounting: this is deliberately not a valid encoded QR matrix.
    private fun syntheticQr(firstBlockCount: Int): QrCode {
        val size = 29
        val secondBlockCount = 9
        val total = firstBlockCount + secondBlockCount
        val indexes = Array(size) { IntArray(size) { -1 } }
        var next = 0
        for (row in 9 until 20) {
            for (column in 9 until 20) {
                indexes[row][column] = next++ % total
            }
        }
        return QrCode(
            version = 3,
            errorCorrection = QrErrorCorrectionLevel.H,
            modules = Array(size) { BooleanArray(size) },
            functionModules = Array(size) { BooleanArray(size) },
            locatorModules = Array(size) { BooleanArray(size) },
            dataCodewordIndexes = indexes,
            interleavedCodewordBlockIndexes = IntArray(total) { if (it < firstBlockCount) 0 else 1 },
        )
    }
}
