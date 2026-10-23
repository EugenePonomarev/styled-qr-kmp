package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.fixtures.QrParitySnapshots
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class QrParityFixtureTest {
    @Test
    fun encodedMatricesMatchFrozenFixtures() {
        for (fixture in QrParitySnapshots.all) {
            val level = QrErrorCorrectionLevel.valueOf(
                fixture.requestedErrorCorrection,
            )
            val text = fixture.text

            val code = if (text != null) {
                QrCodeGenerator.encodeText(
                    text,
                    level,
                    fixture.minVersion,
                    fixture.maxVersion,
                )
            } else {
                QrCodeGenerator.encodeBytes(
                    fixture.bytes.map { it.toByte() }.toByteArray(),
                    level,
                    fixture.minVersion,
                    fixture.maxVersion,
                )
            }

            assertEquals(fixture.version, code.version, fixture.id)
            assertEquals(
                fixture.errorCorrection,
                code.errorCorrection.name,
                fixture.id,
            )
            assertEquals(fixture.size, code.size, fixture.id)

            assertContentEquals(
                fixture.moduleRows,
                rows(code.size) { row, column -> code[row, column] },
                fixture.id,
            )
            assertContentEquals(
                fixture.functionRows,
                rows(code.size, code::isFunctionModule),
                fixture.id,
            )
            assertContentEquals(
                fixture.locatorRows,
                rows(code.size, code::isLocatorModule),
                fixture.id,
            )
        }
    }

    @Test
    fun decoderReadsFrozenMatrices() {
        for (fixture in QrParitySnapshots.all) {
            val modules = Array(fixture.size) { row ->
                BooleanArray(fixture.size) { column ->
                    fixture.moduleRows[row][column] == '1'
                }
            }

            val decoded = QrCodeDecoder.decode(modules)

            assertEquals(fixture.version, decoded.version, fixture.id)
            assertEquals(
                fixture.errorCorrection,
                decoded.errorCorrection.name,
                fixture.id,
            )
            assertEquals(fixture.mask, decoded.mask, fixture.id)
            assertEquals(0, decoded.correctedErrorCount, fixture.id)
            assertEquals(
                fixture.eciAssignment,
                decoded.eciAssignment,
                fixture.id,
            )
            assertEquals(
                fixture.utf8Text,
                decoded.utf8TextOrNull(),
                fixture.id,
            )
            assertContentEquals(
                fixture.bytes.map { it.toByte() }.toByteArray(),
                decoded.copyBytes(),
                fixture.id,
            )
        }
    }

    private fun rows(
        size: Int,
        read: (Int, Int) -> Boolean,
    ): Array<String> = Array(size) { row ->
        buildString(size) {
            for (column in 0 until size) {
                append(if (read(row, column)) '1' else '0')
            }
        }
    }
}
