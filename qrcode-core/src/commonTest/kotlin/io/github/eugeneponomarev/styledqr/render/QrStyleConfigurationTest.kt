package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.test.*

class QrStyleConfigurationTest {
    @Test
    fun defaultsMatchTheSharedStyle() {
        val result = QrStyleConfiguration().build()
        assertTrue(result.isSuccess)
        assertNull(result.error)
        assertEquals(QrStyle(), result.style)
    }

    @Test
    fun allThemeStylesCanBeCopiedWithoutChangingTheirOutput() {
        for (theme in QrThemes.all) {
            for (logo in listOf(null, theme.createLogoOptions())) {
                val original = theme.createStyle(logo)
                assertEquals(
                    original,
                    QrStyleConfiguration.fromStyle(original).build().style,
                )
            }
        }
    }

    @Test
    fun preservesEveryStyleFieldAndNestedConfiguration() {
        val original = QrStyle(
            foreground = QrColor(12, 34, 56, 128),
            background = QrColor(250, 251, 252),
            quietZoneModules = 8,
            moduleShape = QrModuleShape.Diamond,
            moduleScale = 0.77,
            roundedModuleRadiusFraction = 0.4,
            preserveFunctionPatterns = true,
            functionPatternStyle = QrFunctionPatternStyle.MatchDataModules,
            finderPatternShape = QrFinderPatternShape.Circle,
            logo = QrLogoOptions(
                0.12,
                0.01,
                QrColor(1, 2, 3, 128),
                QrLogoBackgroundShape.Circle,
                0.3,
            ),
            foregroundGradient = QrLinearGradient(
                QrColor.Black,
                QrColor(30, 40, 50),
                QrGradientPoint(0.2, 0.3),
                QrGradientPoint(0.8, 0.7),
            ),
        )

        val config = QrStyleConfiguration.fromStyle(original)
        assertEquals(original, config.build().style)

        val independent = QrStyleConfiguration.fromStyle(original)
        config.foregroundHex = "#101010"
        config.logo!!.sizeFraction = 0.2
        config.foregroundGradient!!.startX = 0.1

        assertEquals(original, independent.build().style)
        assertEquals(0.12, original.logo!!.sizeFraction)
        assertEquals(0.2, original.foregroundGradient!!.start.x)
    }

    @Test
    fun legacyDefaultAndExplicitFunctionStyleKeepTheirPriority() {
        val config = QrStyleConfiguration()
        assertEquals(
            QrFunctionPatternStyle.MatchDataModules,
            config.build().style!!.functionPatternStyle,
        )

        config.preserveFunctionPatterns = true
        assertEquals(
            QrFunctionPatternStyle.PreserveAll,
            config.build().style!!.functionPatternStyle,
        )

        config.functionPatternStyle =
            QrFunctionPatternStyle.PreserveFindersAndAlignment

        assertEquals(
            QrFunctionPatternStyle.PreserveFindersAndAlignment,
            config.build().style!!.functionPatternStyle,
        )
    }

    @Test
    fun nestedDefaultsUseTheExistingLogoAndGradientTypes() {
        val config = QrStyleConfiguration().apply {
            logo = QrLogoConfiguration()
            foregroundGradient = QrGradientConfiguration()
        }

        val style = assertNotNull(config.build().style)
        assertEquals(QrLogoOptions(), style.logo)
        assertEquals(
            QrLinearGradient(QrColor.Black, QrColor.Black),
            style.foregroundGradient,
        )
    }

    @Test
    fun invalidInputReturnsAnErrorInsteadOfThrowing() {
        val edits = listOf<(QrStyleConfiguration) -> Unit>(
            { it.foregroundHex = "invalid" },
            { it.backgroundHex = "#123" },
            { it.foregroundAlpha = -1 },
            { it.backgroundAlpha = 256 },
            { it.quietZoneModules = -1 },
            { it.moduleScale = 0.54 },
            { it.moduleScale = Double.NaN },
            { it.roundedModuleRadiusFraction = 0.51 },
            {
                it.logo = QrLogoConfiguration().apply {
                    sizeFraction = 0.26
                }
            },
            {
                it.logo = QrLogoConfiguration().apply {
                    paddingFraction = Double.POSITIVE_INFINITY
                }
            },
            {
                it.logo = QrLogoConfiguration().apply {
                    backgroundAlpha = 256
                }
            },
            {
                it.logo = QrLogoConfiguration().apply {
                    cornerRadiusFraction = -1.0
                }
            },
            {
                it.foregroundGradient = QrGradientConfiguration().apply {
                    startX = Double.NaN
                }
            },
            {
                it.foregroundGradient = QrGradientConfiguration().apply {
                    endX = 0.0
                    endY = 0.0
                }
            },
            {
                it.foregroundGradient = QrGradientConfiguration().apply {
                    startAlpha = 128
                }
            },
            {
                it.foregroundGradient = QrGradientConfiguration().apply {
                    endColorHex = "#FFFFFF"
                }
            },
            {
                it.foregroundGradient = QrGradientConfiguration()
                it.backgroundAlpha = 128
            },
        )

        for (edit in edits) {
            val config = QrStyleConfiguration()
            edit(config)

            val result = config.build()
            assertFalse(result.isSuccess)
            assertNull(result.style)
            assertEquals(
                QrRenderErrorCode.InvalidStyle,
                assertNotNull(result.error).code,
            )
            assertTrue(result.error.message.isNotBlank())
        }
    }

    @Test
    fun configurationCanRecoverAndDoesNotMutatePreviousResults() {
        val config = QrStyleConfiguration()
        val first = assertNotNull(config.build().style)

        config.moduleScale = 0.1
        assertNotNull(config.build().error)

        config.moduleScale = 0.8
        val recovered = config.build()

        assertTrue(recovered.isSuccess)
        assertNull(recovered.error)
        assertEquals(1.0, first.moduleScale)
        assertEquals(0.8, recovered.style!!.moduleScale)
    }
}
