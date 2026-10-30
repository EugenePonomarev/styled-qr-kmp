# Changelog

## [0.5.5] — Scanability reports

### Added

- Add `QrScanabilityReport` to analyze a QR matrix and style, returning typed errors, warnings and available measurements for contrast, output geometry and centre-logo correction budgets.
- Add Swift and Web entry points for scanability analysis, with nullable measurements and structured style errors.
- Show scanability checks in the QR Builder, including contrast warnings and matrix information.
- Add a reproducible SVG gallery, a documentation site and an interactive QR Builder with platform-specific code examples.

### Changed

- Use canonical numeric serialization in SVG output. Generated SVG text can differ byte-for-byte from earlier versions.
- Reuse a shared typed logo-layout analysis without changing the existing public logo layout contract.

### Testing and limitations

- Add common, Android, JVM, JS, Swift and TypeScript tests for scanability, report adapters and the builder.
- Scanability checks do not guarantee camera readability or successful platform rendering. The report does not analyze logo image contents or the contrast of an opaque logo background.

## [0.5.0]

### Added

- Branded Finder Patterns 1.0: independent shapes and colours for outer and centre finder-pattern layers.
- Safe validation for custom finder-pattern colours, including opacity, darkness and minimum contrast requirements.
- Cross-platform finder-pattern support for Android, iOS and Web.
- Safe rendering APIs and explicit raster export options across supported platforms.
- Cross-platform parity fixtures and expanded Android, Web/TypeScript, Kotlin iOS and Swift test coverage.

### Changed

- Finder-pattern rendering now consistently preserves QR 7:5:3 geometry on all supported platforms.
- Gradient inheritance is consistently applied to dark finder-pattern layers when no explicit solid colour is configured.

## 0.4.0 — Safe iOS configuration and rendering

### Added

- Add mutable `QrStyleConfiguration`, `QrLogoConfiguration`, and
  `QrGradientConfiguration` inputs with validated `QrStyleBuildResult` output.
- Add `StyledQrImageRenderer.renderWithOptions` and `QrImageRenderResult`
  so Swift callers can distinguish a rendered image, empty content, and a failure.
- Report structured errors through `QrRenderError` and `QrRenderErrorCode` for
  invalid styles, invalid render parameters, oversized payloads, unsafe logo
  layouts, and rendering failures.
- Expose `lastRenderErrorCode` alongside the existing renderer error message.

### Changed

- Validate iOS image dimensions and scale before rendering.
- Update the UIKit renderer and SwiftUI adapter to handle safe configuration
  results and clear stale error state between renders.

### Tests

- Add shared configuration and image-parameter validation tests, plus iOS
  renderer regression coverage.
- Add a local Swift Package with XCTest integration checks and simulator
  execution in verification and release workflows.
- Upload the verified XCFramework, source commit, checksum, and iOS test
  reports as CI artifacts.

### Compatibility

- Existing rendering entry points and default QR appearance remain supported.
- Direct style constructors retain their existing validation behaviour;
  use `QrStyleConfiguration` for structured validation errors from Swift.
- Android and Web rendering APIs are unchanged.

## 0.3.1 — Encoding and rendering fixes

### Fixed

- Correct QR error-correction codewords per block for affected versions
  and correction levels.
- Report oversized payloads as IllegalArgumentException, allowing Android
  View to display the error and recover after input changes.
- Preserve shared Double defaults when Android XML attributes are omitted.
- Match logo clearance and image clipping to the selected background shape.
- Preserve rectangular logo proportions using centred cropping across renderers.
- Avoid eagerly recycling cached Android View bitmaps.

### Tests

- Add independent codeword fixtures covering all 40 versions and four
  error-correction levels.
- Add regression coverage for overflow recovery, XML defaults, logo clipping,
  and rectangular logo cropping.

## 0.3.0 — Safe linear gradients

### Added

- Add an opaque, contrast-validated foreground gradient to `QrStyle`.
- Use one QR-wide gradient across dark data modules and finder layers
  in SVG, Android Bitmap, and iOS UIImage.
- Export `generateStyledQrSvgWithLinearGradient` for Web.
- Add Android XML attributes for foreground gradients.
- Add `QrTheme.createGradientStyle` without changing existing theme output.
- Export `StyledQrLinearGradientOptions` for the Web gradient API.
- Add pixel-level iOS gradient renderer tests.

### Changed

- Use deterministic SVG gradient identifiers to prevent different inline QR
  gradients from colliding in the same document.
- Explicitly render SVG gradients using sRGB colour interpolation.
- Validate every 8-bit interpolated foreground colour using a channel-wise
  upper bound and exact rounding boundaries, including simultaneous ties.

### Compatibility

- Kotlin callers remain source-compatible because
  `QrStyle.foregroundGradient` defaults to `null`.
- Swift callers that construct `QrStyle` directly must now provide
  the additional `foregroundGradient` initializer argument, usually
  `nil`. `StyledQrImageRenderer` callers that do not construct
  `QrStyle` directly are unaffected.

