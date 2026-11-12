package com.m1rai.downloadmanager

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Publishes a completed app-private partial file into the user's public Downloads folder. */
internal class AndroidDownloadPublisher(
    private val context: Context,
    private val folderName: String = DEFAULT_FOLDER_NAME,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) {
    fun publish(partialFile: File, requestedFileName: String, mimeType: String?): Uri {
        require(partialFile.isFile && partialFile.length() > 0L) {
            "Only a non-empty completed partial file can be published."
        }
        val safeFolder = DownloadPublicationNames.validateFolder(folderName)
        val safeFileName = DownloadPublicationNames.validateFileName(requestedFileName)
        return if (sdkInt >= 29) publishWithMediaStore(partialFile, safeFolder, safeFileName, mimeType)
        else publishWithLegacyStorage(partialFile, safeFolder, safeFileName)
    }

    private fun publishWithMediaStore(source: File, folder: String, name: String, mime: String?): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android could not create the destination download.")
        try {
            val output = resolver.openOutputStream(uri, "w")
                ?: throw IOException("Android could not open the destination download.")
            output.use { sink -> FileInputStream(source).use { it.copyTo(sink) } }
            val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, ready, null, null) == 1) {
                "Android could not publish the completed download."
            }
            return uri
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun publishWithLegacyStorage(source: File, folder: String, name: String): Uri {
        if (sdkInt >= 23 && context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Storage permission is required to save into public Downloads on this Android version.")
        }
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val directory = File(downloads, folder)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create the M1RAI Downloads folder.")
        if (!directory.isDirectory) throw IOException("The M1RAI Downloads destination is not a directory.")
        val destination = DownloadPublicationNames.uniqueFile(directory, name)
        val temporary = File(directory, ".${destination.name}.m1rai-pending")
        if (temporary.exists()) throw IOException("A temporary publication file already exists.")
        try {
            FileInputStream(source).use { input -> FileOutputStream(temporary).use { output -> input.copyTo(output) } }
            if (!temporary.renameTo(destination)) throw IOException("Could not finalize the published download.")
            return Uri.fromFile(destination)
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    companion object { const val DEFAULT_FOLDER_NAME = "M1RAI DOWNLOADS" }
}

internal object DownloadPublicationNames {
    fun validateFolder(value: String): String {
        val name = value.trim()
        require(name.isNotEmpty() && name != "." && name != "..") { "The download folder name is invalid." }
        require(name.length <= 64 && name.none { it == '/' || it == '\\' || it.isISOControl() }) {
            "The download folder name must be a simple folder name."
        }
        return name
    }

    fun validateFileName(value: String): String {
        val name = value.trim()
        require(name.isNotEmpty() && name != "." && name != "..") { "The download filename is invalid." }
        require(name.length <= 240 && name.none { it == '/' || it == '\\' || it.isISOControl() }) {
            "The download filename must not contain a path."
        }
        return name
    }

    fun uniqueFile(directory: File, requestedName: String): File {
        val original = File(validateFileName(requestedName))
        val base = original.nameWithoutExtension.ifBlank { "download" }
        val extension = original.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        var candidate = File(directory, requestedName)
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base ($suffix)$extension")
            suffix++
        }
        return candidate
    }
}
