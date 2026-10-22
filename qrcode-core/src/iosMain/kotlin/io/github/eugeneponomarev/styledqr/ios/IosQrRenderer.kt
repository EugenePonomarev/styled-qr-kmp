@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerPaint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoLayout
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrRenderException
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.ResolvedQrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.ResolvedQrFinderPatternLayer
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.calculateQrRasterGeometry
import io.github.eugeneponomarev.styledqr.render.finderPatternTopLefts
import io.github.eugeneponomarev.styledqr.render.isFinderPatternCore
import io.github.eugeneponomarev.styledqr.render.qrImageTotalModules
import io.github.eugeneponomarev.styledqr.render.resolvedFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.shouldPreserveAsSquare
import io.github.eugeneponomarev.styledqr.render.toPointGeometry
import io.github.eugeneponomarev.styledqr.render.validateQrImagePixelSide
import io.github.eugeneponomarev.styledqr.render.validateQrImageRenderArguments
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextDrawLinearGradient
import platform.CoreGraphics.CGContextEOClip
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGGradientCreateWithColorComponents
import platform.CoreGraphics.CGGradientRelease
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.CoreGraphics.kCGGradientDrawsAfterEndLocation
import platform.CoreGraphics.kCGGradientDrawsBeforeStartLocation
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

/** Renders a [QrCode] using the historical point-based geometry. */
public fun QrCode.toUIImage(
    sizePoints: Double,
    style: QrStyle = QrStyle(),
    scale: Double = 0.0,
    logo: UIImage? = null,
): UIImage {
    validateQrImageRenderArguments(sizePoints, scale)

    val totalModules = qrImageTotalModules(size, style.quietZoneModules)
    if (sizePoints < totalModules) {
        throw QrRenderException(
            QrRenderErrorCode.InvalidRenderParameters,
            "sizePoints must be at least $totalModules to represent every QR module",
        )
    }

    val effectiveScale = if (scale == 0.0) UIScreen.mainScreen.scale else scale
    validateQrImagePixelSide(sizePoints, effectiveScale)

    return renderUIImage(
        sizePoints = sizePoints,
        style = style,
        scale = scale,
        logo = logo,
        moduleSize = sizePoints / totalModules,
        offset = 0.0,
    )
}

/**
 * Renders an image with an explicitly requested physical pixel side.
 *
 * Scale controls the UIImage's point size. Zero resolves to the screen scale.
 * It does not multiply the requested pixel side.
 */
public fun QrCode.toUIImageWithOptions(
    options: QrRasterRenderOptions,
    style: QrStyle = QrStyle(),
    scale: Double = 0.0,
    logo: UIImage? = null,
): UIImage {
    validateQrImageRenderArguments(options.sizePx.toDouble(), scale)

    val pixels = calculateQrRasterGeometry(
        options = options,
        totalModules = qrImageTotalModules(size, style.quietZoneModules),
    )
    val effectiveScale = if (scale == 0.0) UIScreen.mainScreen.scale else scale
    val points = pixels.toPointGeometry(effectiveScale)

    val image = renderUIImage(
        sizePoints = points.sizePoints,
        style = style,
        scale = effectiveScale,
        logo = logo,
        moduleSize = points.moduleSizePoints,
        offset = points.offsetPoints,
    )

    val cgImage = image.CGImage
    if (
        cgImage == null ||
        platform.CoreGraphics.CGImageGetWidth(cgImage).toLong() !=
        options.sizePx.toLong() ||
        platform.CoreGraphics.CGImageGetHeight(cgImage).toLong() !=
        options.sizePx.toLong()
    ) {
        throw QrRenderException(
            QrRenderErrorCode.RenderFailed,
            "UIKit did not produce the requested ${options.sizePx}px image side",
        )
    }

    return image
}

