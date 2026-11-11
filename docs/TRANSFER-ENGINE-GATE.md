# Transfer engine implementation gate

This document records the safe migration boundary between the current Android DownloadManager prototype and an app-controlled transfer engine.

## Why this is a separate phase

Android DownloadManager remains responsible for current transfers. Its opaque system IDs and destination handling cannot be treated as offsets into an app-owned partial file. Reinterpreting existing rows as resumable app-engine tasks would risk corrupting or losing downloads. Existing records must remain visible as legacy/system-managed history while new engine-owned tasks use their own persistent identity and work files.

## Required engine contract

1. Persist task identity, source URL, destination metadata, validator headers, partial-file path, byte offset, and lifecycle state before/after I/O.
2. Stream to an app-owned temporary file; never append until HTTP 206, Content-Range offset, expected length, and a strong ETag or Last-Modified validator have been checked.
3. If the server ignores Range or the representation changed, restart safely from byte zero rather than appending a full response to partial data.
4. Flush and atomically finalize the temporary file before reporting success.
5. Treat pause/cancel as cooperative I/O cancellation, persist the exact committed byte count, and recover interrupted tasks after process death.
6. Use Android's appropriate user-initiated background transfer mechanism for user-started long-running work; validate target-SDK and platform behavior before wiring background execution.

## Implemented prerequisite

TransferStateReducer is a pure Kotlin lifecycle model with guarded transitions, monotonic progress, and unit tests. It is not yet connected to the UI or a network executor, so the app must not advertise pause/resume as implemented.

## Migration rule

Do not convert or delete legacy DownloadManager rows. Introduce a distinct engine task ID namespace and preserve old downloads as read-only history until an explicit, tested migration path exists.
