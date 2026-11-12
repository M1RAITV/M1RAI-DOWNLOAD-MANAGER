package com.m1rai.downloadmanager

internal object DownloadDestination {
    const val DEFAULT_FOLDER_NAME = "M1RAI DOWNLOADS"

    fun relativePath(fileName: String, folderName: String = DEFAULT_FOLDER_NAME): String {
        val safeFolder = DownloadPublicationNames.validateFolder(folderName)
        val safeFileName = DownloadPublicationNames.validateFileName(fileName)
        return "$safeFolder/$safeFileName"
    }
}