private fun QrCode.renderUIImage(
    sizePoints: Double,
    style: QrStyle,
    scale: Double,
    logo: UIImage?,
    moduleSize: Double,
    offset: Double,
): UIImage {
    val logoOptions = if (logo == null) null else style.logo ?: QrLogoOptions()

    val logoLayout = logoOptions?.let {
        try {
            calculateLogoLayout(it)
        } catch (error: IllegalArgumentException) {
            throw QrRenderException(
                QrRenderErrorCode.LogoValidationFailed,
                error.message ?: "Invalid QR logo placement",
                error,
            )
        }
    }
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(sizePoints, sizePoints), false, scale)
    try {
        style.background.toUiColor().setFill()
        UIBezierPath.bezierPathWithRect(CGRectMake(0.0, 0.0, sizePoints, sizePoints)).fill()

        val context = requireNotNull(UIGraphicsGetCurrentContext()) {
            "Unable to get the QR graphics context"
        }

        // The background has already filled the entire image.
        // Translate all QR drawing, including gradients and the logo, together.
        if (offset != 0.0) {
            CGContextTranslateCTM(context, offset, offset)
        }

        CGContextSaveGState(context)
        try {
            if (logoLayout != null) {
                val visibleArea = UIBezierPath.bezierPathWithRect(
                    CGRectMake(0.0, 0.0, sizePoints, sizePoints),
                )
                visibleArea.appendPath(
                    createLogoPath(
                        left = (style.quietZoneModules + logoLayout.leftModule) * moduleSize,
                        top = (style.quietZoneModules + logoLayout.topModule) * moduleSize,
                        boxSize = logoLayout.sizeModules * moduleSize,
                        options = requireNotNull(logoOptions),
                    ),
                )
                visibleArea.usesEvenOddFillRule = true
                visibleArea.addClip()
            }

            val gradient = style.foregroundGradient
            val foregroundPath = gradient?.let { UIBezierPath.bezierPath() }
            if (gradient == null) style.foreground.toUiColor().setFill()
            for (row in 0 until size) {
                for (column in 0 until size) {
                    if (
                        !this[row, column] ||
                        isFinderPatternCore(row, column)
                    ) continue

                    val preserveAsSquare = shouldPreserveAsSquare(row, column, style)
                    val shape = if (preserveAsSquare) {
                        QrModuleShape.Square
                    } else {
                        style.moduleShape
                    }
                    val moduleScale = if (preserveAsSquare) 1.0 else style.moduleScale
                    val modulePath = drawModulePath(
                        left = (column + style.quietZoneModules) * moduleSize,
                        top = (row + style.quietZoneModules) * moduleSize,
                        moduleSize = moduleSize,
                        scale = moduleScale,
                        shape = shape,
                        roundedRadiusFraction = style.roundedModuleRadiusFraction,
                    )
                    if (foregroundPath == null) modulePath.fill() else foregroundPath.appendPath(
                        modulePath
                    )
                }
            }

            foregroundPath?.let { path ->
                drawForegroundGradient(
                    path = path,
                    gradient = requireNotNull(gradient),
                    qrSizeModules = size,
                    quietZoneModules = style.quietZoneModules,
                    moduleSize = moduleSize,
                )
            }

            drawFinderPatterns(
                qrCode = this,
                moduleSize = moduleSize,
                quietZoneModules = style.quietZoneModules,
                appearance = style.resolvedFinderPatternAppearance(),
                roundedRadiusFraction = style.roundedModuleRadiusFraction,
                foreground = style.foreground,
                gradient = gradient,
            )
        } finally {
            CGContextRestoreGState(context)
        }

        if (logo != null) {
            drawLogo(
                moduleSize = moduleSize,
                quietZoneModules = style.quietZoneModules,
                layout = requireNotNull(logoLayout),
                options = requireNotNull(logoOptions),
                logo = logo,
            )
        }

        return requireNotNull(UIGraphicsGetImageFromCurrentImageContext()) {
            "Unable to create the QR UIImage"
        }
    } finally {
        UIGraphicsEndImageContext()
    }
}

