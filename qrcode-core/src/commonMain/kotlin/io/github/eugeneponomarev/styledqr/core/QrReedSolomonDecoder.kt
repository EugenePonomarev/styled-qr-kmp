package io.github.eugeneponomarev.styledqr.core

/**
 * Corrects a received QR Reed–Solomon block over GF(2^8).
 *
 * This is intentionally separate from the encoder's [ReedSolomon] remainder calculation.
 */
internal object QrReedSolomonDecoder {

    fun correct(
        received: ByteArray,
        errorCorrectionCodewords: Int,
    ): Int {
        require(errorCorrectionCodewords in 1 until received.size) {
            "errorCorrectionCodewords must be between 1 and ${received.size - 1}"
        }

        val values = IntArray(received.size) { index -> received[index].toInt() and 0xFF }
        val syndromeCoefficients = IntArray(errorCorrectionCodewords)
        var hasError = false

        val receivedPolynomial = QrGaloisPolynomial(values)
        for (index in 0 until errorCorrectionCodewords) {
            val syndrome = receivedPolynomial.evaluateAt(QrGaloisField.exp(index))
            syndromeCoefficients[syndromeCoefficients.lastIndex - index] = syndrome
            if (syndrome != 0) hasError = true
        }

        if (!hasError) return 0

        val syndromePolynomial = QrGaloisPolynomial(syndromeCoefficients)
        val (errorLocator, errorEvaluator) = runEuclideanAlgorithm(
            a = QrGaloisPolynomial.monomial(errorCorrectionCodewords, coefficient = 1),
            b = syndromePolynomial,
            errorCorrectionCodewords = errorCorrectionCodewords,
        )
        val errorLocations = findErrorLocations(errorLocator)

        if (errorLocations.size > errorCorrectionCodewords / 2) {
            throw QrDecodeException("QR Reed–Solomon block contains too many errors to correct")
        }

        val errorMagnitudes = findErrorMagnitudes(
            errorEvaluator = errorEvaluator,
            errorLocations = errorLocations,
        )

        errorLocations.indices.forEach { index ->
            val position = values.lastIndex - QrGaloisField.log(errorLocations[index])
            if (position !in values.indices) {
                throw QrDecodeException("QR Reed–Solomon error position is outside the block")
            }
            values[position] = values[position] xor errorMagnitudes[index]
        }

        val correctedPolynomial = QrGaloisPolynomial(values)
        for (index in 0 until errorCorrectionCodewords) {
            if (correctedPolynomial.evaluateAt(QrGaloisField.exp(index)) != 0) {
                throw QrDecodeException("QR Reed–Solomon correction failed")
            }
        }

        values.indices.forEach { index ->
            received[index] = values[index].toByte()
        }
        return errorLocations.size
    }

    private fun runEuclideanAlgorithm(
        a: QrGaloisPolynomial,
        b: QrGaloisPolynomial,
        errorCorrectionCodewords: Int,
    ): Pair<QrGaloisPolynomial, QrGaloisPolynomial> {
        var previousRemainder = a
        var remainder = b

        if (previousRemainder.degree < remainder.degree) {
            val temporary = previousRemainder
            previousRemainder = remainder
            remainder = temporary
        }

        var previousAuxiliary = QrGaloisPolynomial.zero()
        var auxiliary = QrGaloisPolynomial.one()

        while (remainder.degree >= errorCorrectionCodewords / 2) {
            val previousPreviousRemainder = previousRemainder
            val previousPreviousAuxiliary = previousAuxiliary
            previousRemainder = remainder
            previousAuxiliary = auxiliary

            if (previousRemainder.isZero) {
                throw QrDecodeException("QR Reed–Solomon Euclidean algorithm reached zero remainder")
            }

            remainder = previousPreviousRemainder
            var quotient = QrGaloisPolynomial.zero()

            val denominatorLeadingTerm = previousRemainder.coefficient(previousRemainder.degree)
            val inverseDenominatorLeadingTerm = QrGaloisField.inverse(denominatorLeadingTerm)

            while (!remainder.isZero && remainder.degree >= previousRemainder.degree) {
                val degreeDifference = remainder.degree - previousRemainder.degree
                val scale = QrGaloisField.multiply(
                    remainder.coefficient(remainder.degree),
                    inverseDenominatorLeadingTerm,
                )

                quotient = quotient.addOrSubtract(
                    QrGaloisPolynomial.monomial(
                        degree = degreeDifference,
                        coefficient = scale,
                    ),
                )
                remainder = remainder.addOrSubtract(
                    previousRemainder.multiplyByMonomial(
                        degree = degreeDifference,
                        coefficient = scale,
                    ),
                )
            }

            auxiliary = quotient.multiply(previousAuxiliary).addOrSubtract(
                previousPreviousAuxiliary,
            )
        }

        val locatorAtZero = auxiliary.coefficient(0)
        if (locatorAtZero == 0) {
            throw QrDecodeException("QR Reed–Solomon error locator is invalid")
        }

        val inverseLocatorAtZero = QrGaloisField.inverse(locatorAtZero)
        return auxiliary.multiplyByScalar(inverseLocatorAtZero) to
            remainder.multiplyByScalar(inverseLocatorAtZero)
    }

