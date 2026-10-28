package io.github.eugeneponomarev.styledqr.gallery

import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout

internal fun renderGalleryManifest(
    cases: List<RenderedGalleryCase>,
): String = cases.joinToString(
    separator = ",\n",
    prefix = "{\n  \"schemaVersion\": 1,\n  \"cases\": [\n",
    postfix = "\n  ]\n}\n",
) {
    "    ${galleryJson(caseManifest(it))}"
}

private fun caseManifest(
    rendered: RenderedGalleryCase,
): Map<String, Any?> {
    val recipe = rendered.recipe
    val qrCode = rendered.qrCode

    return linkedMapOf(
        "id" to recipe.id,
        "filename" to recipe.fileName,
        "caption" to recipe.caption,
        "encoding" to linkedMapOf(
            "payload" to recipe.payload,
            "requestedErrorCorrection" to recipe.requestedErrorCorrection.name,
            "minVersion" to recipe.minVersion,
            "maxVersion" to recipe.maxVersion,
            "actualVersion" to qrCode.version,
            "actualErrorCorrection" to qrCode.errorCorrection.name,
            "matrixSize" to qrCode.size,
        ),
        "themeId" to recipe.theme?.id?.name?.lowercase(),
        "style" to styleManifest(rendered.style),
        "logoSource" to recipe.logoSource?.let {
            "docs/gallery-inputs/$it"
        },
        "logoLayout" to rendered.style.logo?.let {
            val layout = qrCode.calculateLogoLayout(it)

            linkedMapOf(
                "leftModule" to layout.leftModule,
                "topModule" to layout.topModule,
                "sizeModules" to layout.sizeModules,
                "paddingModules" to layout.paddingModules,
                "logoSizeModules" to layout.logoSizeModules,
            )
        },
        "rasterOptions" to recipe.rasterOptions?.let {
            linkedMapOf(
                "sizePx" to it.sizePx,
                "sizeMode" to it.sizeMode.name,
            )
        },
    )
}

private fun styleManifest(style: QrStyle): Map<String, Any?> =
    linkedMapOf(
        "foreground" to colorManifest(style.foreground),
        "background" to colorManifest(style.background),
        "quietZoneModules" to style.quietZoneModules,
        "moduleShape" to style.moduleShape.name,
        "moduleScale" to style.moduleScale,
        "roundedModuleRadiusFraction" to style.roundedModuleRadiusFraction,
        "preserveFunctionPatterns" to style.preserveFunctionPatterns,
        "functionPatternStyle" to style.functionPatternStyle.name,
        "finderPatternShape" to style.finderPatternShape.name,
        "finderPatternAppearance" to linkedMapOf(
            "outerShape" to style.finderPatternAppearance.outerShape.name,
            "centerShape" to style.finderPatternAppearance.centerShape.name,
            "outerColor" to style.finderPatternAppearance.outerColor?.let(::colorManifest),
            "centerColor" to style.finderPatternAppearance.centerColor?.let(::colorManifest),
        ),
        "foregroundGradient" to style.foregroundGradient?.let {
            linkedMapOf(
                "startColor" to colorManifest(it.startColor),
                "endColor" to colorManifest(it.endColor),
                "start" to linkedMapOf(
                    "x" to it.start.x,
                    "y" to it.start.y,
                ),
                "end" to linkedMapOf(
                    "x" to it.end.x,
                    "y" to it.end.y,
                ),
            )
        },
        "logo" to style.logo?.let {
            linkedMapOf(
                "sizeFraction" to it.sizeFraction,
                "paddingFraction" to it.paddingFraction,
                "background" to colorManifest(it.background),
                "backgroundShape" to it.backgroundShape.name,
                "cornerRadiusFraction" to it.cornerRadiusFraction,
            )
        },
    )

private fun colorManifest(color: QrColor): List<Int> = listOf(
    color.red,
    color.green,
    color.blue,
    color.alpha,
)