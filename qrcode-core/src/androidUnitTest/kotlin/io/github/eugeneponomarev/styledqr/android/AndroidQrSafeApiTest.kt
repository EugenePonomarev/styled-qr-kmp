package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Color
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoConfiguration
import io.github.eugeneponomarev.styledqr.render.QrModuleShape
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidQrSafeApiTest {
    private val renderer = StyledQrBitmapRenderer()

    @Test
    fun validBlankInputReturnsAnEmptyResult() {
        for (content in listOf("", " \n\t")) {
            val result = renderer.renderWithOptions(
                content = content,
                options = QrStyleConfiguration(),
                output = QrRasterRenderOptions(300),
            )

            assertTrue(result.isEmpty)
            assertFalse(result.isSuccess)
            assertNull(result.bitmap)
            assertNull(result.error)
        }
    }

    @Test
    fun styleAndOutputAreValidatedEvenForBlankInput() {
        val invalidStyle = renderer.renderWithOptions(
            content = "",
            options = QrStyleConfiguration().apply {
                foregroundHex = "invalid"
            },
            output = QrRasterRenderOptions(300),
        )
        assertFailure(invalidStyle, QrRenderErrorCode.InvalidStyle)

        val invalidSize = renderer.renderWithOptions(
            content = "",
            options = QrStyleConfiguration(),
            output = QrRasterRenderOptions(0),
        )
        assertFailure(
            invalidSize,
            QrRenderErrorCode.InvalidRenderParameters,
        )
    }

    @Test
    fun invalidAndInsufficientSizesReturnParameterErrors() {
        for (mode in QrRasterSizeMode.entries) {
            for (sizePx in listOf(-1, 0, 28)) {
                val result = renderer.renderWithOptions(
                    content = "x",
                    options = QrStyleConfiguration(),
                    output = QrRasterRenderOptions(sizePx, mode),
                )

                assertFailure(
                    result,
                    QrRenderErrorCode.InvalidRenderParameters,
                )
            }
        }
    }

    @Test
    fun invalidVersionBoundsAreDifferentFromPayloadOverflow() {
        for ((minimum, maximum) in listOf(
            0 to 40,
            1 to 41,
            5 to 4,
        )) {
            val result = renderer.renderWithOptions(
                content = "x",
                options = QrStyleConfiguration(),
                output = QrRasterRenderOptions(300),
                minVersion = minimum,
                maxVersion = maximum,
            )

            assertFailure(
                result,
                QrRenderErrorCode.InvalidRenderParameters,
            )
        }

        for ((content, maximum) in listOf(
            "a".repeat(100) to 1,
            "a".repeat(5000) to 40,
        )) {
            val result = renderer.renderWithOptions(
                content = content,
                options = QrStyleConfiguration(),
                output = QrRasterRenderOptions(300),
                maxVersion = maximum,
            )

            assertFailure(result, QrRenderErrorCode.PayloadTooLong)
        }
    }

    @Test
    fun unsafeAndRecycledLogosReturnLogoErrors() {
        val logo = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888)
        logo.eraseColor(Color.RED)

        try {
            val unsafe = renderer.renderWithOptions(
                content = "hi",
                options = QrStyleConfiguration(),
                output = QrRasterRenderOptions(300),
                logo = logo,
            )

            assertFailure(
                unsafe,
                QrRenderErrorCode.LogoValidationFailed,
            )
        } finally {
            logo.recycle()
        }

        val recycled = renderer.renderWithOptions(
            content = "https://example.com",
            options = QrStyleConfiguration(),
            output = QrRasterRenderOptions(300),
            logo = logo,
        )

        assertFailure(
            recycled,
            QrRenderErrorCode.LogoValidationFailed,
        )
    }

    @Test
    fun insufficientSizeTakesPriorityOverUnsafeLogoPlacement() {
        val logo = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888)
        logo.eraseColor(Color.RED)

        try {
            for (mode in QrRasterSizeMode.entries) {
                for ((sizePx, expectedCode) in listOf(
                    28 to QrRenderErrorCode.InvalidRenderParameters,
                    300 to QrRenderErrorCode.LogoValidationFailed,
                )) {
                    val result = renderer.renderWithOptions(
                        content = "hi",
                        options = QrStyleConfiguration().apply {
                            quietZoneModules = 4
                        },
                        output = QrRasterRenderOptions(sizePx, mode),
                        errorCorrection = QrErrorCorrectionLevel.H,
                        logo = logo,
                        minVersion = 1,
                        maxVersion = 1,
                    )

                    assertFailure(result, expectedCode)
                }
            }
        } finally {
            logo.recycle()
        }
    }

    @Test
    fun safeRenderingPreservesStyleLogoVersionBoundsAndSizeMode() {
        val options = QrStyleConfiguration().apply {
            backgroundHex = "#FFF0CC"
            foregroundHex = "#143A5A"
            quietZoneModules = 2
            moduleShape = QrModuleShape.RoundedSquare
            moduleScale = 0.84
            logo = QrLogoConfiguration().apply {
                sizeFraction = 0.16
                paddingFraction = 0.04
                backgroundHex = "#DDEEFF"
                backgroundShape = QrLogoBackgroundShape.Circle
            }
        }
        val style = assertNotNull(options.build().style)
        val code = QrCodeGenerator.encodeText(
            text = "x",
            minVersion = 4,
            maxVersion = 4,
        )
        val logo = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        logo.eraseColor(Color.RED)

        try {
            for (mode in QrRasterSizeMode.entries) {
                val output = QrRasterRenderOptions(300, mode)
                val result = renderer.renderWithOptions(
                    content = "x",
                    options = options,
                    output = output,
                    logo = logo,
                    minVersion = 4,
                    maxVersion = 4,
                )

                assertTrue(result.isSuccess, result.error?.message)
                assertFalse(result.isEmpty)
                assertNull(result.error)

                val actual = assertNotNull(result.bitmap)
                try {
                    val expected = code.toBitmapWithOptions(
                        options = output,
                        style = style,
                        logo = logo,
                    )
                    try {
                        assertEquals(300, actual.width)
                        assertEquals(300, actual.height)
                        assertTrue(actual.sameAs(expected), "$mode")
                    } finally {
                        expected.recycle()
                    }
                } finally {
                    actual.recycle()
                }
            }

            assertFalse(logo.isRecycled)
            assertEquals(2, options.quietZoneModules)
        } finally {
            logo.recycle()
        }
    }

    private fun assertFailure(
        result: QrBitmapRenderResult,
        expectedCode: QrRenderErrorCode,
    ) {
        assertFalse(result.isSuccess)
        assertFalse(result.isEmpty)
        assertNull(result.bitmap)

        val error = assertNotNull(result.error)
        assertEquals(expectedCode, error.code)
        assertTrue(error.message.isNotBlank())
    }
}
