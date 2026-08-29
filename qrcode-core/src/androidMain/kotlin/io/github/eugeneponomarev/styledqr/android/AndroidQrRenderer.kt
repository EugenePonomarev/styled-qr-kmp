package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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

/** Renders a [QrCode] to an Android [Bitmap] using only the Android graphics API. */
public fun QrCode.toBitmap(
    sizePx: Int,
    style: QrStyle = QrStyle(),
    logo: Bitmap? = null,
): Bitmap {
    val totalModules = size + style.quietZoneModules * 2
    require(sizePx >= totalModules) {
        "sizePx must be at least $totalModules to represent every QR module"
    }

    val moduleSize = sizePx.toFloat() / totalModules
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(style.background.toArgb())

    val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.foreground.toArgb()
        this.style = Paint.Style.FILL
    }
    val finderBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.background.toArgb()
        this.style = Paint.Style.FILL
    }
    val logoOptions = if (logo == null) null else style.logo ?: QrLogoOptions()
    val logoLayout = logoOptions?.let { calculateLogoLayout(it) }

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
            val scale = if (preserveAsSquare) 1.0 else style.moduleScale
            canvas.drawModule(
                left = (column + style.quietZoneModules) * moduleSize,
                top = (row + style.quietZoneModules) * moduleSize,
                moduleSize = moduleSize,
                scale = scale.toFloat(),
                shape = shape,
                roundedRadiusFraction = style.roundedModuleRadiusFraction.toFloat(),
                paint = modulePaint,
            )
        }
    }

    canvas.drawFinderPatterns(
        qrCode = this,
        moduleSize = moduleSize,
        quietZoneModules = style.quietZoneModules,
        shape = style.resolvedFinderPatternShape(),
        roundedRadiusFraction = style.roundedModuleRadiusFraction.toFloat(),
        foregroundPaint = modulePaint,
        backgroundPaint = finderBackgroundPaint,
    )

    if (logo != null) {
        canvas.drawLogo(
            moduleSize = moduleSize,
            quietZoneModules = style.quietZoneModules,
            layout = requireNotNull(logoLayout),
            options = requireNotNull(logoOptions),
            bitmap = logo,
        )
    }

    return bitmap
}

private fun Canvas.drawFinderPatterns(
    qrCode: QrCode,
    moduleSize: Float,
    quietZoneModules: Int,
    shape: QrModuleShape,
    roundedRadiusFraction: Float,
    foregroundPaint: Paint,
    backgroundPaint: Paint,
) {
    qrCode.finderPatternTopLefts().forEach { (row, column) ->
        val left = (column + quietZoneModules) * moduleSize
        val top = (row + quietZoneModules) * moduleSize
        drawFinderLayer(left, top, moduleSize * 7f, shape, roundedRadiusFraction, foregroundPaint)
        drawFinderLayer(
            left + moduleSize,
            top + moduleSize,
            moduleSize * 5f,
            shape,
            roundedRadiusFraction,
            backgroundPaint,
        )
        drawFinderLayer(
            left + moduleSize * 2f,
            top + moduleSize * 2f,
            moduleSize * 3f,
            shape,
            roundedRadiusFraction,
            foregroundPaint,
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
    // Square cells must have hard edges; anti-aliasing can create thin white seams at a non-integer scale.
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

private fun Canvas.drawLogo(
    moduleSize: Float,
    quietZoneModules: Int,
    layout: QrLogoLayout,
    options: QrLogoOptions,
    bitmap: Bitmap,
) {
    val boxSize = layout.sizeModules * moduleSize
    val padding = layout.paddingModules * moduleSize
    val logoSize = boxSize - padding * 2f
    val left = (quietZoneModules + layout.leftModule) * moduleSize
    val top = (quietZoneModules + layout.topModule) * moduleSize
    val backgroundRect = RectF(left, top, left + boxSize, top + boxSize)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = options.background.toArgb()
        style = Paint.Style.FILL
    }

    when (options.backgroundShape) {
        QrLogoBackgroundShape.Square -> drawRect(backgroundRect, backgroundPaint)
        QrLogoBackgroundShape.RoundedSquare -> {
            val radius = boxSize * options.cornerRadiusFraction.toFloat()
            drawRoundRect(backgroundRect, radius, radius, backgroundPaint)
        }
        QrLogoBackgroundShape.Circle -> drawCircle(
            backgroundRect.centerX(),
            backgroundRect.centerY(),
            boxSize / 2f,
            backgroundPaint,
        )
    }

    val logoRect = RectF(
        left + padding,
        top + padding,
        left + padding + logoSize,
        top + padding + logoSize,
    )
    drawBitmap(bitmap, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun QrColor.toArgb(): Int =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
