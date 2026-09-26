package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class StyledQrWebGradientTest {

    private val text =
        "https://eugeneponomarev.com"

    private val start =
        "#075985"

    private val end =
        "#6D28D9"

    @Test
    fun webExportMatchesCommonSvgForCustomGradient() {
        val actual =
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                startX = 0.0,
                startY = 1.0,
                endX = 1.0,
                endY = 0.0,
                moduleShape = "circle",
                moduleScale = 0.9,
            )

        val expected =
            QrCodeGenerator
                .encodeText(text)
                .toSvg(
                    QrStyle(
                        moduleShape =
                            QrModuleShape.Circle,
                        moduleScale = 0.9,
                        foregroundGradient =
                            QrLinearGradient(
                                startColor =
                                    QrColor.fromHex(
                                        start,
                                    ),
                                endColor =
                                    QrColor.fromHex(
                                        end,
                                    ),
                                start =
                                    QrGradientPoint(
                                        x = 0.0,
                                        y = 1.0,
                                    ),
                                end =
                                    QrGradientPoint(
                                        x = 1.0,
                                        y = 0.0,
                                    ),
                            ),
                    ),
                )

        assertEquals(
            expected,
            actual,
        )
    }

    @Test
    fun invalidGradientValuesFailWithoutSolidFallback() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = " ",
                startColor = start,
                endColor = end,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = "#GG0000",
                endColor = end,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                startX = 2.0,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                endX = 0.0,
                endY = 0.0,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = "#DDDDDD",
                endColor = end,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                moduleShape = "triangle",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                logoDataUri = "broken",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                logoDataUri = " ",
            )
        }
    }

    @Test
    fun gradientAndLogoUseTheSameCommonSafeLayout() {
        val logo =
            "data:image/png;base64,AA=="

        val actual =
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
                logoDataUri = logo,
            )

        val expected =
            QrCodeGenerator
                .encodeText(text)
                .toSvg(
                    style = QrStyle(
                        logo = QrLogoOptions(
                            background =
                                QrColor.White,
                        ),
                        foregroundGradient =
                            QrLinearGradient(
                                startColor =
                                    QrColor.fromHex(
                                        start,
                                    ),
                                endColor =
                                    QrColor.fromHex(
                                        end,
                                    ),
                            ),
                    ),
                    logoDataUri = logo,
                )

        assertEquals(
            expected,
            actual,
        )

        assertContains(
            actual,
            "<image href=\"$logo\"",
        )

        assertContains(
            actual,
            "url(#qr-foreground)",
        )
    }

    @Test
    fun gradientWithoutLogoDoesNotEmitImageElement() {
        val svg =
            generateStyledQrSvgWithLinearGradient(
                content = text,
                startColor = start,
                endColor = end,
            )

        assertFalse(
            svg.contains("<image "),
        )
    }

    @Test
    fun legacyWebExportRemainsSolid() {
        val legacy =
            generateStyledQrSvg(text)

        assertEquals(
            QrCodeGenerator
                .encodeText(text)
                .toSvg(),
            legacy,
        )

        assertFalse(
            legacy.contains("<defs>"),
        )

        assertFalse(
            legacy.contains(
                "url(#qr-foreground)",
            ),
        )
    }
}