package io.github.eugeneponomarev.styledqr.gallery

import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryGeneratorTest {
    @Test
    fun minimalCaseUsesTheOriginalThemeAndEncodesThePayload() {
        val recipe = GalleryCatalog.recipes.single { it.id == "theme-minimal" }
        val rendered = renderGalleryCase(recipe)
        val decoded = QrCodeDecoder.decode(rendered.qrCode.copyModules())

        assertEquals("theme-minimal", recipe.id)
        assertEquals("theme-minimal.svg", recipe.fileName)
        assertEquals("https://example.com", decoded.utf8TextOrNull())
        assertEquals(QrErrorCorrectionLevel.H, rendered.qrCode.errorCorrection)
        assertTrue(rendered.qrCode.version in recipe.minVersion..recipe.maxVersion)
        assertEquals(17 + 4 * rendered.qrCode.version, rendered.qrCode.size)

        assertEquals(
            SvgQrRenderer.render(
                qrCode = rendered.qrCode,
                style = QrThemes.Minimal.createStyle(),
            ),
            rendered.svg,
        )
    }

    @Test
    fun repeatedRenderingProducesIdenticalUtf8Bytes() {
        val recipe = GalleryCatalog.recipes.single { it.id == "theme-minimal" }

        val first = renderGalleryCase(recipe)
        val second = renderGalleryCase(recipe)

        assertContentEquals(first.svgBytes(), second.svgBytes())
        assertTrue(first.svg.startsWith("<svg "))
        assertFalse('\r' in first.svg)
        assertFalse(first.svg.startsWith("\uFEFF"))
    }

    @Test
    fun comparisonAcceptsIdenticalFiles() = withTemporaryDirectory { directory ->
        val expected = minimalFiles()
        writeFiles(directory, expected)

        assertTrue(compareGallery(directory, expected).matches)
    }

    @Test
    fun comparisonReportsMissingFilesWithoutCreatingThem() =
        withTemporaryDirectory { directory ->
            val expected = minimalFiles()

            val comparison = compareGallery(directory, expected)

            assertEquals(expected.keys.sorted(), comparison.missing)
            assertFalse(comparison.matches)
            assertTrue(snapshot(directory).isEmpty())
        }

    @Test
    fun comparisonReportsChangedFilesWithoutRepairingThem() =
        withTemporaryDirectory { directory ->
            val expected = minimalFiles()
            val fileName = expected.keys.single()

            Files.write(
                directory.resolve(fileName),
                expected.getValue(fileName) + byteArrayOf(10),
            )

            val before = snapshot(directory)
            val comparison = compareGallery(directory, expected)

            assertEquals(listOf(fileName), comparison.changed)
            assertFalse(comparison.matches)
            assertEquals(before, snapshot(directory))
        }

    @Test
    fun comparisonReportsExtraFilesWithoutDeletingThem() =
        withTemporaryDirectory { directory ->
            val expected = minimalFiles()
            writeFiles(directory, expected)
            Files.write(
                directory.resolve("unexpected.svg"),
                expected.values.single(),
            )

            val before = snapshot(directory)
            val comparison = compareGallery(directory, expected)

            assertEquals(listOf("unexpected.svg"), comparison.extra)
            assertFalse(comparison.matches)
            assertEquals(before, snapshot(directory))
        }

    @Test
    fun comparisonDoesNotCreateAnAbsentDirectory() =
        withTemporaryDirectory { parent ->
            val directory = parent.resolve("absent")
            val expected = minimalFiles()

            val comparison = compareGallery(directory, expected)

            assertEquals(expected.keys.sorted(), comparison.missing)
            assertFalse(Files.exists(directory))
        }

    @Test
    fun unsafeCaseIdsAreRejected() {
        val recipe = GalleryCatalog.recipes.single { it.id == "theme-minimal" }

        listOf(
            "",
            "../outside",
            "one/two",
            "one\\two",
            "Uppercase",
            "has space",
        ).forEach { id ->
            assertFailsWith<IllegalArgumentException> {
                recipe.copy(id = id)
            }
        }
    }

    @Test
    fun comparisonRejectsUnsafeFilenames() = withTemporaryDirectory { directory ->
        listOf(
            "../outside.svg",
            "/outside.svg",
            "nested/file.svg",
            "nested\\file.svg",
        ).forEach { fileName ->
            assertFailsWith<IllegalArgumentException> {
                compareGallery(
                    directory = directory,
                    expectedFiles = mapOf(fileName to byteArrayOf()),
                )
            }
        }
    }

    private fun minimalFiles(): Map<String, ByteArray> {
        val rendered = renderGalleryCase(GalleryCatalog.recipes.single { it.id == "theme-minimal" })

        return mapOf(rendered.recipe.fileName to rendered.svgBytes())
    }

    private fun writeFiles(directory: Path, files: Map<String, ByteArray>) {
        files.forEach { (fileName, bytes) ->
            Files.write(directory.resolve(fileName), bytes)
        }
    }

    private fun snapshot(directory: Path): Map<String, List<Byte>> =
        Files.list(directory).use { paths ->
            paths.iterator()
                .asSequence()
                .associate { file ->
                    file.fileName.toString() to Files.readAllBytes(file).toList()
                }
        }

    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("styled-qr-gallery-test-")

        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
