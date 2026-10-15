package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrTables
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Module-aligned bounding square validated for a centre logo.
 * Coordinates are relative to the QR matrix and exclude the quiet zone.
 *
 * [sizeModules] includes padding. [logoSizeModules] is the side of the inner
 * square viewport after padding. Renderers fill this viewport while preserving
 * the image aspect ratio and cropping excess content around the centre.
 *
 * Layout validation covers the complete bounding square. Renderers clear
 * the selected background shape inside it and clip the image to that shape
 * inset by [paddingModules].
 */
public class QrLogoLayout internal constructor(
    public val leftModule: Int,
    public val topModule: Int,
    public val sizeModules: Int,
    public val paddingModules: Int,
) {
    public val logoSizeModules: Int = sizeModules - paddingModules * 2

    internal fun contains(row: Int, column: Int): Boolean =
        row in topModule until topModule + sizeModules &&
            column in leftModule until leftModule + sizeModules
}

/**
 * Calculates and validates a module-aligned centre-logo layout for this QR matrix.
 *
 * A logo visually overwrites encoded modules. This function permits its bounding
 * square only when:
 *
 * - it covers no QR function modules; and
 * - the number of affected codewords in each Reed–Solomon block stays within
 *   its correction capacity, with one correctable codeword reserved as a margin.
 *
 * Every codeword intersecting the bounding square is counted conservatively,
 * including those touched by padding or corners outside a curved background.
 * Selecting a curved background does not relax these validation rules.
 *
 * This function does not modify the QR matrix. Passing its checks does not
 * guarantee optical readability under arbitrary rendering or scanning conditions.
 *
 * @throws IllegalArgumentException if the bounding square does not fit the matrix,
 * covers a function module, or cannot fit within the allowed correction budget.
 */
public fun QrCode.calculateLogoLayout(options: QrLogoOptions): QrLogoLayout {
    val sizeModules = ceil(size * (options.sizeFraction + options.paddingFraction * 2.0)).toInt()
    require(sizeModules in 1..size) {
        "Logo area of $sizeModules modules does not fit QR version $version ($size×$size modules)"
    }

    val requestedPadding = (size * options.paddingFraction).roundToInt()
    val paddingModules = requestedPadding.coerceIn(0, (sizeModules - 1) / 2)
    val leftModule = (size - sizeModules) / 2
    val topModule = (size - sizeModules) / 2
    val layout = QrLogoLayout(leftModule, topModule, sizeModules, paddingModules)

    for (row in topModule until topModule + sizeModules) {
        for (column in leftModule until leftModule + sizeModules) {
            require(!isFunctionModule(row, column)) {
                "Centre logo overlaps a mandatory QR pattern in version $version. " +
                    "Use a smaller logo or a shorter payload."
            }
        }
    }

    val blockCount = QrTables.numErrorCorrectionBlocks(version, errorCorrection)
    val affectedCodewords = Array(blockCount) { linkedSetOf<Int>() }
    for (row in topModule until topModule + sizeModules) {
        for (column in leftModule until leftModule + sizeModules) {
            val codewordIndex = dataCodewordIndexAt(row, column)
            if (codewordIndex >= 0) {
                affectedCodewords[blockIndexForInterleavedCodeword(codewordIndex)] += codewordIndex
            }
        }
    }

    val correctionCapacity = QrTables.errorCorrectionCodewordsPerBlock(version, errorCorrection) / 2
    val allowedAffectedCodewords = correctionCapacity - LOGO_SAFETY_MARGIN_CODEWORDS
    require(allowedAffectedCodewords > 0) {
        "QR version $version at $errorCorrection correction has no safe correction budget for a centre logo"
    }

    val highestAffectedCount = affectedCodewords.maxOf { it.size }
    require(highestAffectedCount <= allowedAffectedCodewords) {
        "Centre logo can alter up to $highestAffectedCount codewords in one correction block, " +
            "but QR version $version at $errorCorrection correction safely allows only " +
            "$allowedAffectedCodewords. Reduce the logo or padding."
    }

    return layout
}

private const val LOGO_SAFETY_MARGIN_CODEWORDS: Int = 1
