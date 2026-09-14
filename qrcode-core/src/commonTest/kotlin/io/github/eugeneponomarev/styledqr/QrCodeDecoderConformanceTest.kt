package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrDecodeException
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.core.QrTables
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun correctsVersionOneHighCorrectionLimit() {
        val source = byteArrayOf(0x42)
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = 1,
            maxVersion = 1,
        )
        val modules = code.copyModules()

        flipOneModuleInEachCodeword(
            code = code,
            modules = modules,
            codewordIndexes = 0 until VERSION_ONE_H_CORRECTION_LIMIT,
        )

        val decoded = QrCodeDecoder.decode(modules)

        assertContentEquals(source, decoded.copyBytes())
        assertEquals(VERSION_ONE_H_CORRECTION_LIMIT, decoded.correctedErrorCount)
    }

    @Test
    fun rejectsOneCodewordErrorBeyondVersionOneHighCorrectionLimit() {
        val source = byteArrayOf(0x42)
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = 1,
            maxVersion = 1,
        )
        val modules = code.copyModules()

        flipOneModuleInEachCodeword(
            code = code,
            modules = modules,
            codewordIndexes = 0..VERSION_ONE_H_CORRECTION_LIMIT,
        )

        assertFailsWith<QrDecodeException> {
            QrCodeDecoder.decode(modules)
        }
    }

    @Test
    fun correctsOneCodewordErrorInEveryVersionFortyHighCorrectionBlock() {
        val source = byteArrayOf(0x42)
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = MULTI_BLOCK_VERSION,
            maxVersion = MULTI_BLOCK_VERSION,
        )
        val modules = code.copyModules()
        val codewordIndexes = firstCodewordIndexForEachBlock(code)

        assertEquals(
            QrTables.numErrorCorrectionBlocks(
                MULTI_BLOCK_VERSION,
                QrErrorCorrectionLevel.H,
            ),
            codewordIndexes.size,
        )

        flipOneModuleInEachCodeword(
            code = code,
            modules = modules,
            codewordIndexes = codewordIndexes,
        )

        val decoded = QrCodeDecoder.decode(modules)

        assertContentEquals(source, decoded.copyBytes())
        assertEquals(codewordIndexes.size, decoded.correctedErrorCount)
    }

    @Test
    fun correctsFormatInformationWhenBothCopiesContainThreeErrors() {
        val source = payloadThatForces(
            version = 1,
            errorCorrection = QrErrorCorrectionLevel.M,
        )
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.M,
            minVersion = 1,
            maxVersion = 1,
        )
        val expected = QrCodeDecoder.decode(code.copyModules())
        val modules = code.copyModules()

        flipCorrectableFormatInformationBits(modules)

        val decoded = QrCodeDecoder.decode(modules)

        assertContentEquals(source, decoded.copyBytes())
        assertEquals(QrErrorCorrectionLevel.M, decoded.errorCorrection)
        assertEquals(expected.mask, decoded.mask)
        assertEquals(0, decoded.correctedErrorCount)
    }

    @Test
    fun acceptsVersionInformationWhenBothCopiesContainThreeErrors() {
        val source = byteArrayOf(0x42)
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = VERSION_INFORMATION_TEST_VERSION,
            maxVersion = VERSION_INFORMATION_TEST_VERSION,
        )
        val modules = code.copyModules()

        flipCorrectableVersionInformationBits(modules)

        val decoded = QrCodeDecoder.decode(modules)

        assertContentEquals(source, decoded.copyBytes())
        assertEquals(VERSION_INFORMATION_TEST_VERSION, decoded.version)
        assertEquals(QrErrorCorrectionLevel.H, decoded.errorCorrection)
        assertEquals(0, decoded.correctedErrorCount)
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

    private fun flipCorrectableFormatInformationBits(
        modules: Array<BooleanArray>,
    ) {
        repeat(BCH_CORRECTION_LIMIT) { index ->
            modules[index][8] = !modules[index][8]
            modules[8][modules.lastIndex - index] =
                !modules[8][modules.lastIndex - index]
        }
    }

    private fun flipCorrectableVersionInformationBits(
        modules: Array<BooleanArray>,
    ) {
        repeat(BCH_CORRECTION_LIMIT) { index ->
            val column = modules.size - 11 + index % 3
            val row = index / 3

            modules[row][column] = !modules[row][column]
            modules[column][row] = !modules[column][row]
        }
    }

    private fun firstCodewordIndexForEachBlock(
        code: QrCode,
    ): List<Int> {
        val seenBlockIndexes = mutableSetOf<Int>()
        val result = ArrayList<Int>()

        for (row in 0 until code.size) {
            for (column in 0 until code.size) {
                val codewordIndex = code.dataCodewordIndexAt(row, column)
                if (codewordIndex < 0) continue

                val blockIndex = code.blockIndexForInterleavedCodeword(codewordIndex)
                if (seenBlockIndexes.add(blockIndex)) {
                    result += codewordIndex
                }
            }
        }

        return result
    }

    private fun flipOneModuleInEachCodeword(
        code: QrCode,
        modules: Array<BooleanArray>,
        codewordIndexes: Iterable<Int>,
    ) {
        codewordIndexes.forEach { codewordIndex ->
            var flipped = false

            for (row in 0 until code.size) {
                for (column in 0 until code.size) {
                    if (code.dataCodewordIndexAt(row, column) == codewordIndex) {
                        modules[row][column] = !modules[row][column]
                        flipped = true
                        break
                    }
                }
                if (flipped) break
            }

            check(flipped) {
                "QR version ${code.version} does not contain codeword $codewordIndex"
            }
        }
    }

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
        const val VERSION_ONE_H_CORRECTION_LIMIT = 8
        const val MULTI_BLOCK_VERSION = 40
        const val BCH_CORRECTION_LIMIT = 3
        const val VERSION_INFORMATION_TEST_VERSION = 7
    }
}
