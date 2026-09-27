package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
import io.github.eugeneponomarev.styledqr.theme.QrThemeId
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
public fun generateStyledQrSvg(
    content: String,
    errorCorrection: String = "H",
    foreground: String = "#000000",
    background: String = "#FFFFFF",
    moduleShape: String = "square",
    moduleScale: Double = 1.0,
    functionPatternStyle: String = "match-data-modules",
): String {
    require(content.isNotBlank()) { "content must not be blank" }

    return QrCodeGenerator.encodeText(
        text = content,
        errorCorrection = errorCorrection.toQrErrorCorrectionLevel(),
    ).toSvg(
        style = QrStyle(
            foreground = QrColor.fromHex(foreground),
            background = QrColor.fromHex(background),
            quietZoneModules = 4,
            moduleShape = moduleShape.toQrModuleShape(),
            moduleScale = moduleScale,
            functionPatternStyle = functionPatternStyle.toQrFunctionPatternStyle(),
            finderPatternShape = QrFinderPatternShape.MatchModuleShape,
        ),
    )
}

/** Generates a custom SVG with a validated global foreground gradient and optional logo. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun generateStyledQrSvgWithLinearGradient(
    content: String,
    options: StyledQrLinearGradientOptions,
): String {
    require(content.isNotBlank()) {
        "content must not be blank"
    }

    val backgroundColor =
        QrColor.fromHex(
            options.background,
        )

    val logo =
        options.logoDataUri
            .takeIf { it.isNotEmpty() }
            ?.let(::validatedWebLogoDataUri)

    return QrCodeGenerator.encodeText(
        text = content,
        errorCorrection =
            options.errorCorrection
                .toQrErrorCorrectionLevel(),
    ).toSvg(
        style = QrStyle(
            background = backgroundColor,
            quietZoneModules = 4,
            moduleShape =
                options.moduleShape
                    .toQrModuleShape(),
            moduleScale =
                options.moduleScale,
            functionPatternStyle =
                options.functionPatternStyle
                    .toQrFunctionPatternStyle(),
            finderPatternShape =
                QrFinderPatternShape
                    .MatchModuleShape,
            logo = logo?.let {
                QrLogoOptions(
                    background =
                        backgroundColor,
                )
            },
            foregroundGradient =
                QrLinearGradient(
                    startColor =
                        QrColor.fromHex(
                            options.startColor,
                        ),
                    endColor =
                        QrColor.fromHex(
                            options.endColor,
                        ),
                    start =
                        QrGradientPoint(
                            options.startX,
                            options.startY,
                        ),
                    end =
                        QrGradientPoint(
                            options.endX,
                            options.endY,
                        ),
                ),
        ),
        logoDataUri = logo,
    )
}

/** Returns the built-in theme identifiers in their display order. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun getStyledQrThemeIds(): Array<String> =
    QrThemes.all.map { it.id.name.lowercase() }.toTypedArray()

/** Generates SVG using a shared built-in theme without a logo. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun generateThemedQrSvg(
    content: String,
    themeId: String,
    errorCorrection: String = "H",
): String {
    require(content.isNotBlank()) { "content must not be blank" }

    val normalizedId = themeId.trim().uppercase()
    val id = QrThemeId.entries.firstOrNull { it.name == normalizedId }
        ?: throw IllegalArgumentException(
            "Unknown themeId '$themeId'. Expected one of: " +
                    getStyledQrThemeIds().joinToString(),
        )

    return QrCodeGenerator.encodeText(
        text = content,
        errorCorrection = errorCorrection.toQrErrorCorrectionLevel(),
    ).toSvg(
        style = QrThemes.get(id).createStyle(),
    )
}

/** Required to build an executable JavaScript distribution. */
public fun main() = Unit

private fun String.toQrErrorCorrectionLevel(): QrErrorCorrectionLevel = when (uppercase()) {
    "L" -> QrErrorCorrectionLevel.L
    "M" -> QrErrorCorrectionLevel.M
    "Q" -> QrErrorCorrectionLevel.Q
    "H" -> QrErrorCorrectionLevel.H
    else -> throw IllegalArgumentException(
        "errorCorrection must be one of L, M, Q, H; was '$this'",
    )
}

