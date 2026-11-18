# NetMirror (100% Native Kotlin & Jetpack Compose)

<p align="center">
  <img src="art/icon.png" width="128" height="128" alt="NetMirror Logo" />
</p>

A native Android rewrite of **NetMirror (v3.4.1)** built in **Kotlin**, **Jetpack Compose**, and **Material Design 3**.

## Architecture

- **100% Native Kotlin Source**: All React Native/Hermes bytecode, Apktool smali, and standalone `enhancer.js` UI/Settings files have been eliminated and rewritten in Kotlin (`app/src/main/java/app/netmirror/nmv2/`).
- **Pre-WebView Native Handshake & Mirror Resolution**: `NetMirrorRepository` probes active gateways (`/check.php` & primary mirrors) via `OkHttp`, performs `verify2.php` session verification natively before rendering, and syncs cookies directly into Android `CookieManager`.
- **Native Network Monitoring**: `NetworkMonitor` uses `ConnectivityManager.NetworkCallback` to detect offline states immediately and auto-retry when connectivity returns.
- **Fullscreen Video Playback**: Supports HTML5 fullscreen video via `WebChromeClient.onShowCustomView` with immersive system bars and full-sensor orientation handling.
- **Dedicated Native Settings Suite**:
  - **1080p Full HD Streaming**: Syncs `/mobile/setting.php` and `CookieManager` (`hd=on`/`hd=off`) with `DataStore` persistence.
  - **App Info**: Displays version, package details, device ABIs, motion engine rate, and sandbox policy.
  - **Advanced Refresh Rate Engine**: Inspects hardware `Display.Mode` capabilities and applies 60Hz, 90Hz, 120Hz, or Dynamic VRR refresh rate preferences on the Activity window.
- **Automated GitHub Releases**: `.github/workflows/release.yml` automatically builds debug and release APKs and creates a GitHub Release on every push to `main`/`master` or `v*` tags.

## Tech Stack

- **Language**: Kotlin 2.2
- **UI**: Jetpack Compose + Material 3 + bundled local Google Fonts (`Plus Jakarta Sans`, `Inter`)
- **Architecture**: MVVM (`AndroidViewModel` + `StateFlow` + `NetMirrorRepository` + `DataStore`)
- **Networking**: OkHttp + `kotlinx.serialization`
- **Build System**: Gradle 9.3.1 (Kotlin DSL), AGP 9.1.1, Compile SDK 36, Target SDK 35, Min SDK 24