private fun drawForegroundGradient(
    path: UIBezierPath,
    gradient: QrLinearGradient,
    qrSizeModules: Int,
    quietZoneModules: Int,
    moduleSize: Double,
) {
    val context = requireNotNull(
        UIGraphicsGetCurrentContext(),
    ) {
        "Unable to get the QR graphics context"
    }

    val colorSpace = requireNotNull(
        CGColorSpaceCreateWithName(kCGColorSpaceSRGB),
    ) {
        "Unable to create sRGB color space"
    }

    try {
        val colors = cValuesOf(
            gradient.startColor.red / 255.0,
            gradient.startColor.green / 255.0,
            gradient.startColor.blue / 255.0,
            1.0,
            gradient.endColor.red / 255.0,
            gradient.endColor.green / 255.0,
            gradient.endColor.blue / 255.0,
            1.0,
        )

        val cgGradient = requireNotNull(
            CGGradientCreateWithColorComponents(
                colorSpace,
                colors,
                null,
                2uL,
            ),
        ) {
            "Unable to create QR foreground gradient"
        }

        try {
            CGContextSaveGState(context)

            try {
                CGContextAddPath(context, path.CGPath)
                CGContextEOClip(context)

                CGContextDrawLinearGradient(
                    context,
                    cgGradient,
                    CGPointMake(
                        (quietZoneModules + gradient.start.x * qrSizeModules) *
                                moduleSize,
                        (quietZoneModules + gradient.start.y * qrSizeModules) *
                                moduleSize,
                    ),
                    CGPointMake(
                        (quietZoneModules + gradient.end.x * qrSizeModules) *
                                moduleSize,
                        (quietZoneModules + gradient.end.y * qrSizeModules) *
                                moduleSize,
                    ),
                    kCGGradientDrawsBeforeStartLocation or
                            kCGGradientDrawsAfterEndLocation,
                )
            } finally {
                CGContextRestoreGState(context)
            }
        } finally {
            CGGradientRelease(cgGradient)
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
}

private fun drawFinderPatterns(
    qrCode: QrCode,
    moduleSize: Double,
    quietZoneModules: Int,
    appearance: ResolvedQrFinderPatternAppearance,
    roundedRadiusFraction: Double,
    foreground: QrColor,
    gradient: QrLinearGradient?,
) {
    drawFinderPatternLayer(
        qrCode = qrCode,
        moduleSize = moduleSize,
        quietZoneModules = quietZoneModules,
        layer = appearance.outer,
        offsetModules = 0.0,
        layerSizeModules = 7.0,
        roundedRadiusFraction = roundedRadiusFraction,
        foreground = foreground,
        gradient = gradient,
    )
    drawFinderPatternLayer(
        qrCode = qrCode,
        moduleSize = moduleSize,
        quietZoneModules = quietZoneModules,
        layer = appearance.middle,
        offsetModules = 1.0,
        layerSizeModules = 5.0,
        roundedRadiusFraction = roundedRadiusFraction,
        foreground = foreground,
        gradient = gradient,
    )
    drawFinderPatternLayer(
        qrCode = qrCode,
        moduleSize = moduleSize,
        quietZoneModules = quietZoneModules,
        layer = appearance.center,
        offsetModules = 2.0,
        layerSizeModules = 3.0,
        roundedRadiusFraction = roundedRadiusFraction,
        foreground = foreground,
        gradient = gradient,
    )
}

private fun drawFinderPatternLayer(
    qrCode: QrCode,
    moduleSize: Double,
    quietZoneModules: Int,
    layer: ResolvedQrFinderPatternLayer,
    offsetModules: Double,
    layerSizeModules: Double,
    roundedRadiusFraction: Double,
    foreground: QrColor,
    gradient: QrLinearGradient?,
) {
    val path = UIBezierPath.bezierPath()

    qrCode.finderPatternTopLefts().forEach { (row, column) ->
        path.appendPath(
            drawFinderLayer(
                left = (column + quietZoneModules + offsetModules) * moduleSize,
                top = (row + quietZoneModules + offsetModules) * moduleSize,
                size = layerSizeModules * moduleSize,
                shape = layer.shape,
                roundedRadiusFraction = roundedRadiusFraction,
            ),
        )
    }

    when (val paint = layer.paint) {
        QrFinderPatternLayerPaint.Foreground -> {
            if (gradient == null) {
                foreground.toUiColor().setFill()
                path.fill()
            } else {
                drawForegroundGradient(
                    path = path,
                    gradient = gradient,
                    qrSizeModules = qrCode.size,
                    quietZoneModules = quietZoneModules,
                    moduleSize = moduleSize,
                )
            }
        }

        is QrFinderPatternLayerPaint.Solid -> {
            paint.color.toUiColor().setFill()
            path.fill()
        }
    }
}

private fun drawFinderLayer(
    left: Double,
    top: Double,
    size: Double,
    shape: QrModuleShape,
    roundedRadiusFraction: Double,
): UIBezierPath = drawModulePath(
    left = left,
    top = top,
    moduleSize = size,
    scale = 1.0,
    shape = shape,
    roundedRadiusFraction = roundedRadiusFraction,
)

private fun drawModulePath(
    left: Double,
    top: Double,
    moduleSize: Double,
    scale: Double,
    shape: QrModuleShape,
    roundedRadiusFraction: Double,
): UIBezierPath {
    val inset = moduleSize * (1.0 - scale) / 2.0
    val width = moduleSize - inset * 2.0
    val centreX = left + moduleSize / 2.0
    val centreY = top + moduleSize / 2.0
    val rect = CGRectMake(
        left + inset,
        top + inset,
        width,
        width,
    )

    return when (shape) {
        QrModuleShape.Square -> UIBezierPath.bezierPathWithRect(rect)
        QrModuleShape.RoundedSquare -> UIBezierPath.bezierPathWithRoundedRect(
            rect,
            width * roundedRadiusFraction,
        )

        QrModuleShape.Circle -> UIBezierPath.bezierPathWithOvalInRect(rect)
        QrModuleShape.Diamond -> UIBezierPath.bezierPath().apply {
            val half = width / 2.0
            moveToPoint(CGPointMake(centreX, centreY - half))
            addLineToPoint(CGPointMake(centreX + half, centreY))
            addLineToPoint(CGPointMake(centreX, centreY + half))
            addLineToPoint(CGPointMake(centreX - half, centreY))
            closePath()
        }
    }
}

private fun createLogoPath(
    left: Double,
    top: Double,
    boxSize: Double,
    options: QrLogoOptions,
    inset: Double = 0.0,
): UIBezierPath {
    val side = boxSize - inset * 2.0
    val rect = CGRectMake(left + inset, top + inset, side, side)
    return when (options.backgroundShape) {
        QrLogoBackgroundShape.Square -> UIBezierPath.bezierPathWithRect(rect)
        QrLogoBackgroundShape.RoundedSquare -> UIBezierPath.bezierPathWithRoundedRect(
            rect,
            (boxSize * options.cornerRadiusFraction - inset).coerceIn(0.0, side / 2.0),
        )

        QrLogoBackgroundShape.Circle -> UIBezierPath.bezierPathWithOvalInRect(rect)
    }
}

private fun drawLogo(
    moduleSize: Double,
    quietZoneModules: Int,
    layout: QrLogoLayout,
    options: QrLogoOptions,
    logo: UIImage,
) {
    val boxSize = layout.sizeModules * moduleSize
    val padding = layout.paddingModules * moduleSize
    val logoSize = boxSize - padding * 2.0
    val left = (quietZoneModules + layout.leftModule) * moduleSize
    val top = (quietZoneModules + layout.topModule) * moduleSize

    val (imageWidth, imageHeight) = logo.size.useContents {
        width to height
    }
    if (!(
                imageWidth.isFinite() &&
                        imageHeight.isFinite() &&
                        imageWidth > 0.0 &&
                        imageHeight > 0.0
                )
    ) {
        throw QrRenderException(
            QrRenderErrorCode.LogoValidationFailed,
            "Logo image must have a finite, positive size",
        )
    }

    val imageScale = maxOf(
        logoSize / imageWidth,
        logoSize / imageHeight,
    )
    val fittedWidth = imageWidth * imageScale
    val fittedHeight = imageHeight * imageScale

    options.background.toUiColor().setFill()
    createLogoPath(left, top, boxSize, options).fill()

    val context = requireNotNull(UIGraphicsGetCurrentContext()) {
        "Unable to get the QR graphics context"
    }

    CGContextSaveGState(context)
    try {
        createLogoPath(
            left,
            top,
            boxSize,
            options,
            inset = padding,
        ).addClip()

        logo.drawInRect(
            CGRectMake(
                left + padding + (logoSize - fittedWidth) / 2.0,
                top + padding + (logoSize - fittedHeight) / 2.0,
                fittedWidth,
                fittedHeight,
            ),
        )
    } finally {
        CGContextRestoreGState(context)
    }
}

private fun QrColor.toUiColor(): UIColor = UIColor(
    red = red / 255.0,
    green = green / 255.0,
    blue = blue / 255.0,
    alpha = alpha / 255.0,
)
