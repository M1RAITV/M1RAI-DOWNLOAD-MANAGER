# Public Download Publication

The app's current Android DownloadManager flow and the HTTP engine publisher target the user's public Downloads subfolder.

- Default folder: `M1RAI DOWNLOADS`.
- The folder name is centralized so a future user preference can change it without changing the storage implementation.
- Android 10 (API 29) and newer: the engine publisher uses `MediaStore.Downloads`, `RELATIVE_PATH=Download/<folder>`, and `IS_PENDING` until the copy is complete. The current DownloadManager flow uses the same relative subfolder.
- Android 5.0 (API 21) through Android 9 (API 28): the current DownloadManager flow writes into `Download/M1RAI DOWNLOADS`. Android 6–9 require runtime `WRITE_EXTERNAL_STORAGE`; the UI requests it before starting or retrying a download. Android 5.0–5.1 does not require a runtime prompt.
- The engine publisher's API 21–28 path copies to a temporary file inside the public Downloads subfolder, then renames it to its final name.
- Existing filenames are never intentionally overwritten; a numbered suffix is selected. MediaStore collision lookup is scoped to the target relative path.
- Partial engine files must be non-empty and regular files. Path traversal and invalid folder/file names are rejected.
- A MediaStore row is deleted if copying or publishing fails.

## Background execution

- Android 8–13 (API 26–33): `EngineTransferForegroundService` is registered as a non-exported `dataSync` foreground service. It creates a low-importance notification channel, enters foreground state before running work, and serializes tasks on a single worker executor.
- Android 14+ (API 34+): use the user-initiated data-transfer JobService contract; do not start the API 26–33 service path.
- Android 5–7 (API 21–25): keep the existing Android DownloadManager path.
- The foreground service implementation is not yet wired to the app UI. A visible user action must create/persist an engine task and call its guarded start method. Pause/cancel controls, notification progress updates, process-death scheduling/recovery, and Android device runtime tests remain future work.

The engine publisher is still not connected to the UI or runner. Device-level validation on API 21 and modern Android is still required.
