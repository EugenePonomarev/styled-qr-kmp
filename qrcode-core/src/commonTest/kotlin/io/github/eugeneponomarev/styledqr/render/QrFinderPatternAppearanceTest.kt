package io.github.eugeneponomarev.styledqr.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QrFinderPatternAppearanceTest {

    @Test
    fun defaultAppearanceUsesTheLegacyFinderShapeAndForegroundPaint() {
        val style = QrStyle(
            moduleShape = QrModuleShape.Circle,
            finderPatternShape = QrFinderPatternShape.RoundedSquare,
        )

        val resolved = style.resolvedFinderPatternAppearance()

        assertEquals(QrModuleShape.RoundedSquare, resolved.outer.shape)
        assertEquals(QrModuleShape.RoundedSquare, resolved.middle.shape)
        assertEquals(QrModuleShape.RoundedSquare, resolved.center.shape)

        assertEquals(QrFinderPatternLayerPaint.Foreground, resolved.outer.paint)
        assertEquals(
            QrFinderPatternLayerPaint.Solid(style.background),
            resolved.middle.paint,
        )
        assertEquals(QrFinderPatternLayerPaint.Foreground, resolved.center.paint)
    }

    @Test
    fun everyLayerShapeResolvesToTheExpectedModuleShape() {
        val cases = listOf(
            QrFinderPatternLayerShape.Inherit to QrModuleShape.Circle,
            QrFinderPatternLayerShape.Square to QrModuleShape.Square,
            QrFinderPatternLayerShape.RoundedSquare to QrModuleShape.RoundedSquare,
            QrFinderPatternLayerShape.Circle to QrModuleShape.Circle,
            QrFinderPatternLayerShape.Diamond to QrModuleShape.Diamond,
        )

        for ((layerShape, expectedShape) in cases) {
            val resolved = QrStyle(
                finderPatternShape = QrFinderPatternShape.Circle,
                finderPatternAppearance = QrFinderPatternAppearance(
                    outerShape = layerShape,
                    centerShape = layerShape,
                ),
            ).resolvedFinderPatternAppearance()

            assertEquals(expectedShape, resolved.outer.shape)
            assertEquals(expectedShape, resolved.center.shape)
        }
    }

    @Test
    fun explicitColorsReplaceOnlyTheirOwnFinderLayers() {
        val outerColor = QrColor(18, 60, 102)
        val centerColor = QrColor(0, 80, 100)

        val resolved = QrStyle(
            finderPatternAppearance = QrFinderPatternAppearance(
                outerColor = outerColor,
                centerColor = centerColor,
            ),
        ).resolvedFinderPatternAppearance()

        assertEquals(
            QrFinderPatternLayerPaint.Solid(outerColor),
            resolved.outer.paint,
        )
        assertEquals(
            QrFinderPatternLayerPaint.Solid(QrColor.White),
            resolved.middle.paint,
        )
        assertEquals(
            QrFinderPatternLayerPaint.Solid(centerColor),
            resolved.center.paint,
        )
    }

    @Test
    fun transparentFinderOverrideIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            QrStyle(
                finderPatternAppearance = QrFinderPatternAppearance(
                    outerColor = QrColor(0, 0, 0, alpha = 254),
                ),
            )
        }
    }

    @Test
    fun lowContrastFinderOverrideIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            QrStyle(
                finderPatternAppearance = QrFinderPatternAppearance(
                    centerColor = QrColor(119, 119, 119),
                ),
            )
        }
    }
}
