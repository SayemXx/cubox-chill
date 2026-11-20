# cubox & chill 0.6.6 validation

November 20, 2026. Final app code: 1baf51e4a79300c66a659c6894a85194e84bae22.

Local APK assembly, 15 unit tests and lint passed. GlassSmokeInstrumentation passed on the phone emulator: app screens, forced TV shell, server-selection dialog, local video playback and controller show/hide. Final app/popup screenshots were manually reviewed. Physical TV and phone hardware were not tested.

Android 12+ dialogs request 24dp background blur only inside their translucent window; full-screen blur-behind is disabled. The emulator reported blur supported/enabled; screenshots confirm the surrounding app stays sharp. System/device blur availability controls the effect. Older devices use the translucent dark fallback.

App-wide glass-style surfaces use transparent gradients, rounded geometry and reflective gradient edges. App cards/navigation do not have live per-view backdrop blur or optical refraction. Background remains black/charcoal. Classic v0.6.4 player controls remain, with settings/subtitle actions opening the matching themed dialogs. No changes to provider engine, gesture algorithms or persistent artwork cache.

Final cloud run: 44705075701. Native builds for all three ABIs, APK assembly, unit tests and lint passed. Release uses this artifact, signed with the existing update key. MovieBox-TUI stays at 0.1.30. Supported ABIs: ARM32, ARM64 and x86_64.
