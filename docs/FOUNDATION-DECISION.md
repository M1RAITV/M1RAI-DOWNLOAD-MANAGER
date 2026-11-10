# Foundation Decision — M1RAI Download Manager

Date: 2026-11-10
Status: Recommended direction; validate through a small Android proof of concept before treating it as final.

## Decision

Build M1RAI as its own Flutter application. Do not fork or embed the full Gopeed application as the product foundation.

For the first direct-download engine, evaluate the Flutter package `background_downloader` (currently published as version 9.6.6 at the time of review). It supports Android and iOS, background transfers, progress/status updates, queues, pause/resume/cancel, and persistence features. Its published package metadata identifies a permissive BSD-3-Clause / MIT licence.

- Package: https://pub.dev/packages/background_downloader
- Changelog: https://pub.dev/packages/background_downloader/changelog
- Documentation: https://github.com/781flyingdutchman/background_downloader

## Why this is the better fit

1. M1RAI gets its own UI, product identity, and project structure rather than inheriting Gopeed's existing application.
2. The package is focused on background file transfers, which is the correct first engine for ordinary direct links.
3. Its permissive licence is a simpler fit for an open-source app that may later be distributed through app stores, subject to dependency and licence verification.
4. It supports Android and iOS, while respecting each platform's background execution limits.
5. We can add link analyzers and source adapters as separate modules later, without making the core download engine responsible for extracting every website.

## Why not adopt full Gopeed now

Gopeed is technically capable and remains a useful reference. However:
- Its repository is GPL-3.0, which creates source and licence obligations for covered distributed derivatives.
- Its current 2.0 line is publicly labelled beta.
- Its published iOS installation guidance says the IPA must be self-signed/installed outside the App Store because BitTorrent-related apps are not accepted there.
- M1RAI does not need to inherit BitTorrent or Gopeed's full feature set for the first release.

Gopeed may still be studied for ideas, but no Gopeed source code is being copied into M1RAI.

## Important limits

- `background_downloader` is a transfer engine, not a universal website video extractor.
- Pause/resume depends on server support and platform behavior.
- Android and iOS impose background execution limits; no app can guarantee every transfer will continue under every OS condition.
- Website-specific media extraction must be added separately and only where permitted.
- All direct and transitive dependency licences must be checked before release.

## Next implementation gate

Create a minimal Android proof of concept using Flutter + background_downloader:
- enqueue a direct HTTPS file;
- show progress and speed where available;
- pause/resume when supported;
- cancel and retry;
- persist task state across app restart;
- verify the resulting file and integrity.

Do not mark the engine adopted until this proof of concept is built and tested. The developer should not need to run local desktop commands; use a GitHub/cloud build workflow compatible with a phone-only maintainer.

## Sources checked

- Gopeed repository and README: https://github.com/GopeedLab/gopeed
- Gopeed iOS installation guidance: https://gopeed.com/docs/install
- background_downloader package: https://pub.dev/packages/background_downloader
- background_downloader changelog: https://pub.dev/packages/background_downloader/changelog

This is an engineering recommendation, not legal advice. Verify exact dependency versions and licence files at the time of implementation and release.
