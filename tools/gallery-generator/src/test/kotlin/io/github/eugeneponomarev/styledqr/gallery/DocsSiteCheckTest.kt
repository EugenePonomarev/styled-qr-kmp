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

            val playground = temporaryDirectory.resolve("playground.html")
            val webPackage = temporaryDirectory.resolve("web-package")

            Files.createDirectories(webPackage)

            docsWebModules.forEach { fileName ->
                Files.writeString(
                    webPackage.resolve(fileName),
                    "// Test fixture: $fileName\n",
                )
            }

            Files.writeString(
                playground,
                """
                <!doctype html>
                <html lang="en">
                <head><title>Builder</title></head>
                <body>
                  <a href="index.html">Home</a>
                  <script type="module">
                    import "./../../qrcode-web/build/packages/js/styled-qr-kmp-qrcode-web.mjs";
                  </script>
                </body>
                </html>
                """.trimIndent().replace(
                    "./../../qrcode-web/",
                    "../../qrcode-web/",
                ) + "\n",
            )

            val generated = generateGallery(logoInputs)
            writeGallery(gallery, generated.files)
            Files.writeString(stylesheet, "body { margin: 0; }\n")

            buildDocsSite(
                galleryDirectory = gallery,
                outputDirectory = site,
                stylesheet = stylesheet,
                version = "0.5.0",
                playgroundTemplate = playground,
                webPackageDirectory = webPackage,
            )

            block(site)
        } finally {
            temporaryDirectory.toFile().deleteRecursively()
        }
    }
}