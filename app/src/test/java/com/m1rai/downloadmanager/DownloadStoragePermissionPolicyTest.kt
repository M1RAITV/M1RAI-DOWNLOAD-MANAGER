package com.m1rai.downloadmanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStoragePermissionPolicyTest {
    @Test fun doesNotRequestRuntimePermissionOnAndroidFive() {
        assertFalse(DownloadStoragePermissionPolicy.requiresRuntimePermission(21))
        assertFalse(DownloadStoragePermissionPolicy.requiresRuntimePermission(22))
    }

    @Test fun requestsRuntimePermissionOnAndroidSixThroughNine() {
        (23..28).forEach { assertTrue(DownloadStoragePermissionPolicy.requiresRuntimePermission(it)) }
    }

    @Test fun doesNotRequestLegacyPermissionOnAndroidTenAndNewer() {
        assertFalse(DownloadStoragePermissionPolicy.requiresRuntimePermission(29))
        assertFalse(DownloadStoragePermissionPolicy.requiresRuntimePermission(36))
    }
}
