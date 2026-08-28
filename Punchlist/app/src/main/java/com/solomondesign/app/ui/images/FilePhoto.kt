package com.solomondesign.app.ui.images

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders an [ImageSource.CapturedFile] photo, decoding off the main thread and down-sampling to
 * roughly [maxEdgePx] so a 3-column grid never inflates full-resolution bitmaps. [fallback] shows
 * while decoding and if the file is gone — the same drawn-swatch degradation the in-memory
 * capture path already uses.
 */
@Composable
fun FilePhoto(
    absolutePath: String,
    contentDescription: String?,
    maxEdgePx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {},
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, absolutePath, maxEdgePx) {
        value = withContext(Dispatchers.IO) { decodeSampled(absolutePath, maxEdgePx) }
    }
    val decoded = bitmap
    if (decoded != null) {
        Image(
            bitmap = decoded,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        fallback()
    }
}

private fun decodeSampled(absolutePath: String, maxEdgePx: Int): ImageBitmap? =
    decodeSampledFile(absolutePath, maxEdgePx)?.asImageBitmap()

/** Bounded decode of a capture file; shared by [FilePhoto] and the markup editor. */
internal fun decodeSampledFile(absolutePath: String, maxEdgePx: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = PhotoSampling.inSampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
    }
    return BitmapFactory.decodeFile(absolutePath, options)
}
