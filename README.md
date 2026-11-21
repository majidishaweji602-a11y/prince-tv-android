# PRINCE TV Android starter

Native Android starter using Kotlin + Media3 ExoPlayer. It is separate from the existing React website.

## Supports
- MPEG-TS progressive streams (`.ts`) through Media3's TS extractor
- HLS (`.m3u8`) through Media3 HLS
- MPEG-DASH (`.mpd`) through Media3 DASH

## Build APK from a phone using GitHub
1. Upload these project files into a NEW GitHub repository, or into a separate `android-app` folder/repository. Do not overwrite the existing website.
2. Open the repository's **Actions** tab.
3. Run **Build PRINCE TV APK** (or push to `main`).
4. Open the completed workflow run and download the `PRINCE-TV-debug-apk` artifact.
5. This debug APK is for testing. A release APK needs a signing key before public distribution.

## Important
- This is a starter player, not the finished channel catalogue, subscription, or 20-minute trial system.
- The app has a URL field so playback can be tested before importing the Tanzania channel catalogue.
- Use only streams you own or are authorized to distribute. Do not commit private MAC addresses, play tokens, passwords, or API keys.
- `usesCleartextTraffic` is enabled for development/testing of legacy HTTP streams. For production, prefer HTTPS and restrict cleartext traffic.
- A stream may still fail if the provider blocks the device, requires an expiring token/headers, or the URL is no longer valid.
