# Engine partial-file storage

`EnginePartialFileStore` creates one deterministic `.part` file per engine task under the app-private `engine-partials` directory.

- The task ID is validated against a restricted filename-safe alphabet and length.
- User-provided filenames are never used as path components.
- Canonical paths are checked to remain beneath app-private storage and beneath the dedicated partial directory.
- A directory symlink escaping the app-private root is rejected.
- Repeated requests for the same task return the same file; different task IDs remain isolated.

This component only allocates safe partial-file locations. It does not schedule work, persist task records, publish completed files, or expose controls in the UI.
