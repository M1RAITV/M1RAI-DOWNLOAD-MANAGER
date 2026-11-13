package com.m1rai.downloadmanager

internal object DownloadStoragePermissionPolicy {
    /** Public Downloads writes need runtime permission only on Android 6 through Android 9. */
    fun requiresRuntimePermission(sdkInt: Int): Boolean = sdkInt in 23..28
}
