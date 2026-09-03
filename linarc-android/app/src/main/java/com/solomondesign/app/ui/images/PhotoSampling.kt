package com.solomondesign.app.ui.images

/**
 * Pure down-sampling math shared by every place that decodes a captured photo file (grid tiles,
 * the full-screen viewer, and the camera's post-capture processing). Kept free of
 * `android.graphics` so the rules are JVM-unit-testable.
 */
object PhotoSampling {

    /**
     * The `BitmapFactory.Options.inSampleSize` to decode an image of [width] x [height] so its
     * long edge lands at or under [maxEdgePx]. Always a power of two, per the BitmapFactory
     * contract (other values are rounded down by the decoder, which would make memory use
     * unpredictable).
     */
    fun inSampleSizeFor(width: Int, height: Int, maxEdgePx: Int): Int {
        if (width <= 0 || height <= 0 || maxEdgePx <= 0) return 1
        var sample = 1
        var longEdge = maxOf(width, height)
        while (longEdge > maxEdgePx) {
            sample *= 2
            longEdge /= 2
        }
        return sample
    }
}
