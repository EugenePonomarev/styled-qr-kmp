package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerShape
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.khronos.webgl.get
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement

class StyledQrOptionsTest {
    private val content = "https://eugeneponomarev.com"

    // Real PNG, 40×20: red edges and a green central square.
    private val logoUri =
        "data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAKUlEQVR4nGP4z8CAB+GTI4QoMnnU4lGLRy0etXjU4lGLRy0etXg4WQwA72wdDlLL0dYAAAAASUVORK5CYII="

    @Test
    fun omittedOverridesMatchSharedDefaults() {
        assertEquals(QrStyle(), resolveStyledQrStyle(StyledQrOptions()))
        assertEquals(
            generateStyledQrSvg(content),
            generateStyledQrSvgWithOptions(content),
        )
    }

    @Test
    fun mapsAllModuleFunctionAndFinderShapes() {
        for (module in QrModuleShape.entries) {
            for (function in QrFunctionPatternStyle.entries) {
                for (finder in QrFinderPatternShape.entries) {
                    val options = StyledQrOptions().apply {
                        moduleShape = module.name
                        functionPatternStyle = function.name
                        finderPatternShape = finder.name
                    }

                    assertEquals(
                        QrStyle(
                            moduleShape = module,
                            functionPatternStyle = function,
                            finderPatternShape = finder,
                        ),
                        resolveStyledQrStyle(options),
                    )
                }
            }
        }
    }

    @Test
    fun fullOptionsMatchTheCorrespondingSharedStyleAndSvg() {
        val options = StyledQrOptions().apply {
            foregroundHex = "#143A5A"
            backgroundHex = "#FFF0CC"
            quietZoneModules = 6
            moduleShape = "rounded-square"
            moduleScale = 0.84
            roundedModuleRadiusFraction = 0.31
            functionPatternStyle = "preserve-finders-and-alignment"
            finderPatternShape = "diamond"
            finderOuterShape = "circle"
            finderCenterShape = "square"
            finderOuterColorHex = "#123C66"
            finderCenterColorHex = "#0F4C5C"
            foregroundGradient = StyledQrGradientOptions(
                "#075985",
                "#6D28D9",
            ).apply {
                startY = 0.5
                endY = 0.5
            }
            logoDataUri = logoUri
            logo = StyledQrLogoOptions().apply {
                sizeFraction = 0.16
                paddingFraction = 0.04
                backgroundHex = "#DDEEFF"
                backgroundAlpha = 128
                backgroundShape = "circle"
                cornerRadiusFraction = 0.4
            }
            minVersion = 4
            maxVersion = 4
        }

        val expected = QrStyle(
            foreground = QrColor.fromHex("#143A5A"),
            background = QrColor.fromHex("#FFF0CC"),
            quietZoneModules = 6,
            moduleShape = QrModuleShape.RoundedSquare,
            moduleScale = 0.84,
            roundedModuleRadiusFraction = 0.31,
            functionPatternStyle =
                QrFunctionPatternStyle.PreserveFindersAndAlignment,
            finderPatternShape = QrFinderPatternShape.Diamond,
            finderPatternAppearance = QrFinderPatternAppearance(
                outerShape = QrFinderPatternLayerShape.Circle,
                centerShape = QrFinderPatternLayerShape.Square,
                outerColor = QrColor.fromHex("#123C66"),
                centerColor = QrColor.fromHex("#0F4C5C"),
            ),
            foregroundGradient = QrLinearGradient(
                QrColor.fromHex("#075985"),
                QrColor.fromHex("#6D28D9"),
                QrGradientPoint(0.0, 0.5),
                QrGradientPoint(1.0, 0.5),
            ),
            logo = QrLogoOptions(
                sizeFraction = 0.16,
                paddingFraction = 0.04,
                background = QrColor(221, 238, 255, 128),
                backgroundShape = QrLogoBackgroundShape.Circle,
                cornerRadiusFraction = 0.4,
            ),
        )

        assertEquals(expected, resolveStyledQrStyle(options))
        assertEquals(
            QrCodeGenerator.encodeText(
                content,
                minVersion = 4,
                maxVersion = 4,
            ).toSvg(expected, logoUri),
            generateStyledQrSvgWithOptions(content, options),
        )
    }

