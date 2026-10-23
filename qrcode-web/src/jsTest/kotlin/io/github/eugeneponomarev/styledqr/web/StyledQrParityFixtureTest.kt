package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.fixtures.QrParitySnapshots
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class StyledQrParityFixtureTest {
    @Test
    fun encodedMatricesMatchFrozenFixtures() {
        for (fixture in QrParitySnapshots.all) {
            val options = StyledQrEncodingOptions().apply {
                errorCorrection = fixture.requestedErrorCorrection
                minVersion = fixture.minVersion
                maxVersion = fixture.maxVersion
            }
            val text = fixture.text

            val matrix = if (text != null) {
                encodeStyledQrText(text, options)
            } else {
                encodeStyledQrBytes(fixture.bytes.toTypedArray(), options)
            }

            assertEquals(fixture.version, matrix.version, fixture.id)
            assertEquals(
                fixture.errorCorrection,
                matrix.errorCorrection,
                fixture.id,
            )
            assertEquals(fixture.size, matrix.size, fixture.id)

            assertContentEquals(
                fixture.moduleRows,
                rows(matrix.size) { row, column ->
                    matrix.get(row, column)
                },
                fixture.id,
            )
            assertContentEquals(
                fixture.functionRows,
                rows(matrix.size, matrix::isFunctionModule),
                fixture.id,
            )
            assertContentEquals(
                fixture.locatorRows,
                rows(matrix.size, matrix::isLocatorModule),
                fixture.id,
            )
        }
    }

    @Test
    fun decoderReadsFrozenMatrices() {
        for (fixture in QrParitySnapshots.all) {
            val modules = Array(fixture.size) { row ->
                Array(fixture.size) { column ->
                    fixture.moduleRows[row][column] == '1'
                }
            }

            val decoded = decodeStyledQrMatrix(modules)

            assertEquals(fixture.version, decoded.version, fixture.id)
            assertEquals(
                fixture.errorCorrection,
                decoded.errorCorrection,
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
                fixture.bytes.toTypedArray(),
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
