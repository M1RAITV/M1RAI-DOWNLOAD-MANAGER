package com.m1rai.downloadmanager

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class HttpFileTransferEngineTest {
    private lateinit var server: MockWebServer
    private lateinit var folder: File
    private lateinit var engine: HttpFileTransferEngine
    @Before fun setUp() { server = MockWebServer(); server.start(); folder = Files.createTempDirectory("m1rai-transfer-test").toFile(); engine = HttpFileTransferEngine(OkHttpClient.Builder().build(), 4096) }
    @After fun tearDown() { server.shutdown(); folder.deleteRecursively() }

    @Test fun downloadsACompleteFileAndCapturesValidators() {
        val payload = "m1rai-complete-payload"
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Length", payload.length).setHeader("ETag", "\"v1\"").setBody(payload))
        val file = File(folder, "complete.part")
        val result = engine.transfer(server.url("/file").toString(), file)
        assertTrue(result is HttpTransferResult.Completed); assertEquals(payload, file.readText())
        assertEquals("\"v1\"", (result as HttpTransferResult.Completed).validators.etag)
    }

    @Test fun resumesOnlyWhenRangeAndValidatorMatch() {
        val prefix = "first-"; val suffix = "second"
        val file = File(folder, "resume.part").apply { writeText(prefix) }
        server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 6-11/12").setHeader("Content-Length", 6).setHeader("ETag", "\"v1\"").setBody(suffix))
        val result = engine.transfer(server.url("/file").toString(), file, TransferValidators(etag = "\"v1\""))
        assertTrue(result is HttpTransferResult.Completed); assertEquals(prefix + suffix, file.readText())
        val request = server.takeRequest(); assertEquals("bytes=6-", request.getHeader("Range")); assertEquals("\"v1\"", request.getHeader("If-Range"))
    }

    @Test fun restartsFromZeroWhenServerIgnoresRange() {
        val file = File(folder, "restart.part").apply { writeText("stale-prefix") }; val body = "fresh-file"
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Length", body.length).setBody(body))
        val result = engine.transfer(server.url("/file").toString(), file, TransferValidators(etag = "\"old\""))
        assertTrue(result is HttpTransferResult.Completed); assertEquals(body, file.readText())
        assertEquals("bytes=12-", server.takeRequest().getHeader("Range"))
    }

    @Test fun changedEtagForPartialResponseTriggersFreshRequest() {
        val file = File(folder, "changed.part").apply { writeText("old") }; val fresh = "new-version"
        server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 3-8/9").setHeader("Content-Length", 6).setHeader("ETag", "\"v2\"").setBody("stale!"))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Length", fresh.length).setHeader("ETag", "\"v2\"").setBody(fresh))
        val result = engine.transfer(server.url("/file").toString(), file, TransferValidators(etag = "\"v1\""))
        assertTrue(result is HttpTransferResult.Completed); assertEquals(fresh, file.readText())
        assertEquals("bytes=3-", server.takeRequest().getHeader("Range")); assertNull(server.takeRequest().getHeader("Range"))
    }

    @Test fun refusesToReportSuccessForNonSuccessfulResponse() {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = engine.transfer(server.url("/missing").toString(), File(folder, "missing.part"))
        assertTrue(result is HttpTransferResult.Failed)
    }
}
