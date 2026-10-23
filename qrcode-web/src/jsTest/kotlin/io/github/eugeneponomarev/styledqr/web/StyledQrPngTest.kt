package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import kotlinx.browser.document
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StyledQrPngTest {
    @Test
    fun integerGridAt300Pixels(): Promise<Unit> =
        checkFinderBorder(
            sizePx = 300,
            matrixStart = 45,
            finderEnd = 115,
            sampleCoordinate = 50,
        )

    @Test
    fun exactMultipleAt290Pixels(): Promise<Unit> =
        checkFinderBorder(
            sizePx = 290,
            matrixStart = 40,
            finderEnd = 110,
            sampleCoordinate = 45,
        )

    @Test
    fun oddOutputAt301Pixels(): Promise<Unit> =
        checkFinderBorder(
            sizePx = 301,
            matrixStart = 45,
            finderEnd = 115,
            sampleCoordinate = 50,
        )

    @Test
    fun fitUsesFractionalModules(): Promise<Unit> =
        verifyPng(
            output = StyledQrExportOptions(300, "fit"),
        ) { context ->
            assertPixel(context, 43, 46, black)
            assertPixel(context, 39, 46, white)
        }

    @Test
    fun preservesBackgroundAlpha(): Promise<Unit> =
        verifyPng(
            options = versionOneOptions().apply {
                backgroundHex = "#FF0000"
                backgroundAlpha = 128
            },
        ) { context ->
            val expected = listOf(255, 0, 0, 128)

            assertPixel(context, 0, 0, expected)
            assertPixel(context, 20, 20, expected)
        }

    @Test
    fun rendersSquareLogoBackground(): Promise<Unit> =
        checkLogo("square")

    @Test
    fun rendersRoundedLogoBackground(): Promise<Unit> =
        checkLogo("rounded-square")

    @Test
    fun rendersCircularLogoBackground(): Promise<Unit> =
        checkLogo("circle")

    @Test
    fun undecodableLogoReturnsLogoValidationFailed(): Promise<Unit> {
        val options = StyledQrOptions().apply {
            minVersion = 4
            maxVersion = 4

            // Valid data-URI syntax, invalid image bytes.
            logoDataUri = "data:image/png;base64,AA=="
        }

        return renderStyledQrPng(
            content = "x",
            output = StyledQrExportOptions(410),
            options = options,
        ).then<Unit> { result ->
            assertFalse(result.isSuccess)
            assertFalse(result.isEmpty)
            assertEquals(null, result.dataUri)
            assertEquals("LogoValidationFailed", result.error?.code)
            assertTrue(assertNotNull(result.error).message.isNotBlank())
        }
    }

    @Test
    fun blankInputReturnsEmptyWithoutDecodingLogo(): Promise<Unit> {
        val options = StyledQrOptions().apply {
            logoDataUri = "data:image/png;base64,AA=="
        }

        return renderStyledQrPng(
            content = "",
            output = StyledQrExportOptions(300),
            options = options,
        ).then<Unit> { result ->
            assertTrue(result.isEmpty)
            assertFalse(result.isSuccess)
            assertEquals(null, result.dataUri)
            assertEquals(null, result.error)
        }
    }

    @Test
    fun validatesOutputBeforeReturningEmpty(): Promise<Unit> =
        renderStyledQrPng(
            content = "",
            output = StyledQrExportOptions(0),
        ).then<Unit> { result ->
            assertFalse(result.isEmpty)
            assertEquals(null, result.dataUri)
            assertEquals("InvalidRenderParameters", result.error?.code)
        }

    @Test
    fun exportGeometryErrorsPrecedeLogoErrorsThroughTheSharedSvgPath(): Promise<Unit> {
        val checks = listOf("integer-modules", "fit").flatMap { mode ->
            val cases = listOf<Triple<Int, String?, String>>(
                Triple(28, rectangularLogo, "InvalidRenderParameters"),
                Triple(300, rectangularLogo, "LogoValidationFailed"),
                Triple(28, null, "InvalidRenderParameters"),
                Triple(28, "invalid", "InvalidRenderParameters"),
            )

            cases.map { (sizePx, uri, expectedCode) ->
                val options = StyledQrOptions().apply {
                    errorCorrection = "H"
                    minVersion = 1
                    maxVersion = 1
                    quietZoneModules = 4
                    logoDataUri = uri
                }

                renderStyledQrPng(
                    content = "hi",
                    output = StyledQrExportOptions(sizePx, mode),
                    options = options,
                ).then<Unit> { result ->
                    assertFalse(result.isSuccess)
                    assertFalse(result.isEmpty)
                    assertEquals(null, result.dataUri)
                    val error = assertNotNull(result.error)
                    assertEquals(expectedCode, error.code, "$mode, $sizePx px")
                    assertTrue(error.message.isNotBlank())
                }
            }
        }

        return Promise.all(checks.toTypedArray()).then<Unit> { Unit }
    }

    @Test
    fun blankExportStillSkipsMatrixDependentGeometryAndLogoChecks(): Promise<Unit> =
        renderStyledQrPng(
            content = " \n\t",
            output = StyledQrExportOptions(28),
            options = versionOneOptions().apply {
                logoDataUri = rectangularLogo
            },
        ).then<Unit> { result ->
            assertTrue(result.isEmpty)
            assertFalse(result.isSuccess)
            assertEquals(null, result.dataUri)
            assertEquals(null, result.error)
        }

    private fun checkFinderBorder(
        sizePx: Int,
        matrixStart: Int,
        finderEnd: Int,
        sampleCoordinate: Int,
    ): Promise<Unit> = verifyPng(
        output = StyledQrExportOptions(sizePx),
    ) { context ->
        for (coordinate in 0 until finderEnd + 10) {
            val expected = if (
                coordinate in matrixStart until finderEnd
            ) {
                black
            } else {
                white
            }

            assertPixel(context, coordinate, sampleCoordinate, expected)
            assertPixel(context, sampleCoordinate, coordinate, expected)
        }
    }

    private fun checkLogo(
        backgroundShape: String,
    ): Promise<Unit> {
        val options = StyledQrOptions().apply {
            minVersion = 4
            maxVersion = 4
            backgroundHex = "#FFF0CC"

            foregroundGradient = StyledQrGradientOptions(
                "#075985",
                "#6D28D9",
            )

            finderOuterShape = "circle"
            finderCenterShape = "diamond"
            finderOuterColorHex = "#123C66"
            finderCenterColorHex = "#0F4C5C"

            logoDataUri = rectangularLogo
            logo = StyledQrLogoOptions().apply {
                backgroundHex = "#DDEEFF"
                this.backgroundShape = backgroundShape
            }
        }

        val code = QrCodeGenerator.encodeText(
            text = "x",
            minVersion = 4,
            maxVersion = 4,
        )
        val style = resolveStyledQrStyle(options)
        val layout = code.calculateLogoLayout(assertNotNull(style.logo))

        // Version 4: 33 matrix modules + 8 quiet-zone modules.
        // At 410 px each module occupies exactly 10 px.
        val boxLeft = (4 + layout.leftModule) * 10
        val boxTop = (4 + layout.topModule) * 10
        val boxSide = layout.sizeModules * 10

        val imageLeft = boxLeft + layout.paddingModules * 10
        val imageTop = boxTop + layout.paddingModules * 10
        val imageSide = layout.logoSizeModules * 10

        return verifyPng(
            output = StyledQrExportOptions(410),
            options = options,
        ) { context ->
            assertPixel(context, 0, 0, listOf(255, 240, 204, 255))

            // Finder overrides remain aligned after applying output geometry.
            assertPixel(context, 75, 45, listOf(18, 60, 102, 255))
            assertPixel(context, 75, 75, listOf(15, 76, 92, 255))

            // Independent logo background, above the embedded image.
            assertPixel(
                context,
                boxLeft + boxSide / 2,
                boxTop + 2,
                listOf(221, 238, 255, 255),
            )

            val green = listOf(0, 255, 0, 255)

            assertPixel(
                context,
                imageLeft + imageSide / 2,
                imageTop + imageSide / 2,
                green,
            )

            // The source has red edges and a green middle.
            // Center cropping removes the red edges; stretching would retain them.
            assertPixel(
                context,
                imageLeft + imageSide / 8,
                imageTop + imageSide / 2,
                green,
            )

            val dataCell = (0 until code.size)
                .asSequence()
                .flatMap { row ->
                    (0 until code.size).asSequence().map { column ->
                        row to column
                    }
                }
                .first { (row, column) ->
                    val insideLogoBox =
                        row in layout.topModule until
                                layout.topModule + layout.sizeModules &&
                                column in layout.leftModule until
                                layout.leftModule + layout.sizeModules

                    code[row, column] &&
                            !code.isFunctionModule(row, column) &&
                            !insideLogoBox
                }

            val gradientPixel = readPixel(
                context,
                (4 + dataCell.second) * 10 + 5,
                (4 + dataCell.first) * 10 + 5,
            )

            assertTrue(gradientPixel[0] in 7..109)
            assertTrue(gradientPixel[1] in 40..89)
            assertTrue(gradientPixel[2] in 133..217)
            assertEquals(255, gradientPixel[3])
        }
    }

    private fun versionOneOptions(): StyledQrOptions =
        StyledQrOptions().apply {
            minVersion = 1
            maxVersion = 1
        }

    private fun verifyPng(
        output: StyledQrExportOptions = StyledQrExportOptions(300),
        options: StyledQrOptions = versionOneOptions(),
        verify: (CanvasRenderingContext2D) -> Unit,
    ): Promise<Unit> = Promise { resolve, reject ->
        renderStyledQrPng(
            content = "x",
            output = output,
            options = options,
        ).then<Unit>(
            onFulfilled = { result ->
                try {
                    assertTrue(
                        result.isSuccess,
                        result.error?.message ?: "Expected successful PNG export",
                    )

                    val image =
                        document.createElement("img") as HTMLImageElement

                    image.onload = {
                        try {
                            assertEquals(output.sizePx, image.naturalWidth)
                            assertEquals(output.sizePx, image.naturalHeight)

                            val canvas =
                                document.createElement("canvas") as HTMLCanvasElement

                            canvas.width = output.sizePx
                            canvas.height = output.sizePx

                            val context =
                                canvas.getContext("2d") as CanvasRenderingContext2D

                            context.drawImage(image, 0.0, 0.0)
                            verify(context)
                            resolve(Unit)
                        } catch (error: Throwable) {
                            reject(error)
                        }
                        null
                    }

                    image.onerror = { _, _, _, _, _ ->
                        reject(
                            IllegalStateException(
                                "Unable to decode the exported PNG",
                            ),
                        )
                        null
                    }

                    image.src = assertNotNull(result.dataUri)
                } catch (error: Throwable) {
                    reject(error)
                }
                Unit
            },
            onRejected = { error ->
                reject(error)
                Unit
            },
        )
    }

    private fun assertPixel(
        context: CanvasRenderingContext2D,
        x: Int,
        y: Int,
        expected: List<Int>,
    ) {
        assertEquals(expected, readPixel(context, x, y), "Pixel at ($x, $y)")
    }

    private fun readPixel(
        context: CanvasRenderingContext2D,
        x: Int,
        y: Int,
    ): List<Int> {
        val pixel = context.getImageData(
            x.toDouble(),
            y.toDouble(),
            1.0,
            1.0,
        ).data

        return (0..3).map { index ->
            pixel[index].toInt() and 0xFF
        }
    }

    private val black = listOf(0, 0, 0, 255)
    private val white = listOf(255, 255, 255, 255)

    private val rectangularLogo =
        "data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAKUlEQVR4nGP4z8CAB+GTI4QoMnnU4lGLRy0etXjU4lGLRy0etXg4WQwA72wdDlLL0dYAAAAASUVORK5CYII="
}
