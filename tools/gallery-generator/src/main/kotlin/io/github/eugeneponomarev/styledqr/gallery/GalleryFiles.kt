package io.github.eugeneponomarev.styledqr.gallery

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal fun writeGallery(
    directory: Path,
    files: Map<String, ByteArray>,
) {
    files.keys.forEach(::requireGalleryFileName)

    val outputDirectory = directory.toAbsolutePath().normalize()

    if (Files.exists(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
        check(Files.isDirectory(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
            "Gallery output must be a directory, not a file or symbolic link: $outputDirectory"
        }

        val existing = Files.list(outputDirectory).use { paths ->
            paths.iterator().asSequence().toList()
        }

        val unexpected = existing
            .map { it.fileName.toString() }
            .filterNot { it in files }
            .sorted()

        check(unexpected.isEmpty()) {
            "Unexpected gallery files: ${unexpected.joinToString()}. " +
                    "Review and remove obsolete files explicitly."
        }

        existing.forEach {
            check(Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS)) {
                "Refusing to overwrite a directory or symbolic link: ${it.fileName}"
            }
        }
    }

    val targets = files.keys.associateWith { fileName ->
        outputDirectory.resolve(fileName).normalize().also {
            check(it.parent == outputDirectory) {
                "Gallery output escapes its directory: $fileName"
            }
        }
    }

    // All path and existing-file checks finish before the first write.
    Files.createDirectories(outputDirectory)

    files.forEach { (fileName, bytes) ->
        Files.write(targets.getValue(fileName), bytes)
    }
}

internal fun verifyGallery(
    galleryDirectory: Path,
    candidateDirectory: Path,
    expectedFiles: Map<String, ByteArray>,
) {
    val gallery = galleryDirectory.toAbsolutePath().normalize()
    val candidate = candidateDirectory.toAbsolutePath().normalize()

    require(
        gallery != candidate &&
                !candidate.startsWith(gallery) &&
                !gallery.startsWith(candidate),
    ) {
        "Gallery and candidate directories must be separate and must not contain each other"
    }

    writeGallery(
        directory = candidate,
        files = expectedFiles,
    )

    val comparison = compareGallery(
        directory = gallery,
        expectedFiles = expectedFiles,
    )

    check(comparison.matches) {
        buildString {
            append("Gallery is out of date.")

            if (comparison.missing.isNotEmpty()) {
                append("\nMissing: ")
                append(comparison.missing.joinToString())
            }
            if (comparison.changed.isNotEmpty()) {
                append("\nChanged: ")
                append(comparison.changed.joinToString())
            }
            if (comparison.extra.isNotEmpty()) {
                append("\nExtra: ")
                append(comparison.extra.joinToString())
            }

            append("\nRun ./gradlew updateGallery and review the generated changes.")
            append("\nRemove obsolete files explicitly; the generator does not delete them.")
        }
    }
}