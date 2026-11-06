# Validation — cubox & chill 0.4.2

Focused compatibility update: version code 6, Android 8.0 minimum unchanged.

- GitHub Actions run 42188433612 passed native compilation for ARMv7, ARM64 and x86_64, APK build, unit tests and Android lint.
- APK includes armeabi-v7a/libmoviebox_mobile.so. ELF header verified as 32-bit ARM (class 1, machine 40). ARM64 and x86_64 libraries are also present and correctly identified.
- Final APK was re-signed locally with the existing key. Certificate SHA-256 matches the previous release: 3eded4b6d33c88be5bb45cbe16f41cf203554f4f30d141220a3952b83886e1c0.
- Signature verification and 16 KB ZIP alignment passed. Package app.moviebox.mobile; version 0.4.2; min API 26.
- No interface, player behavior, cache or provider-engine changes were made. Physical 32-bit device playback has not been tested. Older devices may lack codecs supported by newer phones.

Signed development APK. Install over the previous version without uninstalling to preserve app data. The published APK uses the existing local key, not the ephemeral CI key.
