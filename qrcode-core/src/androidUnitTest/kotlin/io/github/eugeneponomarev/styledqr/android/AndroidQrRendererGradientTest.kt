package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Color
import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.isFinderPatternCore
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidQrRendererGradientTest {

    private val startColor = QrColor.fromHex("#075985")
    private val endColor = QrColor.fromHex("#6D28D9")
    private val backgroundColor = QrColor.fromHex("#F8FAFC")

    @Test
    fun mapsHorizontalGradientAcrossQrMatrixExcludingQuietZone() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val gradient = QrLinearGradient(
            startColor = startColor,
            endColor = endColor,
            start = QrGradientPoint(
                x = 0.0,
                y = 0.5,
            ),
            end = QrGradientPoint(
                x = 1.0,
                y = 0.5,
            ),
        )

        val style = QrStyle(
            background = backgroundColor,
            moduleScale = 1.0,
            foregroundGradient = gradient,
        )

        val pixelsPerModule = 12
        val bitmap = code.toBitmap(
            sizePx = bitmapSize(
                code = code,
                style = style,
                pixelsPerModule = pixelsPerModule,
            ),
            style = style,
        )

        val span = widestDarkSpan(code)

        val leftPixel = bitmap.pixelAtQrModule(
            row = span.row,
            column = span.leftColumn,
            quietZoneModules = style.quietZoneModules,
            pixelsPerModule = pixelsPerModule,
        )

        val rightPixel = bitmap.pixelAtQrModule(
            row = span.row,
            column = span.rightColumn,
            quietZoneModules = style.quietZoneModules,
            pixelsPerModule = pixelsPerModule,
        )

        // Both points are on exactly the same row. A renderer that accidentally
        // maps the horizontal gradient to Y would therefore produce equal colors.
        assertNotEquals(
            leftPixel,
            rightPixel,
        )

        val leftProgress =
            (span.leftColumn + 0.5) / code.size

        val rightProgress =
            (span.rightColumn + 0.5) / code.size

        assertColorNear(
            expected = gradient.expectedColorAt(leftProgress),
            actual = leftPixel,
        )

        assertColorNear(
            expected = gradient.expectedColorAt(rightProgress),
            actual = rightPixel,
        )
    }

    @Test
    fun keepsQuietZoneAndFinderMiddleRingOnSolidBackground() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val style = QrStyle(
            background = backgroundColor,
            foregroundGradient = QrLinearGradient(
                startColor = startColor,
                endColor = endColor,
            ),
        )

        val pixelsPerModule = 10

        val bitmap = code.toBitmap(
            sizePx = bitmapSize(
                code = code,
                style = style,
                pixelsPerModule = pixelsPerModule,
            ),
            style = style,
        )

        val expectedBackground = Color.argb(
            backgroundColor.alpha,
            backgroundColor.red,
            backgroundColor.green,
            backgroundColor.blue,
        )

        // Centre of the top-left quiet-zone module.
        assertEquals(
            expectedBackground,
            bitmap.getPixel(
                pixelsPerModule / 2,
                pixelsPerModule / 2,
            ),
        )

        // Top-left finder pattern:
        //
        // 7×7 foreground
        // 5×5 background
        // 3×3 foreground
        //
        // Matrix position (row=1, column=3) is safely inside the 5×5 ring.
        assertEquals(
            expectedBackground,
            bitmap.pixelAtQrModule(
                row = 1,
                column = 3,
                quietZoneModules = style.quietZoneModules,
                pixelsPerModule = pixelsPerModule,
            ),
        )
    }

    @Test
    fun legacyStyleRemainsSolidBlack() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val style = QrStyle()
        val pixelsPerModule = 10

        val bitmap = code.toBitmap(
            sizePx = bitmapSize(
                code = code,
                style = style,
                pixelsPerModule = pixelsPerModule,
            ),
            style = style,
        )

        // Centre of the 3×3 dark core of the top-left finder.
        assertEquals(
            Color.BLACK,
            bitmap.pixelAtQrModule(
                row = 3,
                column = 3,
                quietZoneModules = style.quietZoneModules,
                pixelsPerModule = pixelsPerModule,
            ),
        )
    }

    private fun bitmapSize(
        code: QrCode,
        style: QrStyle,
        pixelsPerModule: Int,
    ): Int = (
            code.size +
                    style.quietZoneModules * 2
            ) * pixelsPerModule

    private fun Bitmap.pixelAtQrModule(
        row: Int,
        column: Int,
        quietZoneModules: Int,
        pixelsPerModule: Int,
    ): Int {
        val x =
            (column + quietZoneModules) *
                    pixelsPerModule +
                    pixelsPerModule / 2

        val y =
            (row + quietZoneModules) *
                    pixelsPerModule +
                    pixelsPerModule / 2

        return getPixel(x, y)
    }

    private fun widestDarkSpan(
        code: QrCode,
    ): DarkSpan {
        return (0 until code.size)
            .mapNotNull { row ->
                val columns = (0 until code.size)
                    .filter { column ->
                        code[row, column] &&
                                !code.isFinderPatternCore(
                                    row,
                                    column,
                                )
                    }

                if (columns.size < 2) {
                    null
                } else {
                    DarkSpan(
                        row = row,
                        leftColumn = columns.first(),
                        rightColumn = columns.last(),
                    )
                }
            }
            .maxByOrNull { span ->
                span.rightColumn - span.leftColumn
            }
            ?: error(
                "Unable to find two dark data modules " +
                        "on the same QR row",
            )
    }

    private fun QrLinearGradient.expectedColorAt(
        progress: Double,
    ): QrColor {
        fun mix(
            from: Int,
            to: Int,
        ): Int {
            return (
                    from +
                            (to - from) * progress
                    ).roundToInt()
        }

        return QrColor(
            red = mix(
                startColor.red,
                endColor.red,
            ),
            green = mix(
                startColor.green,
                endColor.green,
            ),
            blue = mix(
                startColor.blue,
                endColor.blue,
            ),
        )
    }

    private fun assertColorNear(
        expected: QrColor,
        actual: Int,
        tolerance: Int = 3,
    ) {
        assertTrue(
            abs(
                expected.red -
                        Color.red(actual),
            ) <= tolerance,
            "Red channel differs: " +
                    "expected=${expected.red}, " +
                    "actual=${Color.red(actual)}",
        )

        assertTrue(
            abs(
                expected.green -
                        Color.green(actual),
            ) <= tolerance,
            "Green channel differs: " +
                    "expected=${expected.green}, " +
                    "actual=${Color.green(actual)}",
        )

        assertTrue(
            abs(
                expected.blue -
                        Color.blue(actual),
            ) <= tolerance,
            "Blue channel differs: " +
                    "expected=${expected.blue}, " +
                    "actual=${Color.blue(actual)}",
        )

        assertEquals(
            255,
            Color.alpha(actual),
        )
    }

    private data class DarkSpan(
        val row: Int,
        val leftColumn: Int,
        val rightColumn: Int,
    )
}
