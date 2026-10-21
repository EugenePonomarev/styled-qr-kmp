package io.github.eugeneponomarev.styledqr.android

import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerShape
import kotlin.test.Test
import kotlin.test.assertEquals

class StyledQrViewFinderPatternAppearanceTest {

    @Test
    fun xmlFinderLayerShapeValuesResolveCorrectly() {
        assertEquals(
            QrFinderPatternLayerShape.Inherit,
            finderPatternLayerShapeFrom(0),
        )
        assertEquals(
            QrFinderPatternLayerShape.Square,
            finderPatternLayerShapeFrom(1),
        )
        assertEquals(
            QrFinderPatternLayerShape.RoundedSquare,
            finderPatternLayerShapeFrom(2),
        )
        assertEquals(
            QrFinderPatternLayerShape.Circle,
            finderPatternLayerShapeFrom(3),
        )
        assertEquals(
            QrFinderPatternLayerShape.Diamond,
            finderPatternLayerShapeFrom(4),
        )
    }
}
