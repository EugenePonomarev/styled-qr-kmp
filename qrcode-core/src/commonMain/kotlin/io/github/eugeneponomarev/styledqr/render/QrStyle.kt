package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode

/** RGBA colour used by the platform-independent renderers. */
public data class QrColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    init {
        listOf(red, green, blue, alpha).forEach { channel ->
            require(channel in 0..255) { "A colour channel must be in 0..255, was $channel" }
        }
    }

    internal fun toCss(): String = if (alpha == 255) {
        "#${red.hex()}${green.hex()}${blue.hex()}"
    } else {
        "rgba($red, $green, $blue, ${alpha / 255.0})"
    }

    public companion object {
        public val Black: QrColor = QrColor(0, 0, 0)
        public val White: QrColor = QrColor(255, 255, 255)

        /** Parses a six-digit RGB value such as `#143A5A` or `143A5A`. */
        public fun fromHex(value: String): QrColor {
            val normalized = value.trim().removePrefix("#")
            require(normalized.length == 6 && normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                "Expected a six-digit RGB colour, for example #143A5A"
            }
            return QrColor(
                red = normalized.substring(0, 2).toInt(radix = 16),
                green = normalized.substring(2, 4).toInt(radix = 16),
                blue = normalized.substring(4, 6).toInt(radix = 16),
            )
        }

        private fun Int.hex(): String = toString(radix = 16).padStart(2, '0').uppercase()
    }
}

/** Shape used for ordinary dark data modules. */
public enum class QrModuleShape {
    Square,
    RoundedSquare,
    Circle,
    Diamond,
}

/** Shape used to draw each of the three large QR finder patterns as one composed "eye". */
public enum class QrFinderPatternShape {
    /** Uses the selected [QrModuleShape]. */
    MatchModuleShape,

    Square,
    RoundedSquare,
    Circle,
    Diamond,
}

/** Controls which QR structural modules remain square when a custom shape is selected. */
public enum class QrFunctionPatternStyle {
    /** Keeps every function module square: finder, alignment, timing, format, and version bits. */
    PreserveAll,

    /**
     * Keeps only finder and alignment patterns square. Timing, format, version, and fixed dark
     * modules use the selected [QrModuleShape], so the visible square lines change with the rest
     * of the code.
     */
    PreserveFindersAndAlignment,

    /** Applies the selected shape to every dark QR module. Use only after device scan testing. */
    MatchDataModules,
}

/** Shape used for the solid background placed under a centre logo. */
public enum class QrLogoBackgroundShape {
    Square,
    RoundedSquare,
    Circle,
}

/**
 * Requested layout of a centred logo. The final background area is aligned to whole QR modules
 * and validated against the QR matrix's error-correction budget before rendering.
 */
public data class QrLogoOptions(
    val sizeFraction: Double = 0.18,
    val paddingFraction: Double = 0.04,
    val background: QrColor = QrColor.White,
    val backgroundShape: QrLogoBackgroundShape = QrLogoBackgroundShape.RoundedSquare,
    val cornerRadiusFraction: Double = 0.18,
) {
    init {
        require(sizeFraction in 0.05..0.25) { "Logo sizeFraction must be in 0.05..0.25" }
        require(paddingFraction in 0.0..0.12) { "Logo paddingFraction must be in 0.0..0.12" }
        require(cornerRadiusFraction in 0.0..0.5) { "Logo cornerRadiusFraction must be in 0.0..0.5" }
    }
}

/**
 * Visual configuration. A logo does not alter the encoded payload: renderers validate and clear
 * a module-aligned visual area that stays within the QR error-correction budget.
 */
public data class QrStyle(
    val foreground: QrColor = QrColor.Black,
    val background: QrColor = QrColor.White,
    val quietZoneModules: Int = 4,
    val moduleShape: QrModuleShape = QrModuleShape.Square,
    val moduleScale: Double = 1.0,
    val roundedModuleRadiusFraction: Double = 0.22,
    /**
     * Legacy shorthand retained for source compatibility. [functionPatternStyle] takes priority
     * when it is explicitly supplied.
     */
    val preserveFunctionPatterns: Boolean = false,
    val functionPatternStyle: QrFunctionPatternStyle = if (preserveFunctionPatterns) {
        QrFunctionPatternStyle.PreserveAll
    } else {
        QrFunctionPatternStyle.MatchDataModules
    },
    /**
     * Draws each large finder pattern as a single 7:5:3 composed shape instead of a grid of
     * individual modules. This keeps the selected visual identity while retaining the contrast
     * ratios that scanners use to locate a QR code.
     */
    val finderPatternShape: QrFinderPatternShape = QrFinderPatternShape.MatchModuleShape,
    val logo: QrLogoOptions? = null,
) {
    init {
        require(quietZoneModules >= 0) { "quietZoneModules must not be negative" }
        require(moduleScale in 0.55..1.0) { "moduleScale must be in 0.55..1.0" }
        require(roundedModuleRadiusFraction in 0.0..0.5) {
            "roundedModuleRadiusFraction must be in 0.0..0.5"
        }
    }
}

/** Returns whether a renderer should draw this module as an unscaled square. */
internal fun QrCode.shouldPreserveAsSquare(
    row: Int,
    column: Int,
    style: QrStyle,
): Boolean = when (style.functionPatternStyle) {
    QrFunctionPatternStyle.PreserveAll -> isFunctionModule(row, column)
    QrFunctionPatternStyle.PreserveFindersAndAlignment -> isLocatorModule(row, column)
    QrFunctionPatternStyle.MatchDataModules -> false
}

internal fun QrStyle.resolvedFinderPatternShape(): QrModuleShape = when (finderPatternShape) {
    QrFinderPatternShape.MatchModuleShape -> moduleShape
    QrFinderPatternShape.Square -> QrModuleShape.Square
    QrFinderPatternShape.RoundedSquare -> QrModuleShape.RoundedSquare
    QrFinderPatternShape.Circle -> QrModuleShape.Circle
    QrFinderPatternShape.Diamond -> QrModuleShape.Diamond
}

/** True for the dark/white 7×7 core of one of the three finder patterns. */
internal fun QrCode.isFinderPatternCore(row: Int, column: Int): Boolean {
    val lastStart = size - FINDER_PATTERN_SIZE
    return (row in 0 until FINDER_PATTERN_SIZE && column in 0 until FINDER_PATTERN_SIZE) ||
        (row in 0 until FINDER_PATTERN_SIZE && column in lastStart until size) ||
        (row in lastStart until size && column in 0 until FINDER_PATTERN_SIZE)
}

internal fun QrCode.finderPatternTopLefts(): List<Pair<Int, Int>> = listOf(
    0 to 0,
    0 to size - FINDER_PATTERN_SIZE,
    size - FINDER_PATTERN_SIZE to 0,
)

private const val FINDER_PATTERN_SIZE: Int = 7
