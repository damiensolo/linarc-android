package com.solomondesign.app.ui.images

import androidx.annotation.DrawableRes

/**
 * Where a tile's pixels come from.
 *
 * A sealed interface rather than a flat kind-enum plus nullable fields, so the tile's `when` is
 * exhaustive and "captured but no key" is unrepresentable. Deliberately holds no
 * `android.graphics.Bitmap`, which keeps [ProjectImage] and its repository JVM-unit-testable.
 */
sealed interface ImageSource {
    /** Procedurally drawn demo tile — same technique as PlanScreen's site plan canvas. */
    data class Swatch(val seed: Int) : ImageSource

    /** A drawable already shipped with the app. */
    data class Drawable(@param:DrawableRes val resId: Int) : ImageSource

    /** A real camera capture, held in [CapturedBitmapStore] for this process only. */
    data class Captured(val captureKey: String) : ImageSource

    /**
     * A full-resolution capture persisted as an app-private file by [CapturedMediaStore].
     * A path string rather than a `java.io.File` keeps equality/copy semantics value-like.
     */
    data class CapturedFile(val absolutePath: String) : ImageSource
}

data class ProjectImage(
    val id: String,
    val title: String,
    val area: String,
    val tags: List<String>,
    val capturedAtMillis: Long,
    val authorName: String,
    val source: ImageSource,
    /** Set once Create turned this photo into an issue. */
    val linkedRecordId: String? = null,
    /** True when annotations are baked into the pixels; tiles and thumbnails show a badge. */
    val hasMarkup: Boolean = false,
    /** Album this photo is filed under (Albums view); null/blank means unfiled. */
    val album: String? = null,
)
