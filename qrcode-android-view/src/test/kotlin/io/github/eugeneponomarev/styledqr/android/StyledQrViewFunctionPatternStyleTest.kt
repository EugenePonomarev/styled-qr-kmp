package io.github.eugeneponomarev.styledqr.android

import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class StyledQrViewFunctionPatternStyleTest {

    @Test
    fun absentXmlAttributeUsesTheSharedStyleDefault() {
        assertEquals(
            QrStyle().functionPatternStyle,
            functionPatternStyleFrom(null),
        )
    }

    @Test
    fun explicitXmlValuesKeepTheirExistingMeanings() {
        assertEquals(
            QrFunctionPatternStyle.PreserveAll,
            functionPatternStyleFrom(0),
        )
        assertEquals(
            QrFunctionPatternStyle.PreserveFindersAndAlignment,
            functionPatternStyleFrom(1),
        )
        assertEquals(
            QrFunctionPatternStyle.MatchDataModules,
            functionPatternStyleFrom(2),
        )
    }
}