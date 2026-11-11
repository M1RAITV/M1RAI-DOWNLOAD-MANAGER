package com.m1rai.downloadmanager

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal data class TransferValidators(val etag: String? = null, val lastModified: String? = null)
internal data class TransferProgress(val downloadedBytes: Long, val totalBytes: Long?)
internal sealed interface HttpTransferResult {
    data class Completed(val file: File, val downloadedBytes: Long, val totalBytes: Long?, val validators: TransferValidators) : HttpTransferResult
    data class Paused(val file: File, val downloadedBytes: Long, val totalBytes: Long?, val validators: TransferValidators) : HttpTransferResult
    data class Cancelled(val file: File) : HttpTransferResult
    data class Failed(val message: String, val cause: Throwable? = null) : HttpTransferResult
}

internal class TransferControl {
    private val pauseRequested = AtomicBoolean(false)
    private val cancelRequested = AtomicBoolean(false)
    private val activeCall = AtomicReference<Call?>(null)
    fun pause() { pauseRequested.set(true); activeCall.get()?.cancel() }
    fun cancel() { cancelRequested.set(true); activeCall.get()?.cancel() }
    internal fun isPauseRequested() = pauseRequested.get()
    internal fun isCancelRequested() = cancelRequested.get()
    internal fun attach(call: Call) { activeCall.set(call); if (pauseRequested.get() || cancelRequested.get()) call.cancel() }
    internal fun detach(call: Call) { activeCall.compareAndSet(call, null) }
}

