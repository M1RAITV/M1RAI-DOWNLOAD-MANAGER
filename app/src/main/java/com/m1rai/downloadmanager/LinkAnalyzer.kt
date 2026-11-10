package com.m1rai.downloadmanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal data class LinkAnalysisResult(
    val fileName: String,
    val mimeType: String?,
    val contentLength: Long?,
    val supportsRanges: Boolean,
    val httpStatus: Int
)

internal class LinkAnalyzer(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    suspend fun analyze(url: String): Result<LinkAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val head = Request.Builder().url(url).head().build()
            val headResponse = client.newCall(head).execute()
            val headCode = headResponse.code
            val headHeaders = headResponse.headers
            val headSuccess = headResponse.isSuccessful
            val headLength = headHeaders["Content-Length"]
            val headDisposition = headHeaders["Content-Disposition"]
            val headType = headHeaders["Content-Type"]?.substringBefore(';')?.trim()
            val headRanges = headHeaders["Accept-Ranges"].equals("bytes", ignoreCase = true)
            headResponse.close()

            if (headSuccess && (headLength != null || headType != null || headDisposition != null || headRanges)) {
                return@runCatching LinkAnalysisResult(
                    DownloadMetadataParser.fileName(headDisposition, url),
                    headType,
                    DownloadMetadataParser.contentLength(headLength),
                    headRanges,
                    headCode
                )
            }

            val get = Request.Builder().url(url).header("Range", "bytes=0-0").get().build()
            client.newCall(get).execute().use { response ->
                val contentRange = response.header("Content-Range")
                val totalFromRange = contentRange?.substringAfterLast('/')?.toLongOrNull()
                val length = totalFromRange ?: DownloadMetadataParser.contentLength(response.header("Content-Length"))
                val type = response.header("Content-Type")?.substringBefore(';')?.trim()
                val ranges = response.code == 206 || response.header("Accept-Ranges").equals("bytes", ignoreCase = true)
                LinkAnalysisResult(
                    DownloadMetadataParser.fileName(response.header("Content-Disposition"), url),
                    type,
                    length,
                    ranges,
                    response.code
                )
            }
        }
    }
}
