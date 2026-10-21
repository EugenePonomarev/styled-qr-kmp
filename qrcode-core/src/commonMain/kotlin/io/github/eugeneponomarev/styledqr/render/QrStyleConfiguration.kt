package io.github.eugeneponomarev.styledqr.render

/**
 * Mutable input for a shared QrStyle.
 *
 * Assignments do not validate or throw. build() validates inside Kotlin using
 * the existing shared constructors.
 *
 * Do not mutate a configuration concurrently with building or rendering.
 */
public class QrStyleConfiguration {
    public var foregroundHex: String = "#000000"
    public var foregroundAlpha: Int = 255
    public var backgroundHex: String = "#FFFFFF"
    public var backgroundAlpha: Int = 255
    public var quietZoneModules: Int = 4
    public var moduleShape: QrModuleShape = QrModuleShape.Square
    public var moduleScale: Double = 1.0
    public var roundedModuleRadiusFraction: Double = 0.22
    public var preserveFunctionPatterns: Boolean = false

    /** Null preserves the existing default derived from preserveFunctionPatterns. */
    public var functionPatternStyle: QrFunctionPatternStyle? = null

    public var finderPatternShape: QrFinderPatternShape =
        QrFinderPatternShape.MatchModuleShape

    public var finderOuterShape: QrFinderPatternLayerShape =
        QrFinderPatternLayerShape.Inherit

    public var finderCenterShape: QrFinderPatternLayerShape =
        QrFinderPatternLayerShape.Inherit

    public var finderOuterColorHex: String? = null
    public var finderOuterColorAlpha: Int = 255

    public var finderCenterColorHex: String? = null
    public var finderCenterColorAlpha: Int = 255

    public var logo: QrLogoConfiguration? = null

    /** Overrides [foreground] for dark QR modules and finder layers without a solid override. */
    public var foregroundGradient: QrGradientConfiguration? = null

    public fun build(): QrStyleBuildResult = try {
        QrStyleBuildResult(
            style = QrStyle(
                foreground = configurationColor(foregroundHex, foregroundAlpha),
                background = configurationColor(backgroundHex, backgroundAlpha),
                quietZoneModules = quietZoneModules,
                moduleShape = moduleShape,
                moduleScale = moduleScale,
                roundedModuleRadiusFraction = roundedModuleRadiusFraction,
                preserveFunctionPatterns = preserveFunctionPatterns,
                functionPatternStyle = functionPatternStyle
                    ?: if (preserveFunctionPatterns) {
                        QrFunctionPatternStyle.PreserveAll
                    } else {
                        QrFunctionPatternStyle.MatchDataModules
                    },
                finderPatternShape = finderPatternShape,
                logo = logo?.toOptions(),
                foregroundGradient = foregroundGradient?.toGradient(),
                finderPatternAppearance = QrFinderPatternAppearance(
                    outerShape = finderOuterShape,
                    centerShape = finderCenterShape,
                    outerColor = finderOuterColorHex?.let { hex ->
                        configurationColor(hex, finderOuterColorAlpha)
                    },
                    centerColor = finderCenterColorHex?.let { hex ->
                        configurationColor(hex, finderCenterColorAlpha)
                    },
                ),
            ),
            error = null,
        )
    } catch (error: IllegalArgumentException) {
        QrStyleBuildResult(
            style = null,
            error = QrRenderError(
                QrRenderErrorCode.InvalidStyle,
                error.message ?: "Invalid QR style",
            ),
        )
    }

    public companion object {
        public fun fromStyle(style: QrStyle): QrStyleConfiguration =
            QrStyleConfiguration().apply {
                foregroundHex = style.foreground.rgbHex()
                foregroundAlpha = style.foreground.alpha
                backgroundHex = style.background.rgbHex()
                backgroundAlpha = style.background.alpha
                quietZoneModules = style.quietZoneModules
                moduleShape = style.moduleShape
                moduleScale = style.moduleScale
                roundedModuleRadiusFraction = style.roundedModuleRadiusFraction
                preserveFunctionPatterns = style.preserveFunctionPatterns
                functionPatternStyle = style.functionPatternStyle
                finderPatternShape = style.finderPatternShape

                val finderAppearance = style.finderPatternAppearance
                finderOuterShape = finderAppearance.outerShape
                finderCenterShape = finderAppearance.centerShape
                finderOuterColorHex = finderAppearance.outerColor?.rgbHex()
                finderOuterColorAlpha = finderAppearance.outerColor?.alpha ?: 255
                finderCenterColorHex = finderAppearance.centerColor?.rgbHex()
                finderCenterColorAlpha = finderAppearance.centerColor?.alpha ?: 255

                logo = style.logo?.let { original ->
                    QrLogoConfiguration().apply {
                        sizeFraction = original.sizeFraction
                        paddingFraction = original.paddingFraction
                        backgroundHex = original.background.rgbHex()
                        backgroundAlpha = original.background.alpha
                        backgroundShape = original.backgroundShape
                        cornerRadiusFraction = original.cornerRadiusFraction
                    }
                }

                foregroundGradient = style.foregroundGradient?.let { original ->
                    QrGradientConfiguration().apply {
                        startColorHex = original.startColor.rgbHex()
                        startAlpha = original.startColor.alpha
                        endColorHex = original.endColor.rgbHex()
                        endAlpha = original.endColor.alpha
                        startX = original.start.x
                        startY = original.start.y
                        endX = original.end.x
                        endY = original.end.y
                    }
                }
            }
    }
}

public class QrStyleBuildResult internal constructor(
    public val style: QrStyle?,
    public val error: QrRenderError?,
) {
    public val isSuccess: Boolean get() = style != null
}

public class QrLogoConfiguration {
    public var sizeFraction: Double = 0.18
    public var paddingFraction: Double = 0.04
    public var backgroundHex: String = "#FFFFFF"
    public var backgroundAlpha: Int = 255
    public var backgroundShape: QrLogoBackgroundShape =
        QrLogoBackgroundShape.RoundedSquare
    public var cornerRadiusFraction: Double = 0.18

    internal fun toOptions(): QrLogoOptions = QrLogoOptions(
        sizeFraction,
        paddingFraction,
        configurationColor(backgroundHex, backgroundAlpha),
        backgroundShape,
        cornerRadiusFraction,
    )
}

public class QrGradientConfiguration {
    public var startColorHex: String = "#000000"
    public var startAlpha: Int = 255
    public var endColorHex: String = "#000000"
    public var endAlpha: Int = 255
    public var startX: Double = 0.0
    public var startY: Double = 0.0
    public var endX: Double = 1.0
    public var endY: Double = 1.0

    internal fun toGradient(): QrLinearGradient = QrLinearGradient(
        configurationColor(startColorHex, startAlpha),
        configurationColor(endColorHex, endAlpha),
        QrGradientPoint(startX, startY),
        QrGradientPoint(endX, endY),
    )
}

private fun configurationColor(hex: String, alpha: Int): QrColor =
    QrColor.fromHex(hex).copy(alpha = alpha)

private fun QrColor.rgbHex(): String =
    "#" + listOf(red, green, blue).joinToString("") {
        it.toString(16).padStart(2, '0').uppercase()
    }
