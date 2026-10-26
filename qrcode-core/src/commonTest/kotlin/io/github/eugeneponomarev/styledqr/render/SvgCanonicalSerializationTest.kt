package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SvgCanonicalSerializationTest {

    @Test
    fun defaultSquareUsesCanonicalIntegerGeometry() {
        assertEquals(
            DEFAULT_SQUARE,
            fixtureCode().toSvg(),
        )
    }

    @Test
    fun scaledCircleRoundedSquareAndDiamondUseCanonicalFractionalGeometry() {
        assertEquals(
            SCALED_CIRCLE,
            fixtureCode().toSvg(
                QrStyle(
                    moduleShape = QrModuleShape.Circle,
                    moduleScale = 0.77,
                ),
            ),
        )
        assertEquals(
            SCALED_ROUNDED_SQUARE,
            fixtureCode().toSvg(
                QrStyle(
                    moduleShape = QrModuleShape.RoundedSquare,
                    moduleScale = 0.77,
                ),
            ),
        )
        assertEquals(
            SCALED_DIAMOND,
            fixtureCode().toSvg(
                QrStyle(
                    moduleShape = QrModuleShape.Diamond,
                    moduleScale = 0.77,
                ),
            ),
        )
    }

    @Test
    fun gradientCoordinatesAndFillUseCanonicalSerialization() {
        assertEquals(
            GRADIENT,
            fixtureCode().toSvg(
                QrStyle(
                    moduleShape = QrModuleShape.Circle,
                    moduleScale = 0.77,
                    foregroundGradient = QrLinearGradient(
                        startColor = QrColor.fromHex("#075985"),
                        endColor = QrColor.fromHex("#6D28D9"),
                        start = QrGradientPoint(0.0, 1.0),
                        end = QrGradientPoint(1.0, 0.0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun logoClipAndImageGeometryUseCanonicalSerialization() {
        assertEquals(
            LOGO_CLIP,
            fixtureCode().toSvg(
                style = QrStyle(
                    logo = QrLogoOptions(),
                ),
                logoDataUri = LOGO_DATA_URI,
            ),
        )
    }

    @Test
    fun integerModuleRasterTransformHasNoDecimalSuffix() {
        val svg = SvgQrRenderer.renderWithRasterOptions(
            qrCode = fixtureCode(),
            options = QrRasterRenderOptions(
                sizePx = 300,
                sizeMode = QrRasterSizeMode.IntegerModules,
            ),
        )

        assertEquals(RASTER_INTEGER_MODULES, svg)
        assertContains(svg, """transform="translate(5 5) scale(10)"""")
    }

    @Test
    fun fitRasterTransformUsesCanonicalFractionalScale() {
        val svg = SvgQrRenderer.renderWithRasterOptions(
            qrCode = fixtureCode(),
            options = QrRasterRenderOptions(
                sizePx = 300,
                sizeMode = QrRasterSizeMode.Fit,
            ),
        )

        assertEquals(RASTER_FIT, svg)
        assertContains(svg, """transform="translate(0 0) scale(10.344828)"""")
    }

    @Test
    fun transparentColoursUseCanonicalCssAlpha() {
        val svg = fixtureCode().toSvg(
            style = QrStyle(
                foreground = QrColor(1, 2, 3, alpha = 0),
                background = QrColor(4, 5, 6, alpha = 1),
                logo = QrLogoOptions(
                    background = QrColor(7, 8, 9, alpha = 128),
                    backgroundShape = QrLogoBackgroundShape.Circle,
                ),
            ),
            logoDataUri = LOGO_DATA_URI,
        )

        assertEquals(TRANSPARENT_COLOURS, svg)
        assertContains(svg, "rgba(1, 2, 3, 0)")
        assertContains(svg, "rgba(4, 5, 6, 0.003922)")
        assertContains(svg, "rgba(7, 8, 9, 0.501961)")
    }

    /**
     * A small deterministic matrix keeps full SVG snapshots reviewable while isolating the
     * serialization contract from encoder test coverage, which is maintained separately.
     */
    private fun fixtureCode(): QrCode {
        val modules = Array(FIXTURE_MATRIX_SIZE) {
            BooleanArray(FIXTURE_MATRIX_SIZE)
        }
        modules[9][9] = true
        modules[14][14] = true

        return QrCode(
            version = 1,
            errorCorrection = QrErrorCorrectionLevel.H,
            modules = modules,
            functionModules = Array(FIXTURE_MATRIX_SIZE) {
                BooleanArray(FIXTURE_MATRIX_SIZE)
            },
            locatorModules = Array(FIXTURE_MATRIX_SIZE) {
                BooleanArray(FIXTURE_MATRIX_SIZE)
            },
            dataCodewordIndexes = Array(FIXTURE_MATRIX_SIZE) {
                IntArray(FIXTURE_MATRIX_SIZE) { -1 }
            },
            interleavedCodewordBlockIndexes = IntArray(0),
        )
    }
}

private const val FIXTURE_MATRIX_SIZE: Int = 21
private const val LOGO_DATA_URI: String = "data:image/png;base64,AA=="

private const val DEFAULT_SQUARE: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="#FFFFFF"/><path d="M 13 13 h 1 v 1 h -1 Z M 18 18 h 1 v 1 h -1 Z " fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="18" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="19" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="20" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="18" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="19" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="20" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/></svg>"""

private const val SCALED_CIRCLE: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="#FFFFFF"/><circle cx="13.5" cy="13.5" r="0.385" fill="#000000"/><circle cx="18.5" cy="18.5" r="0.385" fill="#000000"/><circle cx="7.5" cy="7.5" r="3.5" fill="#000000"/><circle cx="7.5" cy="7.5" r="2.5" fill="#FFFFFF"/><circle cx="7.5" cy="7.5" r="1.5" fill="#000000"/><circle cx="21.5" cy="7.5" r="3.5" fill="#000000"/><circle cx="21.5" cy="7.5" r="2.5" fill="#FFFFFF"/><circle cx="21.5" cy="7.5" r="1.5" fill="#000000"/><circle cx="7.5" cy="21.5" r="3.5" fill="#000000"/><circle cx="7.5" cy="21.5" r="2.5" fill="#FFFFFF"/><circle cx="7.5" cy="21.5" r="1.5" fill="#000000"/></svg>"""

private const val SCALED_ROUNDED_SQUARE: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="#FFFFFF"/><rect x="13.115" y="13.115" width="0.77" height="0.77" rx="0.1694" fill="#000000"/><rect x="18.115" y="18.115" width="0.77" height="0.77" rx="0.1694" fill="#000000"/><rect x="4" y="4" width="7" height="7" rx="1.54" fill="#000000"/><rect x="5" y="5" width="5" height="5" rx="1.1" fill="#FFFFFF"/><rect x="6" y="6" width="3" height="3" rx="0.66" fill="#000000"/><rect x="18" y="4" width="7" height="7" rx="1.54" fill="#000000"/><rect x="19" y="5" width="5" height="5" rx="1.1" fill="#FFFFFF"/><rect x="20" y="6" width="3" height="3" rx="0.66" fill="#000000"/><rect x="4" y="18" width="7" height="7" rx="1.54" fill="#000000"/><rect x="5" y="19" width="5" height="5" rx="1.1" fill="#FFFFFF"/><rect x="6" y="20" width="3" height="3" rx="0.66" fill="#000000"/></svg>"""

private const val SCALED_DIAMOND: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="#FFFFFF"/><path d="M 13.5 13.115 L 13.885 13.5 L 13.5 13.885 L 13.115 13.5 Z" fill="#000000"/><path d="M 18.5 18.115 L 18.885 18.5 L 18.5 18.885 L 18.115 18.5 Z" fill="#000000"/><path d="M 7.5 4 L 11 7.5 L 7.5 11 L 4 7.5 Z" fill="#000000"/><path d="M 7.5 5 L 10 7.5 L 7.5 10 L 5 7.5 Z" fill="#FFFFFF"/><path d="M 7.5 6 L 9 7.5 L 7.5 9 L 6 7.5 Z" fill="#000000"/><path d="M 21.5 4 L 25 7.5 L 21.5 11 L 18 7.5 Z" fill="#000000"/><path d="M 21.5 5 L 24 7.5 L 21.5 10 L 19 7.5 Z" fill="#FFFFFF"/><path d="M 21.5 6 L 23 7.5 L 21.5 9 L 20 7.5 Z" fill="#000000"/><path d="M 7.5 18 L 11 21.5 L 7.5 25 L 4 21.5 Z" fill="#000000"/><path d="M 7.5 19 L 10 21.5 L 7.5 24 L 5 21.5 Z" fill="#FFFFFF"/><path d="M 7.5 20 L 9 21.5 L 7.5 23 L 6 21.5 Z" fill="#000000"/></svg>"""

private const val GRADIENT: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><defs><linearGradient id="qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0" gradientUnits="userSpaceOnUse" color-interpolation="sRGB" x1="4" y1="25" x2="25" y2="4"><stop offset="0%" stop-color="#075985"/><stop offset="100%" stop-color="#6D28D9"/></linearGradient></defs><rect width="29" height="29" fill="#FFFFFF"/><circle cx="13.5" cy="13.5" r="0.385" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="18.5" cy="18.5" r="0.385" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="7.5" cy="7.5" r="3.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="7.5" cy="7.5" r="2.5" fill="#FFFFFF"/><circle cx="7.5" cy="7.5" r="1.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="21.5" cy="7.5" r="3.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="21.5" cy="7.5" r="2.5" fill="#FFFFFF"/><circle cx="21.5" cy="7.5" r="1.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="7.5" cy="21.5" r="3.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/><circle cx="7.5" cy="21.5" r="2.5" fill="#FFFFFF"/><circle cx="7.5" cy="21.5" r="1.5" fill="url(#qr-foreground-21-4-7-89-133-109-40-217-0-4607182418800017408-4607182418800017408-0)"/></svg>"""

private const val LOGO_CLIP: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="#FFFFFF"/><defs><clipPath id="qr-logo-21-4-7-7-6-1-RoundedSquare-4595653203753948938" clipPathUnits="userSpaceOnUse"><path clip-rule="evenodd" d="M 0 0 H 29 V 29 H 0 Z M 12.08 11 H 15.92 A 1.08 1.08 0 0 1 17 12.08 V 15.92 A 1.08 1.08 0 0 1 15.92 17 H 12.08 A 1.08 1.08 0 0 1 11 15.92 V 12.08 A 1.08 1.08 0 0 1 12.08 11 Z"/></clipPath></defs><g clip-path="url(#qr-logo-21-4-7-7-6-1-RoundedSquare-4595653203753948938)"><path d="M 13 13 h 1 v 1 h -1 Z M 18 18 h 1 v 1 h -1 Z " fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="18" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="19" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="20" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="18" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="19" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="20" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/></g><path d="M 12.08 11 H 15.92 A 1.08 1.08 0 0 1 17 12.08 V 15.92 A 1.08 1.08 0 0 1 15.92 17 H 12.08 A 1.08 1.08 0 0 1 11 15.92 V 12.08 A 1.08 1.08 0 0 1 12.08 11 Z" fill="#FFFFFF"/><defs><clipPath id="qr-logo-21-4-7-7-6-1-RoundedSquare-4595653203753948938-image" clipPathUnits="userSpaceOnUse"><path d="M 12.08 12 H 15.92 A 0.08 0.08 0 0 1 16 12.08 V 15.92 A 0.08 0.08 0 0 1 15.92 16 H 12.08 A 0.08 0.08 0 0 1 12 15.92 V 12.08 A 0.08 0.08 0 0 1 12.08 12 Z"/></clipPath></defs><image href="data:image/png;base64,AA==" x="12" y="12" width="4" height="4" preserveAspectRatio="xMidYMid slice" clip-path="url(#qr-logo-21-4-7-7-6-1-RoundedSquare-4595653203753948938-image)"/></svg>"""

private const val RASTER_INTEGER_MODULES: String = """<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300" viewBox="0 0 300 300" role="img" aria-label="QR code"><rect width="300" height="300" fill="#FFFFFF"/><g transform="translate(5 5) scale(10)"><path d="M 13 13 h 1 v 1 h -1 Z M 18 18 h 1 v 1 h -1 Z " fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="18" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="19" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="20" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="18" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="19" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="20" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/></g></svg>"""

private const val RASTER_FIT: String = """<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300" viewBox="0 0 300 300" role="img" aria-label="QR code"><rect width="300" height="300" fill="#FFFFFF"/><g transform="translate(0 0) scale(10.344828)"><path d="M 13 13 h 1 v 1 h -1 Z M 18 18 h 1 v 1 h -1 Z " fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="18" y="4" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="19" y="5" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="20" y="6" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/><rect x="4" y="18" width="7" height="7" fill="#000000" shape-rendering="crispEdges"/><rect x="5" y="19" width="5" height="5" fill="#FFFFFF" shape-rendering="crispEdges"/><rect x="6" y="20" width="3" height="3" fill="#000000" shape-rendering="crispEdges"/></g></svg>"""

private const val TRANSPARENT_COLOURS: String = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29" role="img" aria-label="QR code"><rect width="29" height="29" fill="rgba(4, 5, 6, 0.003922)"/><defs><clipPath id="qr-logo-21-4-7-7-6-1-Circle-4595653203753948938" clipPathUnits="userSpaceOnUse"><path clip-rule="evenodd" d="M 0 0 H 29 V 29 H 0 Z M 14 11 a 3 3 0 1 1 0 6 a 3 3 0 1 1 0 -6 Z"/></clipPath></defs><g clip-path="url(#qr-logo-21-4-7-7-6-1-Circle-4595653203753948938)"><path d="M 13 13 h 1 v 1 h -1 Z M 18 18 h 1 v 1 h -1 Z " fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="4" y="4" width="7" height="7" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="5" y="5" width="5" height="5" fill="rgba(4, 5, 6, 0.003922)" shape-rendering="crispEdges"/><rect x="6" y="6" width="3" height="3" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="18" y="4" width="7" height="7" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="19" y="5" width="5" height="5" fill="rgba(4, 5, 6, 0.003922)" shape-rendering="crispEdges"/><rect x="20" y="6" width="3" height="3" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="4" y="18" width="7" height="7" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/><rect x="5" y="19" width="5" height="5" fill="rgba(4, 5, 6, 0.003922)" shape-rendering="crispEdges"/><rect x="6" y="20" width="3" height="3" fill="rgba(1, 2, 3, 0)" shape-rendering="crispEdges"/></g><path d="M 14 11 a 3 3 0 1 1 0 6 a 3 3 0 1 1 0 -6 Z" fill="rgba(7, 8, 9, 0.501961)"/><defs><clipPath id="qr-logo-21-4-7-7-6-1-Circle-4595653203753948938-image" clipPathUnits="userSpaceOnUse"><path d="M 14 12 a 2 2 0 1 1 0 4 a 2 2 0 1 1 0 -4 Z"/></clipPath></defs><image href="data:image/png;base64,AA==" x="12" y="12" width="4" height="4" preserveAspectRatio="xMidYMid slice" clip-path="url(#qr-logo-21-4-7-7-6-1-Circle-4595653203753948938-image)"/></svg>"""
