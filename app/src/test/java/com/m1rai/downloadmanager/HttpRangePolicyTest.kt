package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRangePolicyTest {
    @Test fun parsesKnownAndUnknownTotalRanges() {
        assertEquals(
            ParsedContentRange(start = 100, endInclusive = 199, totalBytes = 500),
            HttpRangePolicy.parseContentRange("bytes 100-199/500")
        )
        assertEquals(
            ParsedContentRange(start = 100, endInclusive = 199, totalBytes = null),
            HttpRangePolicy.parseContentRange("bytes 100-199/*")
        )
    }

    @Test fun rejectsMalformedAndImpossibleRanges() {
        listOf(
            null,
            "",
            "bytes */500",
            "bytes 199-100/500",
            "bytes 100-500/500",
            "items 100-199/500",
            "bytes -1-9/500",
            "bytes 1-2/not-a-number"
        ).forEach { header ->
            assertEquals(null, HttpRangePolicy.parseContentRange(header))
        }
    }

    @Test fun acceptsMatchingOffsetLengthAndStrongEtag() {
        val result = HttpRangePolicy.validateResume(
            requestedOffset = 100,
            responseCode = 206,
            contentRangeHeader = "bytes 100-199/500",
            responseContentLength = 100,
            expectedEtag = "\"v1\"",
            responseEtag = "\"v1\"",
            expectedLastModified = null,
            responseLastModified = null
        )

        assertTrue(result is ResumeDecision.Accepted)
        assertEquals(
            ParsedContentRange(100, 199, 500),
            (result as ResumeDecision.Accepted).range
        )
    }

    @Test fun acceptsMatchingLastModifiedWhenNoEtagWasStored() {
        val result = HttpRangePolicy.validateResume(
            requestedOffset = 25,
            responseCode = 206,
            contentRangeHeader = "bytes 25-49/100",
            responseContentLength = 25,
            expectedEtag = null,
            responseEtag = null,
            expectedLastModified = "Wed, 21 Oct 2015 07:28:00 GMT",
            responseLastModified = "Wed, 21 Oct 2015 07:28:00 GMT"
        )

        assertTrue(result is ResumeDecision.Accepted)
    }

    @Test fun rejectsOffsetStatusLengthAndValidatorMismatches() {
        val common = mapOf(
            "requestedOffset" to 100L,
            "responseCode" to 206,
            "contentRangeHeader" to "bytes 100-199/500",
            "responseContentLength" to 100L,
            "expectedEtag" to "\"v1\"",
            "responseEtag" to "\"v1\"",
            "expectedLastModified" to null,
            "responseLastModified" to null
        )

        fun check(
            offset: Long = 100,
            status: Int = 206,
            range: String? = "bytes 100-199/500",
            length: Long? = 100,
            expectedTag: String? = "\"v1\"",
            actualTag: String? = "\"v1\""
        ): ResumeDecision = HttpRangePolicy.validateResume(
            requestedOffset = offset,
            responseCode = status,
            contentRangeHeader = range,
            responseContentLength = length,
            expectedEtag = expectedTag,
            responseEtag = actualTag,
            expectedLastModified = null,
            responseLastModified = null
        )

        assertTrue(check(offset = 0) is ResumeDecision.RestartFromZero)
        assertTrue(check(status = 200) is ResumeDecision.RestartFromZero)
        assertTrue(check(range = "bytes 0-99/500") is ResumeDecision.RestartFromZero)
        assertTrue(check(length = 99) is ResumeDecision.RestartFromZero)
        assertTrue(check(actualTag = "\"v2\"") is ResumeDecision.RestartFromZero)
        assertTrue(check(actualTag = null) is ResumeDecision.RestartFromZero)
        assertTrue(check(expectedTag = "W/\"v1\"") is ResumeDecision.RestartFromZero)
    }

    @Test fun doesNotAcceptAChangedEtagJustBecauseLastModifiedMatches() {
        val result = HttpRangePolicy.validateResume(
            requestedOffset = 10,
            responseCode = 206,
            contentRangeHeader = "bytes 10-19/100",
            responseContentLength = 10,
            expectedEtag = "\"old\"",
            responseEtag = "\"new\"",
            expectedLastModified = "same-date",
            responseLastModified = "same-date"
        )

        assertTrue(result is ResumeDecision.RestartFromZero)
    }
}
