# cubox & chill 0.6.4 validation

November 20, 2026. Code commit 17c6eea572ddd1b4535a36b13001c6a8720a8f6c.

Local Android build, 15 unit tests and lint passed. Artwork tests cover persistence across cache instances, sliding 30-day last-use retention, memory-hit touches, automatic removal of unused entries, bounded oldest-used eviction, and migration from the previous cache folder.

GitHub Actions run 44692665490 passed native builds for three ABIs, APK assembly, unit tests and lint. Release artifact is from that run and is re-signed with the existing local update key.

Artwork now lives in filesDir/artwork-v1 (128 MB limit), migrated from cacheDir/artwork-v1. Local reads use a separate executor from downloads. Maintenance runs on first artwork use after process startup and daily during artwork use; an app that remains closed is cleaned on next use. URLs remain exact cache keys: a provider changing an image URL requires a new download. The cache does not store video streams.

No physical-device or emulator UI test was performed for this cache-only release. TV input/player fixes and MovieBox-TUI 0.1.30 are unchanged.
