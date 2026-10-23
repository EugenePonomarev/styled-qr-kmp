package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyledQrCoreTest {
    @Test
    fun textMatricesAndPublicMetadataMatchCore() {
        val cases = listOf(
            "x" to 1,
            "Привет, QR 👋" to 4,
            "" to 7,
        )

        for ((text, version) in cases) {
            for (level in QrErrorCorrectionLevel.entries) {
                val options = encodingOptions(version, level.name)
                val actual = encodeStyledQrText(text, options)
                val expected = QrCodeGenerator.encodeText(
                    text = text,
                    errorCorrection = level,
                    minVersion = version,
                    maxVersion = version,
                )

                assertSameMatrix(expected, actual)

                val decoded = decodeStyledQrMatrix(actual.copyModules())
                val expectedDecoded = QrCodeDecoder.decode(expected.copyModules())

                assertEquals(expectedDecoded.version, decoded.version)
                assertEquals(expectedDecoded.errorCorrection.name, decoded.errorCorrection)
                assertEquals(expectedDecoded.mask, decoded.mask)
                assertEquals(0, decoded.correctedErrorCount)
                assertEquals(26, decoded.eciAssignment)
                assertEquals(text, decoded.utf8TextOrNull())

                assertContentEquals(
                    unsignedBytes(expectedDecoded.copyBytes()),
                    decoded.copyBytes(),
                )
            }
        }
    }

    @Test
    fun versionFortyMatrixMatchesCore() {
        val options = encodingOptions(40)
        val actual = encodeStyledQrBytes(arrayOf(0, 127, 128, 255), options)
        val expected = QrCodeGenerator.encodeBytes(
            bytes = byteArrayOf(0, 127, 128.toByte(), 255.toByte()),
            minVersion = 40,
            maxVersion = 40,
        )

        assertSameMatrix(expected, actual)

        val decoded = decodeStyledQrMatrix(actual.copyModules())
        assertEquals(40, decoded.version)
        assertContentEquals(arrayOf(0, 127, 128, 255), decoded.copyBytes())
    }

    @Test
    fun binaryBytesAreUnsignedAndHaveNoImplicitEci() {
        val source = arrayOf(0, 1, 127, 128, 254, 255)
        val matrix = encodeStyledQrBytes(source)
        val decoded = decodeStyledQrMatrix(matrix.copyModules())

        assertContentEquals(source, decoded.copyBytes())
        assertNull(decoded.eciAssignment)
        assertNull(decoded.utf8TextOrNull())

        val copy = decoded.copyBytes()
        copy[0] = 99

        assertEquals(0, decoded.copyBytes()[0])
    }

    @Test
    fun textAndRawUtf8BytesKeepDifferentEciSemantics() {
        val text = "Привет"
        val bytes = unsignedBytes(text.encodeToByteArray())

        val textDecoded = decodeStyledQrMatrix(
            encodeStyledQrText(text).copyModules(),
        )
        val bytesDecoded = decodeStyledQrMatrix(
            encodeStyledQrBytes(bytes).copyModules(),
        )

        assertContentEquals(bytes, textDecoded.copyBytes())
        assertContentEquals(bytes, bytesDecoded.copyBytes())

        assertEquals(26, textDecoded.eciAssignment)
        assertEquals(text, textDecoded.utf8TextOrNull())

        assertNull(bytesDecoded.eciAssignment)
        assertNull(bytesDecoded.utf8TextOrNull())
    }

    @Test
    fun emptyPayloadsRemainEncodable() {
        val text = decodeStyledQrMatrix(
            encodeStyledQrText("").copyModules(),
        )
        val bytes = decodeStyledQrMatrix(
            encodeStyledQrBytes(emptyArray<Int>()).copyModules(),
        )

        assertEquals("", text.utf8TextOrNull())
        assertEquals(26, text.eciAssignment)
        assertTrue(text.copyBytes().isEmpty())

        assertNull(bytes.utf8TextOrNull())
        assertNull(bytes.eciAssignment)
        assertTrue(bytes.copyBytes().isEmpty())
    }

    @Test
    fun reportsActualUpgradedCorrectionLevel() {
        val matrix = encodeStyledQrBytes(
            arrayOf(0x42),
            encodingOptions(1, "L"),
        )

        assertEquals("H", matrix.errorCorrection)
        assertEquals(
            matrix.errorCorrection,
            decodeStyledQrMatrix(matrix.copyModules()).errorCorrection,
        )
    }

    @Test
    fun matrixAndSourceCopiesDoNotExposeInternalState() {
        val source = arrayOf(0x42)
        val matrix = encodeStyledQrBytes(source)
        source[0] = 0

        val modules = matrix.copyModules()
        val original = matrix.get(0, 0)

        modules[0][0] = !original

        assertEquals(original, matrix.get(0, 0))
        assertEquals(original, matrix.copyModules()[0][0])

        assertContentEquals(
            arrayOf(0x42),
            decodeStyledQrMatrix(matrix.copyModules()).copyBytes(),
        )
    }

    @Test
    fun decoderCorrectsOneDamagedDataCodeword() {
        val matrix = encodeStyledQrBytes(
            arrayOf(0x42),
            encodingOptions(1),
        )
        val modules = matrix.copyModules()

        val cell = (0 until matrix.size)
            .asSequence()
            .flatMap { row ->
                (0 until matrix.size).asSequence().map { column ->
                    row to column
                }
            }
            .first { (row, column) ->
                !matrix.isFunctionModule(row, column)
            }

        modules[cell.first][cell.second] =
            !modules[cell.first][cell.second]

        val decoded = decodeStyledQrMatrix(modules)

        assertContentEquals(arrayOf(0x42), decoded.copyBytes())
        assertEquals(1, decoded.correctedErrorCount)
    }

    @Test
    fun binarySvgUsesTheSharedStyleAndGeometry() {
        val bytes = arrayOf(0, 127, 128, 255)
        val matrix = encodeStyledQrBytes(bytes, encodingOptions(4))

        val styleOptions = StyledQrOptions().apply {
            moduleShape = "rounded-square"
            moduleScale = 0.84
            quietZoneModules = 6
            foregroundGradient = StyledQrGradientOptions(
                "#075985",
                "#6D28D9",
            )
            finderOuterShape = "circle"
            finderCenterShape = "diamond"
            finderCenterColorHex = "#0F4C5C"
        }

        val expected = SvgQrRenderer.renderWithRasterOptions(
            qrCode = QrCodeGenerator.encodeBytes(
                bytes = byteArrayOf(0, 127, 128.toByte(), 255.toByte()),
                minVersion = 4,
                maxVersion = 4,
            ),
            options = QrRasterRenderOptions(450),
            style = resolveStyledQrStyle(styleOptions),
        )

        assertEquals(
            expected,
            matrix.toSvg(StyledQrExportOptions(450), styleOptions),
        )
    }

    @Test
    fun rejectsInvalidByteValuesWithoutCoercion() {
        val invalidValues: List<Any?> = listOf(
            -1,
            256,
            1.5,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            "1",
            true,
            null,
        )

        for (value in invalidValues) {
            val input = arrayOf(value).unsafeCast<Array<Int>>()

            assertFailsWith<IllegalArgumentException> {
                encodeStyledQrBytes(input)
            }
        }

        val notAnArray = "bytes".unsafeCast<Array<Int>>()

        assertFailsWith<IllegalArgumentException> {
            encodeStyledQrBytes(notAnArray)
        }
    }

    @Test
    fun rejectsInvalidEncodingParameters() {
        val invalidOptions = listOf(
            StyledQrEncodingOptions().apply {
                minVersion = 0
            },
            StyledQrEncodingOptions().apply {
                maxVersion = 41
            },
            StyledQrEncodingOptions().apply {
                minVersion = 4
                maxVersion = 3
            },
            StyledQrEncodingOptions().apply {
                errorCorrection = "unknown"
            },
            StyledQrEncodingOptions().apply {
                asDynamic().minVersion = 1.5
            },
        )

        for (options in invalidOptions) {
            assertFailsWith<IllegalArgumentException> {
                encodeStyledQrText("x", options)
            }
            assertFailsWith<IllegalArgumentException> {
                encodeStyledQrBytes(arrayOf(0x42), options)
            }
        }

        assertFailsWith<IllegalArgumentException> {
            encodeStyledQrText(42.unsafeCast<String>())
        }

        assertFailsWith<IllegalArgumentException> {
            encodeStyledQrText(
                "x".repeat(100),
                encodingOptions(1),
            )
        }
    }

    @Test
    fun rejectsInvalidMatrixShapesAndCellTypes() {
        assertFailsWith<IllegalArgumentException> {
            decodeStyledQrMatrix(emptyArray<Array<Boolean>>())
        }

        assertFailsWith<IllegalArgumentException> {
            decodeStyledQrMatrix(
                Array(22) { Array(22) { false } },
            )
        }

        val ragged = encodeStyledQrBytes(arrayOf(0x42)).copyModules()
        ragged[0] = arrayOf(true)

        assertFailsWith<IllegalArgumentException> {
            decodeStyledQrMatrix(ragged)
        }

        val numericCell = encodeStyledQrBytes(arrayOf(0x42)).copyModules()
        numericCell[0].asDynamic()[0] = 1

        assertFailsWith<IllegalArgumentException> {
            decodeStyledQrMatrix(numericCell)
        }

        val nonArrayRow = encodeStyledQrBytes(arrayOf(0x42)).copyModules()
        nonArrayRow.asDynamic()[0] = "not a row"

        assertFailsWith<IllegalArgumentException> {
            decodeStyledQrMatrix(nonArrayRow)
        }
    }

    @Test
    fun rejectsInvalidMatrixCoordinates() {
        val matrix = encodeStyledQrText("x")

        assertFailsWith<IllegalArgumentException> {
            matrix.get(-1, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            matrix.get(matrix.size, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            matrix.get(1.5.unsafeCast<Int>(), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            matrix.isFunctionModule(0, Double.NaN.unsafeCast<Int>())
        }
        assertFailsWith<IllegalArgumentException> {
            matrix.isLocatorModule(0, matrix.size)
        }
    }

    private fun encodingOptions(
        version: Int,
        level: String = "H",
    ): StyledQrEncodingOptions = StyledQrEncodingOptions().apply {
        errorCorrection = level
        minVersion = version
        maxVersion = version
    }

    private fun unsignedBytes(bytes: ByteArray): Array<Int> =
        Array(bytes.size) { index ->
            bytes[index].toInt() and 0xFF
        }

    private fun assertSameMatrix(
        expected: QrCode,
        actual: StyledQrMatrix,
    ) {
        assertEquals(expected.version, actual.version)
        assertEquals(expected.errorCorrection.name, actual.errorCorrection)
        assertEquals(expected.size, actual.size)

        val copied = actual.copyModules()
        assertEquals(expected.size, copied.size)

        for (row in 0 until expected.size) {
            assertEquals(expected.size, copied[row].size)

            for (column in 0 until expected.size) {
                val position = "row=$row, column=$column"

                assertEquals(
                    expected[row, column],
                    actual.get(row, column),
                    position,
                )
                assertEquals(
                    expected[row, column],
                    copied[row][column],
                    position,
                )
                assertEquals(
                    expected.isFunctionModule(row, column),
                    actual.isFunctionModule(row, column),
                    position,
                )
                assertEquals(
                    expected.isLocatorModule(row, column),
                    actual.isLocatorModule(row, column),
                    position,
                )
            }
        }
    }
}