    private fun findErrorLocations(
        errorLocator: QrGaloisPolynomial,
    ): IntArray {
        if (errorLocator.degree == 1) {
            return intArrayOf(errorLocator.coefficient(1))
        }

        val result = IntArray(errorLocator.degree)
        var resultSize = 0

        for (value in 1 until QrGaloisField.SIZE) {
            if (errorLocator.evaluateAt(value) == 0) {
                result[resultSize] = QrGaloisField.inverse(value)
                resultSize += 1

                if (resultSize == result.size) break
            }
        }

        if (resultSize != result.size) {
            throw QrDecodeException("QR Reed–Solomon error locator has invalid roots")
        }
        return result
    }

    private fun findErrorMagnitudes(
        errorEvaluator: QrGaloisPolynomial,
        errorLocations: IntArray,
    ): IntArray = IntArray(errorLocations.size) { index ->
        val inverseLocation = QrGaloisField.inverse(errorLocations[index])
        var denominator = 1

        errorLocations.indices.forEach { otherIndex ->
            if (index == otherIndex) return@forEach

            val term = QrGaloisField.multiply(
                errorLocations[otherIndex],
                inverseLocation,
            )
            denominator = QrGaloisField.multiply(denominator, term xor 1)
        }

        QrGaloisField.multiply(
            errorEvaluator.evaluateAt(inverseLocation),
            QrGaloisField.inverse(denominator),
        )
    }
}

private object QrGaloisField {
    const val SIZE: Int = 256

    private val exponentTable: IntArray = IntArray(SIZE * 2)
    private val logarithmTable: IntArray = IntArray(SIZE)

    init {
        var value = 1
        for (index in 0 until SIZE - 1) {
            exponentTable[index] = value
            logarithmTable[value] = index

            value = value shl 1
            if (value and SIZE != 0) value = value xor 0x11D
        }

        for (index in SIZE - 1 until exponentTable.size) {
            exponentTable[index] = exponentTable[index - (SIZE - 1)]
        }
    }

    fun exp(power: Int): Int {
        require(power in 0 until SIZE - 1) { "GF exponent must be in 0..254" }
        return exponentTable[power]
    }

    fun log(value: Int): Int {
        require(value in 1 until SIZE) { "GF logarithm is undefined for $value" }
        return logarithmTable[value]
    }

    fun inverse(value: Int): Int {
        require(value in 1 until SIZE) { "GF inverse is undefined for $value" }
        return exponentTable[SIZE - 1 - logarithmTable[value]]
    }

    fun multiply(first: Int, second: Int): Int {
        require(first in 0 until SIZE && second in 0 until SIZE) {
            "GF values must be in 0..255"
        }
        if (first == 0 || second == 0) return 0

        return exponentTable[logarithmTable[first] + logarithmTable[second]]
    }
}

