#!/usr/bin/env bash

set -euo pipefail

xcframework_path="${1:-qrcode-core/build/XCFrameworks/release/StyledQrKmp.xcframework}"
swift_source="scripts/StyledQrKmpSmoke.swift"

test -f "$swift_source"
test -d "$xcframework_path"

simulator_slice=""
for candidate in "$xcframework_path"/ios-*-simulator; do
    if test -d "$candidate"; then
        simulator_slice="$candidate"
        break
    fi
done
test -n "$simulator_slice"

framework_path="$simulator_slice/StyledQrKmp.framework"
header_path="$framework_path/Headers/StyledQrKmp.h"
test -f "$header_path"

grep -Fq 'swift_name("StyledQrImageRenderer")' "$header_path"
if grep -Fq 'swift_name("StyledQrView")' "$header_path"; then
    echo "StyledQrView must not be exported to Swift; use StyledQrImageRenderer instead." >&2
    exit 1
fi

case "$(uname -m)" in
    arm64) swift_target="arm64-apple-ios13.0-simulator" ;;
    x86_64) swift_target="x86_64-apple-ios13.0-simulator" ;;
    *)
        echo "Unsupported macOS architecture: $(uname -m)" >&2
        exit 1
        ;;
esac

output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT

sdk_path="$(xcrun --sdk iphonesimulator --show-sdk-path)"

xcrun --sdk iphonesimulator swiftc \
    -target "$swift_target" \
    -sdk "$sdk_path" \
    -F "$simulator_slice" \
    -framework StyledQrKmp \
    -c "$swift_source" \
    -o "$output_dir/StyledQrKmpSmoke.o"
