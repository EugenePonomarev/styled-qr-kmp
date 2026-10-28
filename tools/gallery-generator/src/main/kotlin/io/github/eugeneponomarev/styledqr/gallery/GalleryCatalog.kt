package io.github.eugeneponomarev.styledqr.gallery

import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerShape
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.theme.QrTheme
import io.github.eugeneponomarev.styledqr.theme.QrThemes

internal data class GalleryRecipe(
    val id: String,
    val caption: String,
    val theme: QrTheme? = null,
    val styleFactory: (() -> QrStyle)? = null,
    val logoOptions: QrLogoOptions? = null,
    val logoSource: String? = null,
    val rasterOptions: QrRasterRenderOptions? = null,
    val payload: String = "https://example.com",
    val requestedErrorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
    val minVersion: Int = 1,
    val maxVersion: Int = 40,
) {
    init {
        require(GALLERY_ID.matches(id)) {
            "Invalid gallery case ID: $id"
        }
        require((theme != null) != (styleFactory != null)) {
            "Gallery case '$id' must specify exactly one theme or style factory"
        }
        require((logoOptions != null) == (logoSource != null)) {
            "Gallery case '$id' must specify logo options and source together"
        }
        logoSource?.let(::requireGallerySvgFileName)
    }

    val fileName: String
        get() = "$id.svg"

    fun createStyle(): QrStyle {
        theme?.let {
            return it.createStyle(logo = logoOptions)
        }

        return requireNotNull(styleFactory).invoke().also {
            require(it.logo == logoOptions) {
                "Custom style logo must match recipe logoOptions"
            }
        }
    }
}

internal object GalleryCatalog {
    val recipes: List<GalleryRecipe> = listOf(
        themeCase("theme-aurora", "Aurora theme", QrThemes.Aurora),
        themeCase("theme-fintech", "Fintech theme", QrThemes.Fintech),
        themeCase("theme-minimal", "Minimal theme", QrThemes.Minimal),
        themeCase("theme-neon", "Neon theme", QrThemes.Neon),
        themeCase("theme-wedding", "Wedding theme", QrThemes.Wedding),
        themeCase("theme-coffee", "Coffee theme", QrThemes.Coffee),
        themeCase("theme-cyber", "Cyber theme", QrThemes.Cyber),
        GalleryRecipe(
            id = "linear-gradient",
            caption = "Safe linear gradient",
            styleFactory = {
                QrStyle(
                    foreground = QrColor.fromHex("#075985"),
                    background = QrColor.White,
                    moduleShape = QrModuleShape.RoundedSquare,
                    moduleScale = 0.90,
                    functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
                    foregroundGradient = QrLinearGradient(
                        startColor = QrColor.fromHex("#075985"),
                        endColor = QrColor.fromHex("#6D28D9"),
                    ),
                )
            },
        ),
        GalleryRecipe(
            id = "branded-finders",
            caption = "Rounded outer finders with circular centres",
            styleFactory = {
                QrStyle(
                    foreground = QrColor.fromHex("#143A5A"),
                    background = QrColor.White,
                    moduleShape = QrModuleShape.RoundedSquare,
                    moduleScale = 0.94,
                    functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
                    finderPatternAppearance = QrFinderPatternAppearance(
                        outerShape = QrFinderPatternLayerShape.RoundedSquare,
                        centerShape = QrFinderPatternLayerShape.Circle,
                        outerColor = QrColor.fromHex("#075985"),
                        centerColor = QrColor.fromHex("#6D28D9"),
                    ),
                )
            },
        ),
        GalleryRecipe(
            id = "logo-backed",
            caption = "Fintech with a backed centre logo",
            theme = QrThemes.Fintech,
            logoOptions = QrLogoOptions(
                sizeFraction = 0.12,
                paddingFraction = 0.02,
                background = QrColor.White,
            ),
            logoSource = "logo-mark.svg",
        ),
        GalleryRecipe(
            id = "raster-integer-modules",
            caption = "Minimal at 512 pixels with integer modules",
            theme = QrThemes.Minimal,
            rasterOptions = QrRasterRenderOptions(
                sizePx = 512,
                sizeMode = QrRasterSizeMode.IntegerModules,
            ),
        ),
        GalleryRecipe(
            id = "raster-fit",
            caption = "Minimal at 512 pixels with fit sizing",
            theme = QrThemes.Minimal,
            rasterOptions = QrRasterRenderOptions(
                sizePx = 512,
                sizeMode = QrRasterSizeMode.Fit,
            ),
        ),
        GalleryRecipe(
            id = "story-square",
            caption = "A complete short story with square modules",
            theme = QrThemes.Minimal,
            payload = GalleryPayloads.lighthouseStory,
            rasterOptions = storyOutput(),
        ),
        GalleryRecipe(
            id = "story-circle",
            caption = "A complete short story with circular modules",
            styleFactory = {
                storyCircleStyle()
            },
            payload = GalleryPayloads.lighthouseStory,
            rasterOptions = storyOutput(),
        ),
        GalleryRecipe(
            id = "logo-monogram",
            caption = "Fintech with a circular monogram",
            theme = QrThemes.Fintech,
            logoOptions = showcaseLogoOptions(),
            logoSource = "logo-monogram-mark.svg",
        ),
        GalleryRecipe(
            id = "logo-lighthouse",
            caption = "Aurora with a lighthouse illustration",
            theme = QrThemes.Aurora,
            logoOptions = showcaseLogoOptions(),
            logoSource = "logo-lighthouse-mark.svg",
        ),
        GalleryRecipe(
            id = "story-circle-logo",
            caption = "A complete short story with circular modules and a lighthouse",
            styleFactory = {
                storyCircleStyle(logo = storyLogoOptions())
            },
            logoOptions = storyLogoOptions(),
            logoSource = "logo-lighthouse-mark.svg",
            payload = GalleryPayloads.lighthouseStory,
            rasterOptions = storyOutput(),
        ),
        GalleryRecipe(
            id = "story-gradient-logo",
            caption = "A complete short story with a gradient and monogram",
            styleFactory = {
                QrThemes.Fintech.createGradientStyle(
                    gradient = QrLinearGradient(
                        startColor = QrColor.fromHex("#075985"),
                        endColor = QrColor.fromHex("#6D28D9"),
                    ),
                    logo = storyLogoOptions(),
                )
            },
            logoOptions = storyLogoOptions(),
            logoSource = "logo-monogram-mark.svg",
            payload = GalleryPayloads.lighthouseStory,
            rasterOptions = storyOutput(),
        ),
    )

