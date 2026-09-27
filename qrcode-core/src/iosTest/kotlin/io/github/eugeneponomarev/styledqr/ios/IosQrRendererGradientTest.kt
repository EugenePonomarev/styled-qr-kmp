@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.UIKit.UIImage
import platform.posix.memcpy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IosQrRendererGradientTest {

    private val startColor =
        QrColor.fromHex("#075985")

    private val endColor =
        QrColor.fromHex("#6D28D9")

    private val backgroundColor =
        QrColor.fromHex("#F8FAFC")

    @Test
    fun rendersHorizontalGradientAcrossDarkModulesAndFinderLayers() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val gradient = QrLinearGradient(
            startColor = startColor,
            endColor = endColor,
            start = QrGradientPoint(
                x = 0.0,
                y = 0.5,
            ),
            end = QrGradientPoint(
                x = 1.0,
                y = 0.5,
            ),
        )

        val style = QrStyle(
            background = backgroundColor,
            moduleScale = 1.0,
            foregroundGradient = gradient,
        )

        val pixelsPerModule = 12

        val pixels = code.toUIImage(
            sizePoints = imageSize(
                code = code,
                style = style,
                pixelsPerModule = pixelsPerModule,
            ).toDouble(),
            style = style,
            scale = 1.0,
        ).toPixelBuffer()

        val leftPixel =
            pixels.foregroundPixelAtQrColumn(
                column = 0,
                quietZoneModules =
                    style.quietZoneModules,
                pixelsPerModule =
                    pixelsPerModule,
                background =
                    backgroundColor,
            )

        val rightPixel =
            pixels.foregroundPixelAtQrColumn(
                column = code.size - 1,
                quietZoneModules =
                    style.quietZoneModules,
                pixelsPerModule =
                    pixelsPerModule,
                background =
                    backgroundColor,
            )

        assertNotEquals(
            backgroundColor,
            leftPixel,
            "Left finder column must contain gradient foreground",
        )

        assertNotEquals(
            backgroundColor,
            rightPixel,
            "Right finder column must contain gradient foreground",
        )

        assertNotEquals(
            leftPixel,
            rightPixel,
            "A horizontal gradient must produce different colours " +
                    "at opposite QR edges",
        )

        // #075985 -> #6D28D9:
        // red increases, green decreases, blue increases.
        assertTrue(
            leftPixel.red < rightPixel.red,
            "Red channel must increase from left to right: " +
                    "left=${leftPixel.red}, " +
                    "right=${rightPixel.red}",
        )

        assertTrue(
            leftPixel.green > rightPixel.green,
            "Green channel must decrease from left to right: " +
                    "left=${leftPixel.green}, " +
                    "right=${rightPixel.green}",
        )

        assertTrue(
            leftPixel.blue < rightPixel.blue,
            "Blue channel must increase from left to right: " +
                    "left=${leftPixel.blue}, " +
                    "right=${rightPixel.blue}",
        )

        assertEquals(
            255,
            leftPixel.alpha,
        )

        assertEquals(
            255,
            rightPixel.alpha,
        )
    }

    @Test
    fun keepsQuietZoneAndFinderMiddleRingOnSolidBackground() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val style = QrStyle(
            background = backgroundColor,
            foregroundGradient =
                QrLinearGradient(
                    startColor = startColor,
                    endColor = endColor,
                ),
        )

        val pixelsPerModule = 10

        val pixels = code.toUIImage(
            sizePoints = imageSize(
                code = code,
                style = style,
                pixelsPerModule =
                    pixelsPerModule,
            ).toDouble(),
            style = style,
            scale = 1.0,
        ).toPixelBuffer()

        assertEquals(
            backgroundColor,
            pixels.pixelAt(
                x = pixelsPerModule / 2,
                y = pixelsPerModule / 2,
            ),
        )

        assertEquals(
            backgroundColor,
            pixels.pixelAtQrModule(
                row = 1,
                column = 3,
                quietZoneModules =
                    style.quietZoneModules,
                pixelsPerModule =
                    pixelsPerModule,
            ),
        )
    }

    @Test
    fun legacyStyleRemainsSolidBlack() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )

        val style = QrStyle()
        val pixelsPerModule = 10

        val pixels = code.toUIImage(
            sizePoints = imageSize(
                code = code,
                style = style,
                pixelsPerModule =
                    pixelsPerModule,
            ).toDouble(),
            style = style,
            scale = 1.0,
        ).toPixelBuffer()

        assertEquals(
            QrColor.Black,
            pixels.pixelAtQrModule(
                row = 3,
                column = 3,
                quietZoneModules =
                    style.quietZoneModules,
                pixelsPerModule =
                    pixelsPerModule,
            ),
        )
    }

    private fun imageSize(
        code: QrCode,
        style: QrStyle,
        pixelsPerModule: Int,
    ): Int = (code.size + style.quietZoneModules * 2) * pixelsPerModule
}

