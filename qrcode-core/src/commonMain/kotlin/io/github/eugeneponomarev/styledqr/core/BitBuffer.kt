package io.github.eugeneponomarev.styledqr.core

/** A small platform-independent bit buffer used by QR segments and codewords. */
internal class BitBuffer {
    private val bits: MutableList<Boolean> = ArrayList()

    val size: Int
        get() = bits.size

    fun appendBits(value: Int, length: Int) {
        require(length in 0..31) { "Bit length must be between 0 and 31, was $length" }
        require(value >= 0 && (length == 31 || value ushr length == 0)) {
            "Value $value does not fit in $length bits"
        }

        for (index in length - 1 downTo 0) {
            bits += ((value ushr index) and 1) != 0
        }
    }

    fun append(other: BooleanArray) {
        other.forEach { bits += it }
    }

    fun toBooleanArray(): BooleanArray = BooleanArray(bits.size) { bits[it] }

    fun toByteArray(): ByteArray {
        val result = ByteArray((bits.size + 7) / 8)
        bits.forEachIndexed { index, value ->
            if (value) {
                val byteIndex = index ushr 3
                val bitIndex = 7 - (index and 7)
                result[byteIndex] = (result[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }
        return result
    }
}
