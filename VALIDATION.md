# Validation — cubox & chill 0.4.1

Validated November 6, 2026. Developer: Sayem.

## Changes

- Horizontal player swipes preview a target and seek on release: right forwards, left backwards. A full-screen-width drag represents 120 seconds and the target is clamped to the media bounds. Cancelled swipes do not seek. Existing vertical brightness/volume gestures remain separate.
- Home and first-page search catalogs persist in a bounded 4 MB metadata store, including Find your next series. Saved entries are reused for six hours, then retained on screen during background refresh. Refresh titles bypasses the freshness window. Playback links are not cached in this store. The existing 64 MB artwork disk cache remains in use.
- Updates offers Check / download app update. A newer release offers Download update; a matching version offers Download APK again. A Release page link is also provided.
- Version increased to 0.4.1 / code 5. Package remains app.moviebox.mobile and label remains cubox & chill.

## Build and artifact verification

- Application APK, instrumentation APK and unit tests built successfully.
- Nine unit tests passed, zero failures: version/repository validation, tap bursts, horizontal seek direction/bounds, artwork persistence/expiry/eviction.
- Android lint: zero errors, 17 warnings.
- APK Signature Scheme v2 verified. Certificate SHA-256 matches v0.4.0:
  3eded4b6d33c88be5bb45cbe16f41cf203554f4f30d141220a3952b83886e1c0
- 16 KB ZIP alignment checked. ARM64 and x86_64 included; minimum Android API 26.
- MovieBox-TUI engine remains v0.1.29, unchanged from the validated v0.4.0 engine binaries.

## Installed-device checks

Installed as an update in an isolated x86_64 emulator. Final complete instrumentation run passed:

- Saved metadata is readable from a new CatalogStore instance and renders in the home UI after the in-memory catalog is cleared; freshness and query isolation are checked.
- Newer-version and matching-version dialogs both expose the expected APK download buttons.
- Horizontal swipes move playback forward and backward; cancellation leaves the position unchanged.
- Existing first-frame rendering, black unused video area, playback speed, rotation without restarting the player, brightness/volume swipes, repeated-tap feedback and control locking pass.
- Full fixture download matches source bytes; offline playback works after its source server is closed.
- Simultaneous artwork requests share one download; artwork loads from disk with memory cache cleared and the image server closed.
- Native engine, download database and player initialization pass.
- MovieBox search returns 10 results; Black Torch [Hindi] S1E1 resolves and renders its first frame.

The first run timed out in the live-series playback check after all local and new-feature checks passed. A complete retry passed without source changes. Provider/network availability and emulator load can vary; this does not establish compatibility with every live stream, codec or device.

## Installation

Signed development build. Install over the existing app without uninstalling to preserve settings and downloaded media. The release APK uses the same local signing key as v0.4.0; ephemeral CI debug APKs are not compatible update artifacts.

The first successful catalog load needs data. Later openings reuse saved metadata; artwork that is missing, expired or evicted still needs downloading. Live video still requires network access unless downloaded for offline playback.
