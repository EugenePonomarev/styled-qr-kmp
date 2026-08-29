package io.github.eugeneponomarev.styledqr.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFunctionPatternStyle
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlin.math.min

/**
 * Native Android QR-code view. It can be constructed from Kotlin/Java or inflated from XML.
 *
 * The QR bitmap is cached and is regenerated only after the content, style, logo, or view size
 * changes. [lastRenderError] is set when an invalid payload or an unsafe logo cannot be rendered.
 */
public class StyledQrView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    /** Text or URL encoded into the QR code. An empty value draws only the configured background. */
    public var content: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidateQrCode()
        }

    /** QR Reed–Solomon error-correction level. High correction is the default for logo support. */
    public var errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H
        set(value) {
            if (field == value) return
            field = value
            invalidateQrCode()
        }

    /** Platform-independent appearance shared with the SVG and iOS renderers. */
    public var qrStyle: QrStyle = QrStyle()
        set(value) {
            if (field == value) return
            field = value
            invalidateQrCode()
        }

    /** Optional centre logo. Set [qrStyle.logo] to customise its safe reserved area. */
    public var logo: Bitmap? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateQrCode()
        }

    /** A description of the last encoding or logo-safety error, or `null` after a successful draw. */
    public var lastRenderError: String? = null
        private set

    private var cachedBitmap: Bitmap? = null
    private var cachedSidePx: Int = NO_CACHED_SIDE

    init {
        readAttributes(attrs, defStyleAttr)
    }

    /** Updates all mutable QR inputs together. Useful from a ViewModel binding or Compose adapter. */
    @JvmOverloads
    public fun setQrCode(
        content: String,
        errorCorrection: QrErrorCorrectionLevel,
        style: QrStyle,
        logo: Bitmap? = null,
    ) {
        this.content = content
        this.errorCorrection = errorCorrection
        qrStyle = style
        this.logo = logo
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultWidth = defaultSizePx() + paddingLeft + paddingRight
        val defaultHeight = defaultSizePx() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(defaultWidth, widthMeasureSpec),
            resolveSize(defaultHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(qrStyle.background.toAndroidColor())
        if (content.isBlank()) return

        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom
        val sidePx = min(availableWidth, availableHeight)
        if (sidePx <= 0) return

        val bitmap = bitmapFor(sidePx) ?: return
        val left = paddingLeft + (availableWidth - sidePx) / 2f
        val top = paddingTop + (availableHeight - sidePx) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + sidePx, top + sidePx), null)
    }

    override fun onDetachedFromWindow() {
        recycleCachedBitmap()
        super.onDetachedFromWindow()
    }

    private fun bitmapFor(sidePx: Int): Bitmap? {
        if (cachedSidePx == sidePx) return cachedBitmap

        recycleCachedBitmap()
        cachedSidePx = sidePx
        lastRenderError = null

        return try {
            val code = QrCodeGenerator.encodeText(content, errorCorrection)
            val minimumRenderSize = code.size + qrStyle.quietZoneModules * 2
            code.toBitmap(
                sizePx = maxOf(sidePx, minimumRenderSize),
                style = qrStyle,
                logo = logo,
            ).also { cachedBitmap = it }
        } catch (error: IllegalArgumentException) {
            lastRenderError = error.message
            null
        }
    }

    private fun invalidateQrCode() {
        recycleCachedBitmap()
        lastRenderError = null
        requestLayout()
        invalidate()
    }

    private fun recycleCachedBitmap() {
        cachedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        cachedBitmap = null
        cachedSidePx = NO_CACHED_SIDE
    }

    private fun readAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return

        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.StyledQrView,
            defStyleAttr,
            0,
        )
        try {
            content = attributes.getString(R.styleable.StyledQrView_qrContent).orEmpty()
            errorCorrection = errorCorrectionFrom(
                attributes.getInt(R.styleable.StyledQrView_qrErrorCorrection, ERROR_CORRECTION_H),
            )

            val defaultStyle = QrStyle()
            val foreground = if (attributes.hasValue(R.styleable.StyledQrView_qrForegroundColor)) {
                attributes.getColor(
                    R.styleable.StyledQrView_qrForegroundColor,
                    defaultStyle.foreground.toAndroidColor(),
                ).toQrColor()
            } else {
                defaultStyle.foreground
            }
            val background = if (attributes.hasValue(R.styleable.StyledQrView_qrBackgroundColor)) {
                attributes.getColor(
                    R.styleable.StyledQrView_qrBackgroundColor,
                    defaultStyle.background.toAndroidColor(),
                ).toQrColor()
            } else {
                defaultStyle.background
            }
            val logoResId = attributes.getResourceId(R.styleable.StyledQrView_qrLogo, 0)
            val logoOptions = if (logoResId == 0) {
                null
            } else {
                QrLogoOptions(
                    sizeFraction = attributes.getFloat(
                        R.styleable.StyledQrView_qrLogoSizeFraction,
                        DEFAULT_LOGO_OPTIONS.sizeFraction.toFloat(),
                    ).toDouble(),
                    paddingFraction = attributes.getFloat(
                        R.styleable.StyledQrView_qrLogoPaddingFraction,
                        DEFAULT_LOGO_OPTIONS.paddingFraction.toFloat(),
                    ).toDouble(),
                )
            }

            qrStyle = QrStyle(
                foreground = foreground,
                background = background,
                quietZoneModules = attributes.getInt(
                    R.styleable.StyledQrView_qrQuietZoneModules,
                    defaultStyle.quietZoneModules,
                ),
                moduleShape = moduleShapeFrom(
                    attributes.getInt(R.styleable.StyledQrView_qrModuleShape, MODULE_SHAPE_SQUARE),
                ),
                moduleScale = attributes.getFloat(
                    R.styleable.StyledQrView_qrModuleScale,
                    defaultStyle.moduleScale.toFloat(),
                ).toDouble(),
                roundedModuleRadiusFraction = attributes.getFloat(
                    R.styleable.StyledQrView_qrRoundedModuleRadiusFraction,
                    defaultStyle.roundedModuleRadiusFraction.toFloat(),
                ).toDouble(),
                functionPatternStyle = functionPatternStyleFrom(
                    attributes.getInt(
                        R.styleable.StyledQrView_qrFunctionPatternStyle,
                        FUNCTION_PATTERN_PRESERVE_ALL,
                    ),
                ),
                logo = logoOptions,
            )
            logo = logoResId.takeIf { it != 0 }
                ?.let(context::getDrawable)
                ?.toBitmap()
        } finally {
            attributes.recycle()
        }
    }

    private fun defaultSizePx(): Int = (DEFAULT_SIZE_DP * resources.displayMetrics.density).toInt()

    private companion object {
        const val DEFAULT_SIZE_DP: Int = 240
        const val NO_CACHED_SIDE: Int = -1

        const val ERROR_CORRECTION_H: Int = 3
        const val MODULE_SHAPE_SQUARE: Int = 0
        const val FUNCTION_PATTERN_PRESERVE_ALL: Int = 0

        val DEFAULT_LOGO_OPTIONS: QrLogoOptions = QrLogoOptions()
    }
}

