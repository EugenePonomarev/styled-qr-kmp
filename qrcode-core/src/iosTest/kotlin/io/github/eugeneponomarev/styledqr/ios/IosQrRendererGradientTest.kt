@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternAppearance
import io.github.eugeneponomarev.styledqr.render.QrFinderPatternLayerShape
import io.github.eugeneponomarev.styledqr.render.QrGradientPoint
import io.github.eugeneponomarev.styledqr.render.QrLinearGradient
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.calculateLogoLayout
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
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

    @Test
    fun pixelBufferPreservesTopLeftOrigin() {
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(8.0, 12.0),
            false,
            1.0,
        )
        try {
            UIColor(
                red = 0.0,
                green = 1.0,
                blue = 0.0,
                alpha = 1.0,
            ).setFill()
            UIBezierPath.bezierPathWithRect(
                CGRectMake(0.0, 0.0, 8.0, 12.0),
            ).fill()

            UIColor(
                red = 1.0,
                green = 0.0,
                blue = 0.0,
                alpha = 1.0,
            ).setFill()
            UIBezierPath.bezierPathWithRect(
                CGRectMake(0.0, 0.0, 8.0, 4.0),
            ).fill()

            val pixels = requireNotNull(
                UIGraphicsGetImageFromCurrentImageContext(),
            ).toPixelBuffer()

            assertEquals(
                QrColor(255, 0, 0),
                pixels.pixelAt(2, 1),
                "Top rows must remain red",
            )
            assertEquals(
                QrColor(0, 255, 0),
                pixels.pixelAt(2, 10),
                "Bottom rows must remain green",
            )
        } finally {
            UIGraphicsEndImageContext()
        }
    }

    @Test
    fun rectangularLogosAreCentreCroppedAndKeepPadding() {
        val code = QrCodeGenerator.encodeText("https://eugeneponomarev.com")
        val ppm = 20

        for ((width, height) in listOf(200 to 100, 100 to 200)) {
            val logo = createCropTestLogo(width, height)

            for (shape in QrLogoBackgroundShape.entries) {
                val options = QrLogoOptions(
                    sizeFraction = 0.16,
                    paddingFraction = 0.04,
                    backgroundShape = shape,
                    cornerRadiusFraction = 0.45,
                )
                val style = QrStyle(logo = options)
                val layout = code.calculateLogoLayout(options)

                val pixels = code.toUIImage(
                    sizePoints = imageSize(code, style, ppm).toDouble(),
                    style = style,
                    scale = 1.0,
                    logo = logo,
                ).toPixelBuffer()

                val left =
                    (4 + layout.leftModule + layout.paddingModules) * ppm
                val top =
                    (4 + layout.topModule + layout.paddingModules) * ppm
                val side = layout.logoSizeModules * ppm
                val green = QrColor(0, 255, 0)
                val label = "$width x $height, $shape"

                for (fraction in listOf(0.125, 0.5, 0.875)) {
                    val point = (side * fraction).toInt()

                    assertEquals(
                        green,
                        pixels.pixelAt(left + point, top + side / 2),
                        label,
                    )
                    assertEquals(
                        green,
                        pixels.pixelAt(left + side / 2, top + point),
                        label,
                    )
                }

                assertEquals(
                    QrColor.White,
                    pixels.pixelAt(left + side / 2, top - ppm / 2),
                    "Padding: $label",
                )
                assertEquals(
                    if (shape == QrLogoBackgroundShape.Square) {
                        green
                    } else {
                        QrColor.White
                    },
                    pixels.pixelAt(left + ppm / 4, top + ppm / 4),
                    "Image clipping: $label",
                )
            }
        }
    }

    @Test
    fun finderOverridesUseIndependentShapesAndSolidColorsOverGradient() {
        val code = QrCodeGenerator.encodeBytes(
            byteArrayOf(0x42),
        )
        val outerColor = QrColor.fromHex("#123C66")
        val centerColor = QrColor.fromHex("#087E8B")
        val pixelsPerModule = 20

        val style = QrStyle(
            background = backgroundColor,
            foregroundGradient = QrLinearGradient(
                startColor = startColor,
                endColor = endColor,
            ),
            finderPatternAppearance = QrFinderPatternAppearance(
                outerShape = QrFinderPatternLayerShape.RoundedSquare,
                centerShape = QrFinderPatternLayerShape.Circle,
                outerColor = outerColor,
                centerColor = centerColor,
            ),
        )

        val pixels = code.toUIImage(
            sizePoints = imageSize(
                code = code,
                style = style,
                pixelsPerModule = pixelsPerModule,
            ).toDouble(),
            style = style,
            scale = 1.0,
        ).toPixelBuffer()

        assertEquals(
            outerColor,
            pixels.pixelAtQrModule(
                row = 0,
                column = 3,
                quietZoneModules = style.quietZoneModules,
                pixelsPerModule = pixelsPerModule,
            ),
            "Outer finder layer must use its solid override colour",
        )

        assertEquals(
            backgroundColor,
            pixels.pixelAtQrModule(
                row = 1,
                column = 3,
                quietZoneModules = style.quietZoneModules,
                pixelsPerModule = pixelsPerModule,
            ),
            "Middle finder layer must remain the QR background",
        )

        assertEquals(
            centerColor,
            pixels.pixelAtQrModule(
                row = 3,
                column = 3,
                quietZoneModules = style.quietZoneModules,
                pixelsPerModule = pixelsPerModule,
            ),
            "Center finder layer must use its solid override colour",
        )

        val centerLeft =
            (style.quietZoneModules + 2) * pixelsPerModule

        assertEquals(
            backgroundColor,
            pixels.pixelAt(
                x = centerLeft + 1,
                y = centerLeft + 1,
            ),
            "Circle center shape must not fill its bounding-box corner",
        )
    }

    @Test
    fun integerGeometryUsesTheSamePhysicalPixelsAtScaleOneTwoAndThree() {
        val code = QrCodeGenerator.encodeText(
            text = "x",
            minVersion = 1,
            maxVersion = 1,
        )
        assertEquals(21, code.size)

        for (scale in listOf(1.0, 2.0, 3.0)) {
            val image = code.toUIImageWithOptions(
                options = QrRasterRenderOptions(300),
                scale = scale,
            )
            val pixels = image.toPixelBuffer()

            assertEquals(300, pixels.width)
            assertEquals(300, pixels.height)
            assertEquals(scale, image.scale)
            assertEquals(
                300.0 / scale,
                image.size.useContents { width },
                1e-9,
            )
            assertEquals(
                300.0 / scale,
                image.size.useContents { height },
                1e-9,
            )

            // Four quiet-zone modules × 10 px, plus a 5 px offset.
            assertEquals(QrColor.White, pixels.pixelAt(44, 50))
            assertEquals(QrColor.Black, pixels.pixelAt(46, 50))
            assertEquals(QrColor.Black, pixels.pixelAt(113, 50))
            assertEquals(QrColor.White, pixels.pixelAt(116, 50))

            assertEquals(QrColor.White, pixels.pixelAt(50, 44))
            assertEquals(QrColor.Black, pixels.pixelAt(50, 46))
            assertEquals(QrColor.Black, pixels.pixelAt(50, 113))
            assertEquals(QrColor.White, pixels.pixelAt(50, 116))
        }
    }

    @Test
    fun explicitPixelSizeIsPreservedForMultipleAndNonMultipleSides() {
        val code = QrCodeGenerator.encodeText(
            text = "x",
            minVersion = 1,
            maxVersion = 1,
        )

        for (sizePx in listOf(290, 300, 301)) {
            for (scale in listOf(1.0, 2.0, 3.0)) {
                for (mode in QrRasterSizeMode.entries) {
                    val image = code.toUIImageWithOptions(
                        options = QrRasterRenderOptions(sizePx, mode),
                        scale = scale,
                    )
                    val pixels = image.toPixelBuffer()
                    val label = "$sizePx px, scale=$scale, $mode"

                    assertEquals(sizePx, pixels.width, label)
                    assertEquals(sizePx, pixels.height, label)
                    assertEquals(scale, image.scale, label)
                }
            }
        }
    }

    @Test
    fun legacyImageGeometryStillFillsThePointSide() {
        val code = QrCodeGenerator.encodeText(
            text = "x",
            minVersion = 1,
            maxVersion = 1,
        )

        val legacy = code.toUIImage(
            sizePoints = 300.0,
            scale = 1.0,
        ).toPixelBuffer()

        val integer = code.toUIImageWithOptions(
            options = QrRasterRenderOptions(300),
            scale = 1.0,
        ).toPixelBuffer()

        // Historical Fit starts the matrix near 41.38 px.
        // IntegerModules starts it at 45 px.
        assertEquals(QrColor.Black, legacy.pixelAt(43, 46))
        assertEquals(QrColor.White, integer.pixelAt(43, 46))
    }

    @Test
    fun integerOffsetMovesGradientFindersAndLogoTogetherAtEveryScale() {
        val code = QrCodeGenerator.encodeText(
            text = "https://eugeneponomarev.com",
            minVersion = 4,
            maxVersion = 4,
        )
        val pixelsPerModule = 10
        val quietZone = 4
        val referenceSide = (code.size + quietZone * 2) * pixelsPerModule

        // Eleven extra pixels produce a leading offset of five pixels.
        val outputSide = referenceSide + 11
        val expectedOffset = 5

        for ((width, height) in listOf(200 to 100, 100 to 200)) {
            val logo = createCropTestLogo(width, height)

            for (shape in QrLogoBackgroundShape.entries) {
                val style = QrStyle(
                    background = QrColor.fromHex("#FFF0CC"),
                    foregroundGradient = QrLinearGradient(
                        startColor = startColor,
                        endColor = endColor,
                        start = QrGradientPoint(0.0, 0.5),
                        end = QrGradientPoint(1.0, 0.5),
                    ),
                    finderPatternAppearance = QrFinderPatternAppearance(
                        outerShape = QrFinderPatternLayerShape.RoundedSquare,
                        centerShape = QrFinderPatternLayerShape.Circle,
                        outerColor = QrColor.fromHex("#123C66"),
                        centerColor = QrColor.fromHex("#0F4C5C"),
                    ),
                    logo = QrLogoOptions(
                        sizeFraction = 0.16,
                        paddingFraction = 0.04,
                        background = QrColor.fromHex("#DDEEFF"),
                        backgroundShape = shape,
                        cornerRadiusFraction = 0.45,
                    ),
                )

                // An exact multiple gives the historical renderer the same
                // physical module size, with no additional offset.
                val reference = code.toUIImage(
                    sizePoints = referenceSide.toDouble(),
                    style = style,
                    scale = 1.0,
                    logo = logo,
                ).toPixelBuffer()

                for (scale in listOf(1.0, 2.0, 3.0)) {
                    val actual = code.toUIImageWithOptions(
                        options = QrRasterRenderOptions(
                            sizePx = outputSide,
                            sizeMode = QrRasterSizeMode.IntegerModules,
                        ),
                        style = style,
                        scale = scale,
                        logo = logo,
                    ).toPixelBuffer()

                    assertEquals(outputSide, actual.width)
                    assertEquals(outputSide, actual.height)

                    for (row in 0 until code.size) {
                        for (column in 0 until code.size) {
                            val x = (quietZone + column) * pixelsPerModule +
                                    pixelsPerModule / 2
                            val y = (quietZone + row) * pixelsPerModule +
                                    pixelsPerModule / 2

                            val expected = reference.pixelAt(x, y)
                            val observed = actual.pixelAt(
                                x + expectedOffset,
                                y + expectedOffset,
                            )
                            val label =
                                "$width x $height, $shape, scale=$scale, " +
                                        "row=$row, column=$column"

                            // Sample module centres to avoid shape boundaries.
                            // Allow small colour rounding differences.
                            assertTrue(
                                listOf(
                                    expected.red - observed.red,
                                    expected.green - observed.green,
                                    expected.blue - observed.blue,
                                    expected.alpha - observed.alpha,
                                ).all { it in -2..2 },
                                "$label: expected=$expected, actual=$observed",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createCropTestLogo(width: Int, height: Int): UIImage {
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(width.toDouble(), height.toDouble()),
            false,
            1.0,
        )
        try {
            UIColor(
                red = 1.0, green = 0.0, blue = 0.0, alpha = 1.0,
            ).setFill()
            UIBezierPath.bezierPathWithRect(
                CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
            ).fill()

            val side = minOf(width, height).toDouble()
            UIColor(
                red = 0.0, green = 1.0, blue = 0.0, alpha = 1.0,
            ).setFill()
            UIBezierPath.bezierPathWithRect(
                CGRectMake(
                    (width - side) / 2.0,
                    (height - side) / 2.0,
                    side,
                    side,
                ),
            ).fill()

            return requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
        } finally {
            UIGraphicsEndImageContext()
        }
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
            // Copy CGImage pixels without changing the row order.
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