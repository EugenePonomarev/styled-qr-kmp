@file:OptIn(ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerShape
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrGradientConfiguration
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration
import io.github.eugeneponomarev.styledqr.theme.QrThemeId
import io.github.eugeneponomarev.styledqr.theme.QrThemes

/**
 * Full visual options for the Web API.
 *
 * Nullable visual fields are overrides: null means no override.
 * Theme definitions and validation come from the shared core.
 */
@JsExport
public class StyledQrOptions {
    public var themeId: String? = null

    public var errorCorrection: String = "H"
    public var minVersion: Int = 1
    public var maxVersion: Int = 40

    public var foregroundHex: String? = null
    public var foregroundAlpha: Int? = null
    public var backgroundHex: String? = null
    public var backgroundAlpha: Int? = null

    public var quietZoneModules: Int? = null
    public var moduleShape: String? = null
    public var moduleScale: Double? = null
    public var roundedModuleRadiusFraction: Double? = null

    public var preserveFunctionPatterns: Boolean? = null
    public var functionPatternStyle: String? = null

    public var finderPatternShape: String? = null
    public var finderOuterShape: String? = null
    public var finderCenterShape: String? = null

    public var finderOuterColorHex: String? = null
    public var finderOuterColorAlpha: Int? = null
    public var clearFinderOuterColor: Boolean = false

    public var finderCenterColorHex: String? = null
    public var finderCenterColorAlpha: Int? = null
    public var clearFinderCenterColor: Boolean = false

    public var foregroundGradient: StyledQrGradientOptions? = null
    public var clearForegroundGradient: Boolean = false

    public var logoDataUri: String? = null
    public var logo: StyledQrLogoOptions? = null
}

@JsExport
public class StyledQrGradientOptions(
    public val startColor: String,
    public val endColor: String,
) {
    public var startAlpha: Int? = null
    public var endAlpha: Int? = null

    public var startX: Double? = null
    public var startY: Double? = null
    public var endX: Double? = null
    public var endY: Double? = null
}

@JsExport
public class StyledQrLogoOptions {
    public var sizeFraction: Double? = null
    public var paddingFraction: Double? = null
    public var backgroundHex: String? = null
    public var backgroundAlpha: Int? = null
    public var backgroundShape: String? = null
    public var cornerRadiusFraction: Double? = null
}

/**
 * Strict SVG generation with the full shared visual contract.
 *
 * Existing positional APIs retain their historical defaults and behaviour.
 * This entry point rejects blank content; a separate safe API supplies
 * explicit success/empty/error states.
 */
@JsExport
public fun generateStyledQrSvgWithOptions(
    content: String,
    options: StyledQrOptions = StyledQrOptions(),
): String {
    require(content.isNotBlank()) { "content must not be blank" }

    return renderPreparedStyledQrSvg(
        content = content,
        request = prepareStyledQrRequest(options),
    )
}

internal fun resolveStyledQrStyle(options: StyledQrOptions): QrStyle {
    require(
        !options.clearForegroundGradient ||
                options.foregroundGradient == null,
    ) {
        "clearForegroundGradient cannot be combined with foregroundGradient"
    }
    require(
        !options.clearFinderOuterColor ||
                (
                        options.finderOuterColorHex == null &&
                                options.finderOuterColorAlpha == null
                        ),
    ) {
        "clearFinderOuterColor cannot be combined with an outer color override"
    }
    require(
        !options.clearFinderCenterColor ||
                (
                        options.finderCenterColorHex == null &&
                                options.finderCenterColorAlpha == null
                        ),
    ) {
        "clearFinderCenterColor cannot be combined with a center color override"
    }

    val theme = options.themeId?.let {
        QrThemes.get(webEnum<QrThemeId>(it, "themeId"))
    }
    val wantsLogoOptions = options.logo != null || options.logoDataUri != null

    val baseStyle = if (theme != null) {
        theme.createStyle(
            logo = if (wantsLogoOptions) theme.createLogoOptions() else null,
        )
    } else {
        QrStyle(
            logo = if (wantsLogoOptions) QrLogoOptions() else null,
        )
    }
    val configuration = QrStyleConfiguration.fromStyle(baseStyle)

    options.foregroundHex?.let { configuration.foregroundHex = it }
    options.foregroundAlpha?.let {
        configuration.foregroundAlpha = checkedWebInteger(it, "foregroundAlpha")
    }
    options.backgroundHex?.let { configuration.backgroundHex = it }
    options.backgroundAlpha?.let {
        configuration.backgroundAlpha = checkedWebInteger(it, "backgroundAlpha")
    }
    options.quietZoneModules?.let {
        configuration.quietZoneModules = checkedWebInteger(it, "quietZoneModules")
    }
    options.moduleShape?.let {
        configuration.moduleShape = webEnum<QrModuleShape>(it, "moduleShape")
    }
    options.moduleScale?.let { configuration.moduleScale = it }
    options.roundedModuleRadiusFraction?.let {
        configuration.roundedModuleRadiusFraction = it
    }

    options.preserveFunctionPatterns?.let {
        configuration.preserveFunctionPatterns = it
        configuration.functionPatternStyle = null
    }
    options.functionPatternStyle?.let {
        configuration.functionPatternStyle =
            webEnum<QrFunctionPatternStyle>(it, "functionPatternStyle")
    }

    options.finderPatternShape?.let {
        configuration.finderPatternShape =
            webEnum<QrFinderPatternShape>(it, "finderPatternShape")
    }
    options.finderOuterShape?.let {
        configuration.finderOuterShape =
            webEnum<QrFinderPatternLayerShape>(it, "finderOuterShape")
    }
    options.finderCenterShape?.let {
        configuration.finderCenterShape =
            webEnum<QrFinderPatternLayerShape>(it, "finderCenterShape")
    }
    options.finderOuterColorHex?.let { configuration.finderOuterColorHex = it }
    options.finderOuterColorAlpha?.let {
        configuration.finderOuterColorAlpha =
            checkedWebInteger(it, "finderOuterColorAlpha")
    }
    options.finderCenterColorHex?.let { configuration.finderCenterColorHex = it }
    options.finderCenterColorAlpha?.let {
        configuration.finderCenterColorAlpha =
            checkedWebInteger(it, "finderCenterColorAlpha")
    }

    if (options.clearFinderOuterColor) {
        configuration.finderOuterColorHex = null
        configuration.finderOuterColorAlpha = 255
    }
    if (options.clearFinderCenterColor) {
        configuration.finderCenterColorHex = null
        configuration.finderCenterColorAlpha = 255
    }
    if (options.clearForegroundGradient) {
        configuration.foregroundGradient = null
    }

    options.foregroundGradient?.let { override ->
        configuration.foregroundGradient = QrGradientConfiguration().apply {
            startColorHex = override.startColor
            endColorHex = override.endColor
            override.startAlpha?.let {
                startAlpha = checkedWebInteger(it, "gradient.startAlpha")
            }
            override.endAlpha?.let {
                endAlpha = checkedWebInteger(it, "gradient.endAlpha")
            }
            override.startX?.let { startX = it }
            override.startY?.let { startY = it }
            override.endX?.let { endX = it }
            override.endY?.let { endY = it }
        }
    }

    options.logo?.let { override ->
        val logo = requireNotNull(configuration.logo)

        override.sizeFraction?.let { logo.sizeFraction = it }
        override.paddingFraction?.let { logo.paddingFraction = it }
        override.backgroundHex?.let { logo.backgroundHex = it }
        override.backgroundAlpha?.let {
            logo.backgroundAlpha = checkedWebInteger(it, "logo.backgroundAlpha")
        }
        override.backgroundShape?.let {
            logo.backgroundShape =
                webEnum<QrLogoBackgroundShape>(it, "logo.backgroundShape")
        }
        override.cornerRadiusFraction?.let { logo.cornerRadiusFraction = it }
    }

    val built = configuration.build()
    built.error?.let { throw IllegalArgumentException(it.message) }
    return requireNotNull(built.style)
}

internal fun checkedWebInteger(value: Int, name: String): Int {
    val number = value.toDouble()
    require(
        jsTypeOf(value) == "number" &&
                number.isFinite() &&
                number >= Int.MIN_VALUE.toDouble() &&
                number <= Int.MAX_VALUE.toDouble() &&
                number % 1.0 == 0.0,
    ) {
        "$name must be a finite 32-bit integer"
    }
    return value
}

internal inline fun <reified T : Enum<T>> webEnum(
    value: String,
    name: String,
): T {
    val normalized = value.trim()
        .replace("-", "")
        .replace("_", "")
        .lowercase()

    return enumValues<T>().firstOrNull {
        it.name.lowercase() == normalized
    } ?: throw IllegalArgumentException(
        "Unknown $name '$value'. Expected one of: " +
                enumValues<T>().joinToString { it.name },
    )
}
