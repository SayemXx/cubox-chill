# cubox & chill 0.6.2 validation

November 19, 2026. CI run 44460216412 passed native builds (ARMv7, ARM64, x86_64), APK compilation, unit tests and lint for c30f4b592923d6c2a25d0fe2ab88fd21f0973c83.

Removed generic TV focus decoration/key handling from the PlayerView tree. Only the custom toolbar and unlock control use that helper. When controls hide, focus returns to the undecorated root; on opening, Play/Pause receives focus. Media3 handles its own controller buttons. MovieBox-TUI stays at v0.1.30.

This release has not been visually tested on a physical TV or in a TV emulator. The existing unit suite does not exercise this player focus lifecycle. Mi TV confirmation remains necessary. APK uses the same local signing certificate as previous releases.