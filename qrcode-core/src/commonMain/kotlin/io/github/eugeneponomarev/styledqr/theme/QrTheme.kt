package io.github.eugeneponomarev.styledqr.theme

import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrStyle

/** A fixed visual preset that creates an ordinary platform-independent style. */
public class QrTheme internal constructor(
    public val id: QrThemeId,
    private val definition: QrThemeDefinition,
) {

    /**
     * Creates the theme's style.
     *
     * Supplied logo options are preserved without modification. Logo placement is
     * validated by the renderer against the actual QR matrix and correction budget.
     * The logo image itself must be supplied separately to the renderer.
     */
    public fun createStyle(
        logo: QrLogoOptions? = null,
    ): QrStyle = QrStyle(
        foreground = definition.foreground,
        background = definition.background,
        quietZoneModules = 4,
        moduleShape = definition.moduleShape,
        moduleScale = definition.moduleScalePermille / 1000.0,
        roundedModuleRadiusFraction = 0.22,
        functionPatternStyle = definition.functionPatternStyle,
        finderPatternShape = definition.finderPatternShape,
        logo = logo,
    )

    /**
     * Creates the theme's suggested logo options.
     *
     * These are requested dimensions, not a guarantee that a logo fits every QR.
     * Use copy() to customise them before passing them to createStyle().
     */
    public fun createLogoOptions(): QrLogoOptions = QrLogoOptions(
        sizeFraction = definition.logoDefaults.sizePermille / 1000.0,
        paddingFraction = definition.logoDefaults.paddingPermille / 1000.0,
        background = definition.logoDefaults.background,
        backgroundShape = QrLogoBackgroundShape.RoundedSquare,
        cornerRadiusFraction = 0.18,
    )
}