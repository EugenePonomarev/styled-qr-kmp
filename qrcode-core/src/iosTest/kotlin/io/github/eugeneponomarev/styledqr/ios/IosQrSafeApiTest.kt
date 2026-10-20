@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.*
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*
import kotlin.test.*

class IosQrSafeApiTest {
    @Test
    fun invalidParametersReturnTypedErrorsAndTheRendererRecovers() {
        val renderer = StyledQrImageRenderer().apply {
            content = "hi"
        }

        for (points in listOf(
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.MAX_VALUE,
        )) {
            assertNull(renderer.render(points, 1.0))
            assertEquals(
                QrRenderErrorCode.InvalidRenderParameters,
                renderer.lastRenderErrorCode,
            )
        }

        for (scale in listOf(
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.MAX_VALUE,
            0.00001,
        )) {
            assertNull(renderer.render(240.0, scale))
            assertEquals(
                QrRenderErrorCode.InvalidRenderParameters,
                renderer.lastRenderErrorCode,
            )
        }

        assertNotNull(renderer.render(240.0, 0.0))
        assertNull(renderer.lastRenderError)
        assertNull(renderer.lastRenderErrorCode)
    }

    @Test
    fun directImageChecksArgumentsAndPreservesTinyFacadeBehaviour() {
        val code = QrCodeGenerator.encodeText("hi")

        for (points in listOf(0.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                code.toUIImage(points, scale = 1.0)
            }
        }

        assertFailsWith<IllegalArgumentException> {
            code.toUIImage(240.0, scale = -1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            code.toUIImage(
                240.0,
                QrStyle(quietZoneModules = Int.MAX_VALUE),
                1.0,
            )
        }

        val renderer = StyledQrImageRenderer().apply {
            content = "hi"
        }
        val image = assertNotNull(renderer.render(1.0, 1.0))

        assertEquals(
            (code.size + 8).toDouble(),
            image.size.useContents { width },
        )
        assertNull(renderer.lastRenderErrorCode)
    }

    @Test
    fun payloadAndLogoFailuresHaveDistinctCodes() {
        val renderer = StyledQrImageRenderer().apply {
            content = "x".repeat(5000)
        }

        assertNull(renderer.render(240.0, 1.0))
        assertEquals(
            QrRenderErrorCode.PayloadTooLong,
            renderer.lastRenderErrorCode,
        )

        renderer.content = "hi"
        renderer.logo = logo()
        assertNull(renderer.render(240.0, 1.0))
        assertEquals(
            QrRenderErrorCode.LogoValidationFailed,
            renderer.lastRenderErrorCode,
        )

        renderer.logo = null
        assertNotNull(renderer.render(240.0, 1.0))
        assertNull(renderer.lastRenderErrorCode)

        renderer.content = " \n\t"
        assertNull(renderer.render(240.0, 1.0))
        assertNull(renderer.lastRenderErrorCode)
    }

    @Test
    fun safeConfigurationFailuresStayInsideKotlin() {
        val renderer = StyledQrImageRenderer()
        val config = QrStyleConfiguration().apply {
            foregroundGradient = QrGradientConfiguration().apply {
                startX = Double.NaN
            }
        }

        val result = renderer.renderWithOptions(
            "hi",
            config,
            240.0,
            scale = 1.0,
        )

        assertFalse(result.isSuccess)
        assertFalse(result.isEmpty)
        assertNull(result.image)
        assertEquals(QrRenderErrorCode.InvalidStyle, result.error!!.code)
        assertEquals(
            QrRenderErrorCode.InvalidStyle,
            renderer.lastRenderErrorCode,
        )

        config.foregroundGradient = null
        assertTrue(
            renderer.renderWithOptions(
                "hi",
                config,
                240.0,
                scale = 1.0,
            ).isSuccess,
        )
        assertNull(renderer.lastRenderErrorCode)
    }

    @Test
    fun safeCallsPreserveImageGeometryAndExistingStoredInputs() {
        val renderer = StyledQrImageRenderer().apply {
            content = "original"
        }
        val originalStyle = renderer.qrStyle

        val config = QrStyleConfiguration().apply {
            moduleShape = QrModuleShape.RoundedSquare
            foregroundGradient = QrGradientConfiguration().apply {
                startColorHex = "#075985"
                endColorHex = "#6D28D9"
            }
        }

        val safe = renderer.renderWithOptions(
            "https://example.com",
            config,
            240.0,
            scale = 2.0,
        )

        assertTrue(safe.isSuccess)
        assertFalse(safe.isEmpty)
        assertNull(safe.error)

        val image = assertNotNull(safe.image)
        assertEquals(
            480,
            CGImageGetWidth(assertNotNull(image.CGImage)).toInt(),
        )
        assertEquals(240.0, image.size.useContents { width })
        assertEquals(2.0, image.scale)

        val direct = QrCodeGenerator.encodeText("https://example.com")
            .toUIImage(240.0, config.build().style!!, 2.0)

        assertTrue(
            assertNotNull(UIImagePNGRepresentation(image)).isEqualToData(
                assertNotNull(UIImagePNGRepresentation(direct)),
            ),
        )

        assertEquals("original", renderer.content)
        assertSame(originalStyle, renderer.qrStyle)
        assertNull(renderer.logo)
        assertEquals(QrErrorCorrectionLevel.H, renderer.errorCorrection)
    }

    @Test
    fun safeEmptyResultIsDifferentFromFailureAndClearsDiagnostics() {
        val renderer = StyledQrImageRenderer()
        val config = QrStyleConfiguration()

        assertNotNull(renderer.renderWithOptions("hi", config, 0.0).error)

        val empty = renderer.renderWithOptions(
            " \n",
            config,
            240.0,
            scale = 1.0,
        )

        assertTrue(empty.isEmpty)
        assertFalse(empty.isSuccess)
        assertNull(empty.error)
        assertNull(renderer.lastRenderError)
        assertNull(renderer.lastRenderErrorCode)

        config.moduleScale = 0.1
        assertEquals(
            QrRenderErrorCode.InvalidStyle,
            renderer.renderWithOptions("", config, 240.0).error!!.code,
        )
    }

    private fun logo(): UIImage {
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(16.0, 16.0),
            false,
            1.0,
        )
        return try {
            UIColor.redColor.setFill()
            UIBezierPath.bezierPathWithRect(
                CGRectMake(0.0, 0.0, 16.0, 16.0),
            ).fill()
            requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
        } finally {
            UIGraphicsEndImageContext()
        }
    }
}
