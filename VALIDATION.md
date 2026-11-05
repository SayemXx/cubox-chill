# Validation — cubox chill 0.3.0

Validated on November 6, 2026. Developer: **Sayem**.

## Build and packaging

- ARM64 and x86_64 Rust engine release builds: reused unchanged from the previously validated engine build.
- Android application and instrumentation-test APK builds: passed.
- Unit tests for version comparisons, malformed release tags and repository names: **3 passed, 0 failed**.
- Android lint: **0 errors**, 15 non-blocking warnings.
- APK signature: verified, APK Signature Scheme v2.
- Native-library and ZIP alignment: checked for 16 KB page support.
- App label: `cubox chill`; package: `app.moviebox.mobile`; version: `0.3.0` (code 3).
- Minimum Android API: 26 (Android 8.0); ABIs: `arm64-v8a`, `x86_64`.

## Installed-device checks

The final app and test APKs were installed successfully on an isolated x86_64 Android emulator. Device smoke tests passed:

- Native library loads; Rust runtime initializes; JNI errors propagate correctly.
- The app's Downloads screen opens.
- ExoPlayer initializes and releases.
- Persistent download database and cache initialize.
- A video requiring a custom HTTP header renders its first frame.
- The actual player activity renders video; a screenshot pixel check confirms unused video space is black. The screenshot was also inspected visually after dismissing Android's first-use fullscreen tutorial.
- Playback speed and control lock/unlock pass device assertions.
- The complete test video downloads; downloaded byte count matches the source.
- The downloaded video renders after its source HTTP server is closed, proving cache-only playback.

The loopback fixture test disables public-internet validation only during the test; normal app download requirements are restored afterward. An initial test exposed a direct-file preparation bug; it was corrected before the final APK was packaged. A separate test interruption was confirmed by Android as a low-memory termination on the emulator.

## Live-provider checks and limits

The final MovieBox live search probe returned **10 results** from the emulator. Earlier attempts reported all hosts exhausted, demonstrating that availability can vary. The app embeds the actual upstream provider implementation, but availability of MovieBox, 4KHDHub, Dramachi and the BDIX servers depends on the provider and network. This search result does not prove playback from every live server. Every subtitle format, codec, adaptive stream, audio-track combination and picture-in-picture device has not been tested.

The black/red Downloads screen and actual player's black unused area were visually inspected. Live artwork layouts were not exhaustively reviewed across all providers. The player uses Media3, not LibVLC.

## Distribution

This APK is a signed **debug/development build**, suitable for direct installation and testing. It is not a Play Store release. App update checks point to `SayemXx/cubox-chill`; installation of future APK updates requires a published APK signed with the same key. CI-generated debug keys must not be treated as a stable public upgrade channel.

Source for the device tests is in `app/src/androidTest`. The small Big Buck Bunny test fixture and its attribution are packaged only in the test APK, not the application APK.
