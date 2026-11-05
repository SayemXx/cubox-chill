# Validation — cubox chill 0.1.0

Validated on November 5, 2026. Developer: **Sayem**.

## Build and packaging

- ARM64 and x86_64 Rust engine release builds: passed locally and in GitHub Actions.
- Android application and instrumentation-test APK builds: passed.
- Unit tests for version comparisons, malformed release tags and repository names: **3 passed, 0 failed**.
- Android lint: **0 errors**, 12 non-blocking warnings.
- APK signature: verified, APK Signature Scheme v2.
- Native-library and ZIP alignment: checked for 16 KB page support.
- App label: `cubox chill`; package: `app.moviebox.mobile`; version: `0.1.0` (code 1).
- Minimum Android API: 26 (Android 8.0); ABIs: `arm64-v8a`, `x86_64`.

## Installed-device checks

The final app and test APKs were installed successfully on an isolated x86_64 Android emulator. Device smoke tests passed:

- Native library loads; Rust runtime initializes; JNI errors propagate correctly.
- The app's Downloads screen opens.
- ExoPlayer initializes and releases.
- Persistent download database and cache initialize.
- A video requiring a custom HTTP header renders its first frame.
- The complete test video downloads; downloaded byte count matches the source.
- The downloaded video renders after its source HTTP server is closed, proving cache-only playback.

The loopback fixture test disables public-internet validation only during the test; normal app download requirements are restored afterward. An initial test exposed a direct-file preparation bug; it was corrected before the final APK was packaged. A separate test interruption was confirmed by Android as a low-memory termination on the emulator.

## Live-provider limitation

The MovieBox live search probe returned `MovieBox unavailable: All hosts exhausted` from the emulator. This is not recorded as a successful live-provider test. The app embeds the actual upstream provider implementation, but availability of MovieBox, 4KHDHub, Dramachi and the BDIX servers depends on the provider and network. Every live server, subtitle format, codec, and adaptive stream has not been tested.

## Distribution

This APK is a signed **debug/development build**, suitable for direct installation and testing. It is not a Play Store release. App update checks point to `SayemXx/cubox-chill`; installation of future APK updates requires a published APK signed with the same key. CI-generated debug keys must not be treated as a stable public upgrade channel.

Source for the device tests is in `app/src/androidTest`. The small Big Buck Bunny test fixture and its attribution are packaged only in the test APK, not the application APK.
