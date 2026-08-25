package com.solomondesign.app.ui.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CapturedMediaStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun photoFileName_isStableAndJpegSuffixed() {
        assertEquals("photo-42.jpg", CapturedMediaStore.photoFileName(42L))
    }

    @Test
    fun videoFileName_isStableAndMp4Suffixed() {
        assertEquals("video-42.mp4", CapturedMediaStore.videoFileName(42L))
    }

    @Test
    fun deleteAllIn_clearsEveryFileInTheDirectory() {
        val dir = tempFolder.newFolder("captures")
        File(dir, "photo-1.jpg").writeBytes(byteArrayOf(1))
        File(dir, "photo-2.jpg").writeBytes(byteArrayOf(2))

        CapturedMediaStore.deleteAllIn(dir)

        assertEquals(emptyList<File>(), dir.listFiles().orEmpty().toList())
    }

    @Test
    fun deleteAllIn_toleratesAMissingDirectory() {
        CapturedMediaStore.deleteAllIn(File(tempFolder.root, "never-created"))
    }

    @Test
    fun delete_removesExactlyThatFile() {
        val dir = tempFolder.newFolder("captures")
        val keep = File(dir, "photo-1.jpg").apply { writeBytes(byteArrayOf(1)) }
        val gone = File(dir, "photo-2.jpg").apply { writeBytes(byteArrayOf(2)) }

        CapturedMediaStore.delete(gone.absolutePath)

        assertTrue(keep.exists())
        assertFalse(gone.exists())
    }
}
