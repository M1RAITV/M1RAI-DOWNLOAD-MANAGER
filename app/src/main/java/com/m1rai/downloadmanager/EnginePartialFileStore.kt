package com.m1rai.downloadmanager

import java.io.File

/**
 * Owns deterministic, isolated partial-file paths beneath app-private storage.
 * Task IDs, not user-provided filenames, determine the on-disk partial name.
 */
internal class EnginePartialFileStore(
    private val appPrivateDirectory: File
) {
    fun prepare(taskId: String): File {
        require(TASK_ID.matches(taskId)) { "Invalid transfer task ID." }

        val privateRoot = appPrivateDirectory.canonicalFile
        require(privateRoot.isDirectory) { "The app-private root must be an existing directory." }

        val partialDirectory = File(privateRoot, DIRECTORY_NAME)
        if (!partialDirectory.exists() && !partialDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the private partial-file directory.")
        }

        val canonicalDirectory = partialDirectory.canonicalFile
        val privatePrefix = privateRoot.path.trimEnd(File.separatorChar) + File.separator
        require(canonicalDirectory.path.startsWith(privatePrefix) && canonicalDirectory.isDirectory) {
            "The partial-file directory must remain inside app-private storage."
        }

        val candidate = File(canonicalDirectory, "$taskId.part")
        val canonicalCandidate = candidate.canonicalFile
        val directoryPrefix = canonicalDirectory.path.trimEnd(File.separatorChar) + File.separator
        require(canonicalCandidate.path.startsWith(directoryPrefix)) {
            "The partial file must remain inside the partial-file directory."
        }
        if (candidate.exists() && !candidate.isFile) {
            throw IllegalStateException("The partial-file path is not a regular file.")
        }
        if (!candidate.exists() && !candidate.createNewFile()) {
            throw IllegalStateException("Could not create the partial file.")
        }
        return canonicalCandidate
    }

    companion object {
        private const val DIRECTORY_NAME = "engine-partials"
        private val TASK_ID = Regex("[A-Za-z0-9_-]{1,80}")
    }
}
