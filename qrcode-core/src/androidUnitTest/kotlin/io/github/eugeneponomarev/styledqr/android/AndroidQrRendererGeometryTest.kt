package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Color
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrRenderException
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidQrRendererGeometryTest {
    private val versionOne = QrCodeGenerator.encodeText(
        text = "x",
        minVersion = 1,
        maxVersion = 1,
    )

    @Test
    fun integerGridAt300PixelsStartsTheMatrixAt45Pixels() {
        assertEquals(21, versionOne.size)

        val bitmap = versionOne.toBitmapWithOptions(
            QrRasterRenderOptions(sizePx = 300),
        )

        try {
            assertEquals(300, bitmap.width)
            assertEquals(300, bitmap.height)

            // Quiet zone: 4 × 10 px. Additional offset: 5 px.
            // Top finder border occupies [45, 115).
            for (coordinate in 0 until 125) {
                val expected = if (coordinate in 45 until 115) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }

                assertEquals(
                    expected,
                    bitmap.getPixel(coordinate, 50),
                    "Horizontal finder border at x=$coordinate",
                )
                assertEquals(
                    expected,
                    bitmap.getPixel(50, coordinate),
                    "Vertical finder border at y=$coordinate",
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun fitUsesFractionalModulesInsteadOfTheIntegerGrid() {
        val integer = versionOne.toBitmapWithOptions(
            QrRasterRenderOptions(
                sizePx = 300,
                sizeMode = QrRasterSizeMode.IntegerModules,
            ),
        )
        try {
            val fit = versionOne.toBitmapWithOptions(
                QrRasterRenderOptions(
                    sizePx = 300,
                    sizeMode = QrRasterSizeMode.Fit,
                ),
            )
            try {
                // Fit matrix starts at 4 × 300 / 29 ≈ 41.38 px.
                // IntegerModules matrix starts at 45 px.
                assertEquals(Color.BLACK, fit.getPixel(43, 46))
                assertEquals(Color.WHITE, integer.getPixel(43, 46))
                assertEquals(Color.WHITE, fit.getPixel(39, 46))
                assertEquals(Color.BLACK, integer.getPixel(46, 46))
            } finally {
                fit.recycle()
            }
        } finally {
            integer.recycle()
        }
    }

    @Test
    fun exactMultipleProducesTheSameBitmapInBothModes() {
        val integer = versionOne.toBitmapWithOptions(
            QrRasterRenderOptions(
                sizePx = 290,
                sizeMode = QrRasterSizeMode.IntegerModules,
            ),
        )
        try {
            val fit = versionOne.toBitmapWithOptions(
                QrRasterRenderOptions(
                    sizePx = 290,
                    sizeMode = QrRasterSizeMode.Fit,
                ),
            )
            try {
                assertSamePixels(integer, fit, "Exact multiple")
            } finally {
                fit.recycle()
            }
        } finally {
            integer.recycle()
        }
    }

    @Test
    fun explicitIntegerModeMatchesLegacyForAllModuleAndFunctionStyles() {
        val code = QrCodeGenerator.encodeText(
            text = "geometry",
            minVersion = 4,
            maxVersion = 4,
        )

        for (shape in QrModuleShape.entries) {
            for (functionStyle in QrFunctionPatternStyle.entries) {
                val style = QrStyle(
                    background = QrColor.fromHex("#F8FAFC"),
                    moduleShape = shape,
                    moduleScale = 0.84,
                    functionPatternStyle = functionStyle,
                    foregroundGradient = QrLinearGradient(
                        startColor = QrColor.fromHex("#075985"),
                        endColor = QrColor.fromHex("#6D28D9"),
                    ),
                    finderPatternAppearance = QrFinderPatternAppearance(
                        outerColor = QrColor.fromHex("#123C66"),
                        centerColor = QrColor.fromHex("#0F4C5C"),
                    ),
                )

                for (sizePx in listOf(410, 421)) {
                    val legacy = code.toBitmap(
                        sizePx = sizePx,
                        style = style,
                    )
                    try {
                        val explicit = code.toBitmapWithOptions(
                            options = QrRasterRenderOptions(sizePx),
                            style = style,
                        )
                        try {
                            assertSamePixels(
                                legacy,
                                explicit,
                                "$shape, $functionStyle, sizePx=$sizePx",
                            )
                        } finally {
                            explicit.recycle()
                        }
                    } finally {
                        legacy.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun rejectsInvalidOutputSizeWithTheSharedCategory() {
        for (mode in QrRasterSizeMode.entries) {
            for (sizePx in listOf(-1, 0, 28)) {
                val error = assertFailsWith<QrRenderException> {
                    versionOne.toBitmapWithOptions(
                        QrRasterRenderOptions(sizePx, mode),
                    )
                }

                assertEquals(
                    QrRenderErrorCode.InvalidRenderParameters,
                    error.code,
                )
            }
        }
    }

    @Test
    fun rejectsOverflowingQuietZoneBeforeAllocatingABitmap() {
        val error = assertFailsWith<QrRenderException> {
            versionOne.toBitmapWithOptions(
                options = QrRasterRenderOptions(300),
                style = QrStyle(quietZoneModules = Int.MAX_VALUE),
            )
        }

        assertEquals(
            QrRenderErrorCode.InvalidRenderParameters,
            error.code,
        )
    }

    private fun assertSamePixels(
        expected: Bitmap,
        actual: Bitmap,
        message: String,
    ) {
        assertEquals(expected.width, actual.width, message)
        assertEquals(expected.height, actual.height, message)
        assertContentEquals(
            expected.readPixels(),
            actual.readPixels(),
            message,
        )
    }

    private fun Bitmap.readPixels(): IntArray {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels
    }
}
