# Install the iPhone sideloading preview

This IPA is unsigned. It cannot be installed by tapping it in Safari or Files. Sideloadly must sign it for your own device/account. Physical-device installation, playback and downloads have not yet been validated. Requires iOS 16 or newer.

1. Download `cubox-chill-iphone-preview.ipa` from this repository's **iPhone preview** release onto your Windows PC or Mac. Android APK files do not work on iPhone.
2. Install Sideloadly from https://sideloadly.io/ and follow its current prerequisite instructions for your operating system (including Apple's supporting software on Windows when requested).
3. Connect your unlocked iPhone by USB and accept **Trust This Computer** on the phone.
4. Open Sideloadly, select the phone, and choose the IPA.
5. Enter your own Apple account in Sideloadly and follow its signing prompts. Do not send your password or verification code to Sayem, GitHub issues, or this chat.
6. If prompted on the phone, trust your developer profile under **Settings > General > VPN & Device Management**. Enable **Settings > Privacy & Security > Developer Mode** if required, restart, and confirm.
7. Open **cubox & chill**. Test Home, search, series episodes, one movie stream, a short direct-file download and offline playback. Report the iPhone model, iOS version and exact error text if something fails; omit account information and signed stream URLs.

Free-account installations normally expire after seven days and need refreshing/re-signing. Sideloadly provides refresh functionality; follow its setup instructions and keep the same Apple account/bundle identity when updating to preserve app data. Free-account app limits apply. Each person signs for their own device; this is not a universal signed IPA or a TestFlight release.

Known preview limits: foreground-only direct-file downloads; no resumable background downloads, VLC-style gestures, external subtitles or custom-header HLS support. Apple-supported codecs determine playback. Cache files may be reclaimed by iOS. The engine is bundled, so upstream updates require a new app build.

Official tool instructions and FAQ: https://sideloadly.io/ and https://sideloadly.io/faq.html
