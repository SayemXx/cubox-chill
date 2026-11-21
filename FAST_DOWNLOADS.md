# Fast downloads

Progressive HTTP video downloads of at least 8 MiB use up to four concurrent, disjoint byte ranges when the server answers the probe with a valid 206 Content-Range and a strong ETag. Each range response is checked before any bytes enter Media3's existing offline cache. If-Range prevents mixing file versions. Unsupported range servers or missing validators use the standard downloader; a server that stops honoring ranges is downgraded after workers stop and the partial cache is cleared.

The existing DownloadService and DownloadManager retain the queue, per-title pause/resume, foreground notification and removal behavior. Two titles run at once. HLS/DASH continue using Media3's adaptive downloader with a shared four-thread executor. DownloadManager retries I/O failures at least five times using its built-in retry policy and waits for a network connection. Speed and estimated remaining time are calculated from current download progress; initial cached bytes are not counted as new transfer speed.

Completed downloads keep their existing cache format. Partial fast downloads retain both bytes and identity metadata across downloader recreation. If the server changes the ETag or length, partial data is discarded before a new transfer. Expired source URLs may still require choosing a fresh source. Actual speed depends on server and connection limits; four connections do not guarantee four times the speed. Background execution remains subject to Android and device battery restrictions.

## Validation

Run unit tests and lint with Gradle. Build the HTTP integration runner with:

    gradle :app:assembleDebug :app:assembleDebugAndroidTest -PtestRunner=app.moviebox.mobile.FastDownloadInstrumentation

Install app and androidTest APKs on an emulator and run:

    adb shell am instrument -w app.moviebox.mobile.test/app.moviebox.mobile.FastDownloadInstrumentation

The fixture uses a local HTTP server and verifies parallel connections, required request headers, range fallback, pause/resume, changing ETags, truncated response retry, byte-for-byte offline reads, and removal. It never downloads third-party media.

Validated November 21, 2026: local build, unit tests and lint passed. The headless Android emulator integration runner passed parallel ranges, header forwarding, ignored/rejected range fallback, pause/resume, changed ETag, truncated-body retry, byte-for-byte offline integrity and removal. GitHub Actions run 44927907902 also passed native ARM32/ARM64/x86_64 builds, APK assembly, unit tests and lint for source e2f0557d7825f78e6fce27d0325bc11e871ca482. No real-provider speed multiplier or physical-device throughput is claimed.
