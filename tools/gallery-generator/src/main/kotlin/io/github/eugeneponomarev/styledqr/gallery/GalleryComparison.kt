package io.github.eugeneponomarev.styledqr.gallery

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal data class GalleryComparison(
    val missing: List<String>,
    val changed: List<String>,
    val extra: List<String>,
) {
    val matches: Boolean
        get() = missing.isEmpty() && changed.isEmpty() && extra.isEmpty()
}

internal fun compareGallery(
    directory: Path,
    expectedFiles: Map<String, ByteArray>,
): GalleryComparison {
    expectedFiles.keys.forEach(::requireGalleryFileName)

    if (Files.notExists(directory, LinkOption.NOFOLLOW_LINKS)) {
        return GalleryComparison(
            missing = expectedFiles.keys.sorted(),
            changed = emptyList(),
            extra = emptyList(),
        )
    }

    require(Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
        "Gallery path must be a directory, not a file or symbolic link: $directory"
    }

    val actualNames = Files.list(directory).use { paths ->
        paths.iterator()
            .asSequence()
            .map { it.fileName.toString() }
            .toSet()
    }

    val expectedNames = expectedFiles.keys

    val changed = expectedNames.intersect(actualNames)
        .filter { fileName ->
            val file = directory.resolve(fileName)

            !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) ||
                    !Files.readAllBytes(file).contentEquals(expectedFiles.getValue(fileName))
        }
        .sorted()

    return GalleryComparison(
        missing = (expectedNames - actualNames).sorted(),
        changed = changed,
        extra = (actualNames - expectedNames).sorted(),
    )
}
