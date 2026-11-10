# M1RAI Download Manager

A free, open-source Android download manager focused on a clean experience, reliable file transfers, and a codebase developers can understand and improve.

## Status

Early development prototype. This is not yet a production or Play Store release.

## Technology

- Android only
- Kotlin and Jetpack Compose
- Android system DownloadManager for the first direct-link prototype
- GitHub Actions for cloud builds
- MIT licence

## Current prototype

- Accepts direct HTTP and HTTPS file links.
- Queues downloads using Android DownloadManager.
- Displays status, progress, and downloaded size.
- Receives plain-text links shared from other apps.
- Uses Android's system download notification and Downloads folder.

## Known limitations

- Website-specific media extraction is not implemented.
- General pause/resume controls are not implemented in this prototype.
- Speed and ETA are not displayed yet.
- Error details, file conflict handling, and persistent in-app history need improvement.
- No Play Store readiness or real-device testing is claimed.

## Build

The GitHub Actions workflow in .github/workflows/android.yml builds a debug APK in the cloud. Open the Actions tab, select Android Build, and run the workflow. Download the generated artifact when the run succeeds.

## Project plan

See ROADMAP.md and docs/FOUNDATION-DECISION.md.

## Responsible use

Only download files you own or are authorized to download. Do not bypass DRM, paywalls, authentication, private access, or platform protections. Respect copyright, service terms, and applicable law.

## Contributing

See CONTRIBUTING.md.

## Licence

MIT. See LICENSE.
