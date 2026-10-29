package io.github.eugeneponomarev.styledqr.gallery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator

internal val docsWebModules = listOf(
    "kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.mjs",
    "kotlin-kotlin-stdlib.mjs",
    "styled-qr-kmp-qrcode-core.mjs",
    "styled-qr-kmp-qrcode-web.mjs",
)

private const val LOCAL_PREVIEW_IMPORT =
    "../../qrcode-web/build/packages/js/styled-qr-kmp-qrcode-web.mjs"

private const val SITE_PLAYGROUND_IMPORT =
    "./web/styled-qr-kmp-qrcode-web.mjs"

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
        "playground.html" to "Builder",
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
        val qrCode = QrCodeGenerator.encodeText(
            text = recipe.payload,
            errorCorrection = recipe.requestedErrorCorrection,
            minVersion = recipe.minVersion,
            maxVersion = recipe.maxVersion,
        )

        val fileName = escapeHtml(recipe.fileName)
        val caption = escapeHtml(recipe.caption)
        val errorCorrection = escapeHtml(qrCode.errorCorrection.toString())

        """
        <article class="gallery-card">
          <div class="gallery-card-heading">
            <span class="card-kicker">SVG EXAMPLE</span>
            <h3>$caption</h3>
          </div>
          <a class="gallery-image" href="gallery/$fileName">
            <img src="gallery/$fileName" alt="$caption" width="240" height="240">
          </a>
          <div class="gallery-card-footer">
            <p class="gallery-meta">
              Version ${qrCode.version} · ECC $errorCorrection · ${qrCode.size} × ${qrCode.size}
            </p>
            <a class="card-link" href="gallery/$fileName">Open SVG <span aria-hidden="true">↗</span></a>
          </div>
        </article>
        """.trimIndent()
    }

