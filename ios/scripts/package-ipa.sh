#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
app="build/device/Build/Products/Release-iphoneos/CuboxChill.app"
test -f "$app/Info.plist"
test -f "$app/CuboxChill"
platform=$(/usr/libexec/PlistBuddy -c 'Print :DTPlatformName' "$app/Info.plist")
test "$platform" = iphoneos
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/Payload" build/distribution
ditto "$app" "$stage/Payload/CuboxChill.app"
output="$PWD/build/distribution/cubox-chill-iphone-preview.ipa"
(cd "$stage" && ditto -c -k --keepParent Payload "$output")
python3 - "$output" <<'PY'
import sys, zipfile, plistlib
with zipfile.ZipFile(sys.argv[1]) as archive:
    names = archive.namelist()
    assert all(name.startswith('Payload/') for name in names), 'Unexpected IPA root'
    prefix = 'Payload/CuboxChill.app/'
    info = plistlib.loads(archive.read(prefix + 'Info.plist'))
    assert info['DTPlatformName'] == 'iphoneos'
    assert info['UIDeviceFamily'] == [1], 'Expected iPhone-only app'
    assert prefix + info['CFBundleExecutable'] in names
    print('Verified device IPA structure:', info['CFBundleIdentifier'], info['CFBundleShortVersionString'])
PY
(cd build/distribution && shasum -a 256 cubox-chill-iphone-preview.ipa > cubox-chill-iphone-preview.sha256)