    private fun themeCase(
        id: String,
        caption: String,
        theme: QrTheme,
    ): GalleryRecipe = GalleryRecipe(
        id = id,
        caption = caption,
        theme = theme,
    )

    private fun showcaseLogoOptions(): QrLogoOptions = QrLogoOptions(
        sizeFraction = 0.20,
        paddingFraction = 0.02,
        background = QrColor.White,
        backgroundShape = QrLogoBackgroundShape.Circle,
    )

    private fun storyLogoOptions(): QrLogoOptions = QrLogoOptions(
        sizeFraction = 0.16,
        paddingFraction = 0.02,
        background = QrColor.White,
        backgroundShape = QrLogoBackgroundShape.Circle,
    )

    private fun storyOutput(): QrRasterRenderOptions = QrRasterRenderOptions(
        sizePx = 1260,
        sizeMode = QrRasterSizeMode.IntegerModules,
    )

    private fun storyCircleStyle(
        logo: QrLogoOptions? = null,
    ): QrStyle = QrStyle(
        foreground = QrColor.fromHex("#0B3A82"),
        background = QrColor.White,
        moduleShape = QrModuleShape.Circle,
        moduleScale = 0.94,
        functionPatternStyle = QrFunctionPatternStyle.PreserveAll,
        finderPatternAppearance = QrFinderPatternAppearance(
            outerShape = QrFinderPatternLayerShape.RoundedSquare,
            centerShape = QrFinderPatternLayerShape.Circle,
            outerColor = QrColor.fromHex("#0B3A82"),
            centerColor = QrColor.fromHex("#6D28D9"),
        ),
        logo = logo,
    )
}

internal fun requireGalleryFileName(fileName: String) {
    if (fileName != "manifest.json") {
        requireGallerySvgFileName(fileName)
    }
}

internal fun requireGallerySvgFileName(fileName: String) {
    require(
        fileName.endsWith(".svg") &&
                GALLERY_ID.matches(fileName.removeSuffix(".svg")),
    ) {
        "Invalid gallery SVG filename: $fileName"
    }
}

private val GALLERY_ID = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")