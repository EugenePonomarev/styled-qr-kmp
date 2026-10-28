package io.github.eugeneponomarev.styledqr.gallery

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrStyle
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.Base64

internal data class RenderedGalleryCase(
    val recipe: GalleryRecipe,
    val qrCode: QrCode,
    val style: QrStyle,
    val svg: String,
) {
    fun svgBytes(): ByteArray = svg.toByteArray(StandardCharsets.UTF_8)
}

internal data class GeneratedGallery(
    val cases: List<RenderedGalleryCase>,
    val files: Map<String, ByteArray>,
)

internal fun renderGalleryCase(
    recipe: GalleryRecipe,
    logoInputsDirectory: Path? = null,
): RenderedGalleryCase {
    try {
        val qrCode = QrCodeGenerator.encodeText(
            text = recipe.payload,
            errorCorrection = recipe.requestedErrorCorrection,
            minVersion = recipe.minVersion,
            maxVersion = recipe.maxVersion,
        )
        val style = recipe.createStyle()
        val logoDataUri = readLogoDataUri(recipe, logoInputsDirectory)

        require((style.logo != null) == (logoDataUri != null)) {
            "Logo style and image must be supplied together"
        }

        val rasterOptions = recipe.rasterOptions
        val svg = if (rasterOptions == null) {
            SvgQrRenderer.render(
                qrCode = qrCode,
                style = style,
                logoDataUri = logoDataUri,
            )
        } else {
            SvgQrRenderer.renderWithRasterOptions(
                qrCode = qrCode,
                options = rasterOptions,
                style = style,
                logoDataUri = logoDataUri,
            )
        }

        return RenderedGalleryCase(
            recipe = recipe,
            qrCode = qrCode,
            style = style,
            svg = svg,
        )
    } catch (cause: Exception) {
        throw IllegalStateException(
            "Gallery case '${recipe.id}' failed: ${cause.message}",
            cause,
        )
    }
}

internal fun generateGallery(
    logoInputsDirectory: Path,
    recipes: List<GalleryRecipe> = GalleryCatalog.recipes,
): GeneratedGallery {
    require(recipes.isNotEmpty()) {
        "Gallery catalogue must not be empty"
    }
    require(recipes.map { it.id }.distinct().size == recipes.size) {
        "Gallery case IDs must be unique"
    }

    val cases = recipes.map {
        renderGalleryCase(it, logoInputsDirectory)
    }

    val files = linkedMapOf<String, ByteArray>()
    cases.forEach {
        files[it.recipe.fileName] = it.svgBytes()
    }
    files["manifest.json"] = renderGalleryManifest(cases)
        .toByteArray(StandardCharsets.UTF_8)

    return GeneratedGallery(
        cases = cases,
        files = files,
    )
}

private fun readLogoDataUri(
    recipe: GalleryRecipe,
    logoInputsDirectory: Path?,
): String? {
    val source = recipe.logoSource ?: return null
    requireGallerySvgFileName(source)

    val directory = requireNotNull(logoInputsDirectory) {
        "Logo inputs directory is required"
    }.toAbsolutePath().normalize()

    val file = directory.resolve(source).normalize()

    require(file.parent == directory) {
        "Logo source escapes its input directory"
    }
    require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
        "Logo input must be a regular file: $source"
    }

    val bytes = Files.readAllBytes(file)
    require(bytes.isNotEmpty()) {
        "Logo input is empty: $source"
    }

    val text = bytes.toString(StandardCharsets.UTF_8)
    require(!text.startsWith("\uFEFF") && '\r' !in text) {
        "Logo input must use UTF-8 without BOM and LF line endings: $source"
    }

    return "data:image/svg+xml;base64," +
            Base64.getEncoder().encodeToString(bytes)
}

fun main(args: Array<String>) {
    require(
        args.isNotEmpty() &&
                (
                        (args[0] == "update" && args.size == 3) ||
                                (args[0] == "check" && args.size == 4)
                        ),
    ) {
        "Usage: gallery-generator update <logo-inputs> <output-directory>\n" +
                "   or: gallery-generator check <logo-inputs> <gallery-directory> <candidate-directory>"
    }

    val generated = generateGallery(Path.of(args[1]))

    when (args[0]) {
        "update" -> writeGallery(
            directory = Path.of(args[2]),
            files = generated.files,
        )

        "check" -> verifyGallery(
            galleryDirectory = Path.of(args[2]),
            candidateDirectory = Path.of(args[3]),
            expectedFiles = generated.files,
        )
    }

    generated.cases.forEach {
        println(
            "${it.recipe.id}: " +
                    "version=${it.qrCode.version}, " +
                    "ecc=${it.qrCode.errorCorrection}, " +
                    "matrix=${it.qrCode.size}",
        )
    }
}