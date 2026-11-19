# cubox & chill 0.6.3 validation

November 19, 2026. CI 44529076348 passed native builds for three ABIs, APK build, unit tests and lint (commit 42d62f3cec567b3857de43f5987f9de293e888a3).

Local TvFocusInstrumentation passed: original focus/helper checks, five confirmation key pairs dispatched through a real MainActivity to a clickable poster parent while its child held focus, plus sidebar Updates selection. The test deliberately clears pressed state between key events. Executed on the phone emulator with the TV shell explicitly selected, not a physical Mi TV. Log still uses the older short PASS label, but the activity assertions are in the tested source.

Physical remote keycodes and IME interaction on the user's TV remain unverified. Retains v0.6.2 player focus fix and MovieBox-TUI 0.1.30. Release uses the existing local signing key.