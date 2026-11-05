#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK r28 or later}"
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
for pair in aarch64-linux-android:arm64-v8a x86_64-linux-android:x86_64; do
  target="${pair%%:*}"; abi="${pair##*:}"; key="${target//-/_}"
  rustup target add "$target"
  env "CARGO_TARGET_${key^^}_LINKER=$TOOLCHAIN/${target}26-clang" "CC_$key=$TOOLCHAIN/${target}26-clang" "AR_$key=$TOOLCHAIN/llvm-ar" cargo build --manifest-path native/Cargo.toml --target "$target" --release --locked
  mkdir -p "app/src/main/jniLibs/$abi"
  cp "native/target/$target/release/libmoviebox_mobile.so" "app/src/main/jniLibs/$abi/"
done
