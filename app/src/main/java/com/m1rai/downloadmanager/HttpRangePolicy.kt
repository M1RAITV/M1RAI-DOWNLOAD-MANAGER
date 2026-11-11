package com.m1rai.downloadmanager

internal data class ParsedContentRange(
    val start: Long,
    val endInclusive: Long,
    val totalBytes: Long?
)

internal sealed interface ResumeDecision {
    data class Accepted(val range: ParsedContentRange) : ResumeDecision
    data class RestartFromZero(val reason: String) : ResumeDecision
}

/**
 * Conservative HTTP resume checks. A partial response is accepted only when
 * the server starts at the exact local offset, the byte count is consistent,
 * and the representation can be matched with a stored validator.
 */
internal object HttpRangePolicy {
    private val contentRangePattern =
        Regex("""(?i)^bytes\s+(\d+)-(\d+)/(\d+|\*)$""")

    fun parseContentRange(value: String?): ParsedContentRange? {
        val match = contentRangePattern.matchEntire(value?.trim().orEmpty()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        val totalToken = match.groupValues[3]
        val total = if (totalToken == "*") null else totalToken.toLongOrNull() ?: return null
        if (end < start) return null
        if (total != null && (total <= end || total <= 0L)) return null
        return ParsedContentRange(start, end, total)
    }

    fun validateResume(
        requestedOffset: Long,
        responseCode: Int,
        contentRangeHeader: String?,
        responseContentLength: Long?,
        expectedEtag: String?,
        responseEtag: String?,
        expectedLastModified: String?,
        responseLastModified: String?
    ): ResumeDecision {
        if (requestedOffset <= 0L) {
            return ResumeDecision.RestartFromZero("Resume offset must be greater than zero.")
        }
        if (responseCode != 206) {
            return ResumeDecision.RestartFromZero("Server did not return HTTP 206 Partial Content.")
        }
        val range = parseContentRange(contentRangeHeader)
            ?: return ResumeDecision.RestartFromZero("Missing or invalid Content-Range.")
        if (range.start != requestedOffset) {
            return ResumeDecision.RestartFromZero("Server returned a different byte offset.")
        }
        val expectedLength = range.endInclusive - range.start + 1L
        if (responseContentLength != null && responseContentLength != expectedLength) {
            return ResumeDecision.RestartFromZero("Content-Length does not match Content-Range.")
        }

        val hasMatchingValidator = when {
            !expectedEtag.isNullOrBlank() ->
                !expectedEtag.startsWith("W/", ignoreCase = true) &&
                    responseEtag == expectedEtag
            !expectedLastModified.isNullOrBlank() ->
                responseLastModified == expectedLastModified
            else -> false
        }
        if (!hasMatchingValidator) {
            return ResumeDecision.RestartFromZero("The remote file could not be verified as unchanged.")
        }
        return ResumeDecision.Accepted(range)
    }
}
