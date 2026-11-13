# cubox & chill 0.5.1 validation

Fixed TV focus initialization using an app-owned resource ID instead of android.R.id.custom, which is invalid for keyed View.setTag calls. Build, existing unit tests and lint passed. Installed emulator regression test passed: TV focus initialization, repeated setup, focus highlight and blur. The TV helper was exercised directly on a phone emulator; physical Mi TV startup and full remote traversal remain unverified. Same signing certificate as previous releases; ARMv7/ARM64/x86_64 retained.
