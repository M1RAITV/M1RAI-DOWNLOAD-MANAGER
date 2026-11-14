# M1RAI Download Manager

## Product direction
- Android only; local-first Kotlin/Jetpack Compose.
- Phone-first GitHub workflow with cloud builds.
- Direct links and authorized media only. Never bypass DRM, paywalls, authentication, or private access.

## Delivery roadmap
### Phase 0 — Foundation
- [x] Android-only foundation and cloud-built direct-download prototype.
- [ ] Complete dependency and foundation licence audit.

### Phase 1 — Android MVP
- [x] Direct HTTP/HTTPS downloads through Android DownloadManager.
- [x] Link analysis, SQLite metadata, share-to-app, queue/status UI.
- [x] Unit tests and successful cloud debug build.
- [ ] Finish robust user-facing pause/resume/retry and history.

### Phase 2 — Transfer engine
- [x] Experimental app-owned HTTP engine, persistence, partial-file and recovery components.
- [x] API 26–33 foreground service with progress notifications (not connected to UI).
- [x] API 34+ JobInfo contract.
- [x] API 34+ JobService implementation and manifest registration (Build #90 passed; device verification pending).
- [x] Corrected progress notification interpolation after source review; Build #91 verification pending.
- [x] Add API-aware execution coordinator and Android launcher adapter with routing tests (Build #92 verification pending).
- [ ] Connect queue only after device verification; preserve stable DownloadManager flow until then.
- [ ] Device-test interruptions, process death, notification lifecycle, pause/resume and large files.
- [ ] Finalize accessible dark/light UI.

### Phase 3 — Authorized adapters
- [ ] Modular adapters only for permitted sources; verify each independently.

### Phase 4 — Release
- [ ] Security, privacy, dependency licence and Play policy review.
- [ ] Signed release and store listing.

## Verified status
Last previously verified CI: commit 44aec3102cfac1cd0bf143eb9db6fbbf6fa40ff7, run 43433914500; build, JVM tests and debug APK upload passed. Documentation-only commits #86–#89 also passed. Physical-device testing is not claimed. New JobService work is pending CI and device verification.

Preserve working behavior. Make minimal, evidence-based changes. Cloud build success is not proof of device behavior or release readiness.
