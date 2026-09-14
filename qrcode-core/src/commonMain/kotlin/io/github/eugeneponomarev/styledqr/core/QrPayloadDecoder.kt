package io.github.eugeneponomarev.styledqr.core

/**
 * Decodes the payload modes currently produced by [QrCodeGenerator]:
 * Byte mode and an optional ECI designator.
 */
internal object QrPayloadDecoder {

    fun decode(
        dataCodewords: ByteArray,
        version: Int,
    ): QrDecodedBytePayload {
        val reader = QrDataBitReader(dataCodewords)
        val bytes = ArrayList<Byte>()
        var eciAssignment: Int? = null
        var hasByteSegment = false

        segmentLoop@ while (reader.availableBits >= MODE_BITS) {
            when (val mode = reader.readBits(MODE_BITS)) {
                TERMINATOR_MODE_BITS -> break@segmentLoop

                ECI_MODE_BITS -> {
                    if (hasByteSegment || eciAssignment != null) {
                        throw QrDecodeException(
                            "Multiple or mid-payload ECI designators are not supported",
                        )
                    }
                    eciAssignment = readEciAssignment(reader)
                }

                BYTE_MODE_BITS -> {
                    val byteCount = reader.readBits(byteCountBits(version))
                    if (reader.availableBits < byteCount * BITS_PER_BYTE) {
                        throw QrDecodeException("QR Byte-mode segment is truncated")
                    }

                    repeat(byteCount) {
                        bytes.add(reader.readBits(BITS_PER_BYTE).toByte())
                    }
                    hasByteSegment = true
                }

                else -> throw QrDecodeException(
                    "QR mode 0b${mode.toString(radix = 2)} is not supported",
                )
            }
        }

        return QrDecodedBytePayload(
            bytes = ByteArray(bytes.size) { index -> bytes[index] },
            eciAssignment = eciAssignment,
        )
    }

    private fun readEciAssignment(reader: QrDataBitReader): Int {
        val firstByte = reader.readBits(BITS_PER_BYTE)

        return when {
            firstByte and 0x80 == 0 -> firstByte

            firstByte and 0xC0 == 0x80 -> {
                ((firstByte and 0x3F) shl BITS_PER_BYTE) or
                    reader.readBits(BITS_PER_BYTE)
            }

            firstByte and 0xE0 == 0xC0 -> {
                ((firstByte and 0x1F) shl (BITS_PER_BYTE * 2)) or
                    (reader.readBits(BITS_PER_BYTE) shl BITS_PER_BYTE) or
                    reader.readBits(BITS_PER_BYTE)
            }

            else -> throw QrDecodeException("QR ECI designator is invalid")
        }
    }

    private fun byteCountBits(version: Int): Int = when (version) {
        in 1..9 -> 8
        in 10..40 -> 16
        else -> throw QrDecodeException("QR version must be in 1..40; was $version")
    }

    private const val MODE_BITS = 4
    private const val TERMINATOR_MODE_BITS = 0b0000
    private const val BYTE_MODE_BITS = 0b0100
    private const val ECI_MODE_BITS = 0b0111
    private const val BITS_PER_BYTE = 8
}

internal data class QrDecodedBytePayload(
    val bytes: ByteArray,
    val eciAssignment: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrDecodedBytePayload

        if (eciAssignment != other.eciAssignment) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eciAssignment ?: 0
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

private class QrDataBitReader(
    private val bytes: ByteArray,
) {
    private var bitOffset: Int = 0

    val availableBits: Int
        get() = bytes.size * BITS_PER_BYTE - bitOffset

    fun readBits(count: Int): Int {
        if (count !in 0..MAX_BITS_PER_READ || count > availableBits) {
            throw QrDecodeException(
                "Cannot read $count QR payload bits; only $availableBits remain",
            )
        }

        var result = 0
        repeat(count) {
            val byteIndex = bitOffset ushr 3
            val bitIndex = 7 - (bitOffset and 7)

            result = (result shl 1) or (
                (bytes[byteIndex].toInt() ushr bitIndex) and 1
            )
            bitOffset += 1
        }
        return result
    }

    private companion object {
        const val BITS_PER_BYTE = 8
        const val MAX_BITS_PER_READ = 31
    }
}
