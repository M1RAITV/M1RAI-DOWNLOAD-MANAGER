# Foundation Decision — M1RAI Download Manager

Date: 2026-11-13. Android-only Kotlin/Jetpack Compose architecture selected. Android DownloadManager remains the active user-facing direct-link implementation. The app-owned engine is experimental and not integrated into the UI.

## Current architecture
- Android-only native application; phone-first GitHub workflow and cloud CI.
- Direct HTTP/HTTPS downloads via Android DownloadManager.
- Public Downloads destination using the M1RAI DOWNLOADS relative path.
- Compose UI, share-to-app, link analyzer, SQLite task metadata/history.
- Experimental app-owned HTTP engine, persistence, partial-file handling, recovery checks, runner, and publisher.
- API 26–33 foreground service with progress/status notification.
- API 34+ JobInfo contract and execution policy only; no JobService exists or is registered.

## Critical integration boundary
The foreground service is not wired to the normal add-download UI. The JobInfo contract only constructs a scheduling specification. The active user-facing flow still uses Android DownloadManager and has no general pause/resume controls. Do not advertise advanced engine controls as delivered until integrated and device-verified.

## Known gaps
- No physical Android device verification.
- No API 34+ JobService.
- No API-aware execution coordinator connected to the user-facing queue.
- No general pause/resume, speed, or ETA in the active UI.
- Error explanation, conflict handling, and complete history require more work.
- No signed release or Play Store submission.

## Next engineering gate
1. Implement and register API 34+ user-initiated data-transfer JobService.
2. Add an API-aware execution coordinator for API 26–33 service, API 34+ job, and legacy fallback.
3. Add tests for routing and lifecycle.
4. Connect the normal queue flow only after tests pass; preserve the existing DownloadManager path until device verification.
5. Test on physical devices before claiming reliability.

## Licence
The project is MIT. Audit third-party dependency licences before release. Gopeed remains research only; its upstream GPL-3.0 licence has not been adopted into M1RAI.

## References
- https://developer.android.com/reference/android/app/DownloadManager
- https://developer.android.com/develop/background-work/background-tasks/data-transfer-options
- https://developer.android.com/develop/background-work/services/fgs/changes
- https://developer.android.com/google/play/requirements/target-sdk
- https://support.google.com/googleplay/android-developer/answer/9888072

No release or physical-device verification is claimed.
