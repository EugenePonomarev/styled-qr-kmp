package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoLayout
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.finderPatternTopLefts
import io.github.eugeneponomarev.styledqr.render.isFinderPatternCore
import io.github.eugeneponomarev.styledqr.render.resolvedFinderPatternShape
import io.github.eugeneponomarev.styledqr.render.shouldPreserveAsSquare
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext

/** Renders a [QrCode] to a native iOS [UIImage] using UIKit. */
public fun QrCode.toUIImage(
    sizePoints: Double,
    style: QrStyle = QrStyle(),
    scale: Double = 0.0,
    logo: UIImage? = null,
): UIImage {
    val totalModules = size + style.quietZoneModules * 2
    require(sizePoints >= totalModules) {
        "sizePoints must be at least $totalModules to represent every QR module"
    }

    val moduleSize = sizePoints / totalModules
    val logoOptions = if (logo == null) null else style.logo ?: QrLogoOptions()
    val logoLayout = logoOptions?.let { calculateLogoLayout(it) }
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(sizePoints, sizePoints), false, scale)
    try {
        style.background.toUiColor().setFill()
        UIBezierPath.bezierPathWithRect(CGRectMake(0.0, 0.0, sizePoints, sizePoints)).fill()

        style.foreground.toUiColor().setFill()
        for (row in 0 until size) {
            for (column in 0 until size) {
                if (
                    !this[row, column] ||
                        isFinderPatternCore(row, column) ||
                        logoLayout?.contains(row, column) == true
                ) continue

                val preserveAsSquare = shouldPreserveAsSquare(row, column, style)
                val shape = if (preserveAsSquare) {
                    QrModuleShape.Square
                } else {
                    style.moduleShape
                }
                val moduleScale = if (preserveAsSquare) 1.0 else style.moduleScale
                drawModulePath(
                    left = (column + style.quietZoneModules) * moduleSize,
                    top = (row + style.quietZoneModules) * moduleSize,
                    moduleSize = moduleSize,
                    scale = moduleScale,
                    shape = shape,
                    roundedRadiusFraction = style.roundedModuleRadiusFraction,
                ).fill()
            }
        }

        drawFinderPatterns(
            qrCode = this,
            moduleSize = moduleSize,
            quietZoneModules = style.quietZoneModules,
            style = style,
        )

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

private fun drawFinderPatterns(
    qrCode: QrCode,
    moduleSize: Double,
    quietZoneModules: Int,
    style: QrStyle,
) {
    val shape = style.resolvedFinderPatternShape()
    qrCode.finderPatternTopLefts().forEach { (row, column) ->
        val left = (column + quietZoneModules) * moduleSize
        val top = (row + quietZoneModules) * moduleSize

        style.foreground.toUiColor().setFill()
        drawFinderLayer(left, top, moduleSize * 7.0, shape, style.roundedModuleRadiusFraction).fill()
        style.background.toUiColor().setFill()
        drawFinderLayer(
            left + moduleSize,
            top + moduleSize,
            moduleSize * 5.0,
            shape,
            style.roundedModuleRadiusFraction,
        ).fill()
        style.foreground.toUiColor().setFill()
        drawFinderLayer(
            left + moduleSize * 2.0,
            top + moduleSize * 2.0,
            moduleSize * 3.0,
            shape,
            style.roundedModuleRadiusFraction,
        ).fill()
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
    val backgroundRect = CGRectMake(left, top, boxSize, boxSize)

    options.background.toUiColor().setFill()
    val backgroundPath = when (options.backgroundShape) {
        QrLogoBackgroundShape.Square -> UIBezierPath.bezierPathWithRect(backgroundRect)
        QrLogoBackgroundShape.RoundedSquare -> UIBezierPath.bezierPathWithRoundedRect(
            backgroundRect,
            boxSize * options.cornerRadiusFraction,
        )
        QrLogoBackgroundShape.Circle -> UIBezierPath.bezierPathWithOvalInRect(backgroundRect)
    }
    backgroundPath.fill()

    logo.drawInRect(CGRectMake(left + padding, top + padding, logoSize, logoSize))
}

private fun QrColor.toUiColor(): UIColor = UIColor(
    red = red / 255.0,
    green = green / 255.0,
    blue = blue / 255.0,
    alpha = alpha / 255.0,
)
