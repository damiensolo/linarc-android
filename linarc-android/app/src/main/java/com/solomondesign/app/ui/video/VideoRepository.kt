package com.solomondesign.app.ui.video

import androidx.compose.runtime.mutableStateListOf

/**
 * In-memory store of captured [CapturedVideo]s, mirroring `ProjectImageRepository`:
 * process-scoped demo state backed by snapshot state so Today rows and the playback screen
 * recompose when a new clip is published.
 */
object VideoRepository {

    private val clips = mutableStateListOf<CapturedVideo>()

    /** Newest first — [add] prepends, like every capture surface in this prototype. */
    val videos: List<CapturedVideo> get() = clips

    fun add(video: CapturedVideo) {
        clips.add(0, video)
    }

    fun find(id: String): CapturedVideo? = clips.firstOrNull { it.id == id }

    fun clear() {
        clips.clear()
    }
}
