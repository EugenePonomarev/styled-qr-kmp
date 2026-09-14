package io.github.eugeneponomarev.styledqr.core

/** Deinterleaves QR blocks, corrects each Reed–Solomon block, and returns data codewords. */
internal object QrCodewordDecoder {

    fun correctAndExtractData(
        extracted: QrExtractedCodewords,
    ): QrCorrectedDataCodewords {
        val version = extracted.version
        val errorCorrection = extracted.errorCorrection
        val numberOfBlocks = QrTables.numErrorCorrectionBlocks(version, errorCorrection)
        val errorCorrectionCodewordsPerBlock = QrTables.errorCorrectionCodewordsPerBlock(
            version,
            errorCorrection,
        )
        val rawCodewordCount = QrTables.numRawDataModules(version) / BITS_PER_BYTE
        val numberOfShortBlocks = numberOfBlocks - rawCodewordCount % numberOfBlocks
        val shortBlockLength = rawCodewordCount / numberOfBlocks
        val shortDataLength = shortBlockLength - errorCorrectionCodewordsPerBlock

        if (extracted.interleavedCodewords.size != rawCodewordCount) {
            throw QrDecodeException(
                "QR contains ${extracted.interleavedCodewords.size} codewords, " +
                    "expected $rawCodewordCount",
            )
        }

        val paddedBlocks = Array(numberOfBlocks) {
            ByteArray(shortBlockLength + 1)
        }

        var interleavedOffset = 0
        for (index in 0..shortBlockLength) {
            for (blockIndex in paddedBlocks.indices) {
                val isShortBlockPadding =
                    index == shortDataLength && blockIndex < numberOfShortBlocks

                if (!isShortBlockPadding) {
                    paddedBlocks[blockIndex][index] =
                        extracted.interleavedCodewords[interleavedOffset]
                    interleavedOffset += 1
                }
            }
        }

        if (interleavedOffset != extracted.interleavedCodewords.size) {
            throw QrDecodeException("QR codewords could not be fully deinterleaved")
        }

        val result = ByteArray(QrTables.numDataCodewords(version, errorCorrection))
        var dataOffset = 0
        var correctedErrorCount = 0

        paddedBlocks.forEachIndexed { blockIndex, paddedBlock ->
            val isShortBlock = blockIndex < numberOfShortBlocks
            val block = if (isShortBlock) {
                shortBlockWithoutPadding(
                    paddedBlock = paddedBlock,
                    shortDataLength = shortDataLength,
                )
            } else {
                paddedBlock.copyOf()
            }

            correctedErrorCount += QrReedSolomonDecoder.correct(
                received = block,
                errorCorrectionCodewords = errorCorrectionCodewordsPerBlock,
            )

            val dataLength = shortDataLength + if (isShortBlock) 0 else 1
            for (index in 0 until dataLength) {
                result[dataOffset] = block[index]
                dataOffset += 1
            }
        }

        if (dataOffset != result.size) {
            throw QrDecodeException("QR data codeword count is invalid after correction")
        }

        return QrCorrectedDataCodewords(
            dataCodewords = result,
            correctedErrorCount = correctedErrorCount,
        )
    }

    private fun shortBlockWithoutPadding(
        paddedBlock: ByteArray,
        shortDataLength: Int,
    ): ByteArray {
        val result = ByteArray(paddedBlock.size - 1)

        for (index in 0 until shortDataLength) {
            result[index] = paddedBlock[index]
        }
        for (index in shortDataLength until result.size) {
            result[index] = paddedBlock[index + 1]
        }

        return result
    }

    private const val BITS_PER_BYTE = 8
}

internal data class QrCorrectedDataCodewords(
    val dataCodewords: ByteArray,
    val correctedErrorCount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrCorrectedDataCodewords

        if (correctedErrorCount != other.correctedErrorCount) return false
        if (!dataCodewords.contentEquals(other.dataCodewords)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = correctedErrorCount
        result = 31 * result + dataCodewords.contentHashCode()
        return result
    }
}