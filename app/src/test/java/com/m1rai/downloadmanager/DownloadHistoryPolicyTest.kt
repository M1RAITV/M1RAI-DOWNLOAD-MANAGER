package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadHistoryPolicyTest {
    @Test fun activeStatusesBecomeInterruptedWhenSystemEntryDisappears() {
        assertEquals("Interrupted", DownloadHistoryPolicy.statusWhenMissing("Queued"))
        assertEquals("Interrupted", DownloadHistoryPolicy.statusWhenMissing("Downloading"))
        assertEquals("Interrupted", DownloadHistoryPolicy.statusWhenMissing("Paused"))
    }

    @Test fun terminalStatusesRemainUnchangedInHistory() {
        assertEquals("Cancelled", DownloadHistoryPolicy.statusWhenMissing("Cancelled"))
        assertEquals("Completed", DownloadHistoryPolicy.statusWhenMissing("Completed"))
        assertEquals("Failed", DownloadHistoryPolicy.statusWhenMissing("Failed"))
    }

    @Test fun progressIsSafeForUnknownNegativeAndOversizedValues() {
        assertEquals(0, DownloadHistoryPolicy.progressPercent(50, 0))
        assertEquals(0, DownloadHistoryPolicy.progressPercent(-10, 100))
        assertEquals(50, DownloadHistoryPolicy.progressPercent(50, 100))
        assertEquals(100, DownloadHistoryPolicy.progressPercent(150, 100))
        assertEquals(100, DownloadHistoryPolicy.progressPercent(Long.MAX_VALUE, Long.MAX_VALUE))
    }
}
