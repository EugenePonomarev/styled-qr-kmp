package io.github.eugeneponomarev.styledqr.web

import kotlin.test.Test
import kotlin.test.assertFailsWith
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
}