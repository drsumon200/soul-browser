# Ether Browser for Android

[![Build APK](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml)

**Ether Browser** is an open-source, Zen-inspired Android browser built from the ground up using **Kotlin 2.3.0**, **Jetpack Compose**, and **Mozilla GeckoView (v150)**.

Repository: https://github.com/drsumon200/soul-browser

---

## Key Highlights & Implemented Features

### 1. Engine & Native Container Isolation
- **Mozilla GeckoView Core**: High-performance independent web rendering powered by GeckoView 150 (not Android System WebView).
- **Process-Scoped GeckoRuntime**: Singleton runtime configured with dark preferred color scheme, tracking protection, and first-party cookie isolation.
- **Multi-Account Containers**: Emulates Firefox Multi-Account Containers and Zen Browser by mapping custom containers to `GeckoSessionSettings.contextId`. Cookies, localStorage, indexedDB, and cache are strictly isolated between containers (e.g. Personal, Work, Banking).
- **Biometric Private Lock**: Private containers and tabs are locked behind Android Keyguard/Biometrics (`BiometricPrivateLock`).
- **Auto-Wipe on Screen-Off**: Private tabs and sessions automatically purge from memory when the device screen locks (`AutoWipePrivateReceiver`).
- **Clipboard Sensitive Guard**: URLs and text copied inside private containers automatically wipe from the Android clipboard after 60 seconds (`ClipboardAutoClear`).

### 2. Dual-Layer Ad & Tracker Protection
- **Network-Level Adblock Engine (`AdblockFilterEngine`)**: High-speed in-memory domain filter intercepting GeckoView's `onLoadRequest` before ads and trackers load. Includes block counters, custom domain rules, and hosts file importing.
- **DOM-Level Cosmetic Blocker (`WebExtensionManager`)**: Embedded WebExtension (`soul_pro`) running content scripts that hide ad containers, sponsored widgets, and cookie consent notices.
- **Privacy Shield Dashboard (`PrivacyDashboardSheet`)**: Real-time counter badge on the top bar displaying total blocked ads and trackers, with a one-tap option to nuke all cookies and site storage.
- **DNS-over-HTTPS (DoH)**: Support for encrypted DNS providers (Cloudflare 1.1.1.1, Quad9, AdGuard DNS, and Mullvad).

### 3. Zen Navigation & Gesture System
- **Thumb-Friendly Bottom Bar**: Curved floating pill navigation keeping essential controls within thumb reach.
- **Horizontal Workspace Swipe**: Swiping across the bottom bar switches workspaces smoothly.
- **Edge Swipe Back/Forward (`EdgeSwipeNavigationOverlay`)**: Swipe from the left or right edge of the screen to navigate back or forward with visual pill indicators and haptic feedback.
- **One-Handed Reachability (`OneHandedReachOverlay`)**: Pulls the entire active viewport down by 180dp for comfortable one-handed reach.
- **Fullscreen Video Gestures (`VideoGestureOverlay`)**: Swipe vertically on the left side to adjust screen brightness; swipe on the right to adjust media volume.
- **Hardware Keyboard Shortcuts (`HardwareKeyDispatcher`)**: Full physical keyboard support (`Ctrl+T`, `Ctrl+W`, `Ctrl+R`, `Ctrl+L`, `Ctrl+F`, `Ctrl+Tab`).

### 4. Split-Screen Dual Browsing
- **Zen Split View (`SplitViewHost` & `SplitViewController`)**: View two tabs simultaneously side-by-side or stacked vertically with a fluid, draggable ratio divider. Tap either pane to switch focus.

### 5. Smart Omnibox & Search
- **Instant Calculator (`OmniboxCalculator`)**: Type arithmetic expressions (`15 * 80`, `250 / 5`, `12 + 48`) into the address bar to evaluate math in real time.
- **Search Bangs (`BangRegistry`)**: Direct search routing (`!g` for Google, `!yt` for YouTube, `!gh` for GitHub, `!w` for Wikipedia, `!r` for Reddit, `!so` for StackOverflow).
- **Live Search Suggestions (`SearchSuggestionClient`)**: Asynchronous search suggestions while typing.
- **Integrated QR Scanner & Voice Search**: Scan QR code URLs directly via camera or speak to search.

### 6. Reader Mode, Speech & Media
- **Distraction-Free Reader (`ReaderModeExtractor` & `ReaderModeOverlay`)**: Strips clutter, ads, and navigation to present clean typography with font scaling, dark/sepia themes, and reading time estimation.
- **Text-to-Speech Engine (`PageTextToSpeech` & `FloatingTtsPlayer`)**: Reads articles aloud in background with floating player speed controls (1.0x, 1.25x, 1.5x, 2.0x).
- **Full-Page Screenshotter (`FullPageScreenshotter`)**: Captures complete webpage canvas pixels via `GeckoView.capturePixels()` and saves PNG to Android MediaStore.
- **Offline Web Archiver (`WebArchiveSaver`)**: Saves complete offline HTML snapshots to local storage for reading without network connection.
- **Netscape Bookmark Portability (`BookmarkHtmlPort`)**: Export and import standard HTML bookmarks.

### 7. Modern Visual Tab Grid & Speed Dial
- **Visual Tab Grid (`VisualTabGridSheet`)**: 2-column card overview displaying live tab thumbnails (`TabThumbnailCache`), container color tags, close buttons, and workspace filtering.
- **Quick Tab Switcher Pill (`QuickTabSwitcherPill`)**: Quick one-tap pill to return to the previously active tab.
- **Customizable Home Speed Dials (`SpeedDialEditorDialog`)**: Editable shortcut cards on the New Tab page with long-press management.
- **Downloads Manager (`DownloadsManagerSheet`)**: Full downloads bottom sheet with formatted file sizes, delete options, and direct system intent launching.

---

## Build & CI

Requirements: JDK 17, Android SDK Platform 36, Build Tools 36.0.0, Gradle 8.13.

```bash
# Run unit tests
gradle testDebugUnitTest

# Assemble optimized ARM64 debug APK
gradle assembleDebug

# Assemble optimized release APK and Android App Bundle (AAB)
gradle assembleRelease
gradle bundleRelease
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

Automated CI on GitHub Actions runs unit tests, verifies builds, and publishes downloadable release APKs.

---

## License & Attribution

- GeckoView: [Mozilla GeckoView](https://mozilla.github.io/geckoview/) (MPL-2.0)
- Zen Browser Desktop: [Zen Browser](https://github.com/zen-browser/desktop) (MPL-2.0)
- Ether Browser: [Dr Sumon](https://github.com/drsumon200) (MIT / MPL-2.0 compatible)
