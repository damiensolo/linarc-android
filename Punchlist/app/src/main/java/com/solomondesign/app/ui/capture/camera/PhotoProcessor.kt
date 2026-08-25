package com.solomondesign.app.ui.capture.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.solomondesign.app.ui.images.PhotoSampling
import java.io.File

/**
 * Turns a CameraX in-memory capture into an upright JPEG on disk.
 *
 * Deliberately bakes the rotation into the pixels instead of relying on the EXIF orientation tag
 * CameraX would write with a file-based capture: every downstream consumer (grid tiles, the
 * viewer, and the markup editor's export in the next iteration) can then treat the file as plain
 * upright pixels, with no EXIF-awareness required anywhere else.
 */
object PhotoProcessor {

    /**
     * Long-edge cap for the saved photo. ~5MP keeps a field shot sharp enough to read framing
     * detail while a full 12–50MP sensor dump would only slow decode and eat the demo's memory.
     */
    const val MAX_EDGE_PX = 2560

    private const val JPEG_QUALITY = 90

    /** An [ImageCapture][androidx.camera.core.ImageCapture] proxy holds one JPEG plane. */
    fun jpegBytesOf(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    fun toUprightBitmap(
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        maxEdgePx: Int = MAX_EDGE_PX,
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoSampling.inSampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
        }
        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
            ?: error("Couldn't decode the captured photo")
        if (rotationDegrees % 360 == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (upright !== decoded) decoded.recycle()
        return upright
    }

    fun writeJpeg(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    }
}
