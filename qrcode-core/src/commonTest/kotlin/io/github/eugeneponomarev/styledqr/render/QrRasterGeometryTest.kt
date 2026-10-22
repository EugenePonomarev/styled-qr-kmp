package io.github.eugeneponomarev.styledqr.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QrRasterGeometryTest {
    @Test
    fun defaultsToIntegerModules() {
        val options = QrRasterRenderOptions(sizePx = 300)

        assertEquals(QrRasterSizeMode.IntegerModules, options.sizeMode)
    }

    @Test
    fun versionOneAt300PixelsHasTenPixelModulesAndFivePixelOffsets() {
        val totalModules = qrImageTotalModules(
            matrixSize = 21,
            quietZoneModules = 4,
        )
        val geometry = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(sizePx = 300),
            totalModules = totalModules,
        )

        assertEquals(300, geometry.sizePx)
        assertEquals(29, geometry.totalModules)
        assertEquals(10.0, geometry.moduleSizePx)
        assertEquals(5.0, geometry.offsetPx)
        assertEquals(5.0, geometry.trailingOffsetPx)

        val matrixStartPx = geometry.offsetPx + 4 * geometry.moduleSizePx
        val matrixEndPx = matrixStartPx + 21 * geometry.moduleSizePx

        assertEquals(45.0, matrixStartPx)
        assertEquals(255.0, matrixEndPx)
    }

    @Test
    fun exactMultipleNeedsNoAdditionalOffsets() {
        for (mode in QrRasterSizeMode.entries) {
            val geometry = calculateQrRasterGeometry(
                options = QrRasterRenderOptions(
                    sizePx = 290,
                    sizeMode = mode,
                ),
                totalModules = 29,
            )

            assertEquals(10.0, geometry.moduleSizePx)
            assertEquals(0.0, geometry.offsetPx)
            assertEquals(0.0, geometry.trailingOffsetPx)
        }
    }

    @Test
    fun oddRemainingPixelGoesToTheRightAndBottom() {
        val geometry = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(sizePx = 301),
            totalModules = 29,
        )

        assertEquals(10.0, geometry.moduleSizePx)
        assertEquals(5.0, geometry.offsetPx)
        assertEquals(6.0, geometry.trailingOffsetPx)
    }

    @Test
    fun fitUsesTheEntireSideWithFractionalModules() {
        val geometry = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(
                sizePx = 300,
                sizeMode = QrRasterSizeMode.Fit,
            ),
            totalModules = 29,
        )

        assertEquals(300.0 / 29.0, geometry.moduleSizePx, 1e-12)
        assertEquals(0.0, geometry.offsetPx)
        assertEquals(0.0, geometry.trailingOffsetPx)
        assertEquals(
            300.0,
            geometry.moduleSizePx * geometry.totalModules,
            1e-9,
        )
    }

    @Test
    fun onePixelPerModuleIsSupported() {
        for (mode in QrRasterSizeMode.entries) {
            val geometry = calculateQrRasterGeometry(
                options = QrRasterRenderOptions(
                    sizePx = 29,
                    sizeMode = mode,
                ),
                totalModules = 29,
            )

            assertEquals(1.0, geometry.moduleSizePx)
            assertEquals(0.0, geometry.offsetPx)
            assertEquals(0.0, geometry.trailingOffsetPx)
        }
    }

    @Test
    fun integerModePreservesExistingAndroidGeometry() {
        for (totalModules in listOf(21, 29, 33, 177, 185)) {
            for (sizePx in listOf(
                totalModules,
                totalModules + 1,
                totalModules * 2,
                totalModules * 2 + 17,
                1024,
            )) {
                val previous = calculateRasterModuleLayout(
                    sizePx = sizePx,
                    totalModules = totalModules,
                )
                val geometry = calculateQrRasterGeometry(
                    options = QrRasterRenderOptions(sizePx),
                    totalModules = totalModules,
                )

                assertEquals(
                    previous.moduleSizePx.toDouble(),
                    geometry.moduleSizePx,
                )
                assertEquals(
                    previous.offsetPx.toDouble(),
                    geometry.offsetPx,
                )
                assertEquals(
                    sizePx.toDouble(),
                    geometry.offsetPx +
                            geometry.moduleSizePx * totalModules +
                            geometry.trailingOffsetPx,
                )
                assertTrue(
                    geometry.trailingOffsetPx - geometry.offsetPx in 0.0..1.0,
                )
            }
        }
    }

    @Test
    fun largeGeometryDoesNotOverflowIntegerArithmetic() {
        val geometry = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(Int.MAX_VALUE),
            totalModules = Int.MAX_VALUE,
        )

        assertEquals(1.0, geometry.moduleSizePx)
        assertEquals(0.0, geometry.offsetPx)
        assertEquals(0.0, geometry.trailingOffsetPx)
    }

    @Test
    fun rejectsNonPositiveOrInsufficientOutputSizes() {
        for (mode in QrRasterSizeMode.entries) {
            for (sizePx in listOf(Int.MIN_VALUE, -1, 0, 1, 28)) {
                invalid {
                    calculateQrRasterGeometry(
                        options = QrRasterRenderOptions(sizePx, mode),
                        totalModules = 29,
                    )
                }
            }
        }
    }

    @Test
    fun rejectsNonPositiveModuleCountsBeforeCalculatingGeometry() {
        for (mode in QrRasterSizeMode.entries) {
            for (totalModules in listOf(Int.MIN_VALUE, -1, 0)) {
                invalid {
                    calculateQrRasterGeometry(
                        options = QrRasterRenderOptions(300, mode),
                        totalModules = totalModules,
                    )
                }
            }
        }
    }

    @Test
    fun pointConversionKeepsTheSame300PhysicalPixelsAtEveryScale() {
        val pixels = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(300),
            totalModules = 29,
        )

        val atOne = pixels.toPointGeometry(1.0)
        assertEquals(300.0, atOne.sizePoints)
        assertEquals(10.0, atOne.moduleSizePoints)
        assertEquals(5.0, atOne.offsetPoints)

        val atTwo = pixels.toPointGeometry(2.0)
        assertEquals(150.0, atTwo.sizePoints)
        assertEquals(5.0, atTwo.moduleSizePoints)
        assertEquals(2.5, atTwo.offsetPoints)

        val atThree = pixels.toPointGeometry(3.0)
        assertEquals(100.0, atThree.sizePoints)
        assertEquals(10.0 / 3.0, atThree.moduleSizePoints, 1e-12)
        assertEquals(5.0 / 3.0, atThree.offsetPoints, 1e-12)
    }

    @Test
    fun pointConversionSupportsPositiveFractionalScale() {
        val pixels = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(300),
            totalModules = 29,
        )
        val points = pixels.toPointGeometry(0.5)

        assertEquals(600.0, points.sizePoints)
        assertEquals(20.0, points.moduleSizePoints)
        assertEquals(10.0, points.offsetPoints)
    }

    @Test
    fun pointConversionRejectsInvalidEffectiveScale() {
        val pixels = calculateQrRasterGeometry(
            options = QrRasterRenderOptions(300),
            totalModules = 29,
        )

        for (scale in listOf(
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.MIN_VALUE,
        )) {
            invalid {
                pixels.toPointGeometry(scale)
            }
        }
    }

    private fun invalid(block: () -> Unit) {
        val error = assertFailsWith<QrRenderException>(block = block)

        assertEquals(
            QrRenderErrorCode.InvalidRenderParameters,
            error.code,
        )
        assertTrue(error.message.orEmpty().isNotBlank())
    }
}
