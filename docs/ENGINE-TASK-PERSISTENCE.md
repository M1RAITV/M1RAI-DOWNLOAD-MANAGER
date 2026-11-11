# Durable engine task metadata

Database version 3 adds an isolated `engine_transfers` table. Existing Android DownloadManager records remain in `downloads`; no legacy IDs, statuses, destination paths, or history rows are converted.

Each engine record persists its stable task ID, source URL, app-owned partial path, display metadata, lifecycle status, downloaded/total byte counts, ETag, Last-Modified, error, and timestamps. Writes use a SQLite transaction and replace only the matching engine task ID.

## Resume safety gate

Before a restored task is offered to the HTTP transfer engine, the caller must verify that the partial file exists and that its actual length exactly equals the persisted `downloaded_bytes`. `EngineTransferRecord.isPartialFileConsistent` exposes this check. A mismatch is not permission to append: mark the task interrupted and restart from zero or require a fresh verified transfer. The HTTP engine must still validate the remote Content-Range and representation validator.

## Scope not yet connected

This commit adds persistence primitives only. The production UI, HTTP engine, process-death recovery coordinator, MediaStore publishing, Android UIDT/WorkManager scheduling, notifications, and user-facing pause/resume are not wired to this table yet.
