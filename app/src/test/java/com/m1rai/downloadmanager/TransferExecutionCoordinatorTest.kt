package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferExecutionCoordinatorTest {
    private class FakeLauncher(
        var scheduleResult: Boolean = true,
        var failure: RuntimeException? = null
    ) : TransferExecutionLauncher {
        val foregroundTasks = mutableListOf<String>()
        val scheduledTasks = mutableListOf<String>()

        override fun startForegroundService(taskId: String) {
            failure?.let { throw it }
            foregroundTasks += taskId
        }

        override fun scheduleUserInitiatedJob(taskId: String): Boolean {
            failure?.let { throw it }
            scheduledTasks += taskId
            return scheduleResult
        }
    }

    @Test fun api21Through25LeaveExistingDownloadManagerFlowInControl() {
        for (sdk in 21..25) {
            val launcher = FakeLauncher()
            val result = TransferExecutionCoordinator(sdk, launcher).launch("task_1")
            assertEquals(TransferLaunchResult.LegacyDownloadManagerRequired, result)
            assertTrue(launcher.foregroundTasks.isEmpty())
            assertTrue(launcher.scheduledTasks.isEmpty())
        }
    }

    @Test fun api26Through33StartTheExistingForegroundService() {
        for (sdk in 26..33) {
            val launcher = FakeLauncher()
            val result = TransferExecutionCoordinator(sdk, launcher).launch("task_2")
            assertEquals(TransferLaunchResult.StartedForegroundService, result)
            assertEquals(listOf("task_2"), launcher.foregroundTasks)
            assertTrue(launcher.scheduledTasks.isEmpty())
        }
    }

    @Test fun api34AndLaterScheduleUserInitiatedJobs() {
        for (sdk in listOf(34, 35, 36, 37)) {
            val launcher = FakeLauncher()
            val result = TransferExecutionCoordinator(sdk, launcher).launch("task_3")
            assertEquals(TransferLaunchResult.ScheduledUserInitiatedJob, result)
            assertEquals(listOf("task_3"), launcher.scheduledTasks)
            assertTrue(launcher.foregroundTasks.isEmpty())
        }
    }

    @Test fun reportsRejectedJobScheduling() {
        val result = TransferExecutionCoordinator(34, FakeLauncher(scheduleResult = false)).launch("task_4")
        assertTrue(result is TransferLaunchResult.Failed)
    }

    @Test fun reportsPlatformLaunchErrorsWithoutThrowingToCaller() {
        val result = TransferExecutionCoordinator(30, FakeLauncher(failure = IllegalStateException("blocked"))).launch("task_5")
        assertEquals(TransferLaunchResult.Failed("blocked"), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeTaskIdentifiersBeforeCallingPlatform() {
        TransferExecutionCoordinator(34, FakeLauncher()).launch("../unsafe")
    }
}