    @Test
    fun everyThemeUsesSharedDefaultsAndExplicitOverrides() {
        assertEquals(7, QrThemes.all.size)

        for (theme in QrThemes.all) {
            val options = StyledQrOptions().apply {
                themeId = theme.id.name
            }
            assertEquals(
                theme.createStyle(),
                resolveStyledQrStyle(options),
            )

            options.preserveFunctionPatterns = false
            options.roundedModuleRadiusFraction = 0.0

            assertEquals(
                theme.createStyle().copy(
                    preserveFunctionPatterns = false,
                    functionPatternStyle =
                        QrFunctionPatternStyle.MatchDataModules,
                    roundedModuleRadiusFraction = 0.0,
                ),
                resolveStyledQrStyle(options),
            )

            val withLogo = StyledQrOptions().apply {
                themeId = theme.id.name
                logoDataUri = logoUri
                logo = StyledQrLogoOptions().apply {
                    paddingFraction = 0.0
                }
            }

            assertEquals(
                theme.createStyle(
                    logo = theme.createLogoOptions().copy(
                        paddingFraction = 0.0,
                    ),
                ),
                resolveStyledQrStyle(withLogo),
            )
        }
    }

    @Test
    fun alphaIsAvailableWhereTheSharedModelPermitsIt() {
        val options = StyledQrOptions().apply {
            foregroundAlpha = 180
            backgroundAlpha = 0
        }

        assertEquals(
            QrStyle(
                foreground = QrColor(0, 0, 0, 180),
                background = QrColor(255, 255, 255, 0),
            ),
            resolveStyledQrStyle(options),
        )
    }

    @Test
    fun newLogoDefaultsStayWhiteAndLegacyDefaultsStayQrColored() {
        val options = StyledQrOptions().apply {
            backgroundHex = "#FFF0CC"
            logoDataUri = logoUri
        }
        val actual = generateStyledQrSvgWithOptions(content, options)

        assertEquals(
            QrCodeGenerator.encodeText(content).toSvg(
                QrStyle(
                    background = QrColor.fromHex("#FFF0CC"),
                    logo = QrLogoOptions(),
                ),
                logoUri,
            ),
            actual,
        )
        assertNotEquals(
            generateStyledQrSvgWithLogo(
                content = content,
                logoDataUri = logoUri,
                background = "#FFF0CC",
            ),
            actual,
        )
    }

    @Test
    fun rejectsConflictingClearFlagsAndUnsafeColors() {
        assertFailsWith<IllegalArgumentException> {
            resolveStyledQrStyle(StyledQrOptions().apply {
                clearForegroundGradient = true
                foregroundGradient = StyledQrGradientOptions(
                    "#000000",
                    "#143A5A",
                )
            })
        }

        assertFailsWith<IllegalArgumentException> {
            resolveStyledQrStyle(StyledQrOptions().apply {
                clearFinderOuterColor = true
                finderOuterColorHex = "#143A5A"
            })
        }

        assertFailsWith<IllegalArgumentException> {
            resolveStyledQrStyle(StyledQrOptions().apply {
                backgroundHex = "#FFF0CC"
                finderCenterColorHex = "#087E8B"
            })
        }
    }

    @Test
    fun rejectsInvalidVersionBoundsAndHonoursTheMaximumVersion() {
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithOptions(
                "x",
                StyledQrOptions().apply { minVersion = 0 },
            )
        }
        assertFailsWith<IllegalArgumentException> {
            generateStyledQrSvgWithOptions(
                "a".repeat(100),
                StyledQrOptions().apply { maxVersion = 1 },
            )
        }
    }

    @Test
    fun browserActuallyDisplaysTheEmbeddedRectangularPng(): Promise<Unit> {
        val options = StyledQrOptions().apply {
            logoDataUri = logoUri
            minVersion = 4
            maxVersion = 4
        }
        val svg = generateStyledQrSvgWithOptions(content, options)
            .replace("<svg ", "<svg width=\"410\" height=\"410\" ")

        val image = document.createElement("img") as HTMLImageElement

        return Promise { resolve, reject ->
            image.onload = {
                try {
                    assertEquals(410, image.naturalWidth)
                    assertEquals(410, image.naturalHeight)

                    val canvas =
                        document.createElement("canvas") as HTMLCanvasElement
                    canvas.width = 410
                    canvas.height = 410

                    val context =
                        canvas.getContext("2d") as CanvasRenderingContext2D
                    context.drawImage(image, 0.0, 0.0)

                    val pixel = context.getImageData(
                        205.0,
                        205.0,
                        1.0,
                        1.0,
                    ).data

                    assertEquals(0, pixel[0].toInt() and 0xFF)
                    assertEquals(255, pixel[1].toInt() and 0xFF)
                    assertEquals(0, pixel[2].toInt() and 0xFF)
                    assertEquals(255, pixel[3].toInt() and 0xFF)
                    resolve(Unit)
                } catch (error: Throwable) {
                    reject(error)
                }
                null
            }
            image.onerror = { _, _, _, _, _ ->
                reject(IllegalStateException("Unable to load the generated SVG"))
                null
            }
            image.src = "data:image/svg+xml;base64," + window.btoa(svg)
        }
    }
}