## 0.2.9 — Themes, web logos and rendering improvements

- Add seven shared visual themes: Aurora, Fintech, Minimal, Neon,
  Wedding, Coffee, and Cyber.
- Export the theme catalog and themed SVG generation to JavaScript.
- Add themed and custom SVG generation with embedded logos.
- Keep error correction H as the default for web generation.
- Apply theme defaults to logo size, padding, and background.
- Add QR matrix decoding with UTF-8 ECI and Reed–Solomon correction.
- Correct the medium-level QR block layout and expand conformance tests.
- Align Android raster modules to whole pixels.
- Make function-pattern styling consistent across renderers and Android XML.
- Add automatic npm publishing from release tags.

## 0.2.8 — Correct npm package publication

- Package `styled-qr-kmp-web` as a complete publishable ES module.
- Include JavaScript modules, TypeScript definitions, README, and MIT license in the npm tarball.
- Verify npm packaging in CI with Gradle configuration-cache support.

## 0.2.7 — Web / npm package

- Add the `qrcode-web` Kotlin/JS module and publishable ES-module package
  `styled-qr-kmp-web`.
- Export `generateStyledQrSvg` for browser applications.
- Add browser API tests and production Webpack verification.
- Document npm installation and browser usage.

## 0.2.6 — Swift-importable iOS rendering API

- Add StyledQrImageRenderer, a public Kotlin/Native type that returns a native UIImage to
  Swift/UIKit without exposing a Kotlin subclass of UIImageView.
- Replace the non-compilable SwiftUI example with a native Swift UIImageView and
  UIViewRepresentable wrapper around StyledQrImageRenderer.
- Hide the old Kotlin StyledQrView : UIImageView implementation from the Swift framework API.
- Add a macOS CI smoke test that imports the built XCFramework from Swift and compiles the public
  StyledQrImageRenderer call site.

## 0.2.5 — CocoaPods remote installation repair

- Fix the incompatibility between a `:git` Podfile dependency and an XCFramework published only
  as a GitHub Release asset.
- Generate a standalone `StyledQrKmp.podspec` from a template using `VERSION_NAME` in
  `gradle.properties`; it can be loaded through a raw GitHub podspec URL.
- Update the README to use `:podspec` rather than `:git` for CocoaPods installation.
- Make `updateVersionedDocs` and `checkVersionedDocs` compatible with Gradle Configuration Cache.
- Validate the standalone podspec and published XCFramework in the release workflow.

## 0.2.4 — Swift iOS renderer and release metadata

- Align the release tag, Gradle version, and CocoaPods release metadata.

## 0.2.3 — JitPack Android repair

- Align every Android module with compile SDK 36, the supported level for the current Android
  Gradle Plugin and JitPack build image.

## 0.2.2 — iOS publishing repair

- Fixed Kotlin/Native UIKit opt-ins and the iOS image-view content mode used by `StyledQrView`.
- Added a macOS GitHub Release workflow that exports the native iOS XCFramework.
- Added the XCFramework build to the normal GitHub Actions verification workflow.

## 0.2.1 — First public release

- Added public Maven publication metadata, JitPack Android/JVM installation, and source JARs.
- Added the initial CocoaPods specification for the iOS XCFramework release asset.

## 0.2.0 — Platform UI components

- Added `StyledQrView`, a native Android `View` with code and XML APIs, cached rendering, XML
  style attributes, and optional drawable logo support.
- Added the optional `qrcode-compose` module with a `StyledQrCode` composable backed by the same
  Android view and renderer.
- Added native iOS `StyledQrView` based on `UIImageView`, plus a SwiftUI `UIViewRepresentable`
  adapter example.
- Configured the KMP iOS framework name as `StyledQrKmp`.

## 0.1.5 — Composed finder patterns

- Render the three large finder patterns as composed 7:5:3 eyes rather than grids of individual
  modules.
- Match each eye to the selected square, rounded-square, circle, or diamond style.
- Added the same finder rendering to Android, SVG, and iOS renderers.

## 0.1.4 — Styled timing line

- Added a selectable function-pattern treatment for renderers.

## 0.1.3 — Safe logo reservation

- Keep mandatory finder, alignment, timing, format, and version patterns square when requested by
  the renderer style.
- Calculate the centre-logo box on whole QR modules rather than arbitrary pixels.
- Omit rendered modules below the logo box and validate the resulting overwrite against the
  Reed–Solomon correction capacity of every QR block.
- Reject logo placements that cover mandatory QR patterns or exceed the safe correction budget.

## 0.1.2 — JVM target alignment

- Aligned Android Java compilation with Kotlin's JVM 17 target in both modules.

## 0.1.0 — Initial version

- Added a dependency-free Kotlin QR encoder for versions 1–40.
- Added L, M, Q, and H error correction with Reed–Solomon codewords.
- Added UTF-8 text support through ECI and raw byte support.
- Added common SVG, Android `Bitmap`, and iOS `UIImage` renderers.
- Added module shapes, colours, quiet zones, and central logo overlays.
