# Public Download Publication

The engine publisher moves a completed app-private partial file into the user's public Downloads location.

- Default folder: `M1RAI DOWNLOADS`.
- The folder name is a constructor setting, so a future user preference can change it without changing the storage implementation.
- Android 10 (API 29) and newer: publish through `MediaStore.Downloads`, using `RELATIVE_PATH=Download/<folder>` and `IS_PENDING` until the copy is complete.
- Android 5.0–9 (API 21–28): copy to a temporary file inside the public Downloads subfolder, then atomically rename it to its final name. Android 6–9 require the app to obtain `WRITE_EXTERNAL_STORAGE` at runtime before calling the publisher.
- Existing filenames are never overwritten; a numbered suffix is selected.
- Partial files must be non-empty and regular files. Path traversal and invalid folder/file names are rejected.
- A MediaStore row is deleted if copying or publishing fails.

This is a publication primitive only. It is not yet connected to the engine runner, UI, or permission-request flow. The legacy Android DownloadManager path remains unchanged. Device-level validation on API 21 and modern Android is still required.
