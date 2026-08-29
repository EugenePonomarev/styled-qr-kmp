package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrTables
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Integer-aligned area cleared below a centre logo. Coordinates exclude the QR quiet zone.
 *
 * [sizeModules] includes logo padding. [logoSizeModules] is the maximum square into which a
 * platform renderer places the image itself.
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
 * Calculates a centre-logo area that is safe for this exact QR matrix.
 *
 * A QR specification has no arbitrary "unused" data region: every non-function module belongs
 * to a codeword. A logo therefore behaves as a known visual overwrite. This function reserves
 * one complete, module-aligned square in the renderer and permits it only when:
 *
 * - no finder, timing, alignment, format, or version module is covered; and
 * - the number of affected codewords in every Reed–Solomon block remains below its correction
 *   limit, with one codeword kept as a safety margin.
 *
 * It throws [IllegalArgumentException] rather than generating a QR code whose readability cannot
 * be guaranteed by its declared error-correction level.
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
