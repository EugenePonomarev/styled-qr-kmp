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
    ): String {
        val quietZone = style.quietZoneModules
        val canvasSize = qrCode.size + quietZone * 2

        val gradient = style.foregroundGradient
        val gradientId = gradient?.svgGradientId(
            qrSize = qrCode.size,
            quietZoneModules = quietZone,
        )

        val foreground = gradientId?.let { id ->
            "url(#$id)"
        } ?: style.foreground.toCss()

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
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            append(canvasSize)
            append(' ')
            append(canvasSize)
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
                append(" x1=\"$startX\" y1=\"$startY\" x2=\"$endX\" y2=\"$endY\">")
                append("<stop offset=\"0%\" stop-color=\"${value.startColor.toCss()}\"/>")
                append("<stop offset=\"100%\" stop-color=\"${value.endColor.toCss()}\"/>")
                append("</linearGradient></defs>")
            }
            append("<rect width=\"")
            append(canvasSize)
            append("\" height=\"")
            append(canvasSize)
            append("\" fill=\"")
            append(style.background.toCss())
            append("\"/>")

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
                shape = style.resolvedFinderPatternShape(),
                roundedRadiusFraction = style.roundedModuleRadiusFraction,
                foreground = foreground,
                background = style.background.toCss(),
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
            QrModuleShape.Square -> append("<rect x=\"$left\" y=\"$top\" width=\"$width\" height=\"$width\" fill=\"$fill\" shape-rendering=\"crispEdges\"/>")
            QrModuleShape.RoundedSquare -> {
                val radius = width * roundedRadiusFraction
                append("<rect x=\"$left\" y=\"$top\" width=\"$width\" height=\"$width\" rx=\"$radius\" fill=\"$fill\"/>")
            }

            QrModuleShape.Circle -> append("<circle cx=\"$centreX\" cy=\"$centreY\" r=\"${width / 2.0}\" fill=\"$fill\"/>")
            QrModuleShape.Diamond -> {
                val half = width / 2.0
                append("<path d=\"M $centreX ${centreY - half} L ${centreX + half} $centreY L $centreX ${centreY + half} L ${centreX - half} $centreY Z\" fill=\"$fill\"/>")
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
        append(left)
        append(' ')
        append(top)
        append(" h ")
        append(scale)
        append(" v ")
        append(scale)
        append(" h -")
        append(scale)
        append(" Z ")
    }

    private fun StringBuilder.appendFinderPatterns(
        qrCode: QrCode,
        quietZone: Double,
        shape: QrModuleShape,
        roundedRadiusFraction: Double,
        foreground: String,
        background: String,
    ) {
        qrCode.finderPatternTopLefts().forEach { (row, column) ->
            val left = quietZone + column
            val top = quietZone + row
            appendFinderLayer(left, top, 7.0, shape, roundedRadiusFraction, foreground)
            appendFinderLayer(left + 1.0, top + 1.0, 5.0, shape, roundedRadiusFraction, background)
            appendFinderLayer(left + 2.0, top + 2.0, 3.0, shape, roundedRadiusFraction, foreground)
        }
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
            QrModuleShape.Square -> append("<rect x=\"$left\" y=\"$top\" width=\"$size\" height=\"$size\" fill=\"$fill\" shape-rendering=\"crispEdges\"/>")
            QrModuleShape.RoundedSquare -> {
                val radius = size * roundedRadiusFraction
                append("<rect x=\"$left\" y=\"$top\" width=\"$size\" height=\"$size\" rx=\"$radius\" fill=\"$fill\"/>")
            }

            QrModuleShape.Circle -> append("<circle cx=\"$centreX\" cy=\"$centreY\" r=\"${size / 2.0}\" fill=\"$fill\"/>")
            QrModuleShape.Diamond -> {
                val half = size / 2.0
                append("<path d=\"M $centreX ${centreY - half} L ${centreX + half} $centreY L $centreX ${centreY + half} L ${centreX - half} $centreY Z\" fill=\"$fill\"/>")
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
            return "M ${x + radius} $y " +
                "a $radius $radius 0 1 1 0 $side " +
                "a $radius $radius 0 1 1 0 -$side Z"
        }
        val radius = if (options.backgroundShape == QrLogoBackgroundShape.RoundedSquare) {
            (boxSize * options.cornerRadiusFraction - inset).coerceIn(0.0, side / 2.0)
        } else {
            0.0
        }
        if (radius == 0.0) return "M $x $y H $right V $bottom H $x Z"
        return "M ${x + radius} $y H ${right - radius} " +
            "A $radius $radius 0 0 1 $right ${y + radius} V ${bottom - radius} " +
            "A $radius $radius 0 0 1 ${right - radius} $bottom H ${x + radius} " +
            "A $radius $radius 0 0 1 $x ${bottom - radius} V ${y + radius} " +
            "A $radius $radius 0 0 1 ${x + radius} $y Z"
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
        append(left + padding)
        append("\" y=\"")
        append(top + padding)
        append("\" width=\"")
        append(logoSize)
        append("\" height=\"")
        append(logoSize)
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
