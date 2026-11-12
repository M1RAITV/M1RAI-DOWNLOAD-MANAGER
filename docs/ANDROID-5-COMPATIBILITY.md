# Android 5.0 Compatibility Baseline

## Product decision

M1RAI Downloader targets Android 5.0 (API 21) as its minimum supported platform.
Android 4.4 and earlier are out of scope for this architecture because the application
uses Jetpack Compose, whose supported Android baseline is API 21.

## Build configuration

- `minSdk = 21` — Android 5.0 (Lollipop).
- `compileSdk = 36` — compile against the current project SDK.
- `targetSdk = 36` — retain current target behavior; this does not raise the minimum OS.
- Java/Kotlin bytecode target remains 17. Android Gradle Plugin/D8 handles Android
  bytecode conversion; this setting alone is not evidence of device compatibility.

## Compatibility rules

1. Do not use Android framework APIs newer than API 21 without an SDK-version guard
   and a compatible fallback.
2. Keep runtime permissions and notification behavior version-aware. In particular,
   notification runtime permission is relevant only on Android 13 (API 33) and later;
   older versions must not be asked for it at runtime.
3. Treat public Downloads storage and scoped-storage behavior separately:
   Android 10+ and Android 11+ have storage behavior that differs from Android 5–9.
   Do not assume one storage implementation works unchanged across all versions.
4. Keep downloads and metadata in app-private storage unless an explicit export step
   uses the correct platform-specific destination API.
5. Test the download engine independently from UI and platform services. A green
   JVM test suite does not prove installation or runtime behavior on API 21.
6. Preserve the existing legacy Android DownloadManager records and behavior while
   introducing any newer execution path.

## Current verification status

- Repository CI Build #73 passed on commit `cdd0e67d3cd9d3b09d322dde4bae89d8731e1f60`.
- The build completed the debug APK build, unit tests, and APK upload successfully.
- The API 21 minimum is declared in Gradle and accepted by the current build configuration.
- The app's current Compose BOM is `2025.08.01` (Compose 1.9 generation); Compose 1.9 and earlier retain API 21 as their minimum. Do not upgrade to Compose 1.10+ without a deliberate API 23 compatibility decision.
- No Android 5.0 emulator or physical-device runtime test has been performed here. A successful build is not proof of runtime compatibility.
- Next verification gate: install and launch the APK on an API 21 emulator/device, then exercise link analysis, enqueue, notification visibility, cancellation, and history refresh.

## Dependency baseline to keep under review

The app currently uses Jetpack Compose, Activity Compose 1.10.1, Lifecycle Runtime
2.9.2, OkHttp 4.12.0, and AndroidX test/Robolectric dependencies. Re-audit transitive
AndroidX minimum SDK metadata whenever these versions change. Test-only dependencies
do not ship in the APK, but they still affect CI test execution.
