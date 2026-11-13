# cubox & chill 0.5.0 — initial TV support

Build and existing unit tests passed. Android lint: zero errors, 18 warnings. Signed with the existing certificate; APK alignment verified. Version code 8; Android 8+; ARMv7, ARM64, x86_64.

Added TV launcher entry/banner, optional touchscreen declaration, focus highlights, minimum 16sp TV text for shared text views, TV landscape playback, remote media/DPAD/menu handling, and Back-to-hide playback controls. Phone features remain included.

Validation limitation: no dedicated Android TV emulator or physical Mi TV end-to-end remote navigation test was performed. Existing unit tests do not establish remote focus traversal or TV playback correctness. This is an initial TV-compatible development release, not a TV certification or a fully redesigned TV interface.

Remote navigation: DPAD moves focus and OK selects. When playback controls are hidden, Left/Right seek 10 seconds, OK toggles play/pause, Up/Down shows controls. Menu opens player settings. Back hides visible playback controls before exiting. Media Play/Pause buttons are supported.