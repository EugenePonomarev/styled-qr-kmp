package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.toSvg
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QrCodeGeneratorTest {
    @Test
    fun oneBytePayloadProducesVersionOneAtHighCorrection() {
        val code = QrCodeGenerator.encodeBytes(
            bytes = byteArrayOf(0x42),
            errorCorrection = QrErrorCorrectionLevel.H,
        )

        assertEquals(1, code.version)
        assertEquals(21, code.size)
        assertEquals(QrErrorCorrectionLevel.H, code.errorCorrection)

        // Top-left finder pattern and its required white separator.
        assertTrue(code[0, 0])
        assertTrue(code[3, 3])
        assertFalse(code[0, 7])
        assertTrue(code.isFunctionModule(0, 0))
        assertTrue(code.isLocatorModule(0, 0))

        // The timing line is structural, but is not a finder or alignment pattern.
        assertTrue(code.isFunctionModule(6, 8))
        assertFalse(code.isLocatorModule(6, 8))
    }

    @Test
    fun utf8TextWithCyrillicProducesMatrix() {
        val code = QrCodeGenerator.encodeText(
            text = "Привет, мир",
            errorCorrection = QrErrorCorrectionLevel.H,
        )

        assertTrue(code.version >= 2)
        assertEquals(code.size, code.copyModules().size)
        assertTrue(code.copyModules().any { row -> row.any { it } })
    }

    @Test
    fun longPayloadUsesVersionInformationPatterns() {
        val code = QrCodeGenerator.encodeText(
            text = "https://example.com/" + "a".repeat(900),
            errorCorrection = QrErrorCorrectionLevel.H,
        )

        assertTrue(code.version >= 7)
        assertTrue(code.isFunctionModule(0, code.size - 11))
        assertTrue(code.isFunctionModule(code.size - 11, 0))
    }

    @Test
    fun copyModulesDoesNotExposeTheInternalMatrix() {
        val code = QrCodeGenerator.encodeBytes(byteArrayOf(0x42))
        val copy = code.copyModules()
        val original = code[0, 0]

        copy[0][0] = !copy[0][0]

        assertNotEquals(copy[0][0], original)
        assertEquals(original, code[0, 0])
    }

    @Test
    fun svgContainsStyleAndLogo() {
        val code = QrCodeGenerator.encodeText("https://eugeneponomarev.com")
        val svg = code.toSvg(
            style = QrStyle(
                foreground = QrColor.fromHex("#143A5A"),
                moduleShape = QrModuleShape.Circle,
                moduleScale = 0.92,
                logo = QrLogoOptions(sizeFraction = 0.16),
            ),
            logoDataUri = "data:image/svg+xml;base64,PHN2Zy8+",
        )

        assertContains(svg, "viewBox=\"0 0 41 41\"")
        assertContains(svg, "#143A5A")
        assertContains(svg, "<circle")
        assertContains(svg, "<image href=\"data:image/svg+xml;base64,PHN2Zy8+\"")
    }

    @Test
    fun centreLogoUsesWholeModulesAndAvoidsFunctionPatterns() {
        val code = QrCodeGenerator.encodeText(
            text = "https://eugeneponomarev.com",
            errorCorrection = QrErrorCorrectionLevel.H,
        )

        val layout = code.calculateLogoLayout(QrLogoOptions(sizeFraction = 0.16))

        assertEquals(8, layout.sizeModules)
        assertEquals(1, layout.paddingModules)
        assertEquals(6, layout.logoSizeModules)
        for (row in layout.topModule until layout.topModule + layout.sizeModules) {
            for (column in layout.leftModule until layout.leftModule + layout.sizeModules) {
                assertFalse(code.isFunctionModule(row, column))
            }
        }
    }

    @Test
    fun centreLogoIsRejectedWhenItOverlapsAnAlignmentPattern() {
        val code = QrCodeGenerator.encodeText(
            text = "x",
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = 7,
            maxVersion = 7,
        )

        assertFailsWith<IllegalArgumentException> {
            code.calculateLogoLayout(QrLogoOptions(sizeFraction = 0.16))
        }
    }

    @Test
    fun timingLineCanFollowTheSelectedDataShapeWhileLocatorsStaySquare() {
        val code = QrCodeGenerator.encodeBytes(byteArrayOf(0x42))
        val timingCircle = "<circle cx=\"12.5\" cy=\"10.5\""

        val styledTiming = code.toSvg(
            style = QrStyle(
                moduleShape = QrModuleShape.Circle,
                functionPatternStyle = QrFunctionPatternStyle.PreserveFindersAndAlignment,
            ),
        )
        val preservedTiming = code.toSvg(
            style = QrStyle(
                moduleShape = QrModuleShape.Circle,
                functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
            ),
        )

        assertContains(styledTiming, timingCircle)
        assertFalse(preservedTiming.contains(timingCircle))
    }

    @Test
    fun finderPatternsAreDrawnAsComposedEyes() {
        val code = QrCodeGenerator.encodeBytes(byteArrayOf(0x42))
        val svg = code.toSvg(
            style = QrStyle(
                moduleShape = QrModuleShape.Circle,
                functionPatternStyle = QrFunctionPatternStyle.MatchDataModules,
            ),
        )

        assertContains(svg, "<circle cx=\"7.5\" cy=\"7.5\" r=\"3.5\"")
        assertContains(svg, "<circle cx=\"7.5\" cy=\"7.5\" r=\"2.5\"")
        assertContains(svg, "<circle cx=\"7.5\" cy=\"7.5\" r=\"1.5\"")
        assertFalse(svg.contains("<circle cx=\"4.5\" cy=\"4.5\" r=\"0.5\""))
    }

    @Test
    fun finderPatternShapeIsIndependentFromFunctionPatternStyle() {
        val code = QrCodeGenerator.encodeBytes(byteArrayOf(0x42))

        val svg = code.toSvg(
            style = QrStyle(
                moduleShape = QrModuleShape.Circle,
                functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
                finderPatternShape = QrFinderPatternShape.Square,
            ),
        )

        assertContains(svg, "<rect x=")
        assertFalse(svg.contains("<circle cx=\"7.5\" cy=\"7.5\" r=\"3.5\""))
    }
}
