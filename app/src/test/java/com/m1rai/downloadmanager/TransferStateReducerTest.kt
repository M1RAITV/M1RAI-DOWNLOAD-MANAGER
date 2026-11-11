package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferStateReducerTest {
    @Test fun followsTheNormalDownloadLifecycle() {
        var state = TransferSnapshot()
        state = TransferStateReducer.reduce(state, TransferEvent.Start)
        assertEquals(TransferStatus.CONNECTING, state.status)
        state = TransferStateReducer.reduce(state, TransferEvent.Connected(1_000))
        assertEquals(TransferStatus.DOWNLOADING, state.status)
        state = TransferStateReducer.reduce(state, TransferEvent.Progress(450, 1_000))
        assertEquals(45, state.progressPercent)
        state = TransferStateReducer.reduce(state, TransferEvent.Complete)
        assertEquals(TransferStatus.COMPLETED, state.status)
        assertEquals(1_000L, state.downloadedBytes)
        assertEquals(100, state.progressPercent)
    }
    @Test fun progressNeverMovesBackwardsOrPastKnownTotal() {
        var state = TransferSnapshot(status = TransferStatus.DOWNLOADING, downloadedBytes = 300, totalBytes = 500)
        state = TransferStateReducer.reduce(state, TransferEvent.Progress(200, 500))
        assertEquals(300L, state.downloadedBytes)
        state = TransferStateReducer.reduce(state, TransferEvent.Progress(900, 500))
        assertEquals(500L, state.downloadedBytes)
        assertEquals(100, state.progressPercent)
    }
    @Test fun pauseResumeAndRetryPreserveOnlySafeState() {
        var state = TransferSnapshot(status = TransferStatus.DOWNLOADING, downloadedBytes = 120, totalBytes = 400)
        state = TransferStateReducer.reduce(state, TransferEvent.Pause)
        assertEquals(TransferStatus.PAUSED, state.status)
        state = TransferStateReducer.reduce(state, TransferEvent.Resume)
        assertEquals(TransferStatus.CONNECTING, state.status)
        state = TransferStateReducer.reduce(state, TransferEvent.Fail("network"))
        assertEquals(TransferStatus.FAILED, state.status)
        assertEquals("network", state.error)
        state = TransferStateReducer.reduce(state, TransferEvent.Retry)
        assertEquals(TransferStatus.QUEUED, state.status)
        assertEquals(0L, state.downloadedBytes)
        assertNull(state.error)
    }
    @Test fun invalidTransitionsAreIgnored() {
        val completed = TransferSnapshot(status = TransferStatus.COMPLETED, downloadedBytes = 100, totalBytes = 100)
        assertEquals(completed, TransferStateReducer.reduce(completed, TransferEvent.Pause))
        assertEquals(completed, TransferStateReducer.reduce(completed, TransferEvent.Cancel))
        assertEquals(completed, TransferStateReducer.reduce(completed, TransferEvent.Progress(20, 100)))
    }
    @Test fun interruptedTransfersCanBeResumedWithoutClaimingTheyAreDownloading() {
        val interrupted = TransferSnapshot(status = TransferStatus.INTERRUPTED, downloadedBytes = 75, totalBytes = 200)
        val resumed = TransferStateReducer.reduce(interrupted, TransferEvent.Resume)
        assertEquals(TransferStatus.CONNECTING, resumed.status)
        assertEquals(75L, resumed.downloadedBytes)
    }
}
