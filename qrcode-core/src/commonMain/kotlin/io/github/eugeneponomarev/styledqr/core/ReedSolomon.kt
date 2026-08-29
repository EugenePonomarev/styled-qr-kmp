package io.github.eugeneponomarev.styledqr.core

/** Reed–Solomon operations over GF(2^8), using the QR-code primitive polynomial. */
internal object ReedSolomon {
    fun computeDivisor(degree: Int): ByteArray {
        require(degree in 1..255) { "Degree must be in 1..255" }

        val result = ByteArray(degree)
        result[result.lastIndex] = 1
        var root = 1

        repeat(degree) {
            for (index in result.indices) {
                result[index] = multiply(result[index].toInt() and 0xFF, root).toByte()
                if (index + 1 < result.size) {
                    result[index] = (
                        result[index].toInt() xor result[index + 1].toInt()
                    ).toByte()
                }
            }
            root = multiply(root, 0x02)
        }
        return result
    }

    fun computeRemainder(data: ByteArray, divisor: ByteArray): ByteArray {
        val result = ByteArray(divisor.size)

        data.forEach { byte ->
            val factor = (byte.toInt() xor result[0].toInt()) and 0xFF
            for (index in 0 until result.lastIndex) {
                result[index] = (
                    result[index + 1].toInt() xor
                        multiply(divisor[index].toInt() and 0xFF, factor)
                    ).toByte()
            }
            result[result.lastIndex] = multiply(divisor.last().toInt() and 0xFF, factor).toByte()
        }
        return result
    }

    private fun multiply(x: Int, y: Int): Int {
        require(x in 0..255 && y in 0..255)

        var value = 0
        var left = x
        var right = y
        while (right != 0) {
            if ((right and 1) != 0) value = value xor left
            right = right ushr 1
            left = (left shl 1) xor if ((left and 0x80) != 0) 0x11D else 0
        }
        return value
    }
}
