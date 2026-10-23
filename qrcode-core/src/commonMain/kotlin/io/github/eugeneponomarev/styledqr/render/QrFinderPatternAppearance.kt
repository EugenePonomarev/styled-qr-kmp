package io.github.eugeneponomarev.styledqr.render

/**
 * Shape used by one dark layer of every QR finder pattern.
 *
 * [Inherit] retains the legacy [QrStyle.finderPatternShape] behaviour.
 */
public enum class QrFinderPatternLayerShape {
    Inherit,
    Square,
    RoundedSquare,
    Circle,
    Diamond,
}

/**
 * Shared appearance of all three finder patterns.
 *
 * The middle 5×5 layer remains [QrStyle.background]. Only the outer 7×7 and
 * inner 3×3 dark layers can receive independent shapes or solid colours.
 */
public data class QrFinderPatternAppearance(
    val outerShape: QrFinderPatternLayerShape =
        QrFinderPatternLayerShape.Inherit,
    val centerShape: QrFinderPatternLayerShape =
        QrFinderPatternLayerShape.Inherit,
    val outerColor: QrColor? = null,
    val centerColor: QrColor? = null,
)

internal sealed interface QrFinderPatternLayerPaint {
    data object Foreground : QrFinderPatternLayerPaint

    data class Solid(
        val color: QrColor,
    ) : QrFinderPatternLayerPaint
}

internal data class ResolvedQrFinderPatternLayer(
    val shape: QrModuleShape,
    val paint: QrFinderPatternLayerPaint,
)

internal data class ResolvedQrFinderPatternAppearance(
    val outer: ResolvedQrFinderPatternLayer,
    val middle: ResolvedQrFinderPatternLayer,
    val center: ResolvedQrFinderPatternLayer,
)

internal fun QrStyle.resolvedFinderPatternAppearance(): ResolvedQrFinderPatternAppearance {
    val inheritedShape = resolvedFinderPatternShape()

    val outer = ResolvedQrFinderPatternLayer(
        shape = finderPatternAppearance.outerShape.resolve(inheritedShape),
        paint = finderPatternAppearance.outerColor?.let(
            QrFinderPatternLayerPaint::Solid,
        ) ?: QrFinderPatternLayerPaint.Foreground,
    )

    return ResolvedQrFinderPatternAppearance(
        outer = outer,
        middle = ResolvedQrFinderPatternLayer(
            shape = outer.shape,
            paint = QrFinderPatternLayerPaint.Solid(background),
        ),
        center = ResolvedQrFinderPatternLayer(
            shape = finderPatternAppearance.centerShape.resolve(inheritedShape),
            paint = finderPatternAppearance.centerColor?.let(
                QrFinderPatternLayerPaint::Solid,
            ) ?: QrFinderPatternLayerPaint.Foreground,
        ),
    )
}

internal fun QrFinderPatternAppearance.validateAgainst(
    background: QrColor,
) {
    outerColor?.requireOpaqueDarkForegroundAgainst(
        background = background,
        description = "Finder outer color",
    )
    centerColor?.requireOpaqueDarkForegroundAgainst(
        background = background,
        description = "Finder center color",
    )
}

private fun QrFinderPatternLayerShape.resolve(
    inheritedShape: QrModuleShape,
): QrModuleShape = when (this) {
    QrFinderPatternLayerShape.Inherit -> inheritedShape
    QrFinderPatternLayerShape.Square -> QrModuleShape.Square
    QrFinderPatternLayerShape.RoundedSquare -> QrModuleShape.RoundedSquare
    QrFinderPatternLayerShape.Circle -> QrModuleShape.Circle
    QrFinderPatternLayerShape.Diamond -> QrModuleShape.Diamond
}
