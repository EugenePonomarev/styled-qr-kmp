package io.github.eugeneponomarev.styledqr.theme

import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrModuleShape

/** Built-in visual themes shared by all rendering platforms. */
public object QrThemes {

    public val Aurora: QrTheme = createTheme(
        id = QrThemeId.AURORA,
        foreground = "#0B6E69",
        background = "#F4FBF9",
        moduleShape = QrModuleShape.Circle,
        moduleScalePermille = 860,
        logoSizePermille = 180,
    )

    public val Fintech: QrTheme = createTheme(
        id = QrThemeId.FINTECH,
        foreground = "#0B3A82",
        background = "#FFFFFF",
        moduleShape = QrModuleShape.RoundedSquare,
        moduleScalePermille = 900,
        logoSizePermille = 200,
        functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
        finderPatternShape = QrFinderPatternShape.Square,
    )

    public val Minimal: QrTheme = createTheme(
        id = QrThemeId.MINIMAL,
        foreground = "#171717",
        background = "#FFFFFF",
        moduleShape = QrModuleShape.Square,
        moduleScalePermille = 940,
        logoSizePermille = 160,
        functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
        finderPatternShape = QrFinderPatternShape.Square,
    )

    public val Neon: QrTheme = createTheme(
        id = QrThemeId.NEON,
        foreground = "#5B21B6",
        background = "#F7F7FF",
        moduleShape = QrModuleShape.Diamond,
        moduleScalePermille = 840,
        logoSizePermille = 160,
    )

    public val Wedding: QrTheme = createTheme(
        id = QrThemeId.WEDDING,
        foreground = "#79334F",
        background = "#FFF9F7",
        moduleShape = QrModuleShape.Circle,
        moduleScalePermille = 860,
        logoSizePermille = 180,
    )

    public val Coffee: QrTheme = createTheme(
        id = QrThemeId.COFFEE,
        foreground = "#4E2D20",
        background = "#FFF8EE",
        moduleShape = QrModuleShape.RoundedSquare,
        moduleScalePermille = 900,
        logoSizePermille = 170,
    )

    public val Cyber: QrTheme = createTheme(
        id = QrThemeId.CYBER,
        foreground = "#0B1F3A",
        background = "#F5FAFF",
        moduleShape = QrModuleShape.Diamond,
        moduleScalePermille = 820,
        logoSizePermille = 160,
        functionPatternStyle = QrFunctionPatternStyle.MatchDataModules,
        finderPatternShape = QrFinderPatternShape.Square,
    )

    /** Themes in a stable display order. Each access returns a new list. */
    public val all: List<QrTheme>
        get() = listOf(Aurora, Fintech, Minimal, Neon, Wedding, Coffee, Cyber)

    public fun get(id: QrThemeId): QrTheme = when (id) {
        QrThemeId.AURORA -> Aurora
        QrThemeId.FINTECH -> Fintech
        QrThemeId.MINIMAL -> Minimal
        QrThemeId.NEON -> Neon
        QrThemeId.WEDDING -> Wedding
        QrThemeId.COFFEE -> Coffee
        QrThemeId.CYBER -> Cyber
    }

    private fun createTheme(
        id: QrThemeId,
        foreground: String,
        background: String,
        moduleShape: QrModuleShape,
        moduleScalePermille: Int,
        logoSizePermille: Int,
        functionPatternStyle: QrFunctionPatternStyle =
            QrFunctionPatternStyle.MatchDataModules,
        finderPatternShape: QrFinderPatternShape =
            QrFinderPatternShape.MatchModuleShape,
    ): QrTheme {
        val backgroundColor = QrColor.fromHex(background)

        return QrTheme(
            id = id,
            definition = QrThemeDefinition(
                foreground = QrColor.fromHex(foreground),
                background = backgroundColor,
                moduleShape = moduleShape,
                moduleScalePermille = moduleScalePermille,
                functionPatternStyle = functionPatternStyle,
                finderPatternShape = finderPatternShape,
                logoDefaults = QrThemeLogoDefaults(
                    sizePermille = logoSizePermille,
                    paddingPermille = 40,
                    background = backgroundColor,
                ),
            ),
        )
    }
}