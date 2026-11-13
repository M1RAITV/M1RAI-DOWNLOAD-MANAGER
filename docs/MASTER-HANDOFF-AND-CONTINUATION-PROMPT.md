# M1RAI Download Manager — Master Handoff

## Repository
- https://github.com/M1RAITV/M1RAI-DOWNLOAD-MANAGER
- Branch: main
- Android only; Kotlin + Jetpack Compose.
- Maintainer workflow: phone-first GitHub edits and GitHub Actions cloud builds. Never assume PC, Android Studio, or desktop Git.

## Working rules
1. Continue from latest main; inspect current source and docs before edits.
2. Never restart or rebuild from scratch.
3. Preserve working behavior; make minimal, evidence-based changes.
4. Update ROADMAP.md and this handoff after meaningful milestones.
5. Separate implementation, cloud build, unit-test, and physical-device verification claims.
6. When user says Next, proceed autonomously with the next safe roadmap task. Do not ask them to repeat documented context.
7. Only support downloads the user is authorized to make. Never bypass DRM, paywalls, authentication, private access, or platform protections.

## Product direction
Premium, local-first Android download manager. Accept direct links and shared text links, analyze supported files, queue transfers, show progress/status, and organize downloads in the public Downloads directory under the M1RAI DOWNLOADS path. Keep the folder name configurable as a future product capability. MIT project licence; audit third-party dependencies separately.

## Current verified implementation
- Active user-facing flow: direct HTTP/HTTPS via Android DownloadManager.
- Link analyzer, SQLite metadata/history, share-to-app, download list, basic retry/cancel, destination naming.
- Experimental app-owned HTTP engine, persistence, partial-file/recovery components, runner and publisher.
- API 26–33 foreground service exists with progress/status notification but is NOT wired into the normal UI.
- API 34+ JobInfo contract and SDK policy exist, but NO JobService is implemented or registered.
- Current user-facing flow still has no general pause/resume, speed, or ETA.

## Latest verified CI
- Commit: 44aec3102cfac1cd0bf143eb9db6fbbf6fa40ff7
- Run: https://github.com/M1RAITV/M1RAI-DOWNLOAD-MANAGER/actions/runs/43433914500
- assembleDebug: passed; testDebugUnitTest: passed; debug APK upload: passed.
- Artifact: m1rai-download-manager-debug.
- Physical-device installation and behavior: NOT verified.

## Next engineering task
Implement and register the Android 14+ user-initiated data-transfer JobService with task ID validation, progress/status notification, and correct completion/failure lifecycle. Add tests. Then implement a single API-aware execution coordinator and connect it to the queue only after tests pass. Preserve the current DownloadManager path until the replacement is verified on physical devices.

Before implementation, inspect the manifest, Gradle config, current foreground service, runner, persistence, execution policy, and tests. Check current official Android documentation for JobService and foreground-service restrictions. Never mistake the JobInfo contract for a functioning scheduled transfer.

## Continuation
On Next, resume from main and this handoff. Make progress directly where safe, update roadmap/handoff, run CI, and report the exact evidence and Actions link. Do not make the maintainer repeat project history.