package io.github.eugeneponomarev.styledqr.web

import kotlin.test.Test
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
}
