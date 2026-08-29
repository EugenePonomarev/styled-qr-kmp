# Changelog

## 0.2.3 — Fix JitPack Android publishing for 0.2.3

## 0.2.3 — Fix iOS publishing for 0.2.2

## 0.2.1 — First public release

- Added public Maven publication metadata, JitPack Android/JVM installation, and source JARs.
- Added a macOS GitHub Release workflow that exports the native iOS XCFramework and a CocoaPods
  specification that downloads the matching release asset.

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
