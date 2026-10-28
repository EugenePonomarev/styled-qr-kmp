package io.github.eugeneponomarev.styledqr.gallery

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

private val SITE_PAGES = listOf(
    "index.html",
    "gallery.html",
    "getting-started.html",
    "safety.html",
)

private val LINK_ATTRIBUTE = Regex(
    "\\b(?:href|src)\\s*=\\s*\"([^\"]*)\"",
    RegexOption.IGNORE_CASE,
)

private val ID_ATTRIBUTE = Regex(
    "\\bid\\s*=\\s*\"([^\"]+)\"",
    RegexOption.IGNORE_CASE,
)

private val UNRESOLVED_PLACEHOLDER = Regex("\\{\\{[^{}]+}}")

internal fun checkDocsSite(siteDirectory: Path) {
    val root = siteDirectory.toAbsolutePath().normalize()

    require(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
        "Site directory does not exist: $root"
    }

    val expectedFiles = (
            SITE_PAGES +
                    "style.css" +
                    "gallery/manifest.json" +
                    GalleryCatalog.recipes.map { "gallery/${it.fileName}" }
            ).toSet()

    val actualFiles = Files.walk(root).use { paths ->
        paths
            .filter { it != root }
            .map { path ->
                require(!Files.isSymbolicLink(path)) {
                    "Symbolic links are not allowed in the site: $path"
                }

                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    null
                } else {
                    require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        "Unexpected site entry: $path"
                    }

                    root.relativize(path)
                        .toString()
                        .replace('\\', '/')
                }
            }
            .filter { it != null }
            .map { requireNotNull(it) }
            .toList()
            .toSet()
    }

    val missingFiles = (expectedFiles - actualFiles).sorted()
    val extraFiles = (actualFiles - expectedFiles).sorted()

    require(missingFiles.isEmpty() && extraFiles.isEmpty()) {
        "Unexpected site file set. Missing: $missingFiles; extra: $extraFiles"
    }

    val htmlByPage = SITE_PAGES.associateWith { page ->
        Files.readString(root.resolve(page), StandardCharsets.UTF_8)
    }

    val idsByPage = htmlByPage.mapValues { (_, html) ->
        ID_ATTRIBUTE.findAll(html)
            .map { it.groupValues[1] }
            .toSet()
    }

    htmlByPage.forEach { (page, html) ->
        require(!UNRESOLVED_PLACEHOLDER.containsMatchIn(html)) {
            "Unresolved placeholder in $page"
        }

        LINK_ATTRIBUTE.findAll(html).forEach { match ->
            val link = match.groupValues[1]

            require(link.isNotBlank()) {
                "Empty link in $page"
            }

            val uri = try {
                URI(link)
            } catch (cause: Exception) {
                throw IllegalArgumentException(
                    "Invalid link in $page: $link",
                    cause,
                )
            }

            // External HTTPS links are outside this local file check.
            if (uri.isAbsolute) {
                require(uri.scheme.equals("https", ignoreCase = true)) {
                    "Unsupported external link in $page: $link"
                }
                return@forEach
            }

            require(uri.rawAuthority == null) {
                "Protocol-relative links are not supported in $page: $link"
            }
            require(uri.rawQuery == null) {
                "Local links with query parameters are not supported in $page: $link"
            }

            val relativePath = uri.path.orEmpty()

            require(!relativePath.startsWith("/")) {
                "Local links must be relative in $page: $link"
            }

            val sourcePage = root.resolve(page)
            val target = if (relativePath.isEmpty()) {
                sourcePage
            } else {
                sourcePage.parent.resolve(relativePath).normalize()
            }

            require(target.startsWith(root)) {
                "Local link escapes the site in $page: $link"
            }
            require(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                "Missing local link target in $page: $link"
            }

            val fragment = uri.fragment
            if (!fragment.isNullOrEmpty()) {
                val targetName = root.relativize(target)
                    .toString()
                    .replace('\\', '/')

                require(fragment in idsByPage[targetName].orEmpty()) {
                    "Missing anchor in $page: $link"
                }
            }
        }
    }

    println(
        "Documentation site check passed: " +
                "${SITE_PAGES.size} pages, " +
                "${GalleryCatalog.recipes.size} gallery cases.",
    )
}

fun main(args: Array<String>) {
    require(args.size == 1) {
        "Usage: docs-site-check <site-directory>"
    }

    checkDocsSite(Path.of(args[0]))
}