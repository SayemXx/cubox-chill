# Cubox & Chill 0.6.7 validation

November 20, 2026. Final code e403002ac88e8fb90dd37edab421c651e8b1f1f1. GitHub run 44728623073 passed native builds for ARM32/ARM64/x86_64, APK assembly, unit tests and lint. Local build, 15 unit tests and lint also passed.

FrostedDialog captures a <=240px app-window snapshot before opening, runs two separable box-blur passes and clips the opaque result inside the rounded popup behind the glass overlay. Native dialog background blur is explicitly disabled after installing the snapshot. Outside content and popup labels remain sharp. All app/player AlertDialog builders use this path. The snapshot is static while the popup is open; video SurfaceViews are not captured and have a dark backing. No continuous screenshot storage or upload occurs.

GlassSmokeInstrumentation passed after installing the final build. The final popup screenshot was manually reviewed and shows bounded blur with native blur disabled by the app. A command attempting to disable global emulator blur failed with Android SecurityException, so this is not claimed as a global-system-setting test. An initial emulator System UI startup stall was dismissed before repeating the checks. No physical phone/TV performance test was performed.

Launcher play mark and in-app Cubox & Chill name use #A54D60 -> #6A2034 -> #300D19. Player layout, black background, gestures and cache behavior are retained. Release uses the existing signing key.