private data class PixelBuffer(
    val width: Int,
    val height: Int,
    val bytesPerRow: Int,
    val bytes: ByteArray,
) {

    fun pixelAt(
        x: Int,
        y: Int,
    ): QrColor {
        require(x in 0 until width)
        require(y in 0 until height)

        val offset =
            y * bytesPerRow +
                    x * BYTES_PER_PIXEL

        // Premultiplied-first + 32-bit little endian
        // is stored as BGRA bytes in memory.
        return QrColor(
            red = bytes[offset + 2]
                .toInt() and 0xFF,
            green = bytes[offset + 1]
                .toInt() and 0xFF,
            blue = bytes[offset]
                .toInt() and 0xFF,
            alpha = bytes[offset + 3]
                .toInt() and 0xFF,
        )
    }

    fun pixelAtQrModule(
        row: Int,
        column: Int,
        quietZoneModules: Int,
        pixelsPerModule: Int,
    ): QrColor {
        val x =
            (column + quietZoneModules) *
                    pixelsPerModule +
                    pixelsPerModule / 2

        val y =
            (row + quietZoneModules) *
                    pixelsPerModule +
                    pixelsPerModule / 2

        return pixelAt(x, y)
    }

    fun foregroundPixelAtQrColumn(
        column: Int,
        quietZoneModules: Int,
        pixelsPerModule: Int,
        background: QrColor,
    ): QrColor {
        val x =
            (column + quietZoneModules) *
                    pixelsPerModule +
                    pixelsPerModule / 2

        return (0 until height)
            .asSequence()
            .map { y ->
                pixelAt(
                    x = x,
                    y = y,
                )
            }
            .firstOrNull { pixel ->
                pixel != background
            }
            ?: error(
                "No foreground pixel found " +
                        "at QR column $column",
            )
    }

    private companion object {
        const val BYTES_PER_PIXEL: Int = 4
    }
}

private fun UIImage.toPixelBuffer(): PixelBuffer {
    val image = requireNotNull(CGImage) {
        "UIImage has no CGImage backing"
    }

    val width =
        CGImageGetWidth(image).toInt()

    val height =
        CGImageGetHeight(image).toInt()

    val bytesPerRow = width * 4
    val byteCount =
        bytesPerRow * height

    val colorSpace = requireNotNull(
        CGColorSpaceCreateWithName(
            kCGColorSpaceSRGB,
        ),
    ) {
        "Unable to create sRGB color space"
    }

    try {
        val context = requireNotNull(
            CGBitmapContextCreate(
                data = null,
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow =
                    bytesPerRow.toULong(),
                space = colorSpace,
                bitmapInfo =
                    CGImageAlphaInfo
                        .kCGImageAlphaPremultipliedFirst
                        .value or
                            kCGBitmapByteOrder32Little,
            ),
        ) {
            "Unable to create bitmap context"
        }

        try {
            // CGContext and UIKit use opposite Y directions.
            CGContextTranslateCTM(
                context,
                0.0,
                height.toDouble(),
            )

            CGContextScaleCTM(
                context,
                1.0,
                -1.0,
            )

            CGContextDrawImage(
                context,
                CGRectMake(
                    0.0,
                    0.0,
                    width.toDouble(),
                    height.toDouble(),
                ),
                image,
            )

            val bytes =
                ByteArray(byteCount)

            bytes.usePinned { pinned ->
                val source =
                    requireNotNull(
                        CGBitmapContextGetData(
                            context,
                        ),
                    )

                memcpy(
                    pinned.addressOf(0),
                    source,
                    byteCount.toULong(),
                )
            }

            return PixelBuffer(
                width = width,
                height = height,
                bytesPerRow = bytesPerRow,
                bytes = bytes,
            )
        } finally {
            CGContextRelease(context)
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
}