private class QrGaloisPolynomial(
    coefficients: IntArray,
) {
    private val coefficients: IntArray

    init {
        require(coefficients.isNotEmpty()) { "Polynomial must have at least one coefficient" }
        require(coefficients.all { it in 0 until QrGaloisField.SIZE }) {
            "Polynomial coefficients must be in 0..255"
        }

        var firstNonZero = 0
        while (
            firstNonZero < coefficients.lastIndex &&
            coefficients[firstNonZero] == 0
        ) {
            firstNonZero += 1
        }
        this.coefficients = coefficients.copyOfRange(firstNonZero, coefficients.size)
    }

    val degree: Int
        get() = coefficients.lastIndex

    val isZero: Boolean
        get() = coefficients.first() == 0

    fun coefficient(power: Int): Int {
        require(power in 0..degree) { "Polynomial power $power is outside 0..$degree" }
        return coefficients[coefficients.lastIndex - power]
    }

    fun evaluateAt(value: Int): Int {
        require(value in 0 until QrGaloisField.SIZE) { "GF value must be in 0..255" }

        if (value == 0) return coefficient(0)

        var result = coefficients.first()
        for (index in 1 until coefficients.size) {
            result = QrGaloisField.multiply(value, result) xor coefficients[index]
        }
        return result
    }

    fun addOrSubtract(other: QrGaloisPolynomial): QrGaloisPolynomial {
        if (isZero) return other
        if (other.isZero) return this

        val larger =
            if (coefficients.size >= other.coefficients.size) coefficients else other.coefficients
        val smaller =
            if (coefficients.size < other.coefficients.size) coefficients else other.coefficients
        val result = larger.copyOf()
        val difference = larger.size - smaller.size

        smaller.indices.forEach { index ->
            result[index + difference] = result[index + difference] xor smaller[index]
        }
        return QrGaloisPolynomial(result)
    }

    fun multiply(other: QrGaloisPolynomial): QrGaloisPolynomial {
        if (isZero || other.isZero) return zero()

        val result = IntArray(coefficients.size + other.coefficients.size - 1)
        coefficients.indices.forEach { firstIndex ->
            other.coefficients.indices.forEach { secondIndex ->
                result[firstIndex + secondIndex] = result[firstIndex + secondIndex] xor
                        QrGaloisField.multiply(
                            coefficients[firstIndex],
                            other.coefficients[secondIndex],
                        )
            }
        }
        return QrGaloisPolynomial(result)
    }

    fun multiplyByScalar(scalar: Int): QrGaloisPolynomial {
        require(scalar in 0 until QrGaloisField.SIZE) { "GF scalar must be in 0..255" }
        if (scalar == 0) return zero()
        if (scalar == 1) return this

        return QrGaloisPolynomial(
            IntArray(coefficients.size) { index ->
                QrGaloisField.multiply(coefficients[index], scalar)
            },
        )
    }

    fun multiplyByMonomial(
        degree: Int,
        coefficient: Int,
    ): QrGaloisPolynomial {
        require(degree >= 0) { "Polynomial degree must not be negative" }
        require(coefficient in 0 until QrGaloisField.SIZE) {
            "GF coefficient must be in 0..255"
        }
        if (coefficient == 0) return zero()

        val result = IntArray(coefficients.size + degree)
        coefficients.indices.forEach { index ->
            result[index] = QrGaloisField.multiply(coefficients[index], coefficient)
        }
        return QrGaloisPolynomial(result)
    }

    companion object {
        fun zero(): QrGaloisPolynomial = QrGaloisPolynomial(intArrayOf(0))

        fun one(): QrGaloisPolynomial = QrGaloisPolynomial(intArrayOf(1))

        fun monomial(
            degree: Int,
            coefficient: Int,
        ): QrGaloisPolynomial {
            require(degree >= 0) { "Polynomial degree must not be negative" }
            require(coefficient in 0 until QrGaloisField.SIZE) {
                "GF coefficient must be in 0..255"
            }

            val coefficients = IntArray(degree + 1)
            coefficients[0] = coefficient
            return QrGaloisPolynomial(coefficients)
        }
    }
}
