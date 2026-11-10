# M1RAI Download Manager

A planned, modern download manager focused on simplicity, performance, and user control.

## Product direction

- Android only. iOS is explicitly out of scope.
- Local-first core functionality; no account required for ordinary downloads.
- Clean, premium interface with dark and light themes.
- Simple workflow: add or share a link, analyze what is available, choose a supported file or media option, then download.
- Organised, modular source code that developers can understand, maintain, and extend.

## Planned capabilities

- Direct downloads for supported files such as documents, PDFs, archives, images, audio, and video.
- Download queue and active, completed, failed, and cancelled states.
- Progress, transfer speed, downloaded size, and ETA when the server provides enough information.
- Pause, resume, cancel, retry, and history.
- Resume large downloads when the remote server supports range requests.
- Android share-to-app integration.
- Link and media analysis only where technically reliable and permitted.
- Source adapters designed as separate modules, without promising universal website compatibility.

## Delivery roadmap

### Phase 0 — Foundation audit
- [x] Perform a preliminary review of Gopeed as a candidate; see [FOUNDATION-AUDIT.md](docs/FOUNDATION-AUDIT.md).
- [ ] Complete comparative foundation and dependency licence audit before adoption.
- [x] Remove cross-platform/iOS requirements from the project scope.
- [x] Validate the initial native Android shell and direct-download prototype with a cloud-built proof of concept (system DownloadManager only; advanced controls remain pending).
- [ ] Document architecture and project conventions.

### Phase 1 — Android MVP
- [x] Create the initial Kotlin/Jetpack Compose Android shell and direct-link download prototype (early prototype; not release-ready).
- [x] Implement an HTTP metadata analyzer with HEAD and one-byte range fallback.
- [x] Connect link analysis to the app screen with filename, MIME type, size, and range-support details (cloud CI and device verification pending).
- [ ] Replace/extend the prototype engine to support robust pause/resume, retry, and detailed task control.
- [ ] Add durable in-app history and persistent task metadata.
- [x] Pass cloud CI build/tests and verify a debug APK artifact was uploaded (not a release build).

### Phase 2 — Android integration and polish
- [x] Add initial plain-text Android share-to-app intent support (needs device verification).
- [ ] Improve background-download reliability and notifications.
- [ ] Deliver polished dark and light themes and accessible states.
- [ ] Test large files, interrupted connections, and server range support.

### Phase 3 — Permitted media adapters
- [ ] Add modular adapters for sources where downloading is authorized and technically supported.
- [ ] Show available formats and quality only when verified by the source.
- [ ] Test each adapter independently and document limitations.

### Phase 4 — Release and maintenance
- [ ] Complete security, privacy, reliability, and dependency licence review.
- [x] Add initial README, privacy disclosure, security policy, and contribution guide.
- [ ] Prepare signed release builds, store listing, and a versioned changelog.

## Principles and limitations

- Never bypass DRM, paywalls, private access, authentication restrictions, or platform protections.
- Only support downloads that the user is authorized to make and that applicable platform terms and laws permit.
- A publicly viewable link does not automatically grant download permission.
- Do not claim a source, feature, build, or test works until it has been verified.
- Preserve working functionality; make changes for demonstrated reasons and test them.

## Current verified status

- [x] Public GitHub repository created.
- [x] GitHub Codex Connector installation/authorization initiated by the repository owner.
- [x] Repository write access verified by successful commits from connected development tools.
- [ ] Foundation and licence audit completed.
- [x] Application implementation started.

The successful cloud APK is a debug prototype only. It has not been installed or verified on a physical Android device. This roadmap describes intended work, not features already delivered. Update checkboxes only after evidence-backed verification.
