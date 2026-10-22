package io.github.eugeneponomarev.styledqr.web

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
                """width="7(?:\.0)?" height="7(?:\.0)?" rx="[^"]+" fill="#123C66"""",
            ).findAll(svg).count(),
        )
        assertEquals(
            3,
            Regex(
                """width="5(?:\.0)?" height="5(?:\.0)?" rx="[^"]+" fill="#FFFFFF"""",
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
                """width="7(?:\.0)?" height="7(?:\.0)?" rx="[^"]+" fill="#123C66"""",
            ).findAll(svg).count(),
        )
        assertEquals(
            3,
            Regex(
                """<circle cx="[^"]+" cy="[^"]+" r="1\.5" fill="#087E8B"/>""",
            ).findAll(svg).count(),
        )
    }
}
