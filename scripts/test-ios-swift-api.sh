#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
xcframework_path="${1:-$repo_root/qrcode-core/build/XCFrameworks/release/StyledQrKmp.xcframework}"
package_dir="$repo_root/tests/ios-swift"
output_root="$repo_root/build/ios-swift-tests"

test -d "$xcframework_path"
mkdir -p "$package_dir/Frameworks" "$output_root"
run_dir="$(mktemp -d "$output_root/run.XXXXXX")"

ditto "$xcframework_path" "$package_dir/Frameworks/StyledQrKmp.xcframework"
xcodebuild -version > "$run_dir/xcode-version.txt"
xcrun simctl list devices available --json > "$run_dir/simulators.json"

simulator_id="$(python3 - "$run_dir/simulators.json" <<'PY'
import json
import re
import sys

with open(sys.argv[1]) as source:
    devices = json.load(source)["devices"]

runtimes = sorted(
    (key for key in devices if ".iOS-" in key),
    key=lambda key: tuple(int(part) for part in re.findall(r"\d+", key)),
    reverse=True,
)

for runtime in runtimes:
    phones = [
        device for device in devices[runtime]
        if device.get("isAvailable", False)
        and device["name"].startswith("iPhone")
    ]
    phones.sort(key=lambda device: device.get("state") != "Booted")
    if phones:
        print(phones[0]["udid"])
        break
else:
    raise SystemExit(
        "No available iPhone simulator. Install an iOS runtime in Xcode."
    )
PY
)"

echo "Running Swift XCTest on simulator $simulator_id"
echo "Results: $run_dir"

cd "$package_dir"

xcodebuild \
    -scheme StyledQrSwiftChecks \
    -configuration Debug \
    -destination "platform=iOS Simulator,id=$simulator_id" \
    -destination-timeout 180 \
    -derivedDataPath "$repo_root/build/ios-swift-derived-data" \
    -resultBundlePath "$run_dir/SwiftApi.xcresult" \
    CODE_SIGNING_ALLOWED=NO \
    test 2>&1 | tee "$run_dir/xcodebuild.log"
