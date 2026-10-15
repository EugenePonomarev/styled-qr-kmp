package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidQrRendererLogoTest {
    private val code = QrCodeGenerator.encodeText("https://eugeneponomarev.com")
    private val options = QrLogoOptions(
        sizeFraction = 0.16,
        paddingFraction = 0.04,
        cornerRadiusFraction = 0.45,
    )
    private val layout = code.calculateLogoLayout(options)
    private val pixelsPerModule = 20

    @Test
    fun curvedShapesKeepCornerModulesAndClipTheImage() {
        assertEquals(8, layout.sizeModules)
        assertTrue(code[layout.topModule, layout.leftModule + 1])
        val logo = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        try {
            for (shape in QrLogoBackgroundShape.entries) {
                val bitmap = render(options.copy(backgroundShape = shape), logo)
                try {
                    assertEquals(
                        if (shape == QrLogoBackgroundShape.Square) Color.WHITE else Color.BLACK,
                        bitmap.logoPixel(0, 1),
                        "Corner module must follow the $shape clearance",
                    )
                    assertEquals(
                        if (shape == QrLogoBackgroundShape.Square) Color.RED else Color.WHITE,
                        bitmap.logoPixel(1, 1),
                        "Image corners must be clipped for $shape",
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        } finally {
            logo.recycle()
        }
    }

    @Test
    fun transparentLogoBackgroundStillClearsUnderlyingQrModules() {
        assertTrue(code[layout.topModule + 3, layout.leftModule + 3])
        val logo = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            val bitmap = render(
                options.copy(
                    backgroundShape = QrLogoBackgroundShape.Circle,
                    background = QrColor(255, 255, 255, 0),
                ),
                logo,
            )
            try {
                assertEquals(Color.WHITE, bitmap.logoPixel(3, 3))
            } finally {
                bitmap.recycle()
            }
        } finally {
            logo.recycle()
        }
    }

    @Test
    fun rectangularLogosAreCentreCroppedAndKeepPadding() {
        val ppm = pixelsPerModule

        for ((width, height) in listOf(200 to 100, 100 to 200)) {
            val logo = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888,
            )

            try {
                logo.eraseColor(Color.RED)

                val shortSide = minOf(width, height)
                val sourceLeft = (width - shortSide) / 2f
                val sourceTop = (height - shortSide) / 2f

                Canvas(logo).drawRect(
                    sourceLeft,
                    sourceTop,
                    sourceLeft + shortSide,
                    sourceTop + shortSide,
                    Paint().apply { color = Color.GREEN },
                )

                for (shape in QrLogoBackgroundShape.entries) {
                    val bitmap = render(
                        options.copy(backgroundShape = shape),
                        logo,
                    )

                    try {
                        val left =
                            (4 + layout.leftModule + layout.paddingModules) * ppm
                        val top =
                            (4 + layout.topModule + layout.paddingModules) * ppm
                        val side = layout.logoSizeModules * ppm
                        val label = "$width x $height, $shape"

                        for (fraction in listOf(0.125, 0.5, 0.875)) {
                            val point = (side * fraction).toInt()

                            assertEquals(
                                Color.GREEN,
                                bitmap.getPixel(left + point, top + side / 2),
                                label,
                            )
                            assertEquals(
                                Color.GREEN,
                                bitmap.getPixel(left + side / 2, top + point),
                                label,
                            )
                        }

                        assertEquals(
                            Color.WHITE,
                            bitmap.getPixel(left + side / 2, top - ppm / 2),
                            "Padding: $label",
                        )
                        assertEquals(
                            if (shape == QrLogoBackgroundShape.Square) {
                                Color.GREEN
                            } else {
                                Color.WHITE
                            },
                            bitmap.getPixel(left + ppm / 4, top + ppm / 4),
                            "Image clipping: $label",
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            } finally {
                logo.recycle()
            }
        }
    }

    private fun render(options: QrLogoOptions, logo: Bitmap): Bitmap = code.toBitmap(
        sizePx = (code.size + 8) * pixelsPerModule,
        style = QrStyle(logo = options),
        logo = logo,
    )

    private fun Bitmap.logoPixel(row: Int, column: Int): Int = getPixel(
        (4 + layout.leftModule + column) * pixelsPerModule + pixelsPerModule / 2,
        (4 + layout.topModule + row) * pixelsPerModule + pixelsPerModule / 2,
    )
}
