# Gallery generator

Private JVM tooling for the SVG gallery. This module is not published.

Run commands from the repository root:

```shell
./gradlew :gallery-generator:test
./gradlew updateGallery
./gradlew checkGallery
```

Recipes are defined in
[src/main/kotlin/io/github/eugeneponomarev/styledqr/gallery/GalleryCatalog.kt](src/main/kotlin/io/github/eugeneponomarev/styledqr/gallery/GalleryCatalog.kt).

`updateGallery` writes the expected SVG files and generated manifest to
`docs/gallery`. Unexpected files cause an error and must be reviewed and
removed explicitly.

`checkGallery` generates a candidate under this module's `build/gallery-check`
directory and compares the complete gallery file set and bytes. It does not
update `docs/gallery`.

For an independent build-directory preview:

```shell
./gradlew :gallery-generator:generateGalleryPreview
```

Logo inputs must use UTF-8 without BOM and LF line endings. The SVG logo is
embedded as a data URI without changing its source bytes.

The manifest records encoding inputs, actual encoding metadata, theme ID,
the resulting public QrStyle fields, logo source/layout and raster options.
Colours are RGBA arrays. Finder Inherit values retain the public fallback
through finderPatternShape and moduleShape.

Both raster sizing examples are SVG exports with explicit pixel geometry.

Matrix decoding tests verify encoded payloads. They do not prove optical
readability of the styled SVG under arbitrary scanning conditions.