# Platform-aware transfer execution policy

## Purpose

Choose a safe Android execution contract by OS version before connecting the custom HTTP transfer engine to background execution. This policy is pure Kotlin and has no Android framework side effects.

## Current decision table

| Android API | Selected mode | Reason |
|---|---|---|
| 21–25 | Existing Android DownloadManager | Preserve the proven legacy path; do not introduce a custom background runner on these versions yet. |
| 26–33 | Data-sync foreground service | Android 8+ requires notification-channel handling; foreground execution must be started while the app is eligible to start it. |
| 34+ | User-initiated data transfer JobScheduler job | Android 14 introduced the dedicated user-initiated data transfer job path for user-started transfers. |

## Safety constraints before implementation

- The policy does not schedule work or change current app behavior.
- Existing DownloadManager rows and downloads must remain untouched.
- API 26–33 service implementation must declare the correct dataSync service type and foreground-service permissions, create a notification channel on API 26+, and start only from an allowed user-visible context.
- API 34+ implementation must use JobScheduler's user-initiated data transfer contract, provide ongoing user-visible status, and handle job stop/retry callbacks.
- Do not treat a user-initiated job as a general-purpose scheduler for silent, deferred, or periodic downloads.
- Android 15+ dataSync foreground-service time limits are another reason not to route API 34+ transfers through a long-running dataSync service.
- Notification runtime permission is requested only on API 33+ when needed; denial must not crash or silently discard the transfer.
- No worker/service/job integration is included in this change. The custom HTTP engine remains a blocking primitive that must run off the main thread.

## Verification

TransferExecutionPolicyTest covers API boundaries 21, 25, 26, 33, 34, 35, 36, and 37. These are policy tests, not emulator/runtime tests. The next integration gate must add framework-level tests and verify actual notification and job behavior on representative Android API levels before routing production downloads through it.
