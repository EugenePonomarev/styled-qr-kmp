package io.github.eugeneponomarev.styledqr.core

/** Creates standard-compliant QR matrices without a third-party QR dependency. */
public object QrCodeGenerator {
    /**
     * Encodes [text] as UTF-8 text. An ECI designator is added so that Cyrillic and other
     * non-Latin text are decoded as UTF-8 by conforming scanners.
     */
    public fun encodeText(
        text: String,
        errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
        minVersion: Int = 1,
        maxVersion: Int = 40,
    ): QrCode = encodeSegments(
        segments = listOf(
            QrSegment.makeEci(UTF8_ECI_ASSIGNMENT),
            QrSegment.makeBytes(text.encodeToByteArray()),
        ),
        requestedErrorCorrection = errorCorrection,
        minVersion = minVersion,
        maxVersion = maxVersion,
    )

    /**
     * Encodes raw binary bytes. Use [encodeText] for normal URLs and text because it adds UTF-8
     * character-set information for scanners.
     */
    public fun encodeBytes(
        bytes: ByteArray,
        errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
        minVersion: Int = 1,
        maxVersion: Int = 40,
    ): QrCode = encodeSegments(
        segments = listOf(QrSegment.makeBytes(bytes)),
        requestedErrorCorrection = errorCorrection,
        minVersion = minVersion,
        maxVersion = maxVersion,
    )

    private fun encodeSegments(
        segments: List<QrSegment>,
        requestedErrorCorrection: QrErrorCorrectionLevel,
        minVersion: Int,
        maxVersion: Int,
    ): QrCode {
        require(minVersion in 1..40) { "minVersion must be in 1..40" }
        require(maxVersion in minVersion..40) { "maxVersion must be in $minVersion..40" }

        var version = minVersion
        var dataUsedBits: Int? = null
        while (version <= maxVersion) {
            val usedBits = totalBits(segments, version)
            val capacityBits = QrTables.numDataCodewords(version, requestedErrorCorrection) * 8
            if (usedBits != null && usedBits <= capacityBits) {
                dataUsedBits = usedBits
                break
            }
            version += 1
        }

        val finalDataUsedBits = checkNotNull(dataUsedBits) {
            "Payload is too long for QR versions $minVersion..$maxVersion at $requestedErrorCorrection level"
        }

        // If the selected version has spare capacity, use the strongest correction level that fits.
        var actualErrorCorrection = requestedErrorCorrection
        for (candidate in QrErrorCorrectionLevel.entries.reversed()) {
            if (candidate.ordinal < requestedErrorCorrection.ordinal) continue
            if (finalDataUsedBits <= QrTables.numDataCodewords(version, candidate) * 8) {
                actualErrorCorrection = candidate
                break
            }
        }

        val dataCodewords = makeDataCodewords(
            segments = segments,
            version = version,
            dataCapacityBytes = QrTables.numDataCodewords(version, actualErrorCorrection),
        )
        return QrMatrixBuilder(
            version = version,
            errorCorrection = actualErrorCorrection,
            dataCodewords = dataCodewords,
        ).build()
    }

    private fun totalBits(segments: List<QrSegment>, version: Int): Int? {
        var result = 0
        segments.forEach { segment ->
            val countBits = segment.mode.characterCountBits(version)
            if (segment.numChars >= (1 shl countBits)) return null
            result += 4 + countBits + segment.data.size
        }
        return result
    }

    private fun makeDataCodewords(
        segments: List<QrSegment>,
        version: Int,
        dataCapacityBytes: Int,
    ): ByteArray {
        val dataCapacityBits = dataCapacityBytes * 8
        val buffer = BitBuffer()

        segments.forEach { segment ->
            buffer.appendBits(segment.mode.modeBits, 4)
            buffer.appendBits(segment.numChars, segment.mode.characterCountBits(version))
            buffer.append(segment.data)
        }

        check(buffer.size <= dataCapacityBits)
        buffer.appendBits(0, minOf(4, dataCapacityBits - buffer.size))
        buffer.appendBits(0, (8 - buffer.size % 8) % 8)

        var padByte = PAD_BYTE_1
        while (buffer.size < dataCapacityBits) {
            buffer.appendBits(padByte, 8)
            padByte = if (padByte == PAD_BYTE_1) PAD_BYTE_2 else PAD_BYTE_1
        }

        return buffer.toByteArray().also {
            check(it.size == dataCapacityBytes)
        }
    }

    private const val UTF8_ECI_ASSIGNMENT: Int = 26
    private const val PAD_BYTE_1: Int = 0xEC
    private const val PAD_BYTE_2: Int = 0x11
}
