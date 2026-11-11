package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLinkRulesTest {
    @Test fun acceptsHttpAndHttpsLinks() {
        assertTrue(DownloadLinkRules.isValidHttpUrl("https://example.com/file.zip"))
        assertTrue(DownloadLinkRules.isValidHttpUrl("http://downloads.example.org/path?id=42"))
    }

    @Test fun trimsSurroundingWhitespace() {
        assertTrue(DownloadLinkRules.isValidHttpUrl("  https://example.com/file.pdf  "))
    }

    @Test fun rejectsUnsupportedSchemes() {
        assertFalse(DownloadLinkRules.isValidHttpUrl("ftp://example.com/file.zip"))
        assertFalse(DownloadLinkRules.isValidHttpUrl("file:///sdcard/file.zip"))
        assertFalse(DownloadLinkRules.isValidHttpUrl("javascript:alert(1)"))
    }

    @Test fun rejectsMalformedAndHostlessLinks() {
        assertFalse(DownloadLinkRules.isValidHttpUrl(""))
        assertFalse(DownloadLinkRules.isValidHttpUrl("https://"))
        assertFalse(DownloadLinkRules.isValidHttpUrl("not a link"))
        assertFalse(DownloadLinkRules.isValidHttpUrl("https://example.com/a b"))
    }

    @Test fun rejectsEmbeddedCredentials() {
        assertFalse(DownloadLinkRules.isValidHttpUrl("https://user:pass@example.com/file.zip"))
    }

    @Test fun extractsUrlFromSharedText() {
        assertEquals(
            "https://example.com/video.mp4",
            DownloadLinkRules.extractHttpUrl("Check this out: https://example.com/video.mp4")
        )
    }

    @Test fun removesTrailingSentencePunctuation() {
        assertEquals(
            "https://example.com/file.zip",
            DownloadLinkRules.extractHttpUrl("Download: https://example.com/file.zip.")
        )
    }

    @Test fun skipsInvalidCandidatesAndFindsNextValidUrl() {
        assertEquals(
            "https://example.org/file.zip",
            DownloadLinkRules.extractHttpUrl("https://user:pass@example.com/a https://example.org/file.zip")
        )
    }

    @Test fun returnsNullWhenSharedTextHasNoValidHttpUrl() {
        assertNull(DownloadLinkRules.extractHttpUrl("Open this ftp://example.com/file.zip"))
        assertNull(DownloadLinkRules.extractHttpUrl("No link included"))
    }
}
