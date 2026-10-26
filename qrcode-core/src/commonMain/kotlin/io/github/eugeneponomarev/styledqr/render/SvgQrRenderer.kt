package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode

/** Renders a QR matrix as self-contained SVG text on every KMP target. */
public object SvgQrRenderer {
    /**
     * @param logoDataUri An image data URI, for example `data:image/png;base64,...`. It is placed
     * over a safe background only when [QrStyle.logo] is configured.
     */
    public fun render(
        qrCode: QrCode,
        style: QrStyle = QrStyle(),
        logoDataUri: String? = null,
    ): String = renderSvg(
        qrCode = qrCode,
        style = style,
        logoDataUri = logoDataUri,
        geometry = null,
    )

    /**
     * Renders an SVG with an explicit output side measured in physical pixels.
     *
     * The requested geometry applies when rasterizing at that size.
     * Subsequent CSS scaling can change pixel alignment.
     */
    public fun renderWithRasterOptions(
        qrCode: QrCode,
        options: QrRasterRenderOptions,
        style: QrStyle = QrStyle(),
        logoDataUri: String? = null,
    ): String {
        val totalModules = qrImageTotalModules(
            matrixSize = qrCode.size,
            quietZoneModules = style.quietZoneModules,
        )
        val geometry = calculateQrRasterGeometry(options, totalModules)

        return renderSvg(
            qrCode = qrCode,
            style = style,
            logoDataUri = logoDataUri,
            geometry = geometry,
        )
    }

