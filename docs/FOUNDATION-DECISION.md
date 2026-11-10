# Foundation Decision — M1RAI Download Manager

Date: 2026-11-10
Status: Android-only architecture selected; first prototype committed. The engine choice is provisional until feature and reliability tests pass.

## Product platform

M1RAI Download Manager is Android only. iOS is out of scope. The maintainer uses a phone and GitHub, so cloud builds and phone-compatible repository workflows are required.

## App foundation

Use Kotlin and Jetpack Compose for the native Android application. This gives direct access to Android sharing, notifications, storage, and platform download APIs without a cross-platform layer.

## Download-engine decision

The first prototype uses Android's system DownloadManager for direct HTTP/HTTPS downloads. Android's API handles long-running HTTP transfers in the background, retries across failures/connectivity changes, and reports progress/status.

This is a deliberate MVP choice, not the final feature-complete engine. The platform DownloadManager does not provide a general pause/resume control to third-party apps. If true user-controlled pause/resume, segmented downloads, per-connection speed, and richer retry behavior are required, evaluate a focused Kotlin transfer engine based on OkHttp and Android's user-initiated data transfer APIs. Android 14+ provides user-initiated data transfer jobs for user-started network transfers; current Play guidance recommends them for this use case. Do not use a dataSync foreground service without addressing Android 15+ time limits and Play policy.

## Current implementation

- Kotlin/Jetpack Compose application module.
- Direct HTTP/HTTPS URL enqueue via Android DownloadManager.
- System download notifications and public Downloads destination.
- Download list with status and progress polling.
- Plain-text Android share intent.
- GitHub Actions cloud build workflow.
- MIT project licence and initial contributor/privacy/security documentation.

## Known gaps

- The current code is an early prototype and has not yet been built or tested by CI.
- No general pause/resume controls.
- No detailed error explanation, speed, or ETA.
- No robust in-app database/history yet.
- No website-specific media extraction.
- No signed release or Play Store submission.
- No real-device verification has been performed.

## Build and Play compliance

Google Play submissions from 31 August 2026 must target Android 16 / API 36 or higher. The current project is configured for target API 36. Play also requires a privacy policy and accurate Data safety declarations. Intellectual-property policy prohibits apps that encourage unauthorized downloading of copyrighted works.

## Licence

The project is released under MIT. This allows broad use, modification, redistribution, and commercial reuse, provided the copyright and licence notice are retained. Third-party dependencies keep their own licences; those must be checked before release.

## Next engineering gate

1. Run the GitHub Actions build and fix all build errors.
2. Add automated tests for URL validation, filename handling, download states, and error paths.
3. Verify the APK artifact can be downloaded from the workflow.
4. Improve download controls with an Android-compliant user-initiated transfer implementation.
5. Only then expand into advanced link analysis and permitted source adapters.

## References

- Android DownloadManager: https://developer.android.com/reference/android/app/DownloadManager
- Android user-initiated data transfer: https://developer.android.com/develop/background-work/background-tasks/data-transfer-options
- Android foreground service changes: https://developer.android.com/develop/background-work/services/fgs/changes
- Google Play target API requirements: https://developer.android.com/google/play/requirements/target-sdk
- Google Play intellectual property policy: https://support.google.com/googleplay/android-developer/answer/9888072
- Google Play privacy policy and review preparation: https://support.google.com/googleplay/android-developer/answer/9859455

No release or device verification is claimed until evidence is available.
