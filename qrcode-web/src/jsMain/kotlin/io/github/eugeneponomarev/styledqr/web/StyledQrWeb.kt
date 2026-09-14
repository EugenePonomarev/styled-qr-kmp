package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.toSvg
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
