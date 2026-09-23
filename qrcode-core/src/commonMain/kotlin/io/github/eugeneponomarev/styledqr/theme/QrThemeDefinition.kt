package io.github.eugeneponomarev.styledqr.theme

import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrModuleShape

internal data class QrThemeDefinition(
    val foreground: QrColor,
    val background: QrColor,
    val moduleShape: QrModuleShape,
    val moduleScalePermille: Int,
    val functionPatternStyle: QrFunctionPatternStyle,
    val finderPatternShape: QrFinderPatternShape,
    val logoDefaults: QrThemeLogoDefaults,
)

internal data class QrThemeLogoDefaults(
    val sizePermille: Int,
    val paddingPermille: Int,
    val background: QrColor,
)