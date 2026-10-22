package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerPaint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoLayout
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.calculateQrRasterGeometry
import io.github.eugeneponomarev.styledqr.render.qrImageTotalModules
import io.github.eugeneponomarev.styledqr.render.ResolvedQrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import io.github.eugeneponomarev.styledqr.render.calculateRasterModuleLayout
import io.github.eugeneponomarev.styledqr.render.finderPatternTopLefts
import io.github.eugeneponomarev.styledqr.render.isFinderPatternCore
import io.github.eugeneponomarev.styledqr.render.resolvedFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.shouldPreserveAsSquare

/** Renders a [QrCode] to an Android [Bitmap] using the historical integer grid. */
public fun QrCode.toBitmap(
    sizePx: Int,
    style: QrStyle = QrStyle(),
    logo: Bitmap? = null,
): Bitmap {
    val totalModules = size + style.quietZoneModules * 2
    val rasterLayout = calculateRasterModuleLayout(
        sizePx = sizePx,
        totalModules = totalModules,
    )

    return renderBitmap(
        sizePx = sizePx,
        style = style,
        logo = logo,
        moduleSize = rasterLayout.moduleSizePx.toFloat(),
        offset = rasterLayout.offsetPx.toFloat(),
    )
}

/**
 * Renders a QR image with explicit physical pixel size and module geometry.
 *
 * Output parameters are separate from [style]. Invalid output dimensions throw
 * an IllegalArgumentException; safe high-level APIs can expose its error category.
 */
public fun QrCode.toBitmapWithOptions(
    options: QrRasterRenderOptions,
    style: QrStyle = QrStyle(),
    logo: Bitmap? = null,
): Bitmap {
    val geometry = calculateQrRasterGeometry(
        options = options,
        totalModules = qrImageTotalModules(
            matrixSize = size,
            quietZoneModules = style.quietZoneModules,
        ),
    )

    return renderBitmap(
        sizePx = geometry.sizePx,
        style = style,
        logo = logo,
        moduleSize = geometry.moduleSizePx.toFloat(),
        offset = geometry.offsetPx.toFloat(),
    )
}

private fun QrCode.renderBitmap(
    sizePx: Int,
    style: QrStyle,
    logo: Bitmap?,
    moduleSize: Float,
    offset: Float,
): Bitmap {
    val logoOptions = if (logo == null) null else style.logo ?: QrLogoOptions()
    val logoLayout = logoOptions?.let { calculateLogoLayout(it) }
    val logoBackgroundPath = logoLayout?.let { layout ->
        createLogoPath(
            left = offset + (style.quietZoneModules + layout.leftModule) * moduleSize,
            top = offset + (style.quietZoneModules + layout.topModule) * moduleSize,
            boxSize = layout.sizeModules * moduleSize,
            options = requireNotNull(logoOptions),
        )
    }

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(style.background.toArgb())

    val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.foreground.toArgb()
        this.style = Paint.Style.FILL
        style.foregroundGradient?.let { gradient ->
            shader = gradient.toAndroidShader(size, style.quietZoneModules, moduleSize, offset)
        }
    }

    val finderAppearance = style.resolvedFinderPatternAppearance()

    val finderOuterPaint = finderAppearance.outer.paint.toAndroidPaint(modulePaint)
    val finderMiddlePaint = finderAppearance.middle.paint.toAndroidPaint(modulePaint)
    val finderCenterPaint = finderAppearance.center.paint.toAndroidPaint(modulePaint)

    val foregroundSave = canvas.save()
    try {
        logoBackgroundPath?.let { canvas.clipOutPath(it) }
        for (row in 0 until size) {
            for (column in 0 until size) {
                if (!this[row, column] || isFinderPatternCore(row, column)) continue
                val preserveAsSquare = shouldPreserveAsSquare(row, column, style)
                canvas.drawModule(
                    left = offset + (column + style.quietZoneModules) * moduleSize,
                    top = offset + (row + style.quietZoneModules) * moduleSize,
                    moduleSize = moduleSize,
                    scale = if (preserveAsSquare) 1f else style.moduleScale.toFloat(),
                    shape = if (preserveAsSquare) QrModuleShape.Square else style.moduleShape,
                    roundedRadiusFraction = style.roundedModuleRadiusFraction.toFloat(),
                    paint = modulePaint,
                )
            }
        }
        canvas.drawFinderPatterns(
            qrCode = this,
            moduleSize = moduleSize,
            quietZoneModules = style.quietZoneModules,
            offset = offset,
            appearance = finderAppearance,
            roundedRadiusFraction = style.roundedModuleRadiusFraction.toFloat(),
            outerPaint = finderOuterPaint,
            middlePaint = finderMiddlePaint,
            centerPaint = finderCenterPaint,
        )
    } finally {
        canvas.restoreToCount(foregroundSave)
    }

    if (logo != null) {
        canvas.drawLogo(
            moduleSize = moduleSize,
            quietZoneModules = style.quietZoneModules,
            offset = offset,
            layout = requireNotNull(logoLayout),
            options = requireNotNull(logoOptions),
            bitmap = logo,
        )
    }

    return bitmap
}

private fun QrFinderPatternLayerPaint.toAndroidPaint(
    foregroundPaint: Paint,
): Paint = when (this) {
    QrFinderPatternLayerPaint.Foreground -> foregroundPaint
    is QrFinderPatternLayerPaint.Solid -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = this@toAndroidPaint.color.toArgb()
        style = Paint.Style.FILL
    }
}

