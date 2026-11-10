# Foundation Decision — M1RAI Download Manager

Date: 2026-11-10
Status: Android-only direction selected. Implementation choice remains subject to a working proof of concept.

## Product platform

M1RAI Download Manager will be built for **Android only**.

- iOS is out of scope.
- Do not spend time preserving iOS compatibility or selecting libraries for cross-platform support.
- Prioritize Android-native download behavior, sharing, notifications, storage, and background execution.
- The maintainer works from an Android phone and GitHub; use cloud builds and phone-compatible workflows. Do not assume a desktop development environment.

## Recommended app foundation

Use **Kotlin + Jetpack Compose** for the Android application.

Why:
1. Android-only scope makes a native Android stack the simplest direct fit.
2. Kotlin and Android's native APIs give direct control over foreground download services, notifications, Android share intents, storage, and system lifecycle behavior.
3. Jetpack Compose provides a modern native UI without introducing a cross-platform layer we no longer need.
4. We can keep the app modular: UI, download engine, link analysis, queue/state, persistence, and source adapters.

## Download-engine approach

Do not adopt Gopeed as the product base. It remains a useful reference, but it brings a full separate application, GPL-3.0 obligations for covered derivatives, and features we do not need for the Android-first MVP.

Do not adopt the earlier Flutter `background_downloader` recommendation either: Flutter is no longer needed for an Android-only product.

For the native Android MVP, first evaluate Android's platform download APIs and a focused Kotlin/Java HTTP client such as OkHttp for cases requiring more control. Use WorkManager for deferrable/retryable work and a foreground service with a user-visible notification for long-running user-initiated transfers where current Android rules require it. The exact combination must be proven in a small build before finalizing.

## First proof of concept

Create a minimal Android app that:
- accepts a direct HTTPS file URL;
- downloads a file to an appropriate user-visible location;
- displays progress and transfer state;
- supports pause/resume where the server supports HTTP range requests;
- supports cancel and retry;
- survives app process death/restart to the extent supported by Android's lifecycle and chosen service design;
- uses a visible notification for long-running transfers as required by Android;
- verifies the downloaded file.

Build using GitHub Actions or another cloud Android build pipeline. Test with automated unit/instrumentation tests and, where available, a cloud Android emulator/device. Do not claim real-device verification unless it actually happened.

## Product principles

- No account required for core downloads.
- No mandatory ads.
- Dark and light themes.
- Keep user interface simple: add/share link, inspect available information, download.
- Source code should be modular and understandable for developers.
- Website/media adapters must be separate from the direct-download engine and used only where permitted.
- No DRM, paywall, private-access, authentication, or platform-protection bypass.

## Licence and dependency gate

Before shipping, inspect the exact licence of every direct and transitive dependency and retain required notices. This is an engineering recommendation, not legal advice.

## Sources

- Android developer documentation: https://developer.android.com/
- Jetpack Compose: https://developer.android.com/compose
- Android background work: https://developer.android.com/develop/background-work
- Android foreground services: https://developer.android.com/develop/background-work/services/foreground-services
- OkHttp: https://github.com/square/okhttp
- Gopeed reference: https://github.com/GopeedLab/gopeed

This is the selected Android-only direction. Do not reintroduce iOS or Flutter unless the product scope changes.
