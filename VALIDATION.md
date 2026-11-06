# Validation — cubox & chill 0.4.0

Validated on November 6, 2026. Developer: **Sayem**.

## Build and packaging

- MovieBox-TUI engine upgraded to v0.1.29, commit 5fd687e46e165723755e6550aa4c107367d72545, including the upstream series request-header fix.
- ARM64 and x86_64 engine binaries came from successful GitHub Actions run 42103631672 for commit a29fb1049892c5094dc0090c4b98a15d76d1bf16. The native source was unchanged by the subsequent UI/cache changes. Local ARM64 compilation also passed; the redundant local x86_64 compilation was stopped after the CI build succeeded.
- Final application and instrumentation APKs built locally with the existing local signing key.
- Unit tests: **7 passed, 0 failed**. Coverage: version/repository validation, repeated-tap seek accumulation/reset, artwork persistence, expiry and bounded least-recently-used eviction.
- Final Android lint: **0 errors**, 16 warnings.
- APK signature and 16 KB ZIP alignment verified. Signing certificate matches the previously supplied v0.3.0 APK.
- Label: `cubox & chill`; package: `app.moviebox.mobile`; version 0.4.0 (code 4).
- Minimum Android 8.0 (API 26); ABIs: arm64-v8a and x86_64.

## Device checks

The APK installed as an update over the previous app in an isolated x86_64 emulator. Final instrumentation run passed:

- Native engine initializes and JNI errors propagate.
- Downloads screen, persistent download database/cache and ExoPlayer initialize.
- Authenticated test video renders; complete download byte count matches; cached video renders with its HTTP server closed.
- Actual player screen has black unused video space, verified by screenshot pixel check.
- Playback speed, single-tap controls and control lock/unlock work.
- Auto rotation is the default requested sensor mode; locking freezes orientation and unlocking restores it.
- Switching to landscape preserves the player instance and paused playback position. The test uses the player's landscape option because Android's user-rotation lock is intentionally ignored by full-sensor mode. Physical rotation behavior should also be checked on users' devices.
- Left-side brightness and right-side volume swipes increase their respective values; test media volume is restored afterward.
- Four quick taps produce the +30 seconds feedback. Unit tests cover 2/3/4/5-tap accumulation and reset after a pause or side change.
- Simultaneous poster views generate exactly one HTTP image request. After clearing the memory cache and closing the image server, another view loads the same poster from disk.
- MovieBox live search returned 10 results.
- **Black Torch [Hindi] S1E1 resolved an episode stream and rendered its first frame.** This confirms the previously reported series case on the test network, not every episode/provider/server.

Earlier test iterations encountered an emulator playback timeout and test-harness issues involving controller animations and orientation overrides. These were investigated; the final installed build passed the complete run.

## Cache and performance scope

Artwork uses a 12 MB memory cache plus a 64 MB disk cache with 30-day expiry, request sharing and background decoding. Android may reclaim cache storage. First loads, expired/evicted artwork and live streams still require network access. No promise of zero buffering or zero loading on all devices/networks is made.

## Distribution

This is a signed development APK, not a Play Store release. Install without uninstalling to retain app-private downloads and settings. Future APKs must use the same signing key and a higher version code. Unmodified CI debug APKs use a different ephemeral key and are not the update artifact.

The default update repository remains `SayemXx/cubox-chill`. Publishing the APK to GitHub Releases makes it discoverable through the existing app's update button. Upstream engine releases still require integration into a new app APK.