    private fun renderSvg(
        qrCode: QrCode,
        style: QrStyle,
        logoDataUri: String?,
        geometry: QrRasterGeometry?,
    ): String {
        val quietZone = style.quietZoneModules
        val canvasSize = geometry?.totalModules
            ?: (qrCode.size + quietZone * 2)

        val gradient = style.foregroundGradient
        val gradientId = gradient?.svgGradientId(
            qrSize = qrCode.size,
            quietZoneModules = quietZone,
        )

        val foreground = gradientId?.let { id ->
            "url(#$id)"
        } ?: style.foreground.toCss()

        val finderAppearance = style.resolvedFinderPatternAppearance()

        val logoOptions = style.logo?.takeIf { !logoDataUri.isNullOrBlank() }
        val logoLayout = logoOptions?.let(qrCode::calculateLogoLayout)
        val logoClipId = if (logoLayout != null) {
            "qr-logo-${qrCode.size}-$quietZone-${logoLayout.leftModule}-" +
                "${logoLayout.topModule}-${logoLayout.sizeModules}-${logoLayout.paddingModules}-" +
                "${logoOptions.backgroundShape.name}-${logoOptions.cornerRadiusFraction.toBits()}"
        } else {
            null
        }

        return buildString {
            val squareModulesPath = StringBuilder()
            val outputSize = geometry?.sizePx ?: canvasSize

            append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")

            if (geometry != null) {
                append("width=\"")
                append(outputSize)
                append("\" height=\"")
                append(outputSize)
                append("\" ")
            }

            append("viewBox=\"0 0 ")
            append(outputSize)
            append(' ')
            append(outputSize)
            append("\" role=\"img\" aria-label=\"QR code\">")
            gradient?.let { value ->
                val id = requireNotNull(gradientId)

                val startX = quietZone + value.start.x * qrCode.size
                val startY = quietZone + value.start.y * qrCode.size
                val endX = quietZone + value.end.x * qrCode.size
                val endY = quietZone + value.end.y * qrCode.size

                append(
                    "<defs><linearGradient " +
                            "id=\"$id\" " +
                            "gradientUnits=\"userSpaceOnUse\" " +
                            "color-interpolation=\"sRGB\"",
                )
                append(" x1=\"")
                appendSvgNumber(startX)
                append("\" y1=\"")
                appendSvgNumber(startY)
                append("\" x2=\"")
                appendSvgNumber(endX)
                append("\" y2=\"")
                appendSvgNumber(endY)
                append("\">")
                append("<stop offset=\"0%\" stop-color=\"${value.startColor.toCss()}\"/>")
                append("<stop offset=\"100%\" stop-color=\"${value.endColor.toCss()}\"/>")
                append("</linearGradient></defs>")
            }
            append("<rect width=\"")
            append(outputSize)
            append("\" height=\"")
            append(outputSize)
            append("\" fill=\"")
            append(style.background.toCss())
            append("\"/>")

            if (geometry != null) {
                append("<g transform=\"translate(")
                appendSvgNumber(geometry.offsetPx)
                append(' ')
                appendSvgNumber(geometry.offsetPx)
                append(") scale(")
                appendSvgNumber(geometry.moduleSizePx)
                append(")\">")
            }

            if (logoLayout != null && logoClipId != null) {
                val logoPath = logoShapePath(
                    left = quietZone + logoLayout.leftModule.toDouble(),
                    top = quietZone + logoLayout.topModule.toDouble(),
                    boxSize = logoLayout.sizeModules.toDouble(),
                    options = logoOptions,
                )
                append("<defs><clipPath id=\"$logoClipId\" clipPathUnits=\"userSpaceOnUse\">")
                append("<path clip-rule=\"evenodd\" d=\"M 0 0 H $canvasSize V $canvasSize H 0 Z ")
                append(logoPath)
                append("\"/></clipPath></defs>")
                append("<g clip-path=\"url(#$logoClipId)\">")
            }

            for (row in 0 until qrCode.size) {
                for (column in 0 until qrCode.size) {
                    if (
                        !qrCode[row, column] ||
                        qrCode.isFinderPatternCore(row, column)
                    ) continue

                    val preserveAsSquare = qrCode.shouldPreserveAsSquare(row, column, style)
                    val shape = if (preserveAsSquare) {
                        QrModuleShape.Square
                    } else {
                        style.moduleShape
                    }
                    val scale = if (preserveAsSquare) 1.0 else style.moduleScale
                    if (shape == QrModuleShape.Square) {
                        squareModulesPath.appendSquareModule(
                            x = column + quietZone.toDouble(),
                            y = row + quietZone.toDouble(),
                            scale = scale,
                        )
                    } else {
                        appendModule(
                            x = column + quietZone.toDouble(),
                            y = row + quietZone.toDouble(),
                            scale = scale,
                            shape = shape,
                            roundedRadiusFraction = style.roundedModuleRadiusFraction,
                            fill = foreground,
                        )
                    }
                }
            }

            if (squareModulesPath.isNotEmpty()) {
                append("<path d=\"")
                append(squareModulesPath)
                append("\" fill=\"")
                append(foreground)
                append("\" shape-rendering=\"crispEdges\"/>")
            }

            appendFinderPatterns(
                qrCode = qrCode,
                quietZone = quietZone.toDouble(),
                appearance = finderAppearance,
                roundedRadiusFraction = style.roundedModuleRadiusFraction,
                foreground = foreground,
            )

            if (logoClipId != null) append("</g>")

            if (logoOptions != null && logoLayout != null && !logoDataUri.isNullOrBlank()) {
                appendLogo(
                    quietZone = quietZone.toDouble(),
                    layout = logoLayout,
                    options = logoOptions,
                    dataUri = logoDataUri,
                    clipId = requireNotNull(logoClipId),
                )
            }

            if (geometry != null) {
                append("</g>")
            }

            append("</svg>")
        }
    }

