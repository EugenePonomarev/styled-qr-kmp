package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgQrRendererGradientTest {

    private val code = QrCodeGenerator.encodeBytes(
        byteArrayOf(0x42),
    )

    private val gradient = QrLinearGradient(
        startColor = QrColor.fromHex("#075985"),
        endColor = QrColor.fromHex("#6D28D9"),
    )

    @Test
    fun solidOutputAndExistingThemesDoNotUseGradientMarkup() {
        val defaultSvg = code.toSvg()

        assertFalse(
            defaultSvg.contains("<defs>"),
        )
        assertFalse(
            defaultSvg.contains(
                "url(#qr-foreground)",
            ),
        )

        QrThemes.all.forEach { theme ->
            val style = theme.createStyle()
            val svg = code.toSvg(style)

            assertEquals(
                svg,
                code.toSvg(
                    style.copy(
                        foregroundGradient = null,
                    ),
                ),
            )

            assertFalse(
                svg.contains("<defs>"),
            )
            assertFalse(
                svg.contains(
                    "url(#qr-foreground)",
                ),
            )
        }
    }

    @Test
    fun gradientCoordinatesExcludeQuietZoneAndUseOneDeterministicFill() {
        val style = QrStyle(
            foreground = QrColor.fromHex("#123456"),
            foregroundGradient = gradient,
        )

        val svg = code.toSvg(style)

        val quietZone =
            style.quietZoneModules.toDouble()

        val matrixEnd =
            quietZone + code.size

        val totalModules =
            code.size +
                    style.quietZoneModules * 2

        assertEquals(
            1,
            Regex("<linearGradient ")
                .findAll(svg)
                .count(),
        )

        assertContains(
            svg,
            "gradientUnits=\"userSpaceOnUse\" " +
                    "x1=\"$quietZone\" " +
                    "y1=\"$quietZone\" " +
                    "x2=\"$matrixEnd\" " +
                    "y2=\"$matrixEnd\"",
        )

        assertContains(
            svg,
            "<stop offset=\"0%\" " +
                    "stop-color=\"#075985\"/>",
        )

        assertContains(
            svg,
            "<stop offset=\"100%\" " +
                    "stop-color=\"#6D28D9\"/>",
        )

        assertContains(
            svg,
            "fill=\"url(#qr-foreground)\"",
        )

        assertEquals(
            3,
            Regex(
                "width=\"7.0\" " +
                        "height=\"7.0\" " +
                        "fill=\"url\\(#qr-foreground\\)\"",
            ).findAll(svg).count(),
        )

        assertEquals(
            3,
            Regex(
                "width=\"3.0\" " +
                        "height=\"3.0\" " +
                        "fill=\"url\\(#qr-foreground\\)\"",
            ).findAll(svg).count(),
        )

        assertEquals(
            3,
            Regex(
                "width=\"5.0\" " +
                        "height=\"5.0\" " +
                        "fill=\"#FFFFFF\"",
            ).findAll(svg).count(),
        )

        assertContains(
            svg,
            "<rect width=\"$totalModules\" " +
                    "height=\"$totalModules\" " +
                    "fill=\"#FFFFFF\"/>",
        )

        // foregroundGradient takes precedence over the solid foreground.
        assertFalse(
            svg.contains(
                "fill=\"#123456\"",
            ),
        )

        // Rendering must be deterministic.
        assertEquals(
            svg,
            code.toSvg(style),
        )
    }

    @Test
    fun customPointsAndNonSquareModulesUseTheSameGlobalGradient() {
        val style = QrStyle(
            moduleShape = QrModuleShape.Circle,
            foregroundGradient = gradient.copy(
                start = QrGradientPoint(
                    x = 0.0,
                    y = 1.0,
                ),
                end = QrGradientPoint(
                    x = 1.0,
                    y = 0.0,
                ),
            ),
        )

        val svg = code.toSvg(style)

        val quietZone =
            style.quietZoneModules.toDouble()

        val matrixEnd =
            quietZone + code.size

        assertContains(
            svg,
            "x1=\"$quietZone\" " +
                    "y1=\"$matrixEnd\" " +
                    "x2=\"$matrixEnd\" " +
                    "y2=\"$quietZone\"",
        )

        assertContains(
            svg,
            "<circle",
        )

        assertContains(
            svg,
            "fill=\"url(#qr-foreground)\"",
        )

        assertEquals(
            1,
            Regex("<linearGradient ")
                .findAll(svg)
                .count(),
        )
    }

    @Test
    fun logoKeepsSolidBackgroundAndLeavesMatrixUntouched() {
        val withLogo = QrCodeGenerator.encodeText(
            "https://eugeneponomarev.com",
        )

        val before = withLogo.copyModules()

        val svg = withLogo.toSvg(
            style = QrStyle(
                foregroundGradient = gradient,
                logo = QrLogoOptions(
                    sizeFraction = 0.12,
                    paddingFraction = 0.01,
                    background = QrColor.fromHex(
                        "#FDFDFD",
                    ),
                ),
            ),
            logoDataUri =
                "data:image/png;base64,AA==",
        )

        val after = withLogo.copyModules()

        assertContains(
            svg,
            "fill=\"#FDFDFD\"",
        )

        assertContains(
            svg,
            "<image href=" +
                    "\"data:image/png;base64,AA==\"",
        )

        assertTrue(
            svg.contains(
                "url(#qr-foreground)",
            ),
        )

        before.indices.forEach { row ->
            assertContentEquals(
                before[row],
                after[row],
            )
        }
    }

    @Test
    fun gradientDoesNotBypassLogoAlignmentRestrictions() {
        val large = QrCodeGenerator.encodeText(
            text = "x",
            errorCorrection =
                QrErrorCorrectionLevel.H,
            minVersion = 7,
            maxVersion = 7,
        )

        assertFailsWith<IllegalArgumentException> {
            large.toSvg(
                style = QrStyle(
                    foregroundGradient = gradient,
                    logo = QrLogoOptions(),
                ),
                logoDataUri =
                    "data:image/png;base64,AA==",
            )
        }
    }
}