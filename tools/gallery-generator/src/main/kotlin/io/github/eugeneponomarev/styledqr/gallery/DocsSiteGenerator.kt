package io.github.eugeneponomarev.styledqr.gallery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private fun escapeHtml(value: String): String = buildString {
    value.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(character)
        }
    }
}

private fun sitePage(
    title: String,
    currentPage: String,
    content: String,
): String {
    val navigation = listOf(
        "index.html" to "Home",
        "gallery.html" to "Gallery",
        "getting-started.html" to "Getting started",
        "safety.html" to "Safety",
    ).joinToString("\n") { (href, label) ->
        val current = if (href == currentPage) {
            """ aria-current="page""""
        } else {
            ""
        }

        """<a href="$href"$current>$label</a>"""
    }

    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <meta name="color-scheme" content="light">
          <link rel="stylesheet" href="style.css">
          <title>${escapeHtml(title)} · Styled QR KMP</title>
        </head>
        <body>
          <header class="site-header">
            <div class="header-inner">
              <a class="site-name" href="index.html">Styled QR KMP</a>
              <nav aria-label="Main navigation">
                $navigation
              </nav>
            </div>
          </header>
          <main>
        $content
          </main>
          <footer class="site-footer">
            <p>
              <a href="https://github.com/EugenePonomarev/styled-qr-kmp">
                Styled QR KMP on GitHub
              </a>
            </p>
          </footer>
        </body>
        </html>
    """.trimIndent() + "\n"
}

private fun galleryCards(): String =
    GalleryCatalog.recipes.joinToString("\n") { recipe ->
        val fileName = escapeHtml(recipe.fileName)
        val caption = escapeHtml(recipe.caption)

        """
        <article>
          <h3>$caption</h3>
          <a href="gallery/$fileName">
            <img src="gallery/$fileName" alt="$caption" width="240" height="240">
          </a>
          <a class="card-link" href="gallery/$fileName">Open SVG</a>
        </article>
        """.trimIndent()
    }

private fun homeContent(version: String): String = """
    <section class="hero">
      <p class="eyebrow">Kotlin Multiplatform</p>
      <h1>Styled QR KMP</h1>
      <p class="lead">
        Generate styled QR codes with shared encoding and design rules
        for Android, iOS and Web.
      </p>
      <div class="actions">
        <a class="button" href="getting-started.html">Get started</a>
        <a class="button button-secondary" href="gallery.html">Explore the gallery</a>
      </div>
      <p class="version-note">
        Published library version: ${escapeHtml(version)}.
        Gallery examples are generated from repository main.
        SVG bytes may differ from the published release.
      </p>
    </section>

    <section class="content-section">
      <h2>Built for multiple platforms</h2>
      <div class="feature-grid">
        <article>
          <h3>Shared QR engine</h3>
          <p>
            Encode text and render QR codes with themes, shapes,
            gradients and validated centre logos.
          </p>
        </article>
        <article>
          <h3>Native output</h3>
          <p>
            Use Android Bitmap and UI adapters, iOS UIImage,
            or SVG and PNG on the Web.
          </p>
        </article>
        <article>
          <h3>Safety rules</h3>
          <p>
            Logo placement and custom gradient colours are validated
            by the shared library.
          </p>
        </article>
      </div>
      <p>
        Learn more about <a href="safety.html">QR design and scanning</a>.
      </p>
    </section>
""".trimIndent()

private fun galleryContent(): String = """
    <section class="page-intro">
      <p class="eyebrow">Examples</p>
      <h1>Gallery</h1>
      <p class="lead">
        Explore themes, gradients, finder styles and centre logos.
        Select an image to open its original SVG.
      </p>
      <p>
        The long-text examples contain the same complete English short story.
        They use QR version 20, error correction H and a 97 × 97 matrix.
      </p>
    </section>

    <section class="content-section" aria-label="QR gallery">
      ${galleryCards()}
    </section>
""".trimIndent()

private fun gettingStartedContent(version: String): String = """
    <section class="page-intro">
      <p class="eyebrow">Installation</p>
      <h1>Getting started</h1>
      <p class="lead">
        Choose the platform you use and start with a shared theme.
      </p>
      <p>
        The examples below use published version
        <strong>${escapeHtml(version)}</strong>.
      </p>
    </section>

    <section class="prose">
      <h2 id="android">Android and Kotlin</h2>
      <p>
        Add JitPack to the dependency repositories in
        <code>settings.gradle.kts</code>:
      </p>
      <pre><code>dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}</code></pre>

      <p>Add the core module to your application:</p>
      <pre><code>dependencies {
    implementation(
        "com.github.EugenePonomarev.styled-qr-kmp:qrcode-core:${escapeHtml(version)}"
    )
}</code></pre>

      <p>Create a QR matrix and render it as SVG:</p>
      <pre><code>import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import io.github.eugeneponomarev.styledqr.theme.QrThemes

