package io.github.eugeneponomarev.styledqr.core

/**
 * Reads QR metadata and interleaved codewords directly from a finished module matrix.
 *
 * This class deliberately reconstructs function-module positions instead of reading
 * [QrCode] metadata produced by the encoder.
 */
internal class QrMatrixReader(
    private val modules: Array<BooleanArray>,
) {
    private val size: Int = modules.size
    private val version: Int
    private val functionModules: Array<BooleanArray>

    init {
        if (size !in MIN_SIZE..MAX_SIZE || (size - 17) % 4 != 0) {
            throw QrDecodeException(
                "QR matrix size must be one of 21, 25, ..., 177; was $size",
            )
        }
        if (modules.any { it.size != size }) {
            throw QrDecodeException("QR matrix must be square")
        }

        version = (size - 17) / 4
        functionModules = createFunctionModuleMap()
    }

    fun extractCodewords(): QrExtractedCodewords {
        validateVersionInformation()

        val format = readFormatInformation()
        return QrExtractedCodewords(
            version = version,
            errorCorrection = format.errorCorrection,
            mask = format.mask,
            interleavedCodewords = readCodewords(mask = format.mask),
        )
    }

    private fun readFormatInformation(): QrFormatInformation {
        val firstCopy = readFirstFormatBits()
        val secondCopy = readSecondFormatBits()

        var best: QrFormatInformation? = null
        var bestDistance = Int.MAX_VALUE

        QrErrorCorrectionLevel.entries.forEach { errorCorrection ->
            for (mask in 0..7) {
                val candidate = formatBits(
                    errorCorrection = errorCorrection,
                    mask = mask,
                )
                val distance = minOf(
                    hammingDistance(firstCopy, candidate),
                    hammingDistance(secondCopy, candidate),
                )
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = QrFormatInformation(
                        errorCorrection = errorCorrection,
                        mask = mask,
                    )
                }
            }
        }

        if (best == null || bestDistance > MAX_BCH_ERRORS) {
            throw QrDecodeException("QR format information is invalid")
        }
        return best
    }

    private fun readFirstFormatBits(): Int {
        var result = 0

        fun append(index: Int, column: Int, row: Int) {
            if (module(column, row)) result = result or (1 shl index)
        }

        for (index in 0..5) append(index, column = 8, row = index)
        append(index = 6, column = 8, row = 7)
        append(index = 7, column = 8, row = 8)
        append(index = 8, column = 7, row = 8)
        for (index in 9..14) append(index, column = 14 - index, row = 8)

        return result
    }

    private fun readSecondFormatBits(): Int {
        var result = 0

        fun append(index: Int, column: Int, row: Int) {
            if (module(column, row)) result = result or (1 shl index)
        }

        for (index in 0..7) append(index, column = size - 1 - index, row = 8)
        for (index in 8..14) append(index, column = 8, row = size - 15 + index)

        return result
    }

    private fun validateVersionInformation() {
        if (version < 7) return

        var firstCopy = 0
        var secondCopy = 0
        for (index in 0..17) {
            val column = size - 11 + index % 3
            val row = index / 3

            if (module(column, row)) firstCopy = firstCopy or (1 shl index)
            if (module(row, column)) secondCopy = secondCopy or (1 shl index)
        }

        val expected = versionBits(version)
        if (
            minOf(
                hammingDistance(firstCopy, expected),
                hammingDistance(secondCopy, expected),
            ) > MAX_BCH_ERRORS
        ) {
            throw QrDecodeException("QR version information is invalid")
        }
    }

    private fun readCodewords(mask: Int): ByteArray {
        val codewordCount = QrTables.numRawDataModules(version) / BITS_PER_BYTE
        val totalBits = codewordCount * BITS_PER_BYTE
        val result = ByteArray(codewordCount)

        var bitIndex = 0
        var right = size - 1

        while (right > 0) {
            if (right == 6) right -= 1

            for (vertical in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val row = if (upward) size - 1 - vertical else vertical

                for (offset in 0..1) {
                    val column = right - offset
                    if (functionModules[row][column] || bitIndex >= totalBits) continue

                    var dark = module(column, row)
                    if (isMaskApplied(mask, row, column)) dark = !dark

                    if (dark) {
                        val byteIndex = bitIndex ushr 3
                        val bitInByte = 7 - (bitIndex and 7)
                        result[byteIndex] = (
                                result[byteIndex].toInt() or (1 shl bitInByte)
                                ).toByte()
                    }
                    bitIndex += 1
                }
            }
            right -= 2
        }

        if (bitIndex != totalBits) {
            throw QrDecodeException("QR matrix does not contain all codeword bits")
        }
        return result
    }

    private fun createFunctionModuleMap(): Array<BooleanArray> {
        val result = Array(size) { BooleanArray(size) }

        for (index in 0 until size) {
            mark(result, column = 6, row = index)
            mark(result, column = index, row = 6)
        }

        drawFinderMap(result, centerX = 3, centerY = 3)
        drawFinderMap(result, centerX = size - 4, centerY = 3)
        drawFinderMap(result, centerX = 3, centerY = size - 4)

        val alignmentPositions = QrTables.alignmentPatternPositions(version)
        alignmentPositions.forEachIndexed { rowIndex, row ->
            alignmentPositions.forEachIndexed { columnIndex, column ->
                val overlapsFinder =
                    (rowIndex == 0 && columnIndex == 0) ||
                            (rowIndex == 0 && columnIndex == alignmentPositions.lastIndex) ||
                            (rowIndex == alignmentPositions.lastIndex && columnIndex == 0)

                if (!overlapsFinder) {
                    drawAlignmentMap(result, centerX = column, centerY = row)
                }
            }
        }

        markFormatModules(result)
        markVersionModules(result)

        return result
    }

    private fun drawFinderMap(
        map: Array<BooleanArray>,
        centerX: Int,
        centerY: Int,
    ) {
        for (deltaY in -4..4) {
            for (deltaX in -4..4) {
                mark(
                    map = map,
                    column = centerX + deltaX,
                    row = centerY + deltaY,
                )
            }
        }
    }

    private fun drawAlignmentMap(
        map: Array<BooleanArray>,
        centerX: Int,
        centerY: Int,
    ) {
        for (deltaY in -2..2) {
            for (deltaX in -2..2) {
                mark(
                    map = map,
                    column = centerX + deltaX,
                    row = centerY + deltaY,
                )
            }
        }
    }

    private fun markFormatModules(map: Array<BooleanArray>) {
        for (index in 0..5) mark(map, column = 8, row = index)
        mark(map, column = 8, row = 7)
        mark(map, column = 8, row = 8)
        mark(map, column = 7, row = 8)
        for (index in 9..14) mark(map, column = 14 - index, row = 8)

        for (index in 0..7) mark(map, column = size - 1 - index, row = 8)
        for (index in 8..14) mark(map, column = 8, row = size - 15 + index)

        mark(map, column = 8, row = size - 8)
    }

    private fun markVersionModules(map: Array<BooleanArray>) {
        if (version < 7) return

        for (index in 0..17) {
            val column = size - 11 + index % 3
            val row = index / 3

            mark(map, column = column, row = row)
            mark(map, column = row, row = column)
        }
    }

    private fun mark(
        map: Array<BooleanArray>,
        column: Int,
        row: Int,
    ) {
        if (column in 0 until size && row in 0 until size) {
            map[row][column] = true
        }
    }

    private fun module(column: Int, row: Int): Boolean = modules[row][column]

    private fun isMaskApplied(
        mask: Int,
        row: Int,
        column: Int,
    ): Boolean = when (mask) {
        0 -> (row + column) % 2 == 0
        1 -> row % 2 == 0
        2 -> column % 3 == 0
        3 -> (row + column) % 3 == 0
        4 -> (row / 2 + column / 3) % 2 == 0
        5 -> (row * column % 2 + row * column % 3) == 0
        6 -> (row * column % 2 + row * column % 3) % 2 == 0
        7 -> (row * column % 3 + (row + column) % 2) % 2 == 0
        else -> throw QrDecodeException("QR mask must be in 0..7; was $mask")
    }

    private fun formatBits(
        errorCorrection: QrErrorCorrectionLevel,
        mask: Int,
    ): Int {
        val data = (errorCorrection.formatBits shl 3) or mask
        var remainder = data

        repeat(FORMAT_BCH_DEGREE) {
            remainder = (
                    remainder shl 1
                    ) xor if ((remainder ushr (FORMAT_BCH_DEGREE - 1)) != 0) {
                FORMAT_GENERATOR
            } else {
                0
            }
        }

        return ((data shl FORMAT_BCH_DEGREE) or remainder) xor FORMAT_MASK
    }

    private fun versionBits(version: Int): Int {
        var remainder = version

        repeat(VERSION_BCH_DEGREE) {
            remainder = (
                    remainder shl 1
                    ) xor if ((remainder ushr (VERSION_BCH_DEGREE - 1)) != 0) {
                VERSION_GENERATOR
            } else {
                0
            }
        }

        return (version shl VERSION_BCH_DEGREE) or remainder
    }

    private fun hammingDistance(first: Int, second: Int): Int =
        (first xor second).countOneBits()

    private companion object {
        const val MIN_SIZE = 21
        const val MAX_SIZE = 177
        const val BITS_PER_BYTE = 8
        const val MAX_BCH_ERRORS = 3

        const val FORMAT_BCH_DEGREE = 10
        const val FORMAT_GENERATOR = 0x537
        const val FORMAT_MASK = 0x5412

        const val VERSION_BCH_DEGREE = 12
        const val VERSION_GENERATOR = 0x1F25
    }
}

internal data class QrExtractedCodewords(
    val version: Int,
    val errorCorrection: QrErrorCorrectionLevel,
    val mask: Int,
    val interleavedCodewords: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrExtractedCodewords

        if (version != other.version) return false
        if (mask != other.mask) return false
        if (errorCorrection != other.errorCorrection) return false
        if (!interleavedCodewords.contentEquals(other.interleavedCodewords)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + mask
        result = 31 * result + errorCorrection.hashCode()
        result = 31 * result + interleavedCodewords.contentHashCode()
        return result
    }
}

private data class QrFormatInformation(
    val errorCorrection: QrErrorCorrectionLevel,
    val mask: Int,
)
