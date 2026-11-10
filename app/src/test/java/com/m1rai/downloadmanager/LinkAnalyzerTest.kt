package com.m1rai.downloadmanager

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkAnalyzerTest {
    @Test fun readsMetadataFromHeadResponse() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Disposition", "attachment; filename=\"manual.pdf\"")
                .addHeader("Content-Type", "application/pdf")
                .addHeader("Content-Length", "2048")
                .addHeader("Accept-Ranges", "bytes")
                .build()
        )
        server.start()
        try {
            val result = LinkAnalyzer().analyze(server.url("/manual").toString()).getOrThrow()
            assertEquals("manual.pdf", result.fileName)
            assertEquals("application/pdf", result.mimeType)
            assertEquals(2048L, result.contentLength)
            assertTrue(result.supportsRanges)
            assertEquals("HEAD", server.takeRequest().method)
        } finally {
            server.shutdown()
        }
    }

    @Test fun fallsBackToSingleByteRangeGetWhenHeadIsRejected() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(405))
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .addHeader("Content-Range", "bytes 0-0/4096")
                .addHeader("Content-Type", "application/octet-stream")
                .setBody("x")
                .build()
        )
        server.start()
        try {
            val result = LinkAnalyzer().analyze(server.url("/file.bin").toString()).getOrThrow()
            assertEquals(4096L, result.contentLength)
            assertTrue(result.supportsRanges)
            assertEquals("HEAD", server.takeRequest().method)
            val rangeRequest = server.takeRequest()
            assertEquals("GET", rangeRequest.method)
            assertEquals("bytes=0-0", rangeRequest.getHeader("Range"))
        } finally {
            server.shutdown()
        }
    }
}
