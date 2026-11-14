#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export IPHONEOS_DEPLOYMENT_TARGET=16.0
rustup target add aarch64-apple-ios aarch64-apple-ios-sim
for entry in 'aarch64-apple-ios:iphoneos' 'aarch64-apple-ios-sim:iphonesimulator'; do
  target="${entry%%:*}"
  platform="${entry##*:}"
  SDKROOT="$(xcrun --sdk "$platform" --show-sdk-path)" cargo build --manifest-path native/Cargo.toml --release --target "$target"
  mkdir -p "Libraries/$platform"
  cp "native/target/$target/release/libcubox_ios.a" "Libraries/$platform/"
done
