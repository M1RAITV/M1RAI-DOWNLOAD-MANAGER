package com.m1rai.downloadmanager

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EnginePartialFileStoreTest {
    @Test fun createsPartialFileInsideDedicatedPrivateDirectory() {
        val root = Files.createTempDirectory("m1rai-store").toFile()
        try {
            val partial = EnginePartialFileStore(root).prepare("task_123")
            assertTrue(partial.isFile)
            assertEquals("task_123.part", partial.name)
            assertEquals(File(root, "engine-partials").canonicalFile, partial.parentFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun sameTaskResolvesToSameFileAndDifferentTasksAreIsolated() {
        val root = Files.createTempDirectory("m1rai-store").toFile()
        try {
            val store = EnginePartialFileStore(root)
            val first = store.prepare("task-a")
            val same = store.prepare("task-a")
            val second = store.prepare("task-b")
            assertEquals(first.canonicalPath, same.canonicalPath)
            assertNotEquals(first.canonicalPath, second.canonicalPath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsTraversalAndUnsafeTaskIdentifiers() {
        val root = Files.createTempDirectory("m1rai-store").toFile()
        try {
            val store = EnginePartialFileStore(root)
            listOf("../escape", "folder/task", "task.part", "", "a".repeat(81)).forEach { id ->
                try {
                    store.prepare(id)
                    fail("Expected invalid task ID to be rejected: $id")
                } catch (_: IllegalArgumentException) {
                    // Expected.
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsPartialDirectorySymlinkOutsidePrivateRoot() {
        val root = Files.createTempDirectory("m1rai-store-root").toFile()
        val outside = Files.createTempDirectory("m1rai-store-outside").toFile()
        try {
            val link = File(root, "engine-partials")
            try {
                Files.createSymbolicLink(link.toPath(), outside.toPath())
            } catch (_: UnsupportedOperationException) {
                return
            } catch (_: SecurityException) {
                return
            }
            try {
                EnginePartialFileStore(root).prepare("task-safe")
                fail("Expected external partial directory symlink to be rejected.")
            } catch (_: IllegalArgumentException) {
                // Expected.
            }
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}
