package com.m1rai.downloadmanager

import android.app.job.JobInfo
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserInitiatedTransferJobContractTest {
    private val service: ComponentName
        get() = ComponentName(ApplicationProvider.getApplicationContext<Context>(), "com.m1rai.downloadmanager.FutureTransferJobService")

    @Test fun buildsUserInitiatedNetworkTransferWithPersistedTaskIdentity() {
        val job = UserInitiatedTransferJobContract.build(1001, service, "task_123", 34)
        assertEquals(1001, job.id)
        assertTrue(job.isUserInitiated)
        assertEquals(JobInfo.NETWORK_TYPE_ANY, job.networkType)
        assertEquals("task_123", job.extras.getString(UserInitiatedTransferJobContract.EXTRA_TASK_ID))
        assertEquals(service, job.service)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOldAndroidVersions() {
        UserInitiatedTransferJobContract.build(1001, service, "task_123", 33)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeTaskIdentifiers() {
        UserInitiatedTransferJobContract.build(1001, service, "../unsafe", 34)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonpositiveJobIds() {
        UserInitiatedTransferJobContract.build(0, service, "task_123", 34)
    }
}
