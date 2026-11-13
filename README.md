# M1RAI Download Manager

A free, open-source Android download manager built with Kotlin and Jetpack Compose.

## Status

Early development prototype; not a production or Play Store release.

## Current user-facing prototype
- Direct HTTP/HTTPS links queued through Android DownloadManager.
- Download status, progress, and downloaded size.
- Plain-text share-to-app support.
- Downloads saved under the public Downloads directory using the M1RAI DOWNLOADS destination path.

## Experimental transfer engine
An app-owned HTTP engine, persistence, partial-file handling, recovery logic, and runner exist. An API 26–33 foreground service reports progress/status notifications. An Android 14+ JobInfo contract and SDK execution policy also exist. These components are NOT connected to the normal add-download UI. The API 34+ JobService is NOT implemented or registered. Advanced pause/resume must not be described as user-ready.

## Known limitations
- Website-specific media extraction is not implemented.
- The active DownloadManager flow has no general pause/resume controls.
- Speed and ETA are not displayed.
- Error details, file conflict handling, and persistent history need more work.
- No physical-device verification, Play Store readiness, or release build is claimed.

## Build
GitHub Actions builds a debug APK and runs JVM unit tests. See .github/workflows/android.yml and the Actions tab.

## Project plan and handoff
See ROADMAP.md, docs/FOUNDATION-DECISION.md, and docs/MASTER-HANDOFF-AND-CONTINUATION-PROMPT.md.

## Responsible use
Only download files you own or are authorized to download. Do not bypass DRM, paywalls, authentication, private access, or platform protections. Respect copyright, service terms, and applicable law.

## Licence
MIT. See LICENSE.
