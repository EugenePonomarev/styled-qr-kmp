package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.render.calculateRasterModuleLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class QrRasterModuleLayoutTest {

    @Test
    fun usesWholePixelsAndCentersTheQrGrid() {
        val layout = calculateRasterModuleLayout(
            sizePx = 300,
            totalModules = 29,
        )

        assertEquals(10, layout.moduleSizePx)
        assertEquals(5, layout.offsetPx)
    }

    @Test
    fun keepsSingleRemainderPixelOnTheTrailingSide() {
        val layout = calculateRasterModuleLayout(
            sizePx = 291,
            totalModules = 29,
        )

        assertEquals(10, layout.moduleSizePx)
        assertEquals(0, layout.offsetPx)
        assertEquals(
            1,
            291 - layout.offsetPx - layout.moduleSizePx * 29,
        )
    }
}
