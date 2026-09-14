package io.github.eugeneponomarev.styledqr

import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.fixtures.GoldenQrFixture
import io.github.eugeneponomarev.styledqr.fixtures.QrDecoderGoldenFixtures
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QrCodeDecoderGoldenFixturesTest {

    @Test
    fun decodesAllExternalGoldenFixtures() {
        val fixtures = QrDecoderGoldenFixtures.all

        assertTrue(fixtures.isNotEmpty(), "Add at least one external QR fixture")

        fixtures.forEach { fixture ->
            val caseName = "${fixture.id}: ${fixture.provenance}"

            val decoded = decodeFixture(fixture)

            assertContentEquals(fixture.expectedBytes, decoded.copyBytes(), caseName)
            assertEquals(fixture.expectedVersion, decoded.version, caseName)
            assertEquals(
                fixture.expectedErrorCorrection,
                decoded.errorCorrection,
                caseName,
            )
            assertEquals(fixture.expectedMask, decoded.mask, caseName)
            assertEquals(fixture.expectedEciAssignment, decoded.eciAssignment, caseName)
            assertEquals(fixture.expectedUtf8Text, decoded.utf8TextOrNull(), caseName)
            assertEquals(0, decoded.correctedErrorCount, caseName)
        }
    }

    private fun decodeFixture(fixture: GoldenQrFixture) =
        try {
            QrCodeDecoder.decode(fixture.copyModules())
        } catch (throwable: Throwable) {
            throw AssertionError(
                "Fixture '${fixture.id}' failed to decode. " +
                    "${fixture.provenance}. Cause: ${throwable.message}",
            )
        }
}