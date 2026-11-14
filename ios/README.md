# cubox & chill — iPhone preview

Developer: **Sayem**. Native SwiftUI application for **iPhone, iOS 16+**.
This is a separate iOS target; the Android app remains in `app/`.

## Implemented in this first milestone

- Dark/red adaptive browsing grid, search across five providers, details, audio editions, episodes and server selection.
- Real MovieBox-TUI 0.1.29 engine shared with Android through `native/src/core.rs`; iOS C ABI owns/frees its JSON response.
- Disk artwork cache (64 MiB, 30 days), catalog cache (4 MiB; fresh for 6 hours, older titles visible during refresh).
- AVPlayer with black unused space, native transport controls and rotation. Progressive-file header requests use the public AVAssetResourceLoader API.
- Direct-file downloads, cancellation, persisted completed files, offline playback and removal. Downloads are excluded from iCloud backup.
- Honest iOS update guidance; Android APK releases are not installable on iPhone.

## Build on an Apple Silicon Mac

Install Xcode with iOS SDK/simulators, Rust (1.90+), and XcodeGen (`brew install xcodegen`). From repository root:

```sh
bash ios/scripts/build-native.sh
xcodegen generate --spec ios/project.yml
open ios/CuboxChill.xcodeproj
```

Choose the CuboxChill scheme and an iPhone simulator. For a physical iPhone, select your own development team in Signing & Capabilities and use an available bundle identifier. Do not commit certificates or provisioning profiles.

The GitHub **Build iPhone preview** workflow builds an unsigned device application and runs model/cache tests on an iPhone simulator. The artifact is **not an installable IPA**: physical installation/TestFlight requires Apple signing and provisioning. No signing identity is included in this repository.

## Known limits — do not describe this as Android feature parity

- No physical-iPhone provider/playback validation yet. Network, provider availability and Apple-supported codecs determine playback success.
- HLS with custom headers and external subtitles are not yet integrated. Direct-file header support requires device validation.
- Keep the app foregrounded during downloads. Persistent background transfers, resume, download progress and multi-connection acceleration are pending.
- VLC-style gestures, playback resume history and the Android app's full player controls are pending.
- First-page browsing/search only in this milestone.
- Apple signing, store screenshots, privacy/distribution review and TestFlight publication are pending. No claim of App Store acceptance.
- Embedded upstream engine updates require rebuilding this iOS app.

## License

The repository LICENSE and NOTICE apply. MovieBox-TUI and its notices are retained under `vendor/moviebox-tui`. Review all bundled dependency licenses before distribution.
