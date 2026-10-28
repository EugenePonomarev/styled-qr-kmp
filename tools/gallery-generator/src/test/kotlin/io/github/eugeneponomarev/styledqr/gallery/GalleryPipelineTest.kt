package io.github.eugeneponomarev.styledqr.gallery

import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryPipelineTest {
    private val logoInputs: Path
        get() = Path.of(
            requireNotNull(System.getProperty("gallery.logoInputs")) {
                "Run gallery tests through Gradle"
            },
        )

    @Test
    fun catalogueHasEighteenUniqueCasesInTheRequiredOrder() {
        assertEquals(
            listOf(
                "theme-aurora",
                "theme-fintech",
                "theme-minimal",
                "theme-neon",
                "theme-wedding",
                "theme-coffee",
                "theme-cyber",
                "linear-gradient",
                "branded-finders",
                "logo-backed",
                "raster-integer-modules",
                "raster-fit",
                "story-square",
                "story-circle",
                "logo-monogram",
                "logo-lighthouse",
                "story-circle-logo",
                "story-gradient-logo",
            ),
            GalleryCatalog.recipes.map { it.id },
        )
    }

    @Test
    fun allCasesEncodeTheirPayloadAndProduceValidSvg() {
        val generated = generateGallery(logoInputs)

        generated.cases.forEach { rendered ->
            val decoded = QrCodeDecoder.decode(rendered.qrCode.copyModules())

            assertEquals(
                rendered.recipe.payload,
                decoded.utf8TextOrNull(),
                rendered.recipe.id,
            )
            assertEquals(
                rendered.qrCode.version,
                decoded.version,
                rendered.recipe.id,
            )
            assertEquals(
                rendered.qrCode.errorCorrection,
                decoded.errorCorrection,
                rendered.recipe.id,
            )
            assertEquals(4, rendered.style.quietZoneModules)

            val document = parseSvg(rendered.svgBytes())
            assertEquals("svg", document.documentElement.localName)

            val images = document.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg",
                "image",
            )

            if (rendered.recipe.logoSource == null) {
                assertEquals(0, images.length, rendered.recipe.id)
            } else {
                assertEquals(1, images.length, rendered.recipe.id)

                val href = images.item(0).attributes
                    .getNamedItem("href")
                    .nodeValue

                assertTrue(href.startsWith("data:image/svg+xml;base64,"))

                val embeddedLogo = Base64.getDecoder()
                    .decode(href.substringAfter(','))

                assertContentEquals(
                    Files.readAllBytes(
                        logoInputs.resolve(requireNotNull(rendered.recipe.logoSource)),
                    ),
                    embeddedLogo,
                )
                assertEquals(
                    "svg",
                    parseSvg(embeddedLogo).documentElement.localName,
                )
            }
        }

        val integer = generated.cases.single {
            it.recipe.id == "raster-integer-modules"
        }
        val fit = generated.cases.single {
            it.recipe.id == "raster-fit"
        }

        assertEquals(integer.style, fit.style)
        assertTrue("width=\"512\" height=\"512\"" in integer.svg)
        assertTrue("width=\"512\" height=\"512\"" in fit.svg)
        assertFalse(integer.svg == fit.svg)
    }

    @Test
    fun generationIsDeterministicAndUsesCanonicalTextFiles() {
        val first = generateGallery(logoInputs)
        val second = generateGallery(logoInputs)

        assertEquals(
            GalleryCatalog.recipes.map { it.fileName } + "manifest.json",
            first.files.keys.toList(),
        )
        assertEquals(first.files.keys.toList(), second.files.keys.toList())

        first.files.forEach { (fileName, bytes) ->
            assertContentEquals(bytes, second.files.getValue(fileName), fileName)

            val text = bytes.toString(StandardCharsets.UTF_8)
            assertFalse(text.startsWith("\uFEFF"), fileName)
            assertFalse('\r' in text, fileName)
        }

        val manifest = first.files.getValue("manifest.json")
            .toString(StandardCharsets.UTF_8)

        assertTrue(manifest.endsWith("\n"))
        assertFalse(manifest.endsWith("\n\n"))
    }

    @Test
    fun logoValidationFailureIncludesTheCaseId() {
        val recipe = GalleryCatalog.recipes.single {
            it.id == "logo-backed"
        }.copy(
            logoOptions = QrLogoOptions(
                sizeFraction = 0.25,
                paddingFraction = 0.12,
            ),
        )

        val failure = assertFailsWith<IllegalStateException> {
            renderGalleryCase(recipe, logoInputs)
        }

        assertTrue(failure.message.orEmpty().contains("logo-backed"))
        assertTrue(failure.cause is IllegalArgumentException)
    }

    @Test
    fun missingLogoInputFailsInsteadOfRenderingWithoutLogo() =
        withTemporaryDirectory { directory ->
            val recipe = GalleryCatalog.recipes.single {
                it.id == "logo-backed"
            }

            val failure = assertFailsWith<IllegalStateException> {
                renderGalleryCase(recipe, directory)
            }

            assertTrue(failure.message.orEmpty().contains("logo-backed"))
        }

    @Test
    fun jsonEscapingAndNumbersAreStable() {
        assertEquals(
            "\"a\\\"b\\\\c\"",
            galleryJson("a\"b\\c"),
        )
        assertEquals(
            "\"\\b\\f\\n\\r\\t\\u0000\"",
            galleryJson("\b\u000C\n\r\t\u0000"),
        )
        assertEquals(
            "{\"x\":0.12,\"y\":null,\"z\":[true,2]}",
            galleryJson(
                linkedMapOf(
                    "x" to 0.12,
                    "y" to null,
                    "z" to listOf(true, 2),
                ),
            ),
        )

        val controls = (0..31).map { it.toChar() }.joinToString("")
        assertFalse(galleryJson(controls).any { it.code < 0x20 })

        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach {
                assertFailsWith<IllegalArgumentException> {
                    galleryJson(it)
                }
            }
    }

    @Test
    fun checkPassesAfterUpdateWithoutChangingTheGallery() =
        withTemporaryDirectory { directory ->
            val generated = generateGallery(logoInputs)
            val gallery = directory.resolve("gallery")
            val candidate = directory.resolve("candidate")

            writeGallery(gallery, generated.files)
            val before = snapshot(gallery)

            verifyGallery(gallery, candidate, generated.files)

            assertEquals(before, snapshot(gallery))
            assertEquals(before, snapshot(candidate))
        }

    @Test
    fun failedChecksNeverRepairOrDeleteCommittedFiles() {
        val generated = generateGallery(logoInputs)

        val mutations: List<(Path) -> Unit> = listOf(
            { Files.delete(it.resolve("theme-minimal.svg")) },
            { Files.write(it.resolve("theme-minimal.svg"), byteArrayOf(1)) },
            { Files.write(it.resolve("unexpected.svg"), byteArrayOf(1)) },
            { Files.delete(it.resolve("manifest.json")) },
            { Files.write(it.resolve("manifest.json"), byteArrayOf(1)) },
            { Files.write(it.resolve("old-manifest.json"), byteArrayOf(1)) },
        )

        mutations.forEach { mutate ->
            withTemporaryDirectory { directory ->
                val gallery = directory.resolve("gallery")
                val candidate = directory.resolve("candidate")

                writeGallery(gallery, generated.files)
                mutate(gallery)

                val before = snapshot(gallery)

                assertFailsWith<IllegalStateException> {
                    verifyGallery(gallery, candidate, generated.files)
                }

                assertEquals(before, snapshot(gallery))
            }
        }
    }

    @Test
    fun updateRefusesUnexpectedFilesBeforeWritingAnything() =
        withTemporaryDirectory { directory ->
            val generated = generateGallery(logoInputs)
            val gallery = directory.resolve("gallery")

            writeGallery(gallery, generated.files)
            Files.write(gallery.resolve("unexpected.svg"), byteArrayOf(1))
            Files.write(gallery.resolve("theme-minimal.svg"), byteArrayOf(2))

            val before = snapshot(gallery)

            assertFailsWith<IllegalStateException> {
                writeGallery(gallery, generated.files)
            }

            assertEquals(before, snapshot(gallery))
        }

    @Test
    fun checkRejectsOverlappingDirectoriesBeforeWriting() =
        withTemporaryDirectory { directory ->
            val generated = generateGallery(logoInputs)
            val gallery = directory.resolve("gallery")

            writeGallery(gallery, generated.files)
            val before = snapshot(gallery)

            assertFailsWith<IllegalArgumentException> {
                verifyGallery(gallery, gallery, generated.files)
            }
            assertFailsWith<IllegalArgumentException> {
                verifyGallery(
                    gallery,
                    gallery.resolve("candidate"),
                    generated.files,
                )
            }

            assertEquals(before, snapshot(gallery))
        }

    @Test
    fun storyCasesContainTheCompleteLongPayloadAndUseDenseMatrices() {
        val generated = generateGallery(logoInputs)
        val story = GalleryPayloads.lighthouseStory

        assertTrue(story.encodeToByteArray().size > 300)

        val stories = generated.cases.filter {
            it.recipe.payload == story
        }

        assertEquals(
            setOf(
                "story-square",
                "story-circle",
                "story-circle-logo",
                "story-gradient-logo",
            ),
            stories.map { it.recipe.id }.toSet(),
        )

        stories.forEach {
            assertTrue(
                it.qrCode.size >= 81,
                "Expected a dense matrix for ${it.recipe.id}",
            )
        }
    }

    private fun parseSvg(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true,
            )
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun snapshot(directory: Path): Map<String, List<Byte>> =
        Files.list(directory).use { paths ->
            paths.iterator()
                .asSequence()
                .associate {
                    it.fileName.toString() to Files.readAllBytes(it).toList()
                }
        }

    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("styled-qr-gallery-pipeline-")

        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}