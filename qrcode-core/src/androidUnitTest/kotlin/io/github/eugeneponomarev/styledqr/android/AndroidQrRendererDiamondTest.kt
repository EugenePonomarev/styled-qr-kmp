package io.github.eugeneponomarev.styledqr.android

import android.graphics.Color
import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidQrRendererDiamondTest {
    @Test
    fun diamondFindersAndAlignmentKeepOriginalPattern() {
        val code = QrCodeGenerator.encodeText(
            "x",
            minVersion = 4,
            maxVersion = 4,
        )
        val pixelsPerModule = 20
        val bitmap = code.toBitmap(
            sizePx = (code.size + 8) * pixelsPerModule,
            style = QrStyle(
                moduleShape = QrModuleShape.Diamond,
                moduleScale = 0.84,
            ),
        )

        try {
            assertEquals(
                Color.WHITE,
                bitmap.getPixel(90, 90),
                "Finder must remain a diamond",
            )
            assertEquals(
                Color.BLACK,
                bitmap.getPixel(570, 570),
                "Alignment corner module must remain dark",
            )
            assertEquals(
                Color.WHITE,
                bitmap.getPixel(640, 620),
                "Alignment modules must keep their diamond gaps",
            )
            assertEquals(
                Color.WHITE,
                bitmap.getPixel(630, 610),
                "Middle ring must stay clear",
            )
            assertEquals(
                Color.BLACK,
                bitmap.getPixel(610, 610),
                "Alignment centre must stay dark",
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun denseDiamondBitmapsKeepTheirPayload() {
        val text = "x".repeat(800)
        val code = QrCodeGenerator.encodeText(text)
        for (sizePx in listOf(512, 1024)) {
            val style = QrStyle(
                moduleShape = QrModuleShape.Diamond,
                moduleScale = 0.84,
            )
            val bitmap = code.toBitmap(sizePx, style)
            try {
                val totalModules = code.size + style.quietZoneModules * 2
                val modulePx = sizePx / totalModules
                val offset = (sizePx - totalModules * modulePx) / 2
                val modules = Array(code.size) { row ->
                    BooleanArray(code.size) { column ->
                        val x = offset +
                                (column + style.quietZoneModules) * modulePx +
                                modulePx / 2
                        val y = offset +
                                (row + style.quietZoneModules) * modulePx +
                                modulePx / 2
                        Color.red(bitmap.getPixel(x, y)) < 128
                    }
                }
                assertEquals(
                    text,
                    QrCodeDecoder.decode(modules).utf8TextOrNull(),
                    "sizePx=$sizePx",
                )
            } finally {
                bitmap.recycle()
            }
        }
    }
}