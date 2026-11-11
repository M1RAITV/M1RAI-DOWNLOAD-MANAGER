# SQLite Engine Transfer Persistence

The engine coordinator now has a production adapter backed by `DownloadTaskStore`.

## Atomic creation

`SqliteEngineTransferPersistence.insertIfAbsent` delegates to
`DownloadTaskStore.insertEngineTransferIfAbsent`. SQLite enforces the
`engine_transfers.task_id` primary key and the insert uses
`CONFLICT_IGNORE`. A duplicate returns `false`; it never replaces the
existing record. This is the atomic boundary used to protect concurrent task
creation. The coordinator's preliminary lookup remains a fast user-facing
check, not the concurrency guarantee.

## Isolation

Engine records remain in `engine_transfers`. The legacy `downloads` table,
its IDs, and its Android DownloadManager semantics are not migrated or
rewritten by this adapter. Existing version-3 schema and upgrade behavior are
unchanged.

## Verification

Robolectric-backed SQLite tests cover:
- full record insert/read round-trip;
- duplicate task ID rejection without replacing the first record;
- legacy download row preservation while engine records are inserted.

This adapter persists metadata only. It does not schedule work, execute HTTP
transfers, publish files, or connect the coordinator to the UI.
