# M1RAI Download Manager — Master Handoff

## Repository and workflow
- Repository: https://github.com/M1RAITV/M1RAI-DOWNLOAD-MANAGER
- Branch: main. Android only; Kotlin + Jetpack Compose.
- Maintainer uses Android phone and GitHub; use cloud builds, never assume desktop tools.

## Non-negotiable rules
1. Continue from main; inspect code and docs before editing.
2. Never restart from scratch.
3. Preserve working behavior; smallest coherent change only.
4. Bundle related source, tests, roadmap and handoff updates into ONE commit per task, then run CI ONCE.
5. Never start the next task until current CI result is known.
6. Separate implemented, cloud-built, unit-tested and physical-device-verified claims.
7. When user says Next, continue autonomously; do not ask them to repeat recorded context.
8. Only support authorized downloads; no DRM, paywall, authentication or private-access bypass.

## Current architecture
- Active user-facing direct HTTP/HTTPS path remains Android DownloadManager.
- Link analyzer, SQLite metadata, share-to-app, download list, basic retry/cancel and M1RAI DOWNLOADS destination path exist.
- Experimental app-owned HTTP engine, persistence, partial-file/recovery, runner and publisher exist.
- API 26–33 foreground service reports progress but is not yet connected to UI.
- API 34+ JobInfo contract exists.
- API 34+ UserInitiatedTransferJobService has now been added and registered with RUN_USER_INITIATED_JOBS and BIND_JOB_SERVICE. It executes the existing runner, sets the UIDT notification, pauses on onStopJob and returns reschedule=true. This is implementation only until CI and device tests pass.
- No API-aware queue coordinator yet; do not switch the UI away from DownloadManager.

## Last previously verified CI
Commit 44aec3102cfac1cd0bf143eb9db6fbbf6fa40ff7; run https://github.com/M1RAITV/M1RAI-DOWNLOAD-MANAGER/actions/runs/43433914500. Build, unit tests and APK artifact upload passed. Documentation commits #86–#89 also passed. No physical-device verification claimed.

## Next task after this commit passes
Add a single API-aware execution coordinator for API 26–33 foreground service, API 34+ UIDT job and API 21–25 DownloadManager fallback. Add tests. Keep existing user-facing DownloadManager path intact until routing and device tests are complete.

## Continuation
On Next, inspect latest main and CI first. Bundle source, tests, ROADMAP and this handoff into one commit, run CI once, and report evidence. Never make the maintainer repeat project history.
