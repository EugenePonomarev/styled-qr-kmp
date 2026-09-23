package io.github.eugeneponomarev.styledqr.theme

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class QrThemesTest {

    @Test
    fun catalogContainsEveryThemeExactlyOnce() {
        val themes = QrThemes.all

        assertEquals(7, themes.size)
        assertEquals(QrThemeId.entries.toSet(), themes.map { it.id }.toSet())

        themes.forEach { theme ->
            assertSame(theme, QrThemes.get(theme.id))
        }
    }

    @Test
    fun presetsHaveOpaqueContrastingColorsAndFourModuleQuietZone() {
        QrThemes.all.forEach { theme ->
            val style = theme.createStyle()

            assertEquals(255, style.foreground.alpha, theme.id.name)
            assertEquals(255, style.background.alpha, theme.id.name)
            assertEquals(4, style.quietZoneModules, theme.id.name)
            assertTrue(style.moduleScale in 0.82..0.94, theme.id.name)
            assertNull(style.logo, theme.id.name)

            val foreground = luminance(style.foreground)
            val background = luminance(style.background)

            assertTrue(background > foreground, theme.id.name)
            assertTrue(
                (background + 0.05) / (foreground + 0.05) >= 4.5,
                theme.id.name,
            )
        }
    }

    @Test
    fun conservativeThemesExplicitlyPreserveSquareFinders() {
        listOf(QrThemes.Fintech, QrThemes.Minimal)
            .forEach { theme ->
                val style = theme.createStyle()

                assertEquals(
                    QrFinderPatternShape.Square,
                    style.finderPatternShape,
                    theme.id.name,
                )
                assertEquals(
                    QrFunctionPatternStyle.PreserveAll,
                    style.functionPatternStyle,
                    theme.id.name,
                )
            }
    }

    @Test
    fun styleCustomizationDoesNotChangeThePreset() {
        val original = QrThemes.Fintech.createStyle()
        val changed = original.copy(
            foreground = QrColor.Black,
            moduleScale = 1.0,
        )

        assertNotEquals(original, changed)
        assertEquals(original, QrThemes.Fintech.createStyle())
    }

    @Test
    fun logoDefaultsMatchTheThemeAndExplicitOptionsArePreserved() {
        QrThemes.all.forEach { theme ->
            val defaults = theme.createLogoOptions()
            val custom = defaults.copy(
                sizeFraction = 0.12,
                paddingFraction = 0.01,
                background = QrColor.fromHex("#FFF9F7"),
            )

            assertEquals(theme.createStyle().background, defaults.background)
            assertEquals(custom, theme.createStyle(logo = custom).logo)
            assertEquals(defaults, theme.createLogoOptions())
        }
    }

    @Test
    fun logoOptionsWithoutAnImageDoNotReserveAnEmptyArea() {
        val code = QrCodeGenerator.encodeText("https://eugeneponomarev.com")

        QrThemes.all.forEach { theme ->
            assertEquals(
                code.toSvg(style = theme.createStyle()),
                code.toSvg(
                    style = theme.createStyle(logo = theme.createLogoOptions()),
                ),
                theme.id.name,
            )
        }
    }

    @Test
    fun renderingSupportsAnAllowedLogoWithoutChangingTheMatrix() {
        val code = QrCodeGenerator.encodeText(
            text = "https://eugeneponomarev.com",
            errorCorrection = QrErrorCorrectionLevel.H,
        )
        val originalModules = code.copyModules()

        QrThemes.all.forEach { theme ->
            val options = theme.createLogoOptions().copy(
                sizeFraction = 0.12,
                paddingFraction = 0.01,
            )
            val svg = code.toSvg(
                style = theme.createStyle(logo = options),
                logoDataUri = TEST_LOGO,
            )

            assertContains(svg, "<image href=")

            val currentModules = code.copyModules()
            originalModules.indices.forEach { row ->
                assertContentEquals(
                    originalModules[row],
                    currentModules[row],
                    "${theme.id}: row $row",
                )
            }
        }
    }

    @Test
    fun renderingRejectsLogoOverlappingAnAlignmentPattern() {
        val code = QrCodeGenerator.encodeText(
            text = "x",
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = 7,
            maxVersion = 7,
        )

        QrThemes.all.forEach { theme ->
            assertFailsWith<IllegalArgumentException> {
                code.toSvg(
                    style = theme.createStyle(logo = theme.createLogoOptions()),
                    logoDataUri = TEST_LOGO,
                )
            }
        }
    }

    private fun luminance(color: QrColor): Double =
        0.2126 * linearChannel(color.red) +
                0.7152 * linearChannel(color.green) +
                0.0722 * linearChannel(color.blue)

    private fun linearChannel(value: Int): Double {
        val normalized = value / 255.0

        return if (normalized <= 0.04045) {
            normalized / 12.92
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4)
        }
    }

    private companion object {
        const val TEST_LOGO = "data:image/svg+xml;base64,PHN2Zy8+"
    }
}