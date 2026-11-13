# M1RAI Download Manager

## Product direction
- Android only; iOS is out of scope.
- Local-first, no account required for ordinary downloads.
- Premium dark/light UI; phone-first GitHub workflow with cloud builds.
- Direct links and authorized media only; no DRM, paywall, private-access, or authentication bypass.

## Phase 0 — Foundation
- [x] Preliminary Gopeed review (see docs/FOUNDATION-AUDIT.md).
- [ ] Complete comparative foundation and dependency licence audit.
- [x] Android-only Kotlin/Compose foundation and cloud-built direct-download prototype.
- [ ] Document full architecture and conventions.

## Phase 1 — Android MVP
- [x] Direct-link Android shell and HTTP metadata analyzer.
- [x] SQLite metadata store, share-to-app, destination naming, basic queue/status UI.
- [x] Unit tests for URL rules, metadata, destination, transfer state/recovery, and HTTP range behavior.
- [x] Cloud build, JVM tests, and debug APK artifact verified (commit 44aec3102cfac1cd0bf143eb9db6fbbf6fa40ff7; run 43433914500).
- [ ] Complete robust user-facing pause/resume/retry and history integration.

## Phase 2 — Transfer engine and polish
- [x] Experimental app-owned HTTP engine, persistence, partial-file and recovery components.
- [x] API 26–33 foreground service with progress/status notifications (not wired into UI).
- [x] API 34+ JobInfo contract and SDK execution policy (contract only).
- [ ] Implement/register Android 14+ user-initiated transfer JobService.
- [ ] Add one API-aware execution coordinator and connect the normal queue flow.
- [ ] Implement and verify pause/resume/cancel/retry lifecycle.
- [ ] Physical-device tests on Android 8–13 and 14+; process death, network interruption, storage failure, large files.
- [ ] Finalize accessible dark/light themes.

## Phase 3 — Authorized adapters
- [ ] Modular adapters only for sources where downloading is permitted and technically supported.
- [ ] Verify formats/quality per source; document limitations.

## Phase 4 — Release
- [ ] Security, privacy, dependency licence, and Play policy review.
- [ ] Signed release, changelog, and store listing.

## Verified status
Latest cloud run 43433914500 passed assembleDebug, testDebugUnitTest, and APK upload. This is a debug artifact only; no physical-device installation or end-to-end device test is claimed. The experimental engine is not integrated into the user-facing queue. Update checkboxes only with evidence.

## Principles
Preserve working behavior. Make minimal, evidence-based changes. A successful cloud build is not proof of device behavior or release readiness.
