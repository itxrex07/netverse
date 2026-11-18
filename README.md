# NetMirror (Kotlin & Jetpack Compose)

<p align="center">
  <img src="art/icon.png" width="128" height="128" alt="NetMirror Logo" />
</p>

A native Android rewrite of **NetMirror (v3.4.1)** built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**.

## Features

- **Direct Content Launch & Ad-Gate Skip**: Automatically probes active NetMirror gateways (`/check.php` and primary mirrors), suppresses legacy ad-gate overlays and popup redirects, injects `enhancer.js` at page commit, and keeps the startup loader visible until catalog content is ready.
- **Fullscreen Hardware-Accelerated Video Playback**: Supports HTML5 fullscreen video via `WebChromeClient.onShowCustomView` with immersive system bars and full-sensor orientation handling.
- **Dedicated Native Settings Suite**:
  - **1080p Full HD Streaming**: Toggles high-bitrate 1080p streaming with live `/mobile/setting.php` and `CookieManager` synchronization.
  - **App Info**: Displays application version, package details, device ABIs, motion engine state, and sandbox policy.
  - **Advanced Refresh Rate Engine**: Inspects hardware `Display.Mode` capabilities and applies 60Hz, 90Hz, 120Hz, or Dynamic VRR refresh rate preferences on the Activity window.
- **Automated GitHub Releases**: Every push to `main` / `master` (or `v*` tag) triggers `.github/workflows/release.yml` to build debug and release APKs and publish a GitHub Release automatically.
- **Comprehensive Back Gesture Handling**: Routes back navigation across fullscreen video, Settings sub-pages, Settings main screen, in-page JS modals/player (`pressfromAPP()`), WebView history, and app exit.

## Tech Stack

- **Language**: Kotlin 2.2
- **UI**: Jetpack Compose + Material 3 + bundled local Google Fonts (`Plus Jakarta Sans`, `Inter`)
- **Architecture**: MVVM (`AndroidViewModel` + `StateFlow` + `NetMirrorRepository` + `DataStore`)
- **Networking**: OkHttp + `kotlinx.serialization`
- **CI/CD**: GitHub Actions (`.github/workflows/release.yml`)
- **Build System**: Gradle 9.3.1 (Kotlin DSL), AGP 9.1.1, Compile SDK 36, Target SDK 35, Min SDK 24