private fun homeContent(version: String): String = """
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">KOTLIN MULTIPLATFORM · OPEN SOURCE</p>
        <h1>QR codes made<br><span>to stand out.</span></h1>
        <p class="lead">
          Create expressive QR codes for Android, iOS and Web
          with one shared Kotlin engine.
        </p>
        <div class="actions">
            <a class="button button-secondary" href="playground.html">
              Open QR builder <span aria-hidden="true">→</span>
            </a>
          <a class="button" href="getting-started.html">
            Get started <span aria-hidden="true">↗</span>
          </a>
          <a class="button button-secondary" href="gallery.html">
            Explore the gallery <span aria-hidden="true">→</span>
          </a>
        </div>
        <div class="hero-details">
          <span>ANDROID</span>
          <span>iOS</span>
          <span>WEB</span>
          <span class="hero-version">VERSION ${escapeHtml(version)}</span>
        </div>
      </div>

      <div class="hero-art">
        <div class="hero-art-label">
          <span>01 / THE OUTPUT</span>
          <span>SVG · SHARED STYLE</span>
        </div>
        <a class="hero-qr" href="gallery/story-circle-logo.svg">
          <img
            src="gallery/story-circle-logo.svg"
            alt="QR code with circular modules and a lighthouse logo"
            width="420"
            height="420"
          >
        </a>
        <div class="hero-art-caption">
          <span>Long text, circular modules &amp; a centre logo</span>
          <a href="gallery/story-circle-logo.svg">View SVG ↗</a>
        </div>
      </div>
    </section>

    <section class="showcase-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">DESIGNED TO BE SCANNED</p>
          <h2>One engine.<br>Many ways to make it yours.</h2>
        </div>
        <p>
          Start with a theme, then shape the details: modules, finder
          patterns, gradients and a centre logo.
        </p>
      </div>

      <div class="showcase-grid">
        <article class="showcase-card showcase-card-light">
          <div class="showcase-label">
            <span>01</span>
            <span>THEMES</span>
          </div>
          <a href="gallery/theme-aurora.svg">
            <img
              src="gallery/theme-aurora.svg"
              alt="Aurora theme with circular modules"
              width="240"
              height="240"
            >
          </a>
          <div>
            <h3>A distinct look, ready to use.</h3>
            <p>Seven built-in themes share the same styling API.</p>
          </div>
        </article>

        <article class="showcase-card showcase-card-tint">
          <div class="showcase-label">
            <span>02</span>
            <span>GRADIENTS</span>
          </div>
          <a href="gallery/linear-gradient.svg">
            <img
              src="gallery/linear-gradient.svg"
              alt="QR code with a blue-to-purple gradient"
              width="240"
              height="240"
            >
          </a>
          <div>
            <h3>Colour with a purpose.</h3>
            <p>Use validated linear gradients to give your QR a visual identity.</p>
          </div>
        </article>

        <article class="showcase-card showcase-card-light">
          <div class="showcase-label">
            <span>03</span>
            <span>LOGOS</span>
          </div>
          <a href="gallery/logo-monogram.svg">
            <img
              src="gallery/logo-monogram.svg"
              alt="Fintech QR code with a central monogram"
              width="240"
              height="240"
            >
          </a>
          <div>
            <h3>Make room for your brand.</h3>
            <p>Place a centre logo with shared placement validation.</p>
          </div>
        </article>
      </div>

      <a class="text-link" href="gallery.html">
        See all gallery examples <span aria-hidden="true">→</span>
      </a>
    </section>

    <section class="developer-section">
      <div class="developer-copy">
        <p class="eyebrow">BUILT FOR DEVELOPERS</p>
        <h2>From content<br>to QR in a few lines.</h2>
        <p>
          Encode once, choose a style and render the result.
          Use the shared engine with the platform output your app needs.
        </p>
        <a class="text-link" href="getting-started.html">
          Read the quick start <span aria-hidden="true">→</span>
        </a>
      </div>

      <div class="code-panel">
        <div class="code-panel-header">
          <span>KOTLIN</span>
          <span>SHARED API</span>
        </div>
        <pre><code>val qrCode = QrCodeGenerator.encodeText(
    "https://example.com"
)

val style = QrThemes.Fintech.createStyle()
val svg = SvgQrRenderer.render(qrCode, style)</code></pre>
      </div>
    </section>

    <section class="closing-section">
      <div>
        <p class="eyebrow">DESIGN WITH CONFIDENCE</p>
        <h2>A good-looking QR still needs to scan.</h2>
        <p>
          Learn about contrast, quiet zones, logo placement
          and testing your final image.
        </p>
      </div>
      <a class="button button-secondary" href="safety.html">
        Read the safety guide <span aria-hidden="true">→</span>
      </a>
    </section>

    <p class="version-note">
      Published library version: ${escapeHtml(version)}.
      Gallery examples are generated from repository main;
      SVG bytes may differ from the published release.
    </p>
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
    <section class="page-intro docs-intro">
      <p class="eyebrow">GETTING STARTED</p>
      <h1>From dependency<br>to your first QR.</h1>
      <p class="lead">
        Choose your platform, add the library and start with a shared theme.
      </p>
      <p class="version-note">
        Examples use published version ${escapeHtml(version)}.
      </p>
      <nav class="page-nav" aria-label="Choose a platform">
        <a href="#android">Android &amp; Kotlin</a>
        <a href="#ios">iOS</a>
        <a href="#web">Web / npm</a>
      </nav>
    </section>

    <section class="prose docs-content">
      <div class="docs-section">
        <p class="eyebrow">01 / ANDROID &amp; KOTLIN</p>
        <h2 id="android">Add the shared engine.</h2>
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

        <p>Encode text, choose a theme and render an SVG:</p>
        <pre><code>import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import io.github.eugeneponomarev.styledqr.theme.QrThemes

val qrCode = QrCodeGenerator.encodeText("https://example.com")
val style = QrThemes.Fintech.createStyle()
val svg = SvgQrRenderer.render(qrCode, style)</code></pre>

        <p>
          Building an Android interface? See the
          <a href="https://github.com/EugenePonomarev/styled-qr-kmp#ui-components">
            View and Compose examples
          </a>.
        </p>
      </div>

      <div class="docs-section">
        <p class="eyebrow">02 / iOS</p>
        <h2 id="ios">Render a native image.</h2>
        <p>
          Download <code>StyledQrKmp.xcframework.zip</code> from a GitHub
          Release. Add the extracted framework to your Xcode target and
          import <code>StyledQrKmp</code>.
        </p>
        <pre><code>import UIKit
import StyledQrKmp

let renderer = StyledQrImageRenderer()
renderer.content = "https://example.com"

let image = renderer.render(
    sizePoints: 240,
    scale: 0
)</code></pre>
        <p>
          If rendering returns <code>nil</code>, inspect
          <code>renderer.lastRenderError</code>. See the
          <a href="https://github.com/EugenePonomarev/styled-qr-kmp#ios-uikit-and-swiftui">
            UIKit and SwiftUI guide
          </a>
          for integration examples.
        </p>
      </div>

      <div class="docs-section">
        <p class="eyebrow">03 / WEB</p>
        <h2 id="web">Generate SVG in the browser.</h2>
        <p>Install the published ES module:</p>
        <pre><code>npm install styled-qr-kmp-web@${escapeHtml(version)}</code></pre>

        <p>Create options and render a themed QR:</p>
        <pre><code>import {
  StyledQrOptions,
  renderStyledQrSvg,
} from "styled-qr-kmp-web";

const options = new StyledQrOptions();
options.themeId = "fintech";
options.errorCorrection = "H";

const result = renderStyledQrSvg(
  "https://example.com",
  options,
);

if (result.error != null) {
  throw new Error(result.error.message);
}

if (result.svg != null) {
  console.log(result.svg);
}</code></pre>
        <p>
          The result can also represent empty input without an SVG or error.
          For a complete browser example, see the
          <a href="https://github.com/EugenePonomarev/styled-qr-kmp#web--npm">
            Web quick start
          </a>.
        </p>
      </div>
    </section>

    <section class="docs-next">
      <div>
        <p class="eyebrow">NEXT STEP</p>
        <h2>Make it yours.</h2>
        <p>Explore themes and styling examples, then test the final image.</p>
      </div>
      <div class="actions">
        <a class="button" href="gallery.html">Explore the gallery →</a>
        <a class="button button-secondary" href="safety.html">Safety guide →</a>
      </div>
    </section>
""".trimIndent()

