# Process-restart recovery gate

`EngineTransferRecovery` classifies persisted app-engine tasks before a worker is allowed to resume them.

A partial transfer is resumable only when all of these conditions hold:

- Its state is PAUSED, INTERRUPTED, CONNECTING, or DOWNLOADING.
- Its canonical partial-file path remains inside the supplied app-private directory.
- The file is a regular file and its actual length exactly equals the persisted byte offset.
- A strong ETag or Last-Modified validator was persisted.

If the file is absent/empty, its length disagrees with metadata, or no validator exists, recovery requests a fresh transfer from byte zero. It never authorizes appending based on a database offset alone. Completed/cancelled and otherwise ineligible tasks are not automatically recovered.

This is a tested decision gate, not yet a running recovery service. Startup scanning, persisting INTERRUPTED transitions, worker scheduling, engine execution, and notification restoration remain separate integration work.
