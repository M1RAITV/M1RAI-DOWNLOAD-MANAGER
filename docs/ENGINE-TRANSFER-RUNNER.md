# Engine transfer runner

## Scope

The runner connects an existing engine task record, the recovery gate, lifecycle reducer,
and blocking HTTP engine. It is deliberately not a scheduler and does not create threads.
Call it only from a background worker/service/job implementation.

## Safety properties

- It operates only on the separate `engine_transfers` table through
  `EngineTransferPersistence`.
- It never reads, updates, removes, or migrates legacy Android DownloadManager rows.
- It validates the partial file through `EngineTransferRecovery` before network work.
- It persists CONNECTING before starting the engine, checkpoints progress at a bounded
  byte interval, and persists the terminal/paused/interrupted outcome.
- It preserves HTTP validators returned by the engine for later safe resume.
- Unknown, terminal, or non-recoverable tasks are not started.
- It does not own a thread, Android Service, WorkManager request, JobScheduler job,
  notification, or permission prompt.

## Integration contract

The caller must execute `EngineTransferRunner.run` off the main thread and retain the
corresponding `TransferControl` to pause or cancel. The next stage is a platform runner
that owns lifecycle, notification, and scheduler callbacks and delegates the actual
blocking transfer to this class.
