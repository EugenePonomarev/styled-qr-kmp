package io.github.eugeneponomarev.styledqr.core

/** Constants and formulae defined by ISO/IEC 18004 for QR versions 1 through 40. */
internal object QrTables {
    private val errorCorrectionCodewordsPerBlock: Array<IntArray> = arrayOf(
        intArrayOf(-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 30, 18, 18, 22, 22, 30, 22, 33, 12, 17, 17, 18, 20, 24, 30, 18, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
        intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 28, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28),
        intArrayOf(-1, 13, 22, 18, 26, 18, 24, 18, 22, 22, 22, 22, 24, 24, 22, 24, 24, 28, 26, 26, 26, 28, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
        intArrayOf(-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 30, 22, 22, 24, 24, 30, 28, 28, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
    )

    private val numErrorCorrectionBlocks: Array<IntArray> = arrayOf(
        intArrayOf(-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25),
        intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49),
        intArrayOf(-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68),
        intArrayOf(-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81),
    )

    fun numDataCodewords(version: Int, level: QrErrorCorrectionLevel): Int {
        checkVersion(version)
        return numRawDataModules(version) / 8 -
            errorCorrectionCodewordsPerBlock[level.ordinal][version] *
            numErrorCorrectionBlocks[level.ordinal][version]
    }

    fun errorCorrectionCodewordsPerBlock(version: Int, level: QrErrorCorrectionLevel): Int {
        checkVersion(version)
        return errorCorrectionCodewordsPerBlock[level.ordinal][version]
    }

    fun numErrorCorrectionBlocks(version: Int, level: QrErrorCorrectionLevel): Int {
        checkVersion(version)
        return numErrorCorrectionBlocks[level.ordinal][version]
    }

    fun numRawDataModules(version: Int): Int {
        checkVersion(version)

        var result = (16 * version + 128) * version + 64
        if (version >= 2) {
            val numAlign = version / 7 + 2
            result -= (25 * numAlign - 10) * numAlign - 55
            if (version >= 7) result -= 36
        }
        return result
    }

    fun alignmentPatternPositions(version: Int): IntArray {
        checkVersion(version)
        if (version == 1) return intArrayOf()

        val numAlign = version / 7 + 2
        val step = if (version == 32) {
            26
        } else {
            ((version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2)) * 2
        }

        val result = IntArray(numAlign)
        result[0] = 6
        var position = version * 4 + 10
        for (index in result.lastIndex downTo 1) {
            result[index] = position
            position -= step
        }
        return result
    }

    private fun checkVersion(version: Int) {
        require(version in 1..40) { "QR version must be in 1..40, was $version" }
    }
}
