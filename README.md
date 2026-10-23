<!-- Generated from docs/templates/README.md.template. Change VERSION_NAME only in gradle.properties. -->

# Styled QR KMP

[![JitPack](https://jitpack.io/v/EugenePonomarev/styled-qr-kmp.svg)](https://jitpack.io/#EugenePonomarev/styled-qr-kmp)
[![Verify](https://github.com/EugenePonomarev/styled-qr-kmp/actions/workflows/verify.yml/badge.svg)](https://github.com/EugenePonomarev/styled-qr-kmp/actions/workflows/verify.yml)

`Styled QR KMP` is a Kotlin Multiplatform QR-code generator. Its encoding core is implemented in Kotlin and does not depend on ZXing, ML Kit, or another QR generation library.

## Install from GitHub

The public Android and JVM artifacts are distributed through JitPack. Add its repository once in
the consuming project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then choose the smallest module that provides the UI you need:

```kotlin
dependencies {
    // Android XML layouts and imperative View code:
    implementation("com.github.EugenePonomarev.styled-qr-kmp:qrcode-android-view:0.3.1")

    // Or Jetpack Compose (also brings the Android View adapter):
    implementation("com.github.EugenePonomarev.styled-qr-kmp:qrcode-compose:0.3.1")

    // Encoder, SVG renderer, and Android Bitmap renderer only:
    implementation("com.github.EugenePonomarev.styled-qr-kmp:qrcode-core:0.3.1")
}
```

Use the Git tag shown in GitHub Releases in place of `0.3.1`. JitPack builds a public tag on the
first request; its status page is linked from the repository's README badge after the first release.

### Native iOS

Every GitHub Release contains `StyledQrKmp.xcframework.zip`, built on macOS for iPhone devices
and Intel/Apple Silicon simulators. Add the extracted `StyledQrKmp.xcframework` to an Xcode target
under **Frameworks, Libraries, and Embedded Content**, then import `StyledQrKmp`.

For CocoaPods without a private token, reference the standalone podspec at the public release tag:

```ruby
pod "StyledQrKmp",
    :podspec => "https://raw.githubusercontent.com/EugenePonomarev/styled-qr-kmp/0.3.1/StyledQrKmp.podspec"
```

The standalone podspec downloads the matching XCFramework from that GitHub Release. Do not use
`:git` here: it checks out the Kotlin source repository and cannot obtain the separately published
framework archive.

Run `pod install`, then open the generated `.xcworkspace` file.

To update an existing installation, change the podspec URL to the desired
release tag and run `pod update StyledQrKmp`.

Features include:

- QR versions 1–40 and error-correction levels L, M, Q, and H;
- UTF-8 text and URLs through Byte mode with UTF-8 ECI;
- automatic version and mask selection;
- standard Reed–Solomon error correction;
- QR matrix decoding for Byte-mode payloads and ECI, with Reed–Solomon error correction;
- common SVG output with colours, circles, rounded modules, diamonds, quiet zone, and a validated central logo;
- safe QR-wide foreground linear gradients with shared validation;
- Android `Bitmap` output using only Android's `Canvas` API;
- iOS `UIImage` output using UIKit/CoreGraphics.

## UI components

The common Kotlin encoding and styling code is independent of platform UI
frameworks. The `qrcode-core` module also provides native Android and iOS
renderers. Choose the platform layer that matches the host application:

| Platform | Module / API | Use it in |
| --- | --- | --- |
| Android Views | `qrcode-android-view` / `StyledQrView` | Kotlin/Java code and XML layouts |
| Android Compose | `qrcode-compose` / `StyledQrCode` | Jetpack Compose |
| iOS UIKit | `qrcode-core` / `StyledQrImageRenderer` | UIKit code |
| iOS SwiftUI | `examples/ios/StyledQrCodeView.swift` | `UIViewRepresentable` adapter in the app target |
| Web / JavaScript | `qrcode-web` / `generateStyledQrSvg` | Browser applications bundled as ES modules |

### Android View and XML

Add the Android View module, then create `StyledQrView` exactly as any other Android view:

```kotlin
dependencies {
    implementation("com.github.EugenePonomarev.styled-qr-kmp:qrcode-android-view:0.3.1")
}

val qrView = StyledQrView(context).apply {
    setQrCode(
        content = "https://eugeneponomarev.com",
        errorCorrection = QrErrorCorrectionLevel.H,
        style = QrStyle(
            foreground = QrColor.fromHex("#14532D"),
            moduleShape = QrModuleShape.Circle,
            moduleScale = 0.92,
            functionPatternStyle = QrFunctionPatternStyle.MatchDataModules,
        ),
    )
}
```

The same component supports XML. Its `qrLogo` attribute accepts a bitmap, vector, or other Android
drawable resource. Its logo area is still validated by the shared renderer.

```xml
<io.github.eugeneponomarev.styledqr.android.StyledQrView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="240dp"
    android:layout_height="240dp"
    app:qrContent="https://eugeneponomarev.com"
    app:qrErrorCorrection="h"
    app:qrForegroundColor="#14532D"
    app:qrModuleShape="circle"
    app:qrModuleScale="0.92"
    app:qrFunctionPatternStyle="matchDataModules" />
```

A safe foreground gradient can also be configured directly from XML:

```xml
<io.github.eugeneponomarev.styledqr.android.StyledQrView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="240dp"
    android:layout_height="240dp"
    app:qrContent="https://example.com"
    app:qrGradientStartColor="#075985"
    app:qrGradientEndColor="#6D28D9"
    app:qrGradientStartX="0.0"
    app:qrGradientStartY="0.0"
    app:qrGradientEndX="1.0"
    app:qrGradientEndY="1.0" />
```

Both gradient colours must be provided together. The coordinate attributes are
optional and default to the full diagonal from `(0, 0)` to `(1, 1)`.
The same safety validation as the shared `QrStyle` API applies during inflation.

This example shows a subset of the supported XML attributes.
See `qrcode-android-view/src/main/res/values/attrs.xml` for the complete list.

`app:qrFunctionPatternStyle` is optional. When omitted, it defaults to `matchDataModules`,
matching the shared `QrStyle` default. Explicit `preserveAll` and
`preserveFindersAndAlignment` values keep their existing meanings.

`QrStyle` validates its configuration eagerly. Invalid gradient coordinates,
alpha values, or contrast throw `IllegalArgumentException` when the style is
constructed. These construction-time errors occur before rendering and therefore
are not reported through renderer properties such as `StyledQrView.lastRenderError`.

### Jetpack Compose

`qrcode-compose` is optional: it is the only module that brings an AndroidX Compose UI dependency.
It delegates to `StyledQrView`, so styling and logo-safety rules stay identical to XML and
imperative code.

```kotlin
dependencies {
    implementation("com.github.EugenePonomarev.styled-qr-kmp:qrcode-compose:0.3.1")
}

StyledQrCode(
    content = "https://eugeneponomarev.com",
    modifier = Modifier.size(240.dp),
    style = QrStyle(
        foreground = QrColor.fromHex("#143A5A"),
        moduleShape = QrModuleShape.RoundedSquare,
        moduleScale = 0.92,
    ),
)
```

### iOS UIKit and SwiftUI

The generated KMP framework is named `StyledQrKmp`. Kotlin/Native cannot make a Kotlin subclass
of `UIImageView` a usable Swift type, so the iOS public API is the Swift-importable
`StyledQrImageRenderer`. It returns a native `UIImage` that belongs in an ordinary UIKit view.

```swift
let renderer = StyledQrImageRenderer()
renderer.content = "https://eugeneponomarev.com"
imageView.image = renderer.render(sizePoints: 240, scale: 0)
```

If generation returns `nil`, inspect `renderer.lastRenderError`; a blank value simply produces no
image. The renderer keeps all QR encoding, styling, and safe-logo validation in the shared KMP
library.

For a reusable native `UIImageView` and a SwiftUI `UIViewRepresentable`, add the ready-made
`examples/ios/StyledQrCodeView.swift` file to the iOS app target, then use:

```swift
StyledQrCodeView(content: "https://eugeneponomarev.com")
    .frame(width: 240, height: 240)
```

### Web / npm

The browser adapter is available as an ES module on npm:
For the unreleased options-based API, theme overrides, safe results and explicit
PNG/SVG export dimensions, see
[Consistent output across platforms](#consistent-output-across-platforms).

```bash
npm install styled-qr-kmp-web
```

```ts
import { generateStyledQrSvg } from "styled-qr-kmp-web";

const svg = generateStyledQrSvg(
  "https://eugeneponomarev.com",
  "H",
  "#143A5A",
  "#FFFFFF",
  "rounded-square",
  0.88,
);

document.querySelector<HTMLImageElement>("#qr")!.src =
  `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
```

`errorCorrection` accepts `L`, `M`, `Q`, or `H`. Supported module shapes are
`square`, `rounded-square`, `circle`, and `diamond`.

`functionPatternStyle` is an optional seventh argument. It defaults to
`match-data-modules`, which applies the selected shape to data modules and all dark
non-finder function modules, including alignment, timing, format, version, and fixed
dark modules. Finder patterns remain composed eyes in the selected visual shape.

Use `preserve-all` to keep non-finder structural modules square, or
`preserve-finders-and-alignment` to keep alignment patterns square while timing, format,
and version modules follow the selected shape.

## Built-in themes

Seven themes are available on all rendering platforms:
Aurora, Fintech, Minimal, Neon, Wedding, Coffee, and Cyber.

```kotlin
val style = QrThemes.Fintech.createStyle()

val styleWithLogo = QrThemes.Fintech.createStyle(
    logo = QrThemes.Fintech.createLogoOptions(),
)
```

A built-in theme can also be used as the base for a custom safe gradient:
```kotlin
val gradientStyle = QrThemes.Fintech.createGradientStyle(
    gradient = QrLinearGradient(
        startColor = QrColor.fromHex("#075985"),
        endColor = QrColor.fromHex("#6D28D9"),
    ),
)
```

Web applications can select a theme directly:

```typescript
import {
  getStyledQrThemeIds,
  generateThemedQrSvg,
  generateThemedQrSvgWithLogo,
} from "styled-qr-kmp-web";

const themes = getStyledQrThemeIds();
const svg = generateThemedQrSvg("https://example.com", "fintech");

// logoDataUri contains a complete base64 PNG, JPEG, or WebP data URI.
const svgWithLogo = generateThemedQrSvgWithLogo(
  "https://example.com",
  "fintech",
  logoDataUri,
);
```

Error correction defaults to H. Pass an optional final argument to
`generateThemedQrSvg` or `generateThemedQrSvgWithLogo` to select another level.

For a custom style with a logo, use `generateStyledQrSvgWithLogo`.
All logo placements use the shared layout validation and may be rejected
when they overlap mandatory QR patterns or exceed the correction budget.

## Safe linear gradients

Safe Linear Gradients are introduced in version 0.3.0.

`QrStyle.foregroundGradient` fills dark modules and finder layers without an
explicit solid colour override with one global gradient. `foreground` remains
the solid fallback when the gradient is absent.

Coordinates range from `(0, 0)` at the top left of the QR matrix to `(1, 1)` at
the bottom right, excluding the quiet zone. The background, quiet zone, finder
middle rings, and logo background remain solid.

```kotlin
val qrCode = QrCodeGenerator.encodeText("https://example.com")

val style = QrStyle(
    background = QrColor.White,
    moduleShape = QrModuleShape.RoundedSquare,
    moduleScale = 0.90,
    foregroundGradient = QrLinearGradient(
        startColor = QrColor.fromHex("#075985"),
        endColor = QrColor.fromHex("#6D28D9"),
        start = QrGradientPoint(0.0, 0.0),
        end = QrGradientPoint(1.0, 1.0),
    ),
)

val svg = qrCode.toSvg(style)

// Android:
// qrCode.toBitmap(sizePx = 512, style = style)

// iOS:
// qrCode.toUIImage(sizePoints = 240.0, style = style)
```

Safe gradients currently use a conservative dark-on-light policy: every gradient
colour must remain darker than the background. Light-on-dark and inverse-polarity
QR gradients are intentionally rejected in version 0.3.0.

Version 0.3.0 uses a fixed conservative minimum contrast ratio of `4.5:1` for
Safe Linear Gradients. This is a library safety policy, not a claim that
ISO/IEC 18004 defines QR contrast using WCAG ratios.

Gradient validation covers every colour reachable by the shared 8-bit sRGB
interpolation model. A channel-wise upper bound proves simple gradients safe;
otherwise validation checks every interval where the rounded RGB colour is
constant and every rounding boundary. Intermediate colours can have lower
contrast than both endpoints.

Gradient colours and the background must be fully opaque.

`QrStyle` validates these rules eagerly. Invalid coordinates, alpha values, or
contrast throw `IllegalArgumentException` while the style is being constructed,
before rendering begins.

Web consumers can use the separate export without changing existing calls to
`generateStyledQrSvg`:

```ts
import {
  StyledQrLinearGradientOptions,
  generateStyledQrSvgWithLinearGradient,
} from "styled-qr-kmp-web";

const options = new StyledQrLinearGradientOptions(
  "#075985",
  "#6D28D9",
);

options.startX = 0.0;
options.startY = 0.0;
options.endX = 1.0;
options.endY = 1.0;
options.background = "#FFFFFF";
options.moduleShape = "rounded-square";
options.moduleScale = 0.90;
options.errorCorrection = "H";

const svg = generateStyledQrSvgWithLinearGradient(
  "https://example.com",
  options,
);
```

StyledQrLinearGradientOptions requires the start and end colours. Other
properties are mutable options with safe defaults: startX, startY, endX,
endY, background, moduleShape, moduleScale, functionPatternStyle,
errorCorrection, and logoDataUri.

Logo placement retains its existing safety checks. Scan the final image on target
devices before release.

### Swift compatibility

Kotlin/Native exports the `QrStyle` constructor to Swift. Swift callers
constructing `QrStyle` directly must supply all arguments in the generated
initializer, including `foregroundGradient` and `finderPatternAppearance`.

For configurable input, prefer `QrStyleConfiguration` and a safe render method.
Configuration assignments defer validation until building or rendering.

Kotlin callers can omit these constructor arguments because they have defaults.

## Quick start

```kotlin
val qrCode = QrCodeGenerator.encodeText(
    text = "https://eugeneponomarev.com",
    errorCorrection = QrErrorCorrectionLevel.H,
)

val svg = qrCode.toSvg(
    style = QrStyle(
        foreground = QrColor.fromHex("#143A5A"),
        background = QrColor.White,
        moduleShape = QrModuleShape.Circle,
        moduleScale = 0.92,
        logo = QrLogoOptions(sizeFraction = 0.16),
    ),
    logoDataUri = "data:image/png;base64,...",
)
```

On Android:

```kotlin
val bitmap = qrCode.toBitmap(
    sizePx = 1024,
    style = QrStyle(
        foreground = QrColor.fromHex("#143A5A"),
        moduleShape = QrModuleShape.RoundedSquare,
        logo = QrLogoOptions(),
    ),
    logo = myBitmap,
)
```

### Decode a QR matrix

`QrCodeDecoder` accepts an already sampled square QR module matrix without a quiet
zone. It is a matrix decoder, not an image or camera scanner: QR detection,
binarisation, and perspective correction are outside this API.

The initial decoder supports Byte-mode payloads and ECI, including UTF-8 QR codes
created by `QrCodeGenerator`.

```kotlin
val decoded = QrCodeDecoder.decode(qrCode.copyModules())

println(decoded.utf8TextOrNull())
println("Corrected codeword errors: ${decoded.correctedErrorCount}")
```

Use `copyBytes()` when the payload is binary or does not declare UTF-8 through ECI.

## Logo safety and design rules

The QR matrix always remains a square grid. The library changes how each dark
module is rendered, not where modules are located. For reliable scanning:

- use `QrFunctionPatternStyle.PreserveAll` for the most conservative output;
- use `QrFunctionPatternStyle.MatchDataModules` for a fully styled output;
- use a light background and a noticeably darker foreground;
- keep the quiet zone at four modules or more;
- use correction level `H` when placing a logo;
- call `calculateLogoLayout()` or let a renderer call it automatically before showing a logo;
- test the final bitmap or image in the devices and lighting conditions your users will have.

QR does not have a standard mechanism for leaving arbitrary central data modules unused. A logo
therefore visually overwrites a small set of encoded codeword bits. The library makes that
operation conservative: it aligns the box to whole modules, refuses to cover a mandatory QR
pattern, counts affected codewords per Reed–Solomon block, and keeps one correctable codeword as
a safety margin. If the requested logo cannot fit, rendering throws an `IllegalArgumentException`
instead of silently producing an unreliable image.

Logo images preserve their aspect ratio and fill the inner area after padding.
Excess content is cropped around the centre and clipped to the selected logo
background shape. Layout validation still covers the complete bounding square,
including padding and corners outside a curved background.

```kotlin
val layout = qrCode.calculateLogoLayout(
    QrLogoOptions(
        sizeFraction = 0.16,
    ),
)

println(
    "Logo reserve: " +
        "${layout.sizeModules}×${layout.sizeModules} modules",
)
```

The three large finder patterns are not rendered as seven-by-seven grids of dots. They are
composed from an outer 7-module shape, a 5-module background ring, and a 3-module centre. This
creates proper circular, rounded, square, or diamond QR "eyes" instead of square frames made of
individual circles.

The shared `QrStyle` default and the Web API default use `MatchDataModules`, producing a fully
styled result. Use `PreserveAll` when non-finder structural modules should remain square, and
configure `finderPatternShape` separately when using the shared Kotlin API:

```kotlin
QrStyle(
    moduleShape = QrModuleShape.Circle,
    functionPatternStyle =
        QrFunctionPatternStyle.MatchDataModules,
    finderPatternShape =
        QrFinderPatternShape.MatchModuleShape,
)
```

The current encoder intentionally starts with UTF-8 Byte mode. It creates fully valid QR codes,
but does not yet optimise a mixed numeric/alphanumeric payload into the smallest possible version.
That optimisation can be added in a later iteration without changing the public rendering API.

## Consistent output across platforms

The options-based APIs in this section are unreleased. Before publication,
consume artifacts built from this branch, including the locally packed npm
archive.

The shared `QrStyle`, `QrLogoOptions`, `QrStyleConfiguration`, encoder and
validation rules define the visual contract. Use the same payload, explicit
encoding parameters, visual settings and output geometry to request equivalent
results on Android, iOS and Web.

Bitmap, UIImage and rasterized SVG output can differ in antialiasing.
Binary equality of platform image files is not part of this contract.

### Defaults and compatibility

Existing entry points retain their signatures, defaults and historical
behaviour:

- Android's existing bitmap renderer keeps its centred integer module grid.
- Existing point-based iOS entry points keep their sizing behaviour.
- Existing Web SVG functions remain available.
- Historical custom Web logo helpers retain their QR-coloured logo backing.

For the new Web options API, settings are applied in this order:

1. Shared base defaults.
2. The selected built-in theme.
3. Explicit overrides.

Create options with the exported classes. An absent nullable override, including
`null` or `undefined`, retains the inherited value.

When a logo image is supplied:

- Without a theme, logo defaults come from `QrLogoOptions()`.
- With a theme, defaults come from that theme's `createLogoOptions()`.
- Explicit logo fields override those defaults.

The base logo defaults are:

| Property | Default |
| --- | --- |
| Size fraction | `0.18` |
| Padding fraction | `0.04` |
| Background | Opaque white |
| Background shape | Rounded square |
| Corner radius fraction | `0.18` |

The logo background is independent of the QR background. This matters on
coloured QR backgrounds.

Native callers using a theme should pass its logo options explicitly to obtain
the same themed defaults:

```kotlin
val theme = QrThemes.Fintech
val configuration = QrStyleConfiguration.fromStyle(
    theme.createStyle(logo = theme.createLogoOptions()),
)
```

A supplied native logo image without `style.logo` retains the historical
`QrLogoOptions()` fallback.

### Finder inheritance and overrides

Finder patterns remain composed 7:5:3 eyes.

- `finderPatternShape = MatchModuleShape` uses the selected module shape.
- An outer or centre layer with shape `Inherit` uses `finderPatternShape`.
- The centre layer inherits from `finderPatternShape`, independently of an
  explicit outer-layer shape.
- The middle layer follows the outer-layer shape and uses the QR background.
- A missing finder colour uses the foreground gradient, when present, or the
  solid foreground.
- An explicit finder colour replaces the gradient for that layer.

`functionPatternStyle` controls non-finder function modules. It does not replace
the finder-layer settings. An explicit `functionPatternStyle` takes precedence
over the legacy `preserveFunctionPatterns` shorthand.

Web `finderPatternShape` accepts `match-module-shape`, `square`,
`rounded-square`, `circle` and `diamond`.

`finderOuterShape` and `finderCenterShape` accept `inherit`, `square`,
`rounded-square`, `circle` and `diamond`.

To remove an inherited Web setting, use:

```typescript
options.clearForegroundGradient = true;
options.clearFinderOuterColor = true;
options.clearFinderCenterColor = true;
```

Do not combine a clear flag with a replacement override for the same setting.
Such conflicting settings return `InvalidStyle` through the safe API.

Leave `logoDataUri` absent to render without a logo image. Themes provide logo
styling defaults, not image data.

### Colours and validation

Configuration hex colours use six-digit RGB values such as `#123C66`.
Supply alpha separately through the corresponding alpha property, in `0..255`.

Transparency is supported wherever the shared model permits it. Gradients and
explicit finder colours require opaque colours and an opaque QR background,
with the existing dark-on-light and minimum `4.5:1` contrast validation.

Logo size, padding, shape and radius use the shared validation rules. Placement
also checks mandatory QR patterns and the correction budget of each affected
Reed–Solomon block.

Mutable configuration objects defer style validation to `build()` or a safe
render method. Direct `QrStyle` construction retains eager validation.

### Explicit output geometry

Output dimensions are separate from visual styling.

Native renderers accept `QrRasterRenderOptions`; Web export accepts
`StyledQrExportOptions`.

| Mode | Native value | Web value | Geometry |
| --- | --- | --- | --- |
| Integer grid | `IntegerModules` | `integer-modules` | Whole physical pixels per module; centred grid |
| Fit | `Fit` | `fit` | Fills the output side using fractional module sizes |

New output-options APIs default to the integer grid. Both modes require at
least one physical pixel per module, including the quiet zone. Insufficient
dimensions are rejected rather than enlarged.

For version 1, a 21×21 matrix, quiet zone 4 and output side 300 px:

- Total grid: 29×29 modules.
- Integer mode: 10 px per module.
- Additional outer offset: 5 px.
- Matrix origin: 45 px from the top and left.

When the remaining space is odd, the extra pixel goes to the right and bottom.

On iOS, `sizePx` is the physical pixel side. The UIImage side in points is
`sizePx / effectiveScale`. A scale of `0` selects the screen scale.

| Requested pixels | Scale | UIImage side in points |
| --- | --- | --- |
| 300 | 1 | 300 |
| 300 | 2 | 150 |
| 300 | 3 | 100 |

Web PNG export creates a Canvas bitmap of exactly the requested dimensions.
Sized SVG establishes the same geometry when rasterized at that pixel side.
Subsequent CSS scaling or browser zoom can change pixel alignment.

### Equivalent Android, Swift and TypeScript examples

These examples request the same version-4 matrix and visual settings, with
410×410 physical output pixels. Supply the same decoded logo image on each
platform. The logo may be rectangular; renderers preserve its aspect ratio,
centre-crop it and clip it to the selected shape.

Android:

```kotlin
fun renderParityQr(
    logoBitmap: android.graphics.Bitmap,
): io.github.eugeneponomarev.styledqr.android.QrBitmapRenderResult {
    val options = QrStyleConfiguration().apply {
        backgroundHex = "#FFF0CC"
        quietZoneModules = 4
        moduleShape = QrModuleShape.RoundedSquare
        moduleScale = 0.84
        roundedModuleRadiusFraction = 0.22
        functionPatternStyle = QrFunctionPatternStyle.MatchDataModules

        finderOuterShape = QrFinderPatternLayerShape.Circle
        finderCenterShape = QrFinderPatternLayerShape.Diamond
        finderOuterColorHex = "#123C66"
        finderCenterColorHex = "#0F4C5C"

        foregroundGradient = QrGradientConfiguration().apply {
            startColorHex = "#075985"
            endColorHex = "#6D28D9"
        }

        logo = QrLogoConfiguration().apply {
            sizeFraction = 0.18
            paddingFraction = 0.04
            backgroundHex = "#DDEEFF"
            backgroundShape = QrLogoBackgroundShape.RoundedSquare
            cornerRadiusFraction = 0.22
        }
    }

    return StyledQrBitmapRenderer().renderWithOptions(
        content = "x",
        options = options,
        output = QrRasterRenderOptions(
            sizePx = 410,
            sizeMode = QrRasterSizeMode.IntegerModules,
        ),
        errorCorrection = QrErrorCorrectionLevel.H,
        logo = logoBitmap,
        minVersion = 4,
        maxVersion = 4,
    )
}
```

The renderer and result are in
`io.github.eugeneponomarev.styledqr.android`, encoding types are in `.core`,
and style/output types are in `.render`.

Swift:

```swift
import UIKit
import StyledQrKmp

@MainActor
func renderParityQr(logoImage: UIImage) -> QrImageRenderResult {
    let options = QrStyleConfiguration()
    options.backgroundHex = "#FFF0CC"
    options.quietZoneModules = 4
    options.moduleShape = .roundedsquare
    options.moduleScale = 0.84
    options.roundedModuleRadiusFraction = 0.22
    options.functionPatternStyle = .matchdatamodules

    options.finderOuterShape = .circle
    options.finderCenterShape = .diamond
    options.finderOuterColorHex = "#123C66"
    options.finderCenterColorHex = "#0F4C5C"

    let gradient = QrGradientConfiguration()
    gradient.startColorHex = "#075985"
    gradient.endColorHex = "#6D28D9"
    options.foregroundGradient = gradient

    let logoOptions = QrLogoConfiguration()
    logoOptions.sizeFraction = 0.18
    logoOptions.paddingFraction = 0.04
    logoOptions.backgroundHex = "#DDEEFF"
    logoOptions.backgroundShape = .roundedsquare
    logoOptions.cornerRadiusFraction = 0.22
    options.logo = logoOptions

    return StyledQrImageRenderer().renderWithRasterOptions(
        content: "x",
        options: options,
        output: QrRasterRenderOptions(
            sizePx: 410,
            sizeMode: .integermodules
        ),
        errorCorrection: .h,
        scale: 3,
        logo: logoImage,
        minVersion: 4,
        maxVersion: 4
    )
}
```

The Swift example requests 410 physical pixels at scale 3, so its UIImage
logical side is approximately 136.67 points.

TypeScript, in a browser:

```typescript
import {
  StyledQrOptions,
  StyledQrLogoOptions,
  StyledQrGradientOptions,
  StyledQrExportOptions,
  renderStyledQrPng,
} from "styled-qr-kmp-web";
import type { StyledQrPngResult } from "styled-qr-kmp-web";

function renderParityQr(
  logoDataUri: string,
): Promise<StyledQrPngResult> {
  const options = new StyledQrOptions();
  options.errorCorrection = "H";
  options.minVersion = 4;
  options.maxVersion = 4;
  options.backgroundHex = "#FFF0CC";
  options.quietZoneModules = 4;
  options.moduleShape = "rounded-square";
  options.moduleScale = 0.84;
  options.roundedModuleRadiusFraction = 0.22;
  options.functionPatternStyle = "match-data-modules";

  options.finderOuterShape = "circle";
  options.finderCenterShape = "diamond";
  options.finderOuterColorHex = "#123C66";
  options.finderCenterColorHex = "#0F4C5C";

  options.foregroundGradient = new StyledQrGradientOptions(
    "#075985",
    "#6D28D9",
  );

  const logo = new StyledQrLogoOptions();
  logo.sizeFraction = 0.18;
  logo.paddingFraction = 0.04;
  logo.backgroundHex = "#DDEEFF";
  logo.backgroundShape = "rounded-square";
  logo.cornerRadiusFraction = 0.22;
  options.logo = logo;
  options.logoDataUri = logoDataUri;

  return renderStyledQrPng(
    "x",
    new StyledQrExportOptions(410, "integer-modules"),
    options,
  );
}
```

`logoDataUri` must contain a complete base64 PNG, JPEG or WebP image.
PNG export decodes the image before rendering.

The gradient configuration in all three examples uses its default diagonal
from `(0, 0)` to `(1, 1)`.

### Web theme composition and SVG output

Select a shared theme, then override individual settings:

```typescript
import {
  StyledQrOptions,
  StyledQrGradientOptions,
  StyledQrExportOptions,
  renderStyledQrSvg,
  renderStyledQrSvgForExport,
} from "styled-qr-kmp-web";

const options = new StyledQrOptions();
options.themeId = "fintech";
options.foregroundGradient = new StyledQrGradientOptions(
  "#075985",
  "#6D28D9",
);
options.finderOuterShape = "circle";
options.finderCenterColorHex = "#0F4C5C";

const scalable = renderStyledQrSvg("https://example.com", options);
const sized = renderStyledQrSvgForExport(
  "https://example.com",
  new StyledQrExportOptions(300, "integer-modules"),
  options,
);
```

Themes are resolved through the shared `QrThemes`; their definitions are not
duplicated in JavaScript.

### Safe results and error handling

| Platform | Safe entry point | Output property |
| --- | --- | --- |
| Android | `StyledQrBitmapRenderer.renderWithOptions` | `bitmap` |
| iOS | `StyledQrImageRenderer.renderWithRasterOptions` | `image` |
| Web SVG | `renderStyledQrSvg`, `renderStyledQrSvgForExport` | `svg` |
| Web PNG | `renderStyledQrPng` | `dataUri` |

Each result distinguishes:

- Success: `isSuccess` is true, with output and no error.
- Empty: `isEmpty` is true, with no output and no error.
- Failure: both flags are false, with an error containing `code` and `message`.

Valid blank input, including whitespace, returns an empty result from these
high-level APIs. Style and independent generation/output parameters are
validated before the blank-input check. Payload-dependent checks, including
logo placement and image decoding, are skipped for an empty result.

| Error category | Meaning |
| --- | --- |
| `InvalidStyle` | Invalid visual configuration, colour, gradient or contrast |
| `InvalidRenderParameters` | Invalid dimensions, scale, version bounds or generation options |
| `PayloadTooLong` | Payload does not fit the allowed versions and correction level |
| `LogoValidationFailed` | Invalid Web logo data, undecodable PNG-export logo or unsafe placement |
| `RenderFailed` | Other rendering or backend failure |

Native results expose `QrRenderErrorCode`; Web results expose its entry name
as a string. Swift imports the enum cases using names such as `.invalidstyle`
and `.payloadtoolong`.

For example, handle a browser PNG result explicitly:

```typescript
const result = await renderParityQr(logoDataUri);

if (result.error) {
  console.error(result.error.code, result.error.message);
} else if (result.isEmpty) {
  imageElement.removeAttribute("src");
} else if (result.dataUri != null) {
  imageElement.src = result.dataUri;
}
```

SVG generation validates logo data-URI syntax and shared placement safety.
The browser decodes embedded images when displaying the SVG. A successful SVG
result alone does not confirm that embedded image bytes are decodable.

Successful PNG export requires browser Canvas 2D support. Encoding, matrix
decoding and SVG generation are also usable from Node.js.

### Web binary encoding, matrices and decoding

Low-level encoding accepts empty payloads. Text encoding adds UTF-8 ECI 26;
binary encoding adds no implicit character-set declaration.

```typescript
import {
  StyledQrEncodingOptions,
  StyledQrExportOptions,
  encodeStyledQrText,
  encodeStyledQrBytes,
  decodeStyledQrMatrix,
} from "styled-qr-kmp-web";

const encoding = new StyledQrEncodingOptions();
encoding.errorCorrection = "H";
encoding.minVersion = 1;
encoding.maxVersion = 40;

const textMatrix = encodeStyledQrText("Привет", encoding);

const binaryMatrix = encodeStyledQrBytes(
  Array.from(new Uint8Array([0, 127, 128, 255])),
  encoding,
);

const svg = binaryMatrix.toSvg(
  new StyledQrExportOptions(300),
);

const decoded = decodeStyledQrMatrix(textMatrix.copyModules());

console.log(decoded.utf8TextOrNull());
console.log(decoded.copyBytes());
console.log(decoded.version, decoded.errorCorrection, decoded.mask);
console.log(decoded.eciAssignment, decoded.correctedErrorCount);
```

`binaryMatrix.toSvg(output, options)` renders the existing matrix.
Only visual settings from `options` apply; `errorCorrection`, `minVersion`
and `maxVersion` do not re-encode it. Set generation parameters through
`StyledQrEncodingOptions` when creating the matrix.

Matrices expose `version`, the actual selected `errorCorrection`, `size`,
`get(row, column)`, `isFunctionModule`, `isLocatorModule` and `copyModules`.
The encoder may upgrade the requested correction level when capacity allows.

Module arrays are defensive `boolean[][]` copies without a quiet zone.
Byte arrays use JavaScript `number[]` with integer values in `0..255`;
convert typed arrays with `Array.from`.

The decoder accepts prepared square matrices of 21, 25, ..., 177 modules.
It supports the shared Byte/ECI modes and correctable codeword errors.
Camera detection, photograph processing and perspective correction remain
outside this API.

`utf8TextOrNull()` returns text only when ECI 26 declares UTF-8.
Use `copyBytes()` for binary data or other ECI semantics.

Low-level encoding, decoding and matrix rendering methods throw on invalid
input, following the shared core API. The safe render methods above return
categorized results.

### Android XML logo backing

The XML adapter also supports an independent logo background, shape and radius:

```xml
<io.github.eugeneponomarev.styledqr.android.StyledQrView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="240dp"
    android:layout_height="240dp"
    app:qrContent="https://eugeneponomarev.com"
    app:qrErrorCorrection="h"
    app:qrBackgroundColor="#FFF0CC"
    app:qrLogo="@drawable/logo"
    app:qrLogoSizeFraction="0.18"
    app:qrLogoPaddingFraction="0.04"
    app:qrLogoBackgroundColor="#DDEEFF"
    app:qrLogoBackgroundShape="roundedSquare"
    app:qrLogoCornerRadiusFraction="0.22" />
```

Background shapes are `square`, `roundedSquare` and `circle`.
Android XML colour literals use Android's colour syntax, including
`#AARRGGBB` when alpha is supplied.

## Build

Use JDK 17 and Gradle 8.13. The repository includes the Gradle Wrapper:

```bash
./gradlew \
  :qrcode-core:jvmTest \
  :qrcode-core:testDebugUnitTest \
  :qrcode-core:jsBrowserTest \
  :qrcode-web:jsBrowserTest \
  :qrcode-android-view:testDebugUnitTest \
  :qrcode-compose:assembleDebug

./gradlew \
  :qrcode-core:apiCheck \
  :qrcode-android-view:apiCheck \
  :qrcode-compose:apiCheck \
  :qrcode-web:verifyJsNpmPackage \
  :qrcode-web:packJsPackage
npm --prefix tests/web-typescript test
./gradlew checkVersionedDocs
```

The TypeScript consumer installs the packed npm archive into a temporary
workspace, compiles with strict type checking, and runs API checks against
the installed package.

Frozen parity fixtures are shared by the core tests, Web tests and TypeScript
consumer. They cover complete matrices, function and locator markers, and
encoding/decoding metadata.

Regenerate fixtures with `node scripts/update-qr-parity-fixtures.mjs` only
when intentionally updating the recorded encoding contract. Build the Web
package first and review the generated changes. Normal tests and CI do not
regenerate fixtures.

iOS rendering and Swift interoperability require macOS and Xcode. Run the
repository's macOS verification job in addition to the checks above.

The QR encoder, QR matrix decoder, SVG renderer, Android View, and UIKit renderer have no
third-party QR-generation or QR-decoding dependency. Gradle and the Kotlin
Multiplatform/Android plugins are build tools; the optional `qrcode-compose` module adds AndroidX
Compose UI solely to expose a Compose API.

## Release a new version

1. Set `VERSION_NAME` in `gradle.properties`.
2. Run `./gradlew updateVersionedDocs` to regenerate `README.md`
   and `StyledQrKmp.podspec`.
3. Update `CHANGELOG.md` and include the source changes, templates,
   and generated documentation in a pull request.
4. Merge after the `Verify` workflow succeeds, then wait for verification
   of the resulting `main` commit.
5. Create and push a matching tag, such as `0.3.1`,
   from that verified commit. The tag must not have a `v` prefix.

Pushing the tag starts `.github/workflows/release.yml`:

- `native-ios` builds the XCFramework, checks the Swift API, creates
  the GitHub Release, and validates the published CocoaPods artifact.
- After `native-ios` succeeds, `npm-web` tests and packages the Web adapter
  and publishes `styled-qr-kmp-web` to npm.

Automatic npm publication requires a Trusted Publisher configured for
`EugenePonomarev/styled-qr-kmp`, workflow `release.yml`,
with direct `npm publish` allowed.

JitPack builds the Android/JVM artifacts from the same public tag
when that version is first requested.
