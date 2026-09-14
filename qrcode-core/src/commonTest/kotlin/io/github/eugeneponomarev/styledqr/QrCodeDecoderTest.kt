package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrDecodeException
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrCodeDecoderTest {

    @Test
    fun decodesUtf8TextAndReadsMatrixMetadata() {
        val source = "https://example.com/Привет"
        val code = QrCodeGenerator.encodeText(
            text = source,
            errorCorrection = QrErrorCorrectionLevel.M,
        )

        val decoded = QrCodeDecoder.decode(code.copyModules())

        assertEquals(source, decoded.utf8TextOrNull())
        assertContentEquals(source.encodeToByteArray(), decoded.copyBytes())
        assertEquals(code.version, decoded.version)
        assertEquals(code.errorCorrection, decoded.errorCorrection)
        assertTrue(decoded.mask in 0..7)
        assertEquals(0, decoded.correctedErrorCount)
        assertEquals(26, decoded.eciAssignment)
    }

    @Test
    fun decodesRawBytePayloadWithoutAssumingTextEncoding() {
        val source = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())
        val code = QrCodeGenerator.encodeBytes(
            bytes = source,
            errorCorrection = QrErrorCorrectionLevel.H,
        )

        val decoded = QrCodeDecoder.decode(code.copyModules())

        assertContentEquals(source, decoded.copyBytes())
        assertNull(decoded.utf8TextOrNull())
        assertNull(decoded.eciAssignment)
    }

    @Test
    fun correctsOneChangedDataModule() {
        val source = "https://eugeneponomarev.com"
        val code = QrCodeGenerator.encodeText(
            text = source,
            errorCorrection = QrErrorCorrectionLevel.H,
        )
        val modules = code.copyModules()

        flipFirstDataModule(code, modules)

        val decoded = QrCodeDecoder.decode(modules)

        assertEquals(source, decoded.utf8TextOrNull())
        assertEquals(1, decoded.correctedErrorCount)
    }

    @Test
    fun validatesVersionInformationForVersionSevenAndAbove() {
        val source = "Version information"
        val code = QrCodeGenerator.encodeText(
            text = source,
            errorCorrection = QrErrorCorrectionLevel.H,
            minVersion = 7,
            maxVersion = 7,
        )

        val decoded = QrCodeDecoder.decode(code.copyModules())

        assertEquals(7, decoded.version)
        assertEquals(source, decoded.utf8TextOrNull())
    }

    @Test
    fun rejectsInvalidMatrixSize() {
        assertFailsWith<QrDecodeException> {
            QrCodeDecoder.decode(Array(20) { BooleanArray(20) })
        }
    }

    private fun flipFirstDataModule(
        code: QrCode,
        modules: Array<BooleanArray>,
    ) {
        for (row in 0 until code.size) {
            for (column in 0 until code.size) {
                if (!code.isFunctionModule(row, column)) {
                    modules[row][column] = !modules[row][column]
                    return
                }
            }
        }

        error("QR matrix does not contain data modules")
    }
}
