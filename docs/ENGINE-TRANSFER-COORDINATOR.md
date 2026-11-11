# Engine Transfer Coordinator

This stage adds the first orchestration boundary above the existing transfer primitives.

## Responsibilities
- Validate an HTTP(S) URL and a filename that cannot introduce a path.
- Allocate an app-private partial file through `EnginePartialFileStore`.
- Create a QUEUED `EngineTransferRecord` with one injected timestamp.
- Insert through `EngineTransferPersistence`; implementations must make `insertIfAbsent` atomic.
- Delegate recovery eligibility to `EngineTransferRecovery` using the app-private root.

## Explicit non-goals
No SQLite adapter, HTTP execution, lifecycle mutation, completed-file publishing, or job scheduling is added here. The persistence port is tested with an in-memory fake; this does not claim Android SQLite integration.

## Next integration gates
1. Implement the persistence port over `DownloadTaskStore` with a transaction-backed insert-if-absent operation.
2. Add Android database tests for insert collision, read-after-write, and legacy-row isolation.
3. Only then add worker execution with serialized progress writes and pause/cancel recovery.