/** Blocking, app-owned HTTP transfer primitive. Call from a worker thread. */
internal class HttpFileTransferEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build(),
    private val bufferSize: Int = 64 * 1024
) {
    fun transfer(
        url: String,
        partialFile: File,
        previousValidators: TransferValidators = TransferValidators(),
        control: TransferControl = TransferControl(),
        onProgress: (TransferProgress) -> Unit = {}
    ): HttpTransferResult {
        if (control.isCancelRequested()) return HttpTransferResult.Cancelled(partialFile)
        if (!DownloadLinkRules.isValidHttpUrl(url)) return HttpTransferResult.Failed("Only valid HTTP and HTTPS URLs are supported.")
        val parent = partialFile.absoluteFile.parentFile
        if (parent == null || (!parent.exists() && !parent.mkdirs())) return HttpTransferResult.Failed("Could not create the temporary download directory.")
        val offset = if (partialFile.isFile) partialFile.length() else 0L
        val firstRequest = Request.Builder().url(url).apply {
            if (offset > 0L) {
                header("Range", "bytes=$offset-")
                val validator = previousValidators.etag?.takeIf { it.isNotBlank() && !it.startsWith("W/", true) }
                    ?: previousValidators.lastModified?.takeIf { it.isNotBlank() }
                if (validator != null) header("If-Range", validator)
            }
        }.get().build()
        val firstCall = client.newCall(firstRequest)
        control.attach(firstCall)
        try {
            firstCall.execute().use { response ->
                if (control.isCancelRequested()) return HttpTransferResult.Cancelled(partialFile)
                if (control.isPauseRequested()) return HttpTransferResult.Paused(partialFile, partialFile.lengthOrZero(), null, previousValidators)
                if (!response.isSuccessful) return HttpTransferResult.Failed("Server returned HTTP ${response.code}.")
                val responseValidators = TransferValidators(response.header("ETag"), response.header("Last-Modified"))
                val append = if (offset == 0L) {
                    if (response.code != 200) return HttpTransferResult.Failed("Server did not provide a complete file response.")
                    false
                } else if (response.code == 206) {
                    when (HttpRangePolicy.validateResume(
                        requestedOffset = offset,
                        responseCode = response.code,
                        contentRangeHeader = response.header("Content-Range"),
                        responseContentLength = response.body?.contentLength()?.takeIf { it >= 0L },
                        expectedEtag = previousValidators.etag,
                        responseEtag = responseValidators.etag,
                        expectedLastModified = previousValidators.lastModified,
                        responseLastModified = responseValidators.lastModified
                    )) {
                        is ResumeDecision.Accepted -> true
                        is ResumeDecision.RestartFromZero -> return restartFromZero(url, partialFile, control, onProgress)
                    }
                } else if (response.code == 200) false
                else return restartFromZero(url, partialFile, control, onProgress)

                val range = if (append) HttpRangePolicy.parseContentRange(response.header("Content-Range")) else null
                val body = response.body ?: return HttpTransferResult.Failed("Server returned an empty response body.")
                val total = if (append) range?.totalBytes else body.contentLength().takeIf { it >= 0L }
                val initial = if (append) offset else 0L
                val output = if (append) FileOutputStream(partialFile, true) else partialFile.outputStream()
                output.use { sink ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(bufferSize.coerceAtLeast(4096))
                        var written = initial
                        while (true) {
                            if (control.isCancelRequested()) return HttpTransferResult.Cancelled(partialFile)
                            if (control.isPauseRequested()) return HttpTransferResult.Paused(partialFile, partialFile.lengthOrZero(), total, responseValidators)
                            val count = input.read(buffer)
                            if (count < 0) break
                            sink.write(buffer, 0, count)
                            sink.flush()
                            written += count
                            onProgress(TransferProgress(written, total))
                        }
                    }
                }
                if (control.isCancelRequested()) return HttpTransferResult.Cancelled(partialFile)
                if (control.isPauseRequested()) return HttpTransferResult.Paused(partialFile, partialFile.lengthOrZero(), total, responseValidators)
                if (total != null && partialFile.length() != total) return HttpTransferResult.Failed("Downloaded byte count does not match the server's declared size.")
                return HttpTransferResult.Completed(partialFile, partialFile.length(), total, responseValidators)
            }
        } catch (error: IOException) {
            return when {
                control.isCancelRequested() -> HttpTransferResult.Cancelled(partialFile)
                control.isPauseRequested() -> HttpTransferResult.Paused(partialFile, partialFile.lengthOrZero(), null, previousValidators)
                else -> HttpTransferResult.Failed(error.localizedMessage ?: "Network transfer failed.", error)
            }
        } finally { control.detach(firstCall) }
    }

    private fun restartFromZero(url: String, file: File, control: TransferControl, onProgress: (TransferProgress) -> Unit): HttpTransferResult {
        if (control.isCancelRequested()) return HttpTransferResult.Cancelled(file)
        if (control.isPauseRequested()) return HttpTransferResult.Paused(file, file.lengthOrZero(), null, TransferValidators())
        val call = client.newCall(Request.Builder().url(url).get().build())
        control.attach(call)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful || response.code != 200) return HttpTransferResult.Failed("Could not safely restart download (HTTP ${response.code}).")
                val body = response.body ?: return HttpTransferResult.Failed("Server returned an empty response body.")
                val validators = TransferValidators(response.header("ETag"), response.header("Last-Modified"))
                val total = body.contentLength().takeIf { it >= 0L }
                var written = 0L
                file.outputStream().use { sink ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(bufferSize.coerceAtLeast(4096))
                        while (true) {
                            if (control.isCancelRequested()) return HttpTransferResult.Cancelled(file)
                            if (control.isPauseRequested()) return HttpTransferResult.Paused(file, file.lengthOrZero(), total, validators)
                            val count = input.read(buffer)
                            if (count < 0) break
                            sink.write(buffer, 0, count)
                            sink.flush()
                            written += count
                            onProgress(TransferProgress(written, total))
                        }
                    }
                }
                if (total != null && file.length() != total) return HttpTransferResult.Failed("Downloaded byte count does not match the server's declared size.")
                return HttpTransferResult.Completed(file, file.length(), total, validators)
            }
        } catch (error: IOException) {
            return when {
                control.isCancelRequested() -> HttpTransferResult.Cancelled(file)
                control.isPauseRequested() -> HttpTransferResult.Paused(file, file.lengthOrZero(), null, TransferValidators())
                else -> HttpTransferResult.Failed(error.localizedMessage ?: "Network transfer failed.", error)
            }
        } finally { control.detach(call) }
    }

    private fun File.lengthOrZero() = if (isFile) length() else 0L
}
