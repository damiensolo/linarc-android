package com.solomondesign.app.ui.images

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSamplingTest {

    @Test
    fun imagesAtOrUnderTheCap_decodeAtFullSize() {
        assertEquals(1, PhotoSampling.inSampleSizeFor(width = 2560, height = 1920, maxEdgePx = 2560))
        assertEquals(1, PhotoSampling.inSampleSizeFor(width = 800, height = 600, maxEdgePx = 2560))
    }

    @Test
    fun largeImages_halveUntilTheLongEdgeFitsTheCap() {
        // 8000px long edge -> /2 = 4000 (still over 2560) -> /4 = 2000 (fits).
        assertEquals(4, PhotoSampling.inSampleSizeFor(width = 8000, height = 6000, maxEdgePx = 2560))
        // 4000px long edge -> /2 = 2000 (fits).
        assertEquals(2, PhotoSampling.inSampleSizeFor(width = 3000, height = 4000, maxEdgePx = 2560))
    }

    @Test
    fun sampleSizeIsAlwaysAPowerOfTwo_perTheBitmapFactoryContract() {
        val sample = PhotoSampling.inSampleSizeFor(width = 10_000, height = 10_000, maxEdgePx = 512)
        assertEquals(0, sample and (sample - 1))
    }

    @Test
    fun degenerateInputs_failSafeToFullSize() {
        assertEquals(1, PhotoSampling.inSampleSizeFor(width = 0, height = 100, maxEdgePx = 512))
        assertEquals(1, PhotoSampling.inSampleSizeFor(width = 100, height = -1, maxEdgePx = 512))
        assertEquals(1, PhotoSampling.inSampleSizeFor(width = 100, height = 100, maxEdgePx = 0))
    }
}
