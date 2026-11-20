# cubox & chill 0.6.5 validation

November 20, 2026. UI code commit a62916dd160d192ad5d3c29c341957d941217f1d.

Local APK build, 15 unit tests and lint passed. GitHub Actions run 44693971526 passed the native builds for ARM32, ARM64 and x86_64, APK assembly, unit tests and lint. Release APK comes from that run and uses the existing local update signing key.

GlassSmokeInstrumentation ran on the phone emulator: phone About screen, forced TV shell and local-fixture player screens; playback controls show/hide. Screenshots were manually inspected for the phone glass cards/navigation and player controls. Initial screenshots were obscured by an emulator System UI startup stall and first-launch permission state; captures were repeated after resolving those emulator conditions. TV shell was exercised on a phone emulator, not physical TV hardware.

GlassTheme uses translucent gradients and subtle borders, not live background blur. The video surface, shutter and unused space remain black. UI changes do not alter remote event dispatch, gestures, provider engine or artwork caching. MovieBox-TUI remains 0.1.30. No iPhone changes.
