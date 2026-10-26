package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import io.github.eugeneponomarev.styledqr.theme.QrThemeId
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyledQrWebTest {

    @Test
    fun generatesSvgForSupportedOptions() {
        val svg = generateStyledQrSvg(
            content = "https://eugeneponomarev.com",
            errorCorrection = "H",
            foreground = "#143A5A",
            background = "#FFFFFF",
            moduleShape = "rounded-square",
            moduleScale = 0.88,
        )

        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
    }

    @Test
    fun rejectsBlankContent() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvg(content = "")
        }
    }

    @Test
    fun rejectsUnsupportedModuleShape() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvg(
                content = "https://eugeneponomarev.com",
                moduleShape = "triangle",
            )
        }
    }

    @Test
    fun defaultsToFullyStyledFunctionPatterns() {
        val svg = generateStyledQrSvg(
            content = "https://eugeneponomarev.com",
            moduleShape = "diamond",
        )

        assertFalse(svg.contains("shape-rendering=\"crispEdges\""))
    }

    @Test
    fun allowsConservativeFunctionPatterns() {
        val svg = generateStyledQrSvg(
            content = "https://eugeneponomarev.com",
            moduleShape = "diamond",
            functionPatternStyle = "preserve-all",
        )

        assertTrue(svg.contains("shape-rendering=\"crispEdges\""))
    }

    @Test
    fun rejectsUnsupportedFunctionPatternStyle() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvg(
                content = "https://eugeneponomarev.com",
                functionPatternStyle = "unsupported",
            )
        }
    }

    @Test
    fun preservesAlignmentPatternsWhenRequested() {
        val svg = generateStyledQrSvg(
            content = "https://eugeneponomarev.com",
            moduleShape = "diamond",
            functionPatternStyle = "preserve-finders-and-alignment",
        )

        assertTrue(svg.contains("shape-rendering=\"crispEdges\""))
    }

    @Test
    fun finderOverridesUseIndependentShapesAndSolidColors() {
        val svg = generateStyledQrSvg(
            content = "https://eugeneponomarev.com",
            finderOuterShape = "rounded-square",
            finderCenterShape = "circle",
            finderOuterColor = "#123C66",
            finderCenterColor = "#087E8B",
        )

        assertEquals(
            3,
            Regex(
                """width="7" height="7" rx="[^"]+" fill="#123C66"""",
            ).findAll(svg).count(),
        )
        assertEquals(
            3,
            Regex(
                """width="5" height="5" rx="[^"]+" fill="#FFFFFF"""",
            ).findAll(svg).count(),
        )
        assertEquals(
            3,
            Regex(
                """<circle cx="[^"]+" cy="[^"]+" r="1\.5" fill="#087E8B"/>""",
            ).findAll(svg).count(),
        )
    }

    @Test
    fun rejectsUnsupportedFinderLayerShape() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvg(
                content = "https://eugeneponomarev.com",
                finderCenterShape = "shield",
            )
        }
    }

    @Test
    fun rejectsUnsafeFinderOverrideColor() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvg(
                content = "https://eugeneponomarev.com",
                finderOuterColor = "#FFFFFF",
            )
        }
    }

    @Test
    fun finderOverridesAlsoApplyWithLogo() {
        val logo = "data:image/png;base64,AA=="

        val svg = generateStyledQrSvgWithLogo(
            content = "https://eugeneponomarev.com",
            logoDataUri = logo,
            finderOuterShape = "rounded-square",
            finderCenterShape = "circle",
            finderOuterColor = "#123C66",
            finderCenterColor = "#087E8B",
        )

        assertTrue(svg.contains("""<image href="$logo""""))

        assertEquals(
            3,
            Regex(
                """width="7" height="7" rx="[^"]+" fill="#123C66"""",
            ).findAll(svg).count(),
        )
        assertEquals(
            3,
            Regex(
                """<circle cx="[^"]+" cy="[^"]+" r="1\.5" fill="#087E8B"/>""",
            ).findAll(svg).count(),
        )
    }

    @Test
    fun customLogoOverridesUseTheCommonLogoLayout() {
        val content = "https://eugeneponomarev.com"
        val logo = "data:image/png;base64,AA=="

        val actual = generateStyledQrSvgWithLogo(
            content = content,
            logoDataUri = logo,
            logoSizeFraction = 0.05,
            logoPaddingFraction = 0.0,
        )

        val expected = QrCodeGenerator
            .encodeText(content)
            .toSvg(
                style = QrStyle(
                    logo = QrLogoOptions(
                        sizeFraction = 0.05,
                        paddingFraction = 0.0,
                        background = QrColor.White,
                    ),
                ),
                logoDataUri = logo,
            )

        assertEquals(expected, actual)
    }

    @Test
    fun customLogoOverrideKeepsTheDefaultForTheOmittedField() {
        val content = "https://eugeneponomarev.com"
        val logo = "data:image/png;base64,AA=="

        val onlySize = generateStyledQrSvgWithLogo(
            content = content,
            logoDataUri = logo,
            logoSizeFraction = 0.05,
        )

        val expectedOnlySize = QrCodeGenerator
            .encodeText(content)
            .toSvg(
                style = QrStyle(
                    logo = QrLogoOptions(
                        sizeFraction = 0.05,
                        background = QrColor.White,
                    ),
                ),
                logoDataUri = logo,
            )

        val onlyPadding = generateStyledQrSvgWithLogo(
            content = content,
            logoDataUri = logo,
            logoPaddingFraction = 0.0,
        )

        val expectedOnlyPadding = QrCodeGenerator
            .encodeText(content)
            .toSvg(
                style = QrStyle(
                    logo = QrLogoOptions(
                        paddingFraction = 0.0,
                        background = QrColor.White,
                    ),
                ),
                logoDataUri = logo,
            )

        assertEquals(expectedOnlySize, onlySize)
        assertEquals(expectedOnlyPadding, onlyPadding)
    }

    @Test
    fun themedLogoOverridesKeepTheThemeLogoAppearance() {
        val content = "https://eugeneponomarev.com"
        val logo = "data:image/png;base64,AA=="
        val themeId = getStyledQrThemeIds().first()
        val id = QrThemeId.entries.first {
            it.name.equals(themeId, ignoreCase = true)
        }
        val theme = QrThemes.get(id)

        val actual = generateThemedQrSvgWithLogo(
            content = content,
            themeId = themeId,
            logoDataUri = logo,
            logoSizeFraction = 0.05,
            logoPaddingFraction = 0.0,
        )

        val expected = QrCodeGenerator
            .encodeText(content)
            .toSvg(
                style = theme.createStyle(
                    logo = theme.createLogoOptions().copy(
                        sizeFraction = 0.05,
                        paddingFraction = 0.0,
                    ),
                ),
                logoDataUri = logo,
            )

        assertEquals(expected, actual)
    }

    @Test
    fun rejectsInvalidWebLogoOverrides() {
        val content = "https://eugeneponomarev.com"
        val logo = "data:image/png;base64,AA=="

        for (size in listOf(0.049, 0.251, Double.NaN)) {
            assertFailsWith<IllegalArgumentException> {
                generateStyledQrSvgWithLogo(
                    content = content,
                    logoDataUri = logo,
                    logoSizeFraction = size,
                )
            }
        }

        for (padding in listOf(-0.01, 0.121, Double.NaN)) {
            assertFailsWith<IllegalArgumentException> {
                generateStyledQrSvgWithLogo(
                    content = content,
                    logoDataUri = logo,
                    logoPaddingFraction = padding,
                )
            }
        }
    }
}
