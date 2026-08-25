package com.solomondesign.app.ui.images

import android.content.Context
import java.io.File

/**
 * App-private files for full-resolution captures — photo JPEGs and video mp4s.
 * Photos are referenced from [ProjectImage] via [ImageSource.CapturedFile] and videos from
 * `CapturedVideo.videoPath`; both stay plain path strings so the models and repositories
 * remain JVM-unit-testable.
 *
 * Complements [CapturedBitmapStore] rather than replacing it: metadata in [ProjectImageRepository]
 * is process-scoped demo state, so capture files from a *previous* process are orphans with no
 * tile pointing at them. [ensureFreshProcess] wipes them exactly once per process — deliberately
 * not once per Activity recreation, which would delete files the live repository still shows.
 */
object CapturedMediaStore {
    private const val DIR_NAME = "captures"

    private var cleanedThisProcess = false

    /** Pure, so the naming contract is unit-testable without a [Context]. */
    fun photoFileName(nanos: Long): String = "photo-$nanos.jpg"

    /** Pure, like [photoFileName]. */
    fun videoFileName(nanos: Long): String = "video-$nanos.mp4"

    fun directory(context: Context): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(directory(context), photoFileName(System.nanoTime()))

    fun newVideoFile(context: Context): File =
        File(directory(context), videoFileName(System.nanoTime()))

    /** Call once at app start (see `MainActivity`); safe to call again — later calls no-op. */
    @Synchronized
    fun ensureFreshProcess(context: Context) {
        if (cleanedThisProcess) return
        cleanedThisProcess = true
        deleteAllIn(directory(context))
    }

    /** Separated from [ensureFreshProcess] so JVM tests can exercise it against a temp dir. */
    fun deleteAllIn(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun delete(absolutePath: String) {
        File(absolutePath).delete()
    }
}
