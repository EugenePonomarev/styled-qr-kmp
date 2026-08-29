package io.github.eugeneponomarev.styledqr.core

import kotlin.math.abs
import kotlin.math.max

/** Places QR function patterns, data codewords, error correction, and the optimal mask. */
internal class QrMatrixBuilder(
    private val version: Int,
    private val errorCorrection: QrErrorCorrectionLevel,
    private val dataCodewords: ByteArray,
) {
    private val size: Int = version * 4 + 17
    private val modules: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    private val isFunction: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    private val isLocator: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    private val dataBitIndexes: Array<IntArray> = Array(size) { IntArray(size) { -1 } }

    fun build(): QrCode {
        drawFunctionPatterns()
        drawCodewords(addErrorCorrectionAndInterleave())

        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0..7) {
            applyMask(mask)
            drawFormatBits(mask)
            val penalty = penaltyScore()
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
            }
            applyMask(mask)
        }

        applyMask(bestMask)
        drawFormatBits(bestMask)

        return QrCode(
            version = version,
            errorCorrection = errorCorrection,
            modules = Array(size) { row -> modules[row].copyOf() },
            functionModules = Array(size) { row -> isFunction[row].copyOf() },
            locatorModules = Array(size) { row -> isLocator[row].copyOf() },
            dataCodewordIndexes = Array(size) { row ->
                IntArray(size) { column ->
                    dataBitIndexes[row][column].takeIf { it >= 0 }?.div(8) ?: -1
                }
            },
            interleavedCodewordBlockIndexes = interleavedCodewordBlockIndexes(),
        )
    }

    private fun drawFunctionPatterns() {
        for (index in 0 until size) {
            setFunctionModule(6, index, index % 2 == 0)
            setFunctionModule(index, 6, index % 2 == 0)
        }

        drawFinderPattern(3, 3)
        drawFinderPattern(size - 4, 3)
        drawFinderPattern(3, size - 4)

        val alignmentPositions = QrTables.alignmentPatternPositions(version)
        alignmentPositions.forEachIndexed { rowIndex, row ->
            alignmentPositions.forEachIndexed { columnIndex, column ->
                val overlapsFinder =
                    (rowIndex == 0 && columnIndex == 0) ||
                        (rowIndex == 0 && columnIndex == alignmentPositions.lastIndex) ||
                        (rowIndex == alignmentPositions.lastIndex && columnIndex == 0)
                if (!overlapsFinder) drawAlignmentPattern(column, row)
            }
        }

        // These calls mark the locations as function modules before data placement.
        drawFormatBits(mask = 0)
        drawVersionBits()
    }

    private fun drawFinderPattern(centerX: Int, centerY: Int) {
        for (deltaY in -4..4) {
            for (deltaX in -4..4) {
                val column = centerX + deltaX
                val row = centerY + deltaY
                if (column !in 0 until size || row !in 0 until size) continue

                val distance = max(abs(deltaX), abs(deltaY))
                setFunctionModule(column, row, distance != 2 && distance != 4, isLocator = true)
            }
        }
    }

    private fun drawAlignmentPattern(centerX: Int, centerY: Int) {
        for (deltaY in -2..2) {
            for (deltaX in -2..2) {
                val distance = max(abs(deltaX), abs(deltaY))
                setFunctionModule(centerX + deltaX, centerY + deltaY, distance != 1, isLocator = true)
            }
        }
    }

    private fun drawFormatBits(mask: Int) {
        val data = (errorCorrection.formatBits shl 3) or mask
        var remainder = data
        repeat(10) {
            remainder = (remainder shl 1) xor if ((remainder ushr 9) != 0) FORMAT_GENERATOR else 0
        }
        val bits = ((data shl 10) or remainder) xor FORMAT_MASK

        for (index in 0..5) setFunctionModule(8, index, getBit(bits, index))
        setFunctionModule(8, 7, getBit(bits, 6))
        setFunctionModule(8, 8, getBit(bits, 7))
        setFunctionModule(7, 8, getBit(bits, 8))
        for (index in 9..14) setFunctionModule(14 - index, 8, getBit(bits, index))

        for (index in 0..7) setFunctionModule(size - 1 - index, 8, getBit(bits, index))
        for (index in 8..14) setFunctionModule(8, size - 15 + index, getBit(bits, index))
        setFunctionModule(8, size - 8, true)
    }

    private fun drawVersionBits() {
        if (version < 7) return

        var remainder = version
        repeat(12) {
            remainder = (remainder shl 1) xor if ((remainder ushr 11) != 0) VERSION_GENERATOR else 0
        }
        val bits = (version shl 12) or remainder

        for (index in 0..17) {
            val bit = getBit(bits, index)
            val a = size - 11 + index % 3
            val b = index / 3
            setFunctionModule(a, b, bit)
            setFunctionModule(b, a, bit)
        }
    }

    private fun drawCodewords(allCodewords: ByteArray) {
        var bitIndex = 0
        var right = size - 1

        while (right > 0) {
            if (right == 6) right -= 1

            for (vertical in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val row = if (upward) size - 1 - vertical else vertical
                for (offset in 0..1) {
                    val column = right - offset
                    if (!isFunction[row][column] && bitIndex < allCodewords.size * 8) {
                        modules[row][column] = getBit(
                            allCodewords[bitIndex ushr 3].toInt() and 0xFF,
                            7 - (bitIndex and 7),
                        )
                        dataBitIndexes[row][column] = bitIndex
                        bitIndex += 1
                    }
                }
            }
            right -= 2
        }

        check(bitIndex == allCodewords.size * 8) { "Not all QR codewords were placed" }
    }

    private fun addErrorCorrectionAndInterleave(): ByteArray {
        val numBlocks = QrTables.numErrorCorrectionBlocks(version, errorCorrection)
        val blockEccLength = QrTables.errorCorrectionCodewordsPerBlock(version, errorCorrection)
        val rawCodewords = QrTables.numRawDataModules(version) / 8
        val numShortBlocks = numBlocks - rawCodewords % numBlocks
        val shortBlockLength = rawCodewords / numBlocks
        val shortDataLength = shortBlockLength - blockEccLength
        val divisor = ReedSolomon.computeDivisor(blockEccLength)
        val blocks = ArrayList<ByteArray>(numBlocks)

        var dataOffset = 0
        repeat(numBlocks) { blockIndex ->
            val dataLength = shortDataLength + if (blockIndex < numShortBlocks) 0 else 1
            val data = dataCodewords.copyOfRange(dataOffset, dataOffset + dataLength)
            dataOffset += dataLength
            val ecc = ReedSolomon.computeRemainder(data, divisor)
            val paddedData = if (blockIndex < numShortBlocks) data + byteArrayOf(0) else data
            blocks += paddedData + ecc
        }
        check(dataOffset == dataCodewords.size)

        val result = ByteArray(rawCodewords)
        var resultOffset = 0
        for (index in blocks[0].indices) {
            blocks.forEachIndexed { blockIndex, block ->
                val isShortBlockPadding = index == shortDataLength && blockIndex < numShortBlocks
                if (!isShortBlockPadding) {
                    result[resultOffset] = block[index]
                    resultOffset += 1
                }
            }
        }
        check(resultOffset == result.size)
        return result
    }

    /**
     * Mirrors [addErrorCorrectionAndInterleave] and records the source Reed–Solomon block of
     * each codeword in traversal order. A logo can corrupt whole codewords, while correction is
     * applied independently per block, so the renderer needs this mapping for a sound limit.
     */
    private fun interleavedCodewordBlockIndexes(): IntArray {
        val numBlocks = QrTables.numErrorCorrectionBlocks(version, errorCorrection)
        val blockEccLength = QrTables.errorCorrectionCodewordsPerBlock(version, errorCorrection)
        val rawCodewords = QrTables.numRawDataModules(version) / 8
        val numShortBlocks = numBlocks - rawCodewords % numBlocks
        val shortBlockLength = rawCodewords / numBlocks
        val shortDataLength = shortBlockLength - blockEccLength
        val result = IntArray(rawCodewords)
        var resultOffset = 0

        // Short data blocks contain one artificial zero byte at this position. The byte is not
        // interleaved, so it must not appear in the mapping either.
        for (index in 0..shortBlockLength) {
            for (blockIndex in 0 until numBlocks) {
                val isShortBlockPadding = index == shortDataLength && blockIndex < numShortBlocks
                if (!isShortBlockPadding) {
                    result[resultOffset] = blockIndex
                    resultOffset += 1
                }
            }
        }

        check(resultOffset == result.size)
        return result
    }

    private fun applyMask(mask: Int) {
        require(mask in 0..7)

        for (row in 0 until size) {
            for (column in 0 until size) {
                if (isFunction[row][column]) continue

                val invert = when (mask) {
                    0 -> (row + column) % 2 == 0
                    1 -> row % 2 == 0
                    2 -> column % 3 == 0
                    3 -> (row + column) % 3 == 0
                    4 -> (row / 2 + column / 3) % 2 == 0
                    5 -> (row * column % 2 + row * column % 3) == 0
                    6 -> (row * column % 2 + row * column % 3) % 2 == 0
                    7 -> (row * column % 3 + (row + column) % 2) % 2 == 0
                    else -> error("Unreachable")
                }
                if (invert) modules[row][column] = !modules[row][column]
            }
        }
    }

    private fun penaltyScore(): Int {
        var result = 0

        // N1: runs of five or more modules of one colour.
        for (row in 0 until size) {
            var runColour = modules[row][0]
            var runLength = 1
            for (column in 1 until size) {
                if (modules[row][column] == runColour) {
                    runLength += 1
                } else {
                    result += runPenalty(runLength)
                    runColour = modules[row][column]
                    runLength = 1
                }
            }
            result += runPenalty(runLength)
        }
        for (column in 0 until size) {
            var runColour = modules[0][column]
            var runLength = 1
            for (row in 1 until size) {
                if (modules[row][column] == runColour) {
                    runLength += 1
                } else {
                    result += runPenalty(runLength)
                    runColour = modules[row][column]
                    runLength = 1
                }
            }
            result += runPenalty(runLength)
        }

        // N2: 2x2 blocks of one colour.
        for (row in 0 until size - 1) {
            for (column in 0 until size - 1) {
                val colour = modules[row][column]
                if (
                    colour == modules[row][column + 1] &&
                    colour == modules[row + 1][column] &&
                    colour == modules[row + 1][column + 1]
                ) {
                    result += PENALTY_N2
                }
            }
        }

        // N3: finder-like 1:1:3:1:1 patterns preceded or followed by four light modules.
        for (row in 0 until size) {
            for (column in 0..size - 7) {
                if (matchesFinderLikeRow(row, column) && hasQuietRunBeforeOrAfterRow(row, column)) {
                    result += PENALTY_N3
                }
            }
        }
        for (column in 0 until size) {
            for (row in 0..size - 7) {
                if (matchesFinderLikeColumn(row, column) && hasQuietRunBeforeOrAfterColumn(row, column)) {
                    result += PENALTY_N3
                }
            }
        }

        // N4: deviation from 50% dark modules.
        var darkModules = 0
        modules.forEach { row -> row.forEach { if (it) darkModules += 1 } }
        val totalModules = size * size
        result += abs(darkModules * 20 - totalModules * 10) / totalModules * PENALTY_N4

        return result
    }

    private fun runPenalty(length: Int): Int = if (length >= 5) PENALTY_N1 + length - 5 else 0

    private fun matchesFinderLikeRow(row: Int, column: Int): Boolean =
        modules[row][column] &&
            !modules[row][column + 1] &&
            modules[row][column + 2] &&
            modules[row][column + 3] &&
            modules[row][column + 4] &&
            !modules[row][column + 5] &&
            modules[row][column + 6]

    private fun matchesFinderLikeColumn(row: Int, column: Int): Boolean =
        modules[row][column] &&
            !modules[row + 1][column] &&
            modules[row + 2][column] &&
            modules[row + 3][column] &&
            modules[row + 4][column] &&
            !modules[row + 5][column] &&
            modules[row + 6][column]

    private fun hasQuietRunBeforeOrAfterRow(row: Int, column: Int): Boolean {
        val hasBefore = column >= 4 && (column - 4 until column).all { !modules[row][it] }
        val hasAfter = column + 11 <= size && (column + 7 until column + 11).all { !modules[row][it] }
        return hasBefore || hasAfter
    }

    private fun hasQuietRunBeforeOrAfterColumn(row: Int, column: Int): Boolean {
        val hasBefore = row >= 4 && (row - 4 until row).all { !modules[it][column] }
        val hasAfter = row + 11 <= size && (row + 7 until row + 11).all { !modules[it][column] }
        return hasBefore || hasAfter
    }

    private fun setFunctionModule(
        column: Int,
        row: Int,
        dark: Boolean,
        isLocator: Boolean = false,
    ) {
        modules[row][column] = dark
        isFunction[row][column] = true
        if (isLocator) this.isLocator[row][column] = true
    }

    private fun getBit(value: Int, index: Int): Boolean = ((value ushr index) and 1) != 0

    private companion object {
        const val FORMAT_GENERATOR = 0x537
        const val FORMAT_MASK = 0x5412
        const val VERSION_GENERATOR = 0x1F25
        const val PENALTY_N1 = 3
        const val PENALTY_N2 = 3
        const val PENALTY_N3 = 40
        const val PENALTY_N4 = 10
    }
}
