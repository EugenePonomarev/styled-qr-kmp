package io.github.eugeneponomarev.styledqr.fixtures

import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel

internal class GoldenQrFixture(
    val id: String,
    val provenance: String,
    val expectedBytes: ByteArray,
    val expectedVersion: Int,
    val expectedErrorCorrection: QrErrorCorrectionLevel,
    val expectedMask: Int,
    val expectedEciAssignment: Int?,
    val expectedUtf8Text: String?,
    private val matrix: String,
) {
    fun copyModules(): Array<BooleanArray> {
        val rows = matrix
            .trimIndent()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
        val expectedSize = expectedVersion * MODULES_PER_VERSION + INITIAL_QR_SIZE

        check(rows.size == expectedSize) {
            "$id: expected $expectedSize matrix rows, but got ${rows.size}"
        }

        return Array(expectedSize) { rowIndex ->
            val row = rows[rowIndex]

            check(row.length == expectedSize) {
                "$id: row $rowIndex must contain $expectedSize modules, but has ${row.length}"
            }

            BooleanArray(expectedSize) { columnIndex ->
                when (row[columnIndex]) {
                    '#' -> true
                    '.' -> false
                    else -> error("$id: unexpected matrix symbol '${row[columnIndex]}'")
                }
            }
        }
    }

    private companion object {
        const val MODULES_PER_VERSION = 4
        const val INITIAL_QR_SIZE = 17
    }
}