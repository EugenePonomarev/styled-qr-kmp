package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.core.QrTables
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QrCodeDecoderConformanceTest {

    @Test
    fun roundTripsEveryVersionForEveryRequestedErrorCorrectionLevel() {
        for (version in 1..40) {
            for (errorCorrection in QrErrorCorrectionLevel.entries) {
                val actualErrorCorrection = actualErrorCorrectionFor(
                    version = version,
                    requestedErrorCorrection = errorCorrection,
                )
                val source = payloadThatForces(
                    version = version,
                    errorCorrection = actualErrorCorrection,
                )
                val caseName =
                    "version=$version, requested=$errorCorrection, actual=$actualErrorCorrection"
                val code = QrCodeGenerator.encodeBytes(
                    bytes = source,
                    errorCorrection = errorCorrection,
                    minVersion = version,
                    maxVersion = version,
                )

                assertEquals(version, code.version, caseName)
                assertEquals(actualErrorCorrection, code.errorCorrection, caseName)

                val decoded = QrCodeDecoder.decode(code.copyModules())

                assertContentEquals(source, decoded.copyBytes(), caseName)
                assertEquals(version, decoded.version, caseName)
                assertEquals(actualErrorCorrection, decoded.errorCorrection, caseName)
                assertEquals(0, decoded.correctedErrorCount, caseName)
                assertNull(decoded.eciAssignment, caseName)
                assertNull(decoded.utf8TextOrNull(), caseName)
            }
        }
    }

    private fun actualErrorCorrectionFor(
        version: Int,
        requestedErrorCorrection: QrErrorCorrectionLevel,
    ): QrErrorCorrectionLevel {
        val requestedCapacityBits = dataCapacityBits(
            version = version,
            errorCorrection = requestedErrorCorrection,
        )

        for (
        ordinal in QrErrorCorrectionLevel.H.ordinal downTo
                requestedErrorCorrection.ordinal
        ) {
            val candidate = QrErrorCorrectionLevel.entries[ordinal]
            if (dataCapacityBits(version, candidate) >= requestedCapacityBits) {
                return candidate
            }
        }

        error("No error-correction level is available for version $version")
    }

    private fun payloadThatForces(
        version: Int,
        errorCorrection: QrErrorCorrectionLevel,
    ): ByteArray {
        val headerBits = MODE_BITS + byteCountBits(version)
        val requestedCapacityBits = dataCapacityBits(
            version = version,
            errorCorrection = errorCorrection,
        )
        val strongestHigherCapacityBits = QrErrorCorrectionLevel.entries
            .filter { level -> level.ordinal > errorCorrection.ordinal }
            .maxOfOrNull { level -> dataCapacityBits(version, level) }

        val byteCount = if (strongestHigherCapacityBits == null) {
            1
        } else {
            (strongestHigherCapacityBits - headerBits) / BITS_PER_BYTE + 1
        }
        val usedBits = headerBits + byteCount * BITS_PER_BYTE

        check(usedBits <= requestedCapacityBits) {
            "Cannot force $errorCorrection for version $version"
        }
        check(strongestHigherCapacityBits == null || usedBits > strongestHigherCapacityBits) {
            "Payload does not force $errorCorrection for version $version"
        }

        return ByteArray(byteCount) { index ->
            (
                version * VERSION_MULTIPLIER +
                        errorCorrection.ordinal * LEVEL_MULTIPLIER +
                        index * INDEX_MULTIPLIER
                ).toByte()
        }
    }

    private fun dataCapacityBits(
        version: Int,
        errorCorrection: QrErrorCorrectionLevel,
    ): Int = QrTables.numDataCodewords(version, errorCorrection) * BITS_PER_BYTE

    private fun byteCountBits(version: Int): Int = when (version) {
        in 1..9 -> BYTE_COUNT_BITS_FOR_SMALL_VERSIONS
        in 10..40 -> BYTE_COUNT_BITS_FOR_LARGE_VERSIONS
        else -> error("Unsupported QR version: $version")
    }

    private companion object {
        const val MODE_BITS = 4
        const val BITS_PER_BYTE = 8
        const val BYTE_COUNT_BITS_FOR_SMALL_VERSIONS = 8
        const val BYTE_COUNT_BITS_FOR_LARGE_VERSIONS = 16

        const val VERSION_MULTIPLIER = 73
        const val LEVEL_MULTIPLIER = 41
        const val INDEX_MULTIPLIER = 29
    }
}
