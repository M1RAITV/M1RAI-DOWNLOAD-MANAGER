# Foundation Audit — Preliminary

Date: 2026-11-10
Repository: M1RAITV/M1RAI-DOWNLOAD-MANAGER
Status: Preliminary research; no foundation adopted yet.

## Candidate: Gopeed

- Upstream: https://github.com/GopeedLab/gopeed
- Official documentation: https://gopeed.com/docs
- Architecture: Flutter UI with a Go download core.
- Stated platform coverage: Android, iOS, desktop, and web.
- Existing capabilities include HTTP/HTTPS, task queues, pause/resume/retry, task recovery, browser integration, JavaScript extensions, and an API.
- The upstream README identifies Gopeed 2.0.0 as a public beta. Its current development instructions require Go 1.25+ and Flutter 3.41+.
- Mobile builds require a cgo/gomobile toolchain in addition to Flutter.

## Licence assessment

The upstream repository declares GPL-3.0. The licence text is present in the upstream LICENSE file.

If M1RAI distributes a modified/combined derivative that is a covered work, GPL-3.0 source-distribution and licence obligations must be respected. This is compatible with an open-source product, but it is a material product decision. Do not remove upstream copyright or licence notices. A legal review may be appropriate before distribution.

## Risks and open questions

1. Gopeed 2.0 is beta; assess whether the stable branch is a better foundation.
2. Its iOS distribution guidance currently describes self-signing/sideloading rather than a normal App Store release. The project's BitTorrent feature is cited as an App Store review obstacle.
3. Confirm which Gopeed modules are actually needed; avoid carrying unrelated protocols/features if they complicate the mobile app.
4. Verify whether its download engine can be cleanly separated from its existing UI and adapted to M1RAI's intended workflow.
5. Audit all transitive dependencies and their licences before shipping.
6. Compare against a clean Flutter implementation using a permissively licensed download engine/library.

## Preliminary recommendation

Keep Gopeed as the leading technical candidate and as a source of architectural reference. Do not fork or copy code into M1RAI yet. First compare stable versus beta, inspect mobile integration boundaries, and assess the GPL-3.0 implications against the intended distribution model.

## Evidence

- Gopeed official introduction: https://gopeed.com/docs
- Gopeed official development guide: https://gopeed.com/docs/dev
- Gopeed upstream repository and README: https://github.com/GopeedLab/gopeed
- Gopeed upstream licence: https://github.com/GopeedLab/gopeed/blob/main/LICENSE
- Flutter supported platforms: https://docs.flutter.dev/reference/supported-platforms

This is a preliminary technical review, not a legal opinion or a claim that Gopeed has been adopted or tested in M1RAI.