private fun QrLinearGradient.toAndroidShader(
    qrSizeModules: Int,
    quietZoneModules: Int,
    moduleSizePx: Float,
    offsetPx: Float,
): LinearGradient = LinearGradient(
    offsetPx + (quietZoneModules + start.x * qrSizeModules).toFloat() * moduleSizePx,
    offsetPx + (quietZoneModules + start.y * qrSizeModules).toFloat() * moduleSizePx,
    offsetPx + (quietZoneModules + end.x * qrSizeModules).toFloat() * moduleSizePx,
    offsetPx + (quietZoneModules + end.y * qrSizeModules).toFloat() * moduleSizePx,
    startColor.toArgb(),
    endColor.toArgb(),
    Shader.TileMode.CLAMP,
)

private fun Canvas.drawFinderPatterns(
    qrCode: QrCode,
    moduleSize: Float,
    quietZoneModules: Int,
    offset: Float,
    appearance: ResolvedQrFinderPatternAppearance,
    roundedRadiusFraction: Float,
    outerPaint: Paint,
    middlePaint: Paint,
    centerPaint: Paint,
) {
    qrCode.finderPatternTopLefts().forEach { (row, column) ->
        val left = offset + (column + quietZoneModules) * moduleSize
        val top = offset + (row + quietZoneModules) * moduleSize

        drawFinderLayer(
            left = left,
            top = top,
            size = moduleSize * 7f,
            shape = appearance.outer.shape,
            roundedRadiusFraction = roundedRadiusFraction,
            paint = outerPaint,
        )
        drawFinderLayer(
            left = left + moduleSize,
            top = top + moduleSize,
            size = moduleSize * 5f,
            shape = appearance.middle.shape,
            roundedRadiusFraction = roundedRadiusFraction,
            paint = middlePaint,
        )
        drawFinderLayer(
            left = left + moduleSize * 2f,
            top = top + moduleSize * 2f,
            size = moduleSize * 3f,
            shape = appearance.center.shape,
            roundedRadiusFraction = roundedRadiusFraction,
            paint = centerPaint,
        )
    }
}

private fun Canvas.drawFinderLayer(
    left: Float,
    top: Float,
    size: Float,
    shape: QrModuleShape,
    roundedRadiusFraction: Float,
    paint: Paint,
) = drawModule(
    left = left,
    top = top,
    moduleSize = size,
    scale = 1f,
    shape = shape,
    roundedRadiusFraction = roundedRadiusFraction,
    paint = paint,
)

private fun Canvas.drawModule(
    left: Float,
    top: Float,
    moduleSize: Float,
    scale: Float,
    shape: QrModuleShape,
    roundedRadiusFraction: Float,
    paint: Paint,
) {
    // Square cells must have hard edges; antialiasing can create thin white seams at a noninteger scale.
    paint.isAntiAlias = shape != QrModuleShape.Square
    val inset = moduleSize * (1f - scale) / 2f
    val rect = RectF(
        left + inset,
        top + inset,
        left + moduleSize - inset,
        top + moduleSize - inset,
    )

    when (shape) {
        QrModuleShape.Square -> drawRect(rect, paint)
        QrModuleShape.RoundedSquare -> {
            val radius = rect.width() * roundedRadiusFraction
            drawRoundRect(rect, radius, radius, paint)
        }
        QrModuleShape.Circle -> drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, paint)
        QrModuleShape.Diamond -> {
            val path = Path().apply {
                moveTo(rect.centerX(), rect.top)
                lineTo(rect.right, rect.centerY())
                lineTo(rect.centerX(), rect.bottom)
                lineTo(rect.left, rect.centerY())
                close()
            }
            drawPath(path, paint)
        }
    }
}

private fun createLogoPath(
    left: Float,
    top: Float,
    boxSize: Float,
    options: QrLogoOptions,
    inset: Float = 0f,
): Path {
    val rect = RectF(left + inset, top + inset, left + boxSize - inset, top + boxSize - inset)
    return Path().apply {
        when (options.backgroundShape) {
            QrLogoBackgroundShape.Square -> addRect(rect, Path.Direction.CW)
            QrLogoBackgroundShape.RoundedSquare -> {
                val radius = (boxSize * options.cornerRadiusFraction.toFloat() - inset)
                    .coerceIn(0f, rect.width() / 2f)
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            QrLogoBackgroundShape.Circle -> addOval(rect, Path.Direction.CW)
        }
    }
}

private fun Canvas.drawLogo(
    moduleSize: Float,
    quietZoneModules: Int,
    offset: Float,
    layout: QrLogoLayout,
    options: QrLogoOptions,
    bitmap: Bitmap,
) {
    val boxSize = layout.sizeModules * moduleSize
    val padding = layout.paddingModules * moduleSize
    val left = offset + (quietZoneModules + layout.leftModule) * moduleSize
    val top = offset + (quietZoneModules + layout.topModule) * moduleSize
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = options.background.toArgb()
        style = Paint.Style.FILL
    }
    drawPath(createLogoPath(left, top, boxSize, options), backgroundPaint)

    val logoRect = RectF(
        left + padding,
        top + padding,
        left + boxSize - padding,
        top + boxSize - padding,
    )
    val imageSave = save()
    try {
        clipPath(createLogoPath(left, top, boxSize, options, inset = padding))
        val imageScale = maxOf(
            logoRect.width() / bitmap.width,
            logoRect.height() / bitmap.height,
        )

        val imageWidth = bitmap.width * imageScale
        val imageHeight = bitmap.height * imageScale

        val fittedRect = RectF(
            logoRect.centerX() - imageWidth / 2f,
            logoRect.centerY() - imageHeight / 2f,
            logoRect.centerX() + imageWidth / 2f,
            logoRect.centerY() + imageHeight / 2f,
        )

        drawBitmap(
            bitmap,
            null,
            fittedRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    } finally {
        restoreToCount(imageSave)
    }
}

private fun QrColor.toArgb(): Int =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
