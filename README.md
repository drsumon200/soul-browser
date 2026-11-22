# Soul Browser for Android

[![Build APK](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/drsumon200/soul-browser/actions/workflows/build-apk.yml)

Soul Browser is a Zen-inspired Android browser prototype using Kotlin, Jetpack Compose and Mozilla GeckoView.

Repository: https://github.com/drsumon200/soul-browser

## Current prototype scope
- Dark, compact, rounded UI inspired by Zen Browser's design language.
- GeckoView-based web rendering.
- Workspaces to group tabs.
- Inactive sessions request media suspension to reduce background work.
- Named containers mapped to GeckoView `GeckoSessionSettings.contextId`, which partitions cookies and web storage between contexts.
- Private containers use GeckoView private mode and are excluded from saved tab metadata. Bookmark creation is disabled while a private-container tab is active.
- Workspace default-container selection is remembered.
- Basic navigation, tab overview, quick links, local bookmarks and tracking protection enabled by default.
- Basic tab restoration: regular tabs' URL/title/workspace/container metadata is saved locally and reopened on next launch.

## Development status
This is an early alpha prototype, not a release-ready browser. The UI is an original Jetpack Compose implementation inspired by Zen's interaction patterns. It is not an official Zen Browser build and does not yet contain merged code from the upstream Zen Android port. The public port is available at https://github.com/ApoBen/Zen-Android and is licensed MPL-2.0; any future code reuse must preserve its notices and comply with that license.

The prototype includes GeckoView rendering, workspace-scoped tabs, named containers backed by GeckoView context IDs, private containers, tracking protection defaults, navigation controls, and reduced activity for background tabs. Regular tab URL/title/workspace/container metadata is persisted and restored; private tab URLs are deliberately not persisted. This is metadata-based restoration, not full browser-history/scroll/form-state restoration. It has not been compiled or tested on a physical Honor 200. Downloads, history, extension management, content/ad blocking lists, container deletion/cleanup, robust tab lifecycle, and production security review remain outstanding. Bookmarks are stored locally as URL/title pairs. Private-container pages cannot be added to bookmarks from the browser UI.

The workspace, container and regular-tab metadata is persisted locally. Container isolation uses GeckoView's documented context ID API: sessions sharing a context ID share cookies and localStorage, while different IDs partition that storage. A workspace is a tab collection with a default container; tabs can still be opened in another container. Private containers use GeckoView private mode and their tab metadata is not written to preferences. Clearing or deleting a persistent container's underlying Gecko storage is not implemented yet.

## Build
Requirements: Android Studio, JDK 17, Android SDK 35, Gradle 8.11.1. The first device target is Honor 200 (arm64-v8a), so the initial APK is ABI-filtered for 64-bit ARM.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. Push the `main` branch to `drsumon200/soul-browser` to trigger an APK build, or run the workflow manually from the Actions tab. The workflow publishes the debug APK as a downloadable artifact. A successful build has not yet been verified in this development environment.

## Upstream references and licensing
- Zen Browser desktop: https://github.com/zen-browser/desktop (MPL-2.0)
- Unofficial Zen Android exploration: https://github.com/ApoBen/Zen-Android (MPL-2.0)
- GeckoView context isolation API: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoSessionSettings.Builder.html
- GeckoView: https://mozilla.github.io/geckoview/ (MPL-2.0)

This project is not affiliated with the Zen Browser or Mozilla teams. The current UI is an original Android implementation inspired by Zen's interaction patterns, not a direct port of desktop frontend code.
