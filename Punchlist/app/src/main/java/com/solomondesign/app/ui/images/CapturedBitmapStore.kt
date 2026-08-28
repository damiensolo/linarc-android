package com.solomondesign.app.ui.images

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateMapOf

/**
 * Process-scoped bitmaps for photos captured this session.
 *
 * Deliberately separate from [ProjectImage] so the model and repository stay free of
 * `android.graphics` and remain JVM-unit-testable. Persisting these would mean file IO or Room —
 * a new dependency, which this project's conventions say to ask about first.
 *
 * Eviction is graceful: once a key is gone the tile falls back to a drawn swatch plus a
 * "Photo unavailable" caption.
 */
object CapturedBitmapStore {
    private const val MAX_ENTRIES = 24

    private val bitmaps = mutableStateMapOf<String, Bitmap>()
    private val order = ArrayDeque<String>()

    /** Pure, so key generation is unit-testable even though [put] is not. */
    fun nextCaptureKey(nanos: Long): String = "cap-$nanos"

    fun put(bitmap: Bitmap): String {
        val key = nextCaptureKey(System.nanoTime())
        bitmaps[key] = bitmap
        order.addLast(key)
        while (order.size > MAX_ENTRIES) {
            val evicted = order.removeFirst()
            bitmaps.remove(evicted)
        }
        return key
    }

    fun get(key: String): Bitmap? = bitmaps[key]

    fun remove(key: String) {
        bitmaps.remove(key)
        order.remove(key)
    }

    fun clear() {
        bitmaps.clear()
        order.clear()
    }
}
