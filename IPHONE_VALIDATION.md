# iPhone preview validation — November 14, 2026

App version 0.1.0 (1), iPhone only, iOS 16+. Code tested: `ba9156149ff44887d77ab7f7f62ad8fed064f533`.

Successful macOS build/test run: https://github.com/SayemXx/cubox-chill/actions/runs/43567218108

- Swift source type-check passed.
- Rust C bridge ownership/null-input test passed on macOS.
- Shared provider engine compiled for ARM64 iPhone and ARM64 iPhone simulator.
- Unsigned Release iPhone application built successfully.
- Three simulator unit tests passed: catalog JSON decoding, header conversion, cache expiry/oversize rejection.
- One simulator UI test passed: launch, Home/Downloads/My Cubox navigation, and orientation changes.
- Exported home-screen screenshot visually reviewed: black/red interface, real MovieBox homepage titles/posters loaded, bottom navigation visible.
- Build archive downloaded and SHA-256 matched the Actions artifact digest: `09ecbe5a826eb1589fdc36970c19ea914758ed533066416d90e7777202f59b80`.
- Android native build, APK build, unit tests and lint passed after the shared-engine extraction: https://github.com/SayemXx/cubox-chill/actions/runs/43566244574

Not verified: physical iPhone installation, live video playback/seek, offline downloads/playback, all providers, all screen sizes, or App Store acceptance. These checks do not prove player/download correctness. The iPhone preview's feature limitations are listed in `ios/README.md`.

The artifact contains an **unsigned .app**, not an installable IPA. Apple signing/provisioning and physical-device validation are still required. No new Android release was published for this iPhone work.
