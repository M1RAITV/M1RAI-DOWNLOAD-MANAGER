package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferExecutionPolicyTest {
    @Test fun api21Through25KeepTheExistingSystemDownloadManagerPath() {
        for (sdk in 21..25) {
            val plan = TransferExecutionPolicy.plan(sdk)
            assertEquals(TransferExecutionMode.LEGACY_DOWNLOAD_MANAGER, plan.mode)
            assertEquals(false, plan.requiresNotificationChannel)
            assertEquals(false, plan.requiresForegroundServiceType)
            assertEquals(false, plan.requiresUserInitiatedJob)
        }
    }

    @Test fun api26Through33UseTheForegroundServiceContract() {
        for (sdk in 26..33) {
            val plan = TransferExecutionPolicy.plan(sdk)
            assertEquals(TransferExecutionMode.DATA_SYNC_FOREGROUND_SERVICE, plan.mode)
            assertEquals(true, plan.requiresNotificationChannel)
            assertEquals(true, plan.requiresForegroundServiceType)
            assertEquals(false, plan.requiresUserInitiatedJob)
        }
    }

    @Test fun api34AndLaterPreferUserInitiatedTransferJobs() {
        for (sdk in listOf(34, 35, 36, 37)) {
            val plan = TransferExecutionPolicy.plan(sdk)
            assertEquals(TransferExecutionMode.USER_INITIATED_DATA_TRANSFER_JOB, plan.mode)
            assertEquals(true, plan.requiresUserInitiatedJob)
            assertEquals(false, plan.requiresForegroundServiceType)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSdkValues() {
        TransferExecutionPolicy.plan(0)
    }
}
