package io.github.eugeneponomarev.styledqr.web

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StyledQrExportTest {
    @Test
    fun integerGridAt300PixelsStartsAt45(): Promise<Unit> =
        checkIntegerGrid(
            sizePx = 300,
            matrixStart = 45,
            finderEnd = 115,
            sampleCoordinate = 50,
        )

    @Test
    fun exactMultipleAt290PixelsStartsAt40(): Promise<Unit> =
        checkIntegerGrid(
            sizePx = 290,
            matrixStart = 40,
            finderEnd = 110,
            sampleCoordinate = 45,
        )

    @Test
    fun oddOutputAt301PixelsKeepsTheLeadingFivePixelOffset(): Promise<Unit> =
        checkIntegerGrid(
            sizePx = 301,
            matrixStart = 45,
            finderEnd = 115,
            sampleCoordinate = 50,
        )

    @Test
    fun fitUsesFractionalModules(): Promise<Unit> {
        val result = renderStyledQrSvgForExport(
            content = "x",
            output = StyledQrExportOptions(300, "fit"),
            options = versionOneOptions(),
        )

        assertTrue(result.isSuccess)

        return rasterize(assertNotNull(result.svg), 300) { context ->
            assertPixel(context, 43, 46, black)
            assertPixel(context, 39, 46, white)
        }
    }

    @Test
    fun transparentBackgroundIsPaintedOnce(): Promise<Unit> {
        val options = versionOneOptions().apply {
            backgroundHex = "#FF0000"
            backgroundAlpha = 128
        }
        val result = renderStyledQrSvgForExport(
            content = "x",
            output = StyledQrExportOptions(300),
            options = options,
        )

        assertTrue(result.isSuccess)

        return rasterize(assertNotNull(result.svg), 300) { context ->
            val expected = listOf(255, 0, 0, 128)

            // Additional outer padding.
            assertPixel(context, 0, 0, expected)

            // Quiet zone inside the scaled QR grid.
            assertPixel(context, 20, 20, expected)
        }
    }

    @Test
    fun validatesExportParametersAndPreservesEmptyResults() {
        val empty = renderStyledQrSvgForExport(
            content = "",
            output = StyledQrExportOptions(300),
        )
        assertTrue(empty.isEmpty)

        val invalidOutputs = listOf(
            StyledQrExportOptions(0),
            StyledQrExportOptions(-1),
            StyledQrExportOptions(300, "unknown"),
            StyledQrExportOptions(300).apply {
                asDynamic().sizePx = 300.5
            },
        )

        for (output in invalidOutputs) {
            val result = renderStyledQrSvgForExport(
                content = "",
                output = output,
            )

            assertEquals(
                "InvalidRenderParameters",
                result.error?.code,
            )
        }

        val insufficient = renderStyledQrSvgForExport(
            content = "x",
            output = StyledQrExportOptions(28),
            options = versionOneOptions(),
        )
        assertEquals(
            "InvalidRenderParameters",
            insufficient.error?.code,
        )
    }

    private fun checkIntegerGrid(
        sizePx: Int,
        matrixStart: Int,
        finderEnd: Int,
        sampleCoordinate: Int,
    ): Promise<Unit> {
        val result = renderStyledQrSvgForExport(
            content = "x",
            output = StyledQrExportOptions(sizePx),
            options = versionOneOptions(),
        )

        assertTrue(result.isSuccess)

        return rasterize(assertNotNull(result.svg), sizePx) { context ->
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
    }

    private fun versionOneOptions(): StyledQrOptions =
        StyledQrOptions().apply {
            minVersion = 1
            maxVersion = 1
        }

    private fun rasterize(
        svg: String,
        sizePx: Int,
        verify: (CanvasRenderingContext2D) -> Unit,
    ): Promise<Unit> = Promise { resolve, reject ->
        val image = document.createElement("img") as HTMLImageElement

        image.onload = {
            try {
                assertEquals(sizePx, image.naturalWidth)
                assertEquals(sizePx, image.naturalHeight)

                val canvas =
                    document.createElement("canvas") as HTMLCanvasElement

                canvas.width = sizePx
                canvas.height = sizePx

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
            reject(IllegalStateException("Unable to load the exported SVG"))
            null
        }

        image.src = "data:image/svg+xml;base64," + window.btoa(svg)
    }

    private fun assertPixel(
        context: CanvasRenderingContext2D,
        x: Int,
        y: Int,
        expected: List<Int>,
    ) {
        val pixel = context.getImageData(
            x.toDouble(),
            y.toDouble(),
            1.0,
            1.0,
        ).data

        val actual = (0..3).map { index ->
            pixel[index].toInt() and 0xFF
        }

        assertEquals(expected, actual, "Pixel at ($x, $y)")
    }

    private val black = listOf(0, 0, 0, 255)
    private val white = listOf(255, 255, 255, 255)
}