private fun errorCorrectionFrom(value: Int): QrErrorCorrectionLevel = when (value) {
    0 -> QrErrorCorrectionLevel.L
    1 -> QrErrorCorrectionLevel.M
    2 -> QrErrorCorrectionLevel.Q
    else -> QrErrorCorrectionLevel.H
}

private fun moduleShapeFrom(value: Int): QrModuleShape = when (value) {
    1 -> QrModuleShape.RoundedSquare
    2 -> QrModuleShape.Circle
    3 -> QrModuleShape.Diamond
    else -> QrModuleShape.Square
}

private fun functionPatternStyleFrom(value: Int): QrFunctionPatternStyle = when (value) {
    1 -> QrFunctionPatternStyle.PreserveFindersAndAlignment
    2 -> QrFunctionPatternStyle.MatchDataModules
    else -> QrFunctionPatternStyle.PreserveAll
}

private fun QrColor.toAndroidColor(): Int = Color.argb(alpha, red, green, blue)

private fun Int.toQrColor(): QrColor = QrColor(
    red = Color.red(this),
    green = Color.green(this),
    blue = Color.blue(this),
    alpha = Color.alpha(this),
)

private fun Drawable.toBitmap(): Bitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_LOGO_SIZE_PX
    val height = intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_LOGO_SIZE_PX
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        setBounds(0, 0, width, height)
        draw(Canvas(bitmap))
    }
}

private const val DEFAULT_LOGO_SIZE_PX: Int = 160
