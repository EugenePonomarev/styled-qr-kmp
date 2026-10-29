package io.github.eugeneponomarev.styledqr.render

/**
 * Measures the minimum contrast of the rendered foreground and finder layers against the QR
 * background. Null means that transparency prevents a complete measurement without a backdrop.
 * The caller indicates whether a logo is actually rendered, not just configured.
 *
 * Logo pixels and the contrast of an opaque logo background are not measured: the image itself
 * is unknown. A translucent logo background makes the overall result indeterminate.
 */
internal fun QrStyle.minimumRenderedContrast(logoIsRendered: Boolean): Double? {
    if (background.alpha != 255) return null
    if (logoIsRendered && logo?.background?.alpha?.let { it != 255 } == true) return null

    val gradient = foregroundGradient
    val foregroundContrast = if (gradient != null) {
        if (gradient.startColor.alpha != 255 || gradient.endColor.alpha != 255) return null
        gradient.minimumContrastAgainst(background)
    } else {
        if (foreground.alpha != 255) return null
        foreground.contrastRatioAgainst(background)
    }

    val appearance = resolvedFinderPatternAppearance()
    var minimum = foregroundContrast
    for (paint in listOf(appearance.outer.paint, appearance.center.paint)) {
        if (paint is QrFinderPatternLayerPaint.Solid) {
            if (paint.color.alpha != 255) return null
            minimum = minOf(minimum, paint.color.contrastRatioAgainst(background))
        }
    }
    return minimum
}

/** Exact over all RGB colours reachable by colorAt for representable progress in 0..1. */
internal fun QrLinearGradient.minimumContrastAgainst(background: QrColor): Double {
    var minimum = minOf(
        startColor.contrastRatioAgainst(background),
        endColor.contrastRatioAgainst(background),
    )
    var previousBits = 0L
    for (bits in quantizationBoundaries()) {
        if (bits == previousBits) continue
        minimum = minOf(minimum, colorAt(Double.fromBits(bits)).contrastRatioAgainst(background))
        previousBits = bits
    }
    return minimum
}
