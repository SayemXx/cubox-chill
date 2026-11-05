# cubox chill

Developer: **Sayem**. Independent native Android client using the real MovieBox-TUI Rust provider engine. No Termux, server, or separate player installation is required.

## Features

- MovieBox, 4KHDHub, Dramachi, CircleFTP and DhakaFlix search.
- Movie details, series seasons/episodes, audio editions where upstream supplies them.
- Selection of quality and individual server/mirror; upstream resolution for 4KHDHub links.
- Black-and-red cinema theme with featured artwork, horizontal poster rows, movie/series filters and bottom navigation.
- Integrated Media3 player with pure-black unused video space and loading shutter; Fit, stretched Fill and cropped Zoom modes.
- Playback speed (0.25–2×), audio/subtitle track selection, subtitle size, 10-second seek and double-tap gestures, saved playback position, rotation, control lock, repeat and picture-in-picture on supported devices.
- Automatic sensor rotation by default, with portrait/landscape modes under Player controls → Screen rotation. Control lock freezes orientation; unlocking restores the selected mode.
- MovieBox-TUI v0.1.29 includes the upstream MovieBox series request-header fix.
- Media3 offline download queue with persistent progress, pause/resume, retry, removal and offline playback. Direct files, HLS and DASH use the same library.
- Daily-on-open and manual upstream release checks, plus optional checks for the app's own GitHub APK releases.

BDIX sources require a supported ISP network. Availability depends on upstream servers. A source may reject a network, expire a signed URL, or be unavailable; selecting a different provider/server can help. The app does not host media.

## Build

Requires JDK 21, Android SDK platform 36.1, Gradle 9.5.0, Rust stable (1.90+), and Android NDK r28+. On Windows, the GNU Rust host needs a 64-bit MinGW linker, or use the MSVC host with Visual Studio build tools.

1. Set `ANDROID_HOME`, `ANDROID_NDK_HOME`, and `JAVA_HOME` for your machine.
2. Run `scripts/build-native.ps1` on Windows, or `bash scripts/build-native.sh` on Linux.
3. Run `python scripts/collect-licenses.py` to bundle the Rust dependency notices.
4. Run `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
5. Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 8+ ARM64 device (or x86_64 emulator).

Both Rust and Android dependencies are downloaded by their standard package managers. The provider source is vendored for reproducibility; `native/Cargo.lock` pins transitive Rust dependencies. The app is Java with native Android views and a JSON JNI bridge, not a web wrapper. `native/src/lib.rs` connects directly to the upstream library.

## Updates

The terminal self-updater cannot safely replace a library embedded in an Android APK. Updates therefore have two explicit layers:

1. **Upstream engine release:** the app checks `mesamirh/MovieBox-Tui/releases/latest`, compares numeric versions and shows release notes when a newer engine exists.
2. **cubox chill release:** Sayem integrates and tests an engine revision, increments the APK version, signs it with the same private signing key and publishes an APK to the app repository's GitHub Releases. Users can then use Updates → Check cubox chill updates → Download APK, followed by Android's installation confirmation.

The default app release repository is `SayemXx/cubox-chill`. Change it in the app under Updates, or compile with `-PupdateRepository=OWNER/REPOSITORY`. Users tap **Check cubox chill updates → Download APK**, open the downloaded file, and confirm **Update** in Android. Do not uninstall first. An upstream-engine notice also offers **Check app update**; if a new engine has not yet been packaged, users must wait for the next APK release.

Published APK updates must retain the signing key used for the previous install. The locally signed development APKs use the same local key. Debug APKs are for testing; CI debug signing keys are ephemeral and are not an upgrade channel. Source uploads do not by themselves create an APK release. Keep the signing key private and backed up; establish a production release process before Play Store distribution.

The included `upstream-watch.yml` records the latest upstream release daily for developer review. It does not automatically trust new source, publish a release, or claim that an upstream binary can update the mobile engine. `android.yml` builds and tests APK artifacts after source changes. Keep public release signing credentials in repository secrets, never in source control.

To integrate an update: inspect release changes, replace the vendored snapshot, preserve upstream license files, update `ENGINE_VERSION`/`ENGINE_COMMIT`, regenerate the Rust lockfile if needed, run native builds and Android tests, smoke-test providers and playback/downloads, then release a signed APK.

## Storage and downloads

Offline media and download metadata live in app-private storage. Removing the app removes the library. Downloads are played within cubox chill, not exported as a single MP4 when a source uses segmented media. An expired link may require selecting the source again. Android may stop long-running data-sync work under system limits; reopen the app and resume as needed.

## Scope

The five built-in movie/series providers are connected. Stremio add-on configuration, live-TV playlists, casting, whole-season one-click downloads and a VLC decoder fallback are not included. The controls are inspired by full-featured players, but playback uses Media3 and device codecs, so format support differs from desktop VLC. Subtitle support depends on the source format.

## Credits and licensing

- App developer: Sayem.
- MovieBox-TUI: upstream contributors, MIT or Apache-2.0, source and complete license texts under `vendor/moviebox-tui/`.
- Android Media3: Android Open Source Project, Apache-2.0.
- This application is independent and is not endorsed by MovieBox-TUI or VideoLAN.

See `VALIDATION.md` for the checks actually performed on the delivered build.