    private fun StringBuilder.appendModule(
        x: Double,
        y: Double,
        scale: Double,
        shape: QrModuleShape,
        roundedRadiusFraction: Double,
        fill: String,
    ) {
        val inset = (1.0 - scale) / 2.0
        val left = x + inset
        val top = y + inset
        val width = scale
        val centreX = x + 0.5
        val centreY = y + 0.5

        when (shape) {
            QrModuleShape.Square -> append("<rect x=\"")
                .appendSvgNumber(left)
                .append("\" y=\"")
                .appendSvgNumber(top)
                .append("\" width=\"")
                .appendSvgNumber(width)
                .append("\" height=\"")
                .appendSvgNumber(width)
                .append("\" fill=\"")
                .append(fill)
                .append("\" shape-rendering=\"crispEdges\"/>")

            QrModuleShape.RoundedSquare -> {
                val radius = width * roundedRadiusFraction
                append("<rect x=\"")
                    .appendSvgNumber(left)
                    .append("\" y=\"")
                    .appendSvgNumber(top)
                    .append("\" width=\"")
                    .appendSvgNumber(width)
                    .append("\" height=\"")
                    .appendSvgNumber(width)
                    .append("\" rx=\"")
                    .appendSvgNumber(radius)
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"/>")
            }

            QrModuleShape.Circle -> append("<circle cx=\"")
                .appendSvgNumber(centreX)
                .append("\" cy=\"")
                .appendSvgNumber(centreY)
                .append("\" r=\"")
                .appendSvgNumber(width / 2.0)
                .append("\" fill=\"")
                .append(fill)
                .append("\"/>")

            QrModuleShape.Diamond -> {
                val half = width / 2.0
                append("<path d=\"M ")
                    .appendSvgNumber(centreX)
                    .append(' ')
                    .appendSvgNumber(centreY - half)
                    .append(" L ")
                    .appendSvgNumber(centreX + half)
                    .append(' ')
                    .appendSvgNumber(centreY)
                    .append(" L ")
                    .appendSvgNumber(centreX)
                    .append(' ')
                    .appendSvgNumber(centreY + half)
                    .append(" L ")
                    .appendSvgNumber(centreX - half)
                    .append(' ')
                    .appendSvgNumber(centreY)
                    .append(" Z\" fill=\"")
                    .append(fill)
                    .append("\"/>")
            }
        }
    }

    /** One SVG path prevents anti-aliased seams between adjacent square modules. */
    private fun StringBuilder.appendSquareModule(
        x: Double,
        y: Double,
        scale: Double,
    ) {
        val inset = (1.0 - scale) / 2.0
        val left = x + inset
        val top = y + inset
        append("M ")
        appendSvgNumber(left)
        append(' ')
        appendSvgNumber(top)
        append(" h ")
        appendSvgNumber(scale)
        append(" v ")
        appendSvgNumber(scale)
        append(" h -")
        appendSvgNumber(scale)
        append(" Z ")
    }

    private fun StringBuilder.appendFinderPatterns(
        qrCode: QrCode,
        quietZone: Double,
        appearance: ResolvedQrFinderPatternAppearance,
        roundedRadiusFraction: Double,
        foreground: String,
    ) {
        val outerFill = appearance.outer.paint.toSvgFill(foreground)
        val middleFill = appearance.middle.paint.toSvgFill(foreground)
        val centerFill = appearance.center.paint.toSvgFill(foreground)

        qrCode.finderPatternTopLefts().forEach { (row, column) ->
            val left = quietZone + column
            val top = quietZone + row

            appendFinderLayer(
                left = left,
                top = top,
                size = 7.0,
                shape = appearance.outer.shape,
                roundedRadiusFraction = roundedRadiusFraction,
                fill = outerFill,
            )
            appendFinderLayer(
                left = left + 1.0,
                top = top + 1.0,
                size = 5.0,
                shape = appearance.middle.shape,
                roundedRadiusFraction = roundedRadiusFraction,
                fill = middleFill,
            )
            appendFinderLayer(
                left = left + 2.0,
                top = top + 2.0,
                size = 3.0,
                shape = appearance.center.shape,
                roundedRadiusFraction = roundedRadiusFraction,
                fill = centerFill,
            )
        }
    }

    private fun QrFinderPatternLayerPaint.toSvgFill(
        foreground: String,
    ): String = when (this) {
        QrFinderPatternLayerPaint.Foreground -> foreground
        is QrFinderPatternLayerPaint.Solid -> color.toCss()
    }

    private fun StringBuilder.appendFinderLayer(
        left: Double,
        top: Double,
        size: Double,
        shape: QrModuleShape,
        roundedRadiusFraction: Double,
        fill: String,
    ) {
        val centreX = left + size / 2.0
        val centreY = top + size / 2.0
        when (shape) {
            QrModuleShape.Square -> append("<rect x=\"")
                .appendSvgNumber(left)
                .append("\" y=\"")
                .appendSvgNumber(top)
                .append("\" width=\"")
                .appendSvgNumber(size)
                .append("\" height=\"")
                .appendSvgNumber(size)
                .append("\" fill=\"")
                .append(fill)
                .append("\" shape-rendering=\"crispEdges\"/>")

            QrModuleShape.RoundedSquare -> {
                val radius = size * roundedRadiusFraction
                append("<rect x=\"")
                    .appendSvgNumber(left)
                    .append("\" y=\"")
                    .appendSvgNumber(top)
                    .append("\" width=\"")
                    .appendSvgNumber(size)
                    .append("\" height=\"")
                    .appendSvgNumber(size)
                    .append("\" rx=\"")
                    .appendSvgNumber(radius)
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"/>")
            }

            QrModuleShape.Circle -> append("<circle cx=\"")
                .appendSvgNumber(centreX)
                .append("\" cy=\"")
                .appendSvgNumber(centreY)
                .append("\" r=\"")
                .appendSvgNumber(size / 2.0)
                .append("\" fill=\"")
                .append(fill)
                .append("\"/>")

            QrModuleShape.Diamond -> {
                val half = size / 2.0
                append("<path d=\"M ")
                    .appendSvgNumber(centreX)
                    .append(' ')
                    .appendSvgNumber(centreY - half)
                    .append(" L ")
                    .appendSvgNumber(centreX + half)
                    .append(' ')
                    .appendSvgNumber(centreY)
                    .append(" L ")
                    .appendSvgNumber(centreX)
                    .append(' ')
                    .appendSvgNumber(centreY + half)
                    .append(" L ")
                    .appendSvgNumber(centreX - half)
                    .append(' ')
                    .appendSvgNumber(centreY)
                    .append(" Z\" fill=\"")
                    .append(fill)
                    .append("\"/>")
            }
        }
    }

    private fun logoShapePath(
        left: Double,
        top: Double,
        boxSize: Double,
        options: QrLogoOptions,
        inset: Double = 0.0,
    ): String {
        val x = left + inset
        val y = top + inset
        val side = boxSize - inset * 2.0
        val right = x + side
        val bottom = y + side
        if (options.backgroundShape == QrLogoBackgroundShape.Circle) {
            val radius = side / 2.0
            return buildString {
                append("M ")
                appendSvgNumber(x + radius)
                append(' ')
                appendSvgNumber(y)
                append(" a ")
                appendSvgNumber(radius)
                append(' ')
                appendSvgNumber(radius)
                append(" 0 1 1 0 ")
                appendSvgNumber(side)
                append(" a ")
                appendSvgNumber(radius)
                append(' ')
                appendSvgNumber(radius)
                append(" 0 1 1 0 -")
                appendSvgNumber(side)
                append(" Z")
            }
        }
        val radius = if (options.backgroundShape == QrLogoBackgroundShape.RoundedSquare) {
            (boxSize * options.cornerRadiusFraction - inset).coerceIn(0.0, side / 2.0)
        } else {
            0.0
        }
        if (radius == 0.0) {
            return buildString {
                append("M ")
                appendSvgNumber(x)
                append(' ')
                appendSvgNumber(y)
                append(" H ")
                appendSvgNumber(right)
                append(" V ")
                appendSvgNumber(bottom)
                append(" H ")
                appendSvgNumber(x)
                append(" Z")
            }
        }
        return buildString {
            append("M ")
            appendSvgNumber(x + radius)
            append(' ')
            appendSvgNumber(y)
            append(" H ")
            appendSvgNumber(right - radius)
            append(" A ")
            appendSvgNumber(radius)
            append(' ')
            appendSvgNumber(radius)
            append(" 0 0 1 ")
            appendSvgNumber(right)
            append(' ')
            appendSvgNumber(y + radius)
            append(" V ")
            appendSvgNumber(bottom - radius)
            append(" A ")
            appendSvgNumber(radius)
            append(' ')
            appendSvgNumber(radius)
            append(" 0 0 1 ")
            appendSvgNumber(right - radius)
            append(' ')
            appendSvgNumber(bottom)
            append(" H ")
            appendSvgNumber(x + radius)
            append(" A ")
            appendSvgNumber(radius)
            append(' ')
            appendSvgNumber(radius)
            append(" 0 0 1 ")
            appendSvgNumber(x)
            append(' ')
            appendSvgNumber(bottom - radius)
            append(" V ")
            appendSvgNumber(y + radius)
            append(" A ")
            appendSvgNumber(radius)
            append(' ')
            appendSvgNumber(radius)
            append(" 0 0 1 ")
            appendSvgNumber(x + radius)
            append(' ')
            appendSvgNumber(y)
            append(" Z")
        }
    }

    private fun StringBuilder.appendLogo(
        quietZone: Double,
        layout: QrLogoLayout,
        options: QrLogoOptions,
        dataUri: String,
        clipId: String,
    ) {
        val boxSize = layout.sizeModules.toDouble()
        val padding = layout.paddingModules.toDouble()
        val logoSize = boxSize - padding * 2.0
        val left = quietZone + layout.leftModule
        val top = quietZone + layout.topModule
        val backgroundPath = logoShapePath(left, top, boxSize, options)
        val imagePath = logoShapePath(left, top, boxSize, options, inset = padding)
        val imageClipId = "$clipId-image"

        append("<path d=\"$backgroundPath\" fill=\"${options.background.toCss()}\"/>")
        append("<defs><clipPath id=\"$imageClipId\" clipPathUnits=\"userSpaceOnUse\">")
        append("<path d=\"$imagePath\"/></clipPath></defs>")
        append("<image href=\"")
        append(dataUri.escapeXmlAttribute())
        append("\" x=\"")
        appendSvgNumber(left + padding)
        append("\" y=\"")
        appendSvgNumber(top + padding)
        append("\" width=\"")
        appendSvgNumber(logoSize)
        append("\" height=\"")
        appendSvgNumber(logoSize)
        append("\" preserveAspectRatio=\"xMidYMid slice\" clip-path=\"url(#$imageClipId)\"/>")
    }

    private fun String.escapeXmlAttribute(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun QrLinearGradient.svgGradientId(
        qrSize: Int,
        quietZoneModules: Int,
    ): String = buildString {
        append("qr-foreground")
        append('-')
        append(qrSize)
        append('-')
        append(quietZoneModules)

        append('-')
        append(startColor.red)
        append('-')
        append(startColor.green)
        append('-')
        append(startColor.blue)

        append('-')
        append(endColor.red)
        append('-')
        append(endColor.green)
        append('-')
        append(endColor.blue)

        append('-')
        append(start.x.toBits())
        append('-')
        append(start.y.toBits())
        append('-')
        append(end.x.toBits())
        append('-')
        append(end.y.toBits())
    }
}

/** Convenience extension for common Kotlin, Android, and iOS code. */
public fun QrCode.toSvg(
    style: QrStyle = QrStyle(),
    logoDataUri: String? = null,
): String = SvgQrRenderer.render(this, style, logoDataUri)