private fun String.toQrModuleShape(): QrModuleShape = when (lowercase()) {
    "square" -> QrModuleShape.Square
    "rounded-square", "rounded_square" -> QrModuleShape.RoundedSquare
    "circle" -> QrModuleShape.Circle
    "diamond" -> QrModuleShape.Diamond
    else -> throw IllegalArgumentException(
        "moduleShape must be square, rounded-square, circle, or diamond; was '$this'",
    )
}

private fun String.toQrFunctionPatternStyle(): QrFunctionPatternStyle = when (lowercase()) {
    "preserve-all", "preserve_all", "preserveall" ->
        QrFunctionPatternStyle.PreserveAll

    "preserve-finders-and-alignment",
    "preserve_finders_and_alignment",
    "preservefindersandalignment" ->
        QrFunctionPatternStyle.PreserveFindersAndAlignment

    "match-data-modules",
    "match_data_modules",
    "matchdatamodules" ->
        QrFunctionPatternStyle.MatchDataModules

    else -> throw IllegalArgumentException(
        "functionPatternStyle must be one of preserve-all, " +
            "preserve-finders-and-alignment, match-data-modules; was '$this'",
    )
}

/** Generates a themed SVG with an embedded PNG, JPEG, or WebP logo. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun generateThemedQrSvgWithLogo(
    content: String,
    themeId: String,
    logoDataUri: String,
    errorCorrection: String = "H",
): String {
    require(content.isNotBlank()) { "content must not be blank" }

    val normalizedId = themeId.trim().uppercase()
    val id = QrThemeId.entries.firstOrNull { it.name == normalizedId }
        ?: throw IllegalArgumentException("Unknown QR theme: '$themeId'")

    val theme = QrThemes.get(id)
    val logo = validatedWebLogoDataUri(logoDataUri)

    return QrCodeGenerator.encodeText(
        text = content,
        errorCorrection = errorCorrection.toQrErrorCorrectionLevel(),
    ).toSvg(
        style = theme.createStyle(logo = theme.createLogoOptions()),
        logoDataUri = logo,
    )
}

/** Generates a custom SVG with an embedded PNG, JPEG, or WebP logo. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun generateStyledQrSvgWithLogo(
    content: String,
    logoDataUri: String,
    errorCorrection: String = "H",
    foreground: String = "#000000",
    background: String = "#FFFFFF",
    moduleShape: String = "square",
    moduleScale: Double = 1.0,
    functionPatternStyle: String = "match-data-modules",
): String {
    require(content.isNotBlank()) { "content must not be blank" }

    val logo = validatedWebLogoDataUri(logoDataUri)
    val backgroundColor = QrColor.fromHex(background)

    return QrCodeGenerator.encodeText(
        text = content,
        errorCorrection = errorCorrection.toQrErrorCorrectionLevel(),
    ).toSvg(
        style = QrStyle(
            foreground = QrColor.fromHex(foreground),
            background = backgroundColor,
            quietZoneModules = 4,
            moduleShape = moduleShape.toQrModuleShape(),
            moduleScale = moduleScale,
            functionPatternStyle = functionPatternStyle.toQrFunctionPatternStyle(),
            finderPatternShape = QrFinderPatternShape.MatchModuleShape,
            logo = QrLogoOptions(background = backgroundColor),
        ),
        logoDataUri = logo,
    )
}

private fun validatedWebLogoDataUri(value: String): String {
    val normalized = value.trim()
    val payloadLength = normalized.length - normalized.indexOf(',') - 1

    require(WEB_LOGO_DATA_URI.matches(normalized) && payloadLength % 4 == 0) {
        "logoDataUri must be a base64 data URI for a PNG, JPEG, or WebP image"
    }

    return normalized
}

private val WEB_LOGO_DATA_URI: Regex = Regex(
    "data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}",
)

@OptIn(ExperimentalJsExport::class)
@JsExport
public class StyledQrLinearGradientOptions(
    public val startColor: String,
    public val endColor: String,
) {
    public var startX: Double = 0.0
    public var startY: Double = 0.0
    public var endX: Double = 1.0
    public var endY: Double = 1.0

    public var background: String = "#FFFFFF"

    public var moduleShape: String =
        "square"

    public var moduleScale: Double =
        1.0

    public var functionPatternStyle: String =
        "match-data-modules"

    public var errorCorrection: String =
        "H"

    public var logoDataUri: String =
        ""
}
