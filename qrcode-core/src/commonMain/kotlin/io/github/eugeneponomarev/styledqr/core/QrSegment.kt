package io.github.eugeneponomarev.styledqr.core

internal enum class QrMode(
    val modeBits: Int,
    private val characterCountBits: IntArray,
) {
    Byte(modeBits = 0b0100, characterCountBits = intArrayOf(8, 16, 16)),
    Eci(modeBits = 0b0111, characterCountBits = intArrayOf(0, 0, 0)),
    ;

    fun characterCountBits(version: Int): Int = when (version) {
        in 1..9 -> characterCountBits[0]
        in 10..26 -> characterCountBits[1]
        in 27..40 -> characterCountBits[2]
        else -> error("Unsupported QR version: $version")
    }
}

internal class QrSegment(
    val mode: QrMode,
    val numChars: Int,
    val data: BooleanArray,
) {
    companion object {
        fun makeBytes(data: ByteArray): QrSegment {
            val bits = BitBuffer()
            data.forEach { byte -> bits.appendBits(byte.toInt() and 0xFF, 8) }
            return QrSegment(QrMode.Byte, data.size, bits.toBooleanArray())
        }

        /** Creates an ECI designator. Assignment 26 is the standard UTF-8 indicator. */
        fun makeEci(assignmentValue: Int): QrSegment {
            require(assignmentValue in 0..999_999) {
                "ECI assignment value must be in 0..999999"
            }

            val bits = BitBuffer()
            when (assignmentValue) {
                in 0..127 -> bits.appendBits(assignmentValue, 8)
                in 128..16_383 -> bits.appendBits(0b10_0000_0000_0000 or assignmentValue, 16)
                else -> bits.appendBits(0b110_0000_0000_0000_0000_0000 or assignmentValue, 24)
            }
            return QrSegment(QrMode.Eci, 0, bits.toBooleanArray())
        }
    }
}