val qrCode = QrCodeGenerator.encodeText("https://example.com")
val style = QrThemes.Fintech.createStyle()
val svg = SvgQrRenderer.render(qrCode, style)</code></pre>

      <p>
        For Android UI integrations, see the
        <a href="https://github.com/EugenePonomarev/styled-qr-kmp#ui-components">
          View and Compose examples
        </a>.
      </p>

      <h2 id="ios">iOS</h2>
      <p>
        Download <code>StyledQrKmp.xcframework.zip</code> from a GitHub Release,
        add the extracted framework to your Xcode target and import
        <code>StyledQrKmp</code>.
      </p>
      <p>
        See the
        <a href="https://github.com/EugenePonomarev/styled-qr-kmp#ios-uikit-and-swiftui">
          UIKit and SwiftUI examples
        </a>
        for rendering and integration.
      </p>

      <h2 id="web">Web / npm</h2>
      <p>Install the published package:</p>
      <pre><code>npm install styled-qr-kmp-web@${escapeHtml(version)}</code></pre>
      <p>
        See the
        <a href="https://github.com/EugenePonomarev/styled-qr-kmp#web--npm">
          Web quick start
        </a>
        for a browser example using <code>renderStyledQrSvg</code>.
      </p>
    </section>
""".trimIndent()

private fun safetyContent(): String = """
    <section class="page-intro">
      <p class="eyebrow">Design and scanning</p>
      <h1>QR safety</h1>
      <p class="lead">
        Design for clear contrast and test the final image on target devices.
      </p>
    </section>

    <section class="prose">
      <h2>Contrast and quiet zone</h2>
      <p>
        Use a light background, a visibly darker foreground and a quiet zone
        of at least four modules. Ordinary solid foreground colours are not
        universally rejected for low contrast.
      </p>
      <p>
        Gradients and explicitly customised finder colours have stricter
        validation: colours must be opaque, darker than an opaque background
        and have at least a 4.5:1 contrast ratio.
      </p>

      <h2>Function patterns</h2>
      <p>
        Function patterns help scanners locate and interpret the code.
        Use <code>QrFunctionPatternStyle.PreserveAll</code> when you want
        non-finder function modules to remain square.
      </p>

      <h2>Centre logos</h2>
      <p>
        Logo placement is aligned to whole modules. The library rejects
        placements that overlap mandatory QR patterns or exceed its
        per-block Reed–Solomon correction budget.
      </p>
      <p>
        Use error correction H for logo examples and keep the logo area
        small enough for the selected payload and QR version.
      </p>

      <h2>Test the final image</h2>
      <p>
        Validation does not guarantee optical readability in every situation.
        Test the final image at the intended size, on target devices and under
        realistic lighting conditions.
      </p>
      <p><a href="gallery.html">Explore the gallery</a></p>
    </section>
""".trimIndent()

internal fun buildDocsSite(
    galleryDirectory: Path,
    outputDirectory: Path,
    stylesheet: Path,
    version: String,
) {
    require(version.isNotBlank()) {
        "Library version must not be blank"
    }

    require(Files.isRegularFile(stylesheet, LinkOption.NOFOLLOW_LINKS)) {
        "Missing site stylesheet: $stylesheet"
    }

    val gallery = galleryDirectory.toAbsolutePath().normalize()
    val output = outputDirectory.toAbsolutePath().normalize()

    require(!output.startsWith(gallery) && !gallery.startsWith(output)) {
        "Gallery and site output directories must not overlap"
    }

    val fileNames = GalleryCatalog.recipes.map { it.fileName } + "manifest.json"

    fileNames.forEach { fileName ->
        requireGalleryFileName(fileName)

        val source = gallery.resolve(fileName).normalize()

        require(source.parent == gallery) {
            "Gallery file escapes its input directory: $fileName"
        }
        require(Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            "Missing gallery file: $source"
        }
    }

    val pages = linkedMapOf(
        "index.html" to sitePage(
            title = "Home",
            currentPage = "index.html",
            content = homeContent(version),
        ),
        "gallery.html" to sitePage(
            title = "Gallery",
            currentPage = "gallery.html",
            content = galleryContent(),
        ),
        "getting-started.html" to sitePage(
            title = "Getting started",
            currentPage = "getting-started.html",
            content = gettingStartedContent(version),
        ),
        "safety.html" to sitePage(
            title = "Safety",
            currentPage = "safety.html",
            content = safetyContent(),
        ),
    )

    val galleryOutput = output.resolve("gallery")
    Files.createDirectories(galleryOutput)

    Files.copy(
        stylesheet,
        output.resolve("style.css"),
        StandardCopyOption.REPLACE_EXISTING,
    )

    fileNames.forEach { fileName ->
        Files.copy(
            gallery.resolve(fileName),
            galleryOutput.resolve(fileName),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    pages.forEach { (fileName, html) ->
        Files.writeString(
            output.resolve(fileName),
            html,
            StandardCharsets.UTF_8,
        )
    }
}

fun main(args: Array<String>) {
    require(args.size == 4) {
        "Usage: docs-site-generator " +
                "<gallery-directory> <output-directory> <stylesheet> <version>"
    }

    buildDocsSite(
        galleryDirectory = Path.of(args[0]),
        outputDirectory = Path.of(args[1]),
        stylesheet = Path.of(args[2]),
        version = args[3],
    )

    println("Built documentation site with ${GalleryCatalog.recipes.size} gallery cases.")
}