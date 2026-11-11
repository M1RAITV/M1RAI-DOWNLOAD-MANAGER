# HTTP transfer engine core

`HttpFileTransferEngine` is the first app-owned streaming executor. It is intentionally not wired into the production UI or task database yet.

## Safety guarantees

- Writes only to an app-selected temporary/partial file; it does not publish into public Downloads.
- Resume requires a correct HTTP 206 Content-Range offset, consistent response length, and a matching strong ETag or Last-Modified validator.
- A server that ignores Range, changes validators, or returns an invalid partial range triggers a fresh full GET before any bytes are appended.
- A successful result is returned only when a declared total byte count matches the local file length.
- Pause and cancel interrupt the active OkHttp call and preserve the partial file; callers must persist the returned validators and byte count.
- The caller must run this blocking API on a worker thread and must not treat this primitive as process-death/background scheduling by itself.

## Not yet delivered

No task database integration, MediaStore publishing, Android UIDT/WorkManager scheduler, notification UI, or end-user pause/resume controls are included in this milestone. Do not claim full download-manager functionality until those layers are implemented and tested.
