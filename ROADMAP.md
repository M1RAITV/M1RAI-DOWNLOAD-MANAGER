# M1RAI Download Manager

A planned, modern download manager focused on simplicity, performance, and user control.

## Product direction

- Android first, with iOS considered in the architecture from the beginning.
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
- [x] Recommend an independent Flutter app with a focused background-download package; see [FOUNDATION-DECISION.md](docs/FOUNDATION-DECISION.md).
- [ ] Validate the recommended download engine with an Android proof of concept before adoption.
- [ ] Document architecture and project conventions.

### Phase 1 — Android MVP
- [ ] Create the application shell and navigation.
- [ ] Implement direct-link analysis for ordinary downloadable files.
- [ ] Implement the download engine, queue, progress, pause/resume, cancel, and retry.
- [ ] Add completed/failed history and local persistence.
- [ ] Build and test an installable Android APK.

### Phase 2 — Android integration and polish
- [ ] Add Android share-to-app support.
- [ ] Improve background-download reliability and notifications.
- [ ] Deliver polished dark and light themes and accessible states.
- [ ] Test large files, interrupted connections, and server range support.

### Phase 3 — Permitted media adapters
- [ ] Add modular adapters for sources where downloading is authorized and technically supported.
- [ ] Show available formats and quality only when verified by the source.
- [ ] Test each adapter independently and document limitations.

### Phase 4 — iOS
- [ ] Implement iOS share-sheet and Files integration.
- [ ] Adapt download lifecycle and background behavior to iOS restrictions.
- [ ] Build and test through an appropriate Apple-compatible CI/signing workflow.

### Phase 5 — Release and maintenance
- [ ] Complete security, privacy, reliability, and licence review.
- [ ] Publish user documentation and contribution guidelines.
- [ ] Prepare signed release builds and a versioned changelog.

## Principles and limitations

- Never bypass DRM, paywalls, private access, authentication restrictions, or platform protections.
- Only support downloads that the user is authorized to make and that applicable platform terms and laws permit.
- A publicly viewable link does not automatically grant download permission.
- Do not claim a source, feature, build, or test works until it has been verified.
- Preserve working functionality; make changes for demonstrated reasons and test them.

## Current verified status

- [x] Public GitHub repository created.
- [x] GitHub Codex Connector installation/authorization initiated by the repository owner.
- [ ] Repository write access verified by a successful commit from the connected development tools.
- [ ] Foundation and licence audit completed.
- [ ] Application implementation started.

This roadmap describes intended work, not features already delivered. Update checkboxes only after evidence-backed verification.
