# Changelog

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