private fun safetyContent(): String = """
    <section class="page-intro docs-intro">
      <p class="eyebrow">SAFETY GUIDE</p>
      <h1>Made to look good.<br>Built to be tested.</h1>
      <p class="lead">
        A successful QR design balances visual identity with readability.
        Start with these principles and test the final image on target devices.
      </p>
      <nav class="page-nav" aria-label="Safety topics">
        <a href="#contrast">Contrast</a>
        <a href="#patterns">Function patterns</a>
        <a href="#logos">Centre logos</a>
        <a href="#testing">Testing</a>
      </nav>
    </section>

    <section class="prose docs-content">
      <div class="docs-section">
        <p class="eyebrow">01 / COLOUR &amp; SPACING</p>
        <h2 id="contrast">Keep the code clear.</h2>
        <p>
          Use a light background, a visibly darker foreground and a quiet zone
          of at least four modules. Ordinary solid foreground colours are not
          universally rejected for low contrast.
        </p>
        <div class="docs-callout">
          <strong>Gradient and finder colour validation</strong>
          <p>
            Gradients and explicitly customised finder colours must be opaque,
            darker than an opaque background and have at least a 4.5:1 contrast
            ratio.
          </p>
        </div>
      </div>

      <div class="docs-section">
        <p class="eyebrow">02 / STRUCTURE</p>
        <h2 id="patterns">Preserve the important patterns.</h2>
        <p>
          Function patterns help scanners locate and interpret the code.
          Use <code>QrFunctionPatternStyle.PreserveAll</code> when you want
          non-finder function modules to remain square.
        </p>
      </div>

      <div class="docs-section">
        <p class="eyebrow">03 / CENTRE LOGOS</p>
        <h2 id="logos">Give your logo a safe space.</h2>
        <p>
          Logo placement is aligned to whole modules. The library rejects
          placements that overlap mandatory QR patterns or exceed its
          per-block Reed–Solomon correction budget.
        </p>
        <p>
          Use error correction H for logo examples and keep the logo area
          small enough for the selected payload and QR version.
        </p>
      </div>

      <div class="docs-section">
        <p class="eyebrow">04 / REAL-WORLD CHECKS</p>
        <h2 id="testing">Scan the final result.</h2>
        <p>
          Validation does not guarantee optical readability in every situation.
          Test the final image at the intended size, on target devices and under
          realistic lighting conditions.
        </p>
      </div>
    </section>

    <section class="docs-next">
      <div>
        <p class="eyebrow">SEE IT IN PRACTICE</p>
        <h2>Explore the possibilities.</h2>
        <p>See themes, gradients, finder styles and logo examples.</p>
      </div>
      <a class="button" href="gallery.html">Explore the gallery →</a>
    </section>
""".trimIndent()

internal fun buildDocsSite(
    galleryDirectory: Path,
    outputDirectory: Path,
    stylesheet: Path,
    playgroundTemplate: Path,
    webPackageDirectory: Path,
    version: String,
) {
    require(version.isNotBlank()) {
        "Library version must not be blank"
    }

    require(Files.isRegularFile(stylesheet, LinkOption.NOFOLLOW_LINKS)) {
        "Missing site stylesheet: $stylesheet"
    }

    require(Files.isRegularFile(playgroundTemplate, LinkOption.NOFOLLOW_LINKS)) {
        "Missing playground template: $playgroundTemplate"
    }

    val webPackage = webPackageDirectory.toAbsolutePath().normalize()

    docsWebModules.forEach { fileName ->
        val source = webPackage.resolve(fileName)

        require(Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            "Missing Web module: $source"
        }
    }

    val playgroundSource = Files.readString(
        playgroundTemplate,
        StandardCharsets.UTF_8,
    )

    require(playgroundSource.split(LOCAL_PREVIEW_IMPORT).size == 2) {
        "Playground must contain exactly one local preview import"
    }

    val playgroundHtml = playgroundSource.replace(
        LOCAL_PREVIEW_IMPORT,
        SITE_PLAYGROUND_IMPORT,
    )

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

    val webOutput = output.resolve("web")
    Files.createDirectories(webOutput)

    docsWebModules.forEach { fileName ->
        Files.copy(
            webPackage.resolve(fileName),
            webOutput.resolve(fileName),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    Files.writeString(
        output.resolve("playground.html"),
        playgroundHtml,
        StandardCharsets.UTF_8,
    )
}

fun main(args: Array<String>) {
    require(args.size == 6) {
        "Usage: docs-site-generator " +
                "<gallery-directory> <output-directory> <stylesheet> " +
                "<playground-template> <web-package-directory> <version>"
    }

    buildDocsSite(
        galleryDirectory = Path.of(args[0]),
        outputDirectory = Path.of(args[1]),
        stylesheet = Path.of(args[2]),
        playgroundTemplate = Path.of(args[3]),
        webPackageDirectory = Path.of(args[4]),
        version = args[5],
    )

    println(
        "Built documentation site with " +
                "${GalleryCatalog.recipes.size} gallery cases and a QR builder.",
    )
}