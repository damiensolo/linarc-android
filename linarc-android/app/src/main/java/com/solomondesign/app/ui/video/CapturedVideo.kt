package com.solomondesign.app.ui.video

/**
 * One captured video: the mp4 in
 * [com.solomondesign.app.ui.images.CapturedMediaStore] plus the spoken description dictated
 * right after recording. Plain strings/numbers only, so the model and repository stay
 * JVM-unit-testable — same rule as `ProjectImage`.
 */
data class CapturedVideo(
    val id: String,
    val title: String,
    /** Reviewed/edited description. Starts as the dictated transcript, but the user may edit. */
    val note: String,
    val videoPath: String,
    /** The raw dictation, kept verbatim for provenance even after [note] is edited. */
    val transcript: String,
    val durationSeconds: Int,
    val capturedAtMillis: Long,
    val authorName: String,
)

/** "95" -> "1:35"; clips are capped well under an hour so mm:ss is always enough. */
fun formatVideoDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}
