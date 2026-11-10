package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadMetadataParserTest {
    @Test fun readsQuotedFileName() {
        assertEquals("report.pdf", DownloadMetadataParser.fileName("attachment; filename=\"report.pdf\"", "fallback"))
    }

    @Test fun prefersUtf8ExtendedFileName() {
        assertEquals("café.pdf", DownloadMetadataParser.fileName("attachment; filename*=UTF-8''caf%C3%A9.pdf", "fallback"))
    }

    @Test fun removesPathTraversalAndUnsafeCharacters() {
        assertEquals("evil_.zip", DownloadMetadataParser.fileName("attachment; filename=\"../../evil?.zip\"", "fallback"))
    }

    @Test fun fallsBackToUrlName() {
        assertEquals("archive.zip", DownloadMetadataParser.fileName(null, "https://example.com/archive.zip?token=x"))
    }

    @Test fun parsesOnlyNonNegativeContentLengths() {
        assertEquals(1024L, DownloadMetadataParser.contentLength(" 1024 "))
        assertNull(DownloadMetadataParser.contentLength("-1"))
        assertNull(DownloadMetadataParser.contentLength("unknown"))
    }
}
