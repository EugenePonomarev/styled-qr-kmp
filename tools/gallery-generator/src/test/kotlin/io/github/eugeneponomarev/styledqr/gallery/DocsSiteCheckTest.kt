package io.github.eugeneponomarev.styledqr.gallery

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocsSiteCheckTest {
    private val logoInputs: Path
        get() = Path.of(
            requireNotNull(System.getProperty("gallery.logoInputs")) {
                "Run gallery tests through Gradle"
            },
        )

    @Test
    fun generatedSitePassesChecks() = withSite { site ->
        checkDocsSite(site)
    }

    @Test
    fun brokenLocalLinkIsReported() = withSite { site ->
        val page = site.resolve("index.html")
        val original = Files.readString(page)

        Files.writeString(
            page,
            original.replace(
                """href="gallery.html"""",
                """href="missing.html"""",
            ),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            checkDocsSite(site)
        }

        assertTrue(failure.message.orEmpty().contains("missing.html"))
    }

    private fun withSite(block: (Path) -> Unit) {
        val temporaryDirectory = Files.createTempDirectory("styled-qr-docs-site-")

        try {
            val gallery = temporaryDirectory.resolve("gallery-input")
            val site = temporaryDirectory.resolve("site")
            val stylesheet = temporaryDirectory.resolve("style.css")

            val generated = generateGallery(logoInputs)
            writeGallery(gallery, generated.files)
            Files.writeString(stylesheet, "body { margin: 0; }\n")

            buildDocsSite(
                galleryDirectory = gallery,
                outputDirectory = site,
                stylesheet = stylesheet,
                version = "0.5.0",
            )

            block(site)
        } finally {
            temporaryDirectory.toFile().deleteRecursively()
        }
    }
}