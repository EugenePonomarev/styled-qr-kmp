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
        val foreground = style.foreground.toCss()
        val logoOptions = style.logo?.takeIf { !logoDataUri.isNullOrBlank() }
        val logoLayout = logoOptions?.let(qrCode::calculateLogoLayout)

        return buildString {
            val squareModulesPath = StringBuilder()
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            append(canvasSize)
            append(' ')
            append(canvasSize)
            append("\" role=\"img\" aria-label=\"QR code\">")
            append("<rect width=\"")
            append(canvasSize)
            append("\" height=\"")
            append(canvasSize)
            append("\" fill=\"")
            append(style.background.toCss())
            append("\"/>")

            for (row in 0 until qrCode.size) {
                for (column in 0 until qrCode.size) {
                    if (
                        !qrCode[row, column] ||
                            qrCode.isFinderPatternCore(row, column) ||
                            logoLayout?.contains(row, column) == true
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

            if (logoOptions != null && logoLayout != null && !logoDataUri.isNullOrBlank()) {
                appendLogo(
                    quietZone = quietZone.toDouble(),
                    layout = logoLayout,
                    options = logoOptions,
                    dataUri = logoDataUri,
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

    private fun StringBuilder.appendLogo(
        quietZone: Double,
        layout: QrLogoLayout,
        options: QrLogoOptions,
        dataUri: String,
    ) {
        val boxSize = layout.sizeModules.toDouble()
        val padding = layout.paddingModules.toDouble()
        val logoSize = boxSize - padding * 2.0
        val left = quietZone + layout.leftModule
        val top = quietZone + layout.topModule

        when (options.backgroundShape) {
            QrLogoBackgroundShape.Square -> append("<rect x=\"$left\" y=\"$top\" width=\"$boxSize\" height=\"$boxSize\" fill=\"${options.background.toCss()}\"/>")
            QrLogoBackgroundShape.RoundedSquare -> {
                val radius = boxSize * options.cornerRadiusFraction
                append("<rect x=\"$left\" y=\"$top\" width=\"$boxSize\" height=\"$boxSize\" rx=\"$radius\" fill=\"${options.background.toCss()}\"/>")
            }
            QrLogoBackgroundShape.Circle -> {
                append("<circle cx=\"${left + boxSize / 2.0}\" cy=\"${top + boxSize / 2.0}\" r=\"${boxSize / 2.0}\" fill=\"${options.background.toCss()}\"/>")
            }
        }

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
        append("\" preserveAspectRatio=\"xMidYMid meet\"/>")
    }

    private fun String.escapeXmlAttribute(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}

/** Convenience extension for common Kotlin, Android, and iOS code. */
public fun QrCode.toSvg(
    style: QrStyle = QrStyle(),
    logoDataUri: String? = null,
): String = SvgQrRenderer.render(this, style, logoDataUri)
