package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyledQrSafeSvgTest {
    private val logoUri =
        "data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAKUlEQVR4nGP4z8CAB+GTI4QoMnnU4lGLRy0etXjU4lGLRy0etXg4WQwA72wdDlLL0dYAAAAASUVORK5CYII="

    @Test
    fun successfulResultMatchesStrictGeneration() {
        val options = StyledQrOptions().apply {
            themeId = "fintech"
            moduleShape = "circle"
            moduleScale = 0.84
            minVersion = 4
            maxVersion = 4
        }

        val result = renderStyledQrSvg("x", options)

        assertTrue(result.isSuccess, result.error?.message)
        assertFalse(result.isEmpty)
        assertNull(result.error)
        assertEquals(
            generateStyledQrSvgWithOptions("x", options),
            result.svg,
        )

        assertEquals("fintech", options.themeId)
        assertEquals(4, options.minVersion)
        assertEquals(4, options.maxVersion)
    }

    @Test
    fun validBlankInputReturnsAnEmptyResult() {
        for (content in listOf("", " \n\t")) {
            val result = renderStyledQrSvg(content)

            assertTrue(result.isEmpty)
            assertFalse(result.isSuccess)
            assertNull(result.svg)
            assertNull(result.error)
        }
    }

    @Test
    fun styleAndEncodingParametersAreValidatedBeforeBlankContent() {
        val invalidStyle = renderStyledQrSvg(
            "",
            StyledQrOptions().apply {
                moduleScale = 0.1
            },
        )
        assertFailure(invalidStyle, QrRenderErrorCode.InvalidStyle)

        val invalidParameters = renderStyledQrSvg(
            "",
            StyledQrOptions().apply {
                minVersion = 0
            },
        )
        assertFailure(
            invalidParameters,
            QrRenderErrorCode.InvalidRenderParameters,
        )
    }

    @Test
    fun rejectsInvalidGenerationParametersIncludingJsFractionalNumbers() {
        val invalidOptions = listOf(
            StyledQrOptions().apply { errorCorrection = "unsupported" },
            StyledQrOptions().apply { minVersion = 0 },
            StyledQrOptions().apply { maxVersion = 41 },
            StyledQrOptions().apply {
                minVersion = 2
                maxVersion = 1
            },
            StyledQrOptions().apply {
                asDynamic().minVersion = 1.5
            },
        )

        for (options in invalidOptions) {
            assertFailure(
                renderStyledQrSvg("x", options),
                QrRenderErrorCode.InvalidRenderParameters,
            )
        }
    }

    @Test
    fun payloadOverflowHasItsOwnCategory() {
        assertFailure(
            renderStyledQrSvg("a".repeat(5000)),
            QrRenderErrorCode.PayloadTooLong,
        )
        assertFailure(
            renderStyledQrSvg(
                "a".repeat(100),
                StyledQrOptions().apply { maxVersion = 1 },
            ),
            QrRenderErrorCode.PayloadTooLong,
        )
    }

    @Test
    fun malformedAndUnsafeLogosReturnLogoErrors() {
        assertFailure(
            renderStyledQrSvg(
                "https://example.com",
                StyledQrOptions().apply {
                    logoDataUri = "https://example.com/logo.png"
                },
            ),
            QrRenderErrorCode.LogoValidationFailed,
        )

        assertFailure(
            renderStyledQrSvg(
                "hi",
                StyledQrOptions().apply {
                    logoDataUri = logoUri
                },
            ),
            QrRenderErrorCode.LogoValidationFailed,
        )
    }

    @Test
    fun overflowingSvgSideReturnsParameterError() {
        assertFailure(
            renderStyledQrSvg(
                "x",
                StyledQrOptions().apply {
                    quietZoneModules = Int.MAX_VALUE
                },
            ),
            QrRenderErrorCode.InvalidRenderParameters,
        )
    }

    @Test
    fun failedCallDoesNotPreventRecovery() {
        val options = StyledQrOptions().apply {
            foregroundGradient = StyledQrGradientOptions(
                "#075985",
                "#6D28D9",
            ).apply {
                startX = Double.NaN
            }
        }

        assertFailure(
            renderStyledQrSvg("x", options),
            QrRenderErrorCode.InvalidStyle,
        )

        options.foregroundGradient = null

        val empty = renderStyledQrSvg("", options)
        assertTrue(empty.isEmpty)
        assertNull(empty.error)

        val recovered = renderStyledQrSvg("x", options)
        assertTrue(recovered.isSuccess)
        assertNotNull(recovered.svg)
        assertNull(recovered.error)
    }

    private fun assertFailure(
        result: StyledQrSvgResult,
        expectedCode: QrRenderErrorCode,
    ) {
        assertFalse(result.isSuccess)
        assertFalse(result.isEmpty)
        assertNull(result.svg)

        val error = assertNotNull(result.error)
        assertEquals(expectedCode.name, error.code)
        assertTrue(error.message.isNotBlank())
    }
}
