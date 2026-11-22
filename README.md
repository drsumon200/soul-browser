# Soul Browser for Android

[![Build APK](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml)

Soul Browser is a Zen-inspired Android browser prototype using Kotlin, Jetpack Compose and Mozilla GeckoView.

Repository: https://github.com/drsumon200/soul-browser

## Implemented

- GeckoView rendering with a process-scoped GeckoRuntime owned by SoulApplication.
- Workspaces that group tabs and remember a default container.
- Named containers mapped to GeckoSessionSettings.contextId for cookie and web-storage isolation.
- Private containers use GeckoView private mode. Private tab metadata and URLs are excluded from persistent tab storage.
- Lazy tab restoration: startup restores regular-tab metadata only; GeckoSession is opened only when a tab is activated.
- Safe GeckoView session handoff using releaseSession before attaching a different session.
- Back navigation follows GeckoView NavigationDelegate can-go-back state.
- Address-bar URL detection and DuckDuckGo search fallback for plain text.
- JavaScript alert and confirm prompts, HTML select prompts, single/multiple file pickers.
- Download response handling through GeckoView ContentDelegate and Android's document save picker.
- Per-container site-data clearing closes that container's sessions before calling StorageController.clearDataForSessionContext.
- BrowserViewModel owns core tab/workspace/container/bookmark state and debounces tab metadata writes by 500 ms.
- Thumb-friendly bottom navigation: horizontal swipe switches workspaces and upward swipe opens the tab drawer.
- Tracking protection enabled by default; background sessions are deactivated and media suspension requested.

## Current limitations

This remains an alpha prototype, not a release-ready browser. It has not yet been tested on a physical Honor 200. Automated CI builds validate compilation and unit tests, but do not replace device testing.

- Session restoration is metadata-based. It reopens URLs, not full page history, scroll position, form state, or live JavaScript state.
- Folder upload prompts are currently dismissed; single and multiple file selection are supported.
- Download save flow depends on the Android document picker and has not been device-tested for every MIME type or large-file edge case.
- Authentication, date/time, color, WebAuthn, WebExtension management, history UI, content-blocking lists, and robust popup/new-window tab routing remain future work.
- Container cleanup clears cookies and storage for the selected Gecko session context after closing its open tabs. It does not delete the container definition.
- Bookmarks and browser metadata are stored in local SharedPreferences; bookmark encryption and migration to Room/DataStore are not implemented.
- Release signing, Play Store packaging, accessibility review, security audit, and broad automated UI tests are outstanding.

The UI is an original Jetpack Compose implementation inspired by Zen Browser's interaction patterns. It is not an official Zen Browser build or a direct port of desktop frontend code. The project is not affiliated with Mozilla or the Zen Browser team.

## Build

Requirements: JDK 17, Android SDK Platform 36, Build Tools 36.0.0 and Gradle 8.13. The initial device target is Honor 200 (arm64-v8a), so the APK is currently ABI-filtered for 64-bit ARM.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions runs unit tests before assembling the debug APK and publishes the APK as a downloadable artifact.

## Licensing and references

- GeckoView: https://mozilla.github.io/geckoview/ (MPL-2.0)
- GeckoView context isolation API: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoSessionSettings.Builder.html
- Zen Browser desktop: https://github.com/zen-browser/desktop (MPL-2.0)
- Unofficial Zen Android exploration: https://github.com/ApoBen/Zen-Android (MPL-2.0)

Any future code reuse from MPL-2.0 projects must preserve notices and comply with that license.
