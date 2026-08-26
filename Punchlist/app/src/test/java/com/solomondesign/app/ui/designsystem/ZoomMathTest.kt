package com.solomondesign.app.ui.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pinch-zoom clamp math behind [ZoomableContainer] — pure functions, so the pan limits
 * that keep a zoomed photo or plan sheet from being dragged off-screen are pinned on the JVM.
 */
class ZoomMathTest {

    @Test
    fun maxPan_isZeroAtFitScale_andGrowsWithZoom() {
        assertEquals(0f, maxPanForZoom(1080, 1f), 0f)
        // At 3x, two-thirds of the content is off-screen; half of that overflow per side.
        assertEquals(1080f, maxPanForZoom(1080, 3f), 0.001f)
    }

    @Test
    fun clampPan_limitsBothDirections_andPassesInRangeValuesThrough() {
        // At 2x on a 1000px edge the content may pan ±500px.
        assertEquals(500f, clampPan(9_999f, 1000, 2f), 0f)
        assertEquals(-500f, clampPan(-9_999f, 1000, 2f), 0f)
        assertEquals(123f, clampPan(123f, 1000, 2f), 0f)
        // At fit scale there is nowhere to pan.
        assertEquals(0f, clampPan(300f, 1000, 1f), 0f)
    }

    @Test
    fun doubleTapPan_centersOnTheTappedPoint_withinTheClamp() {
        // Tapping dead-center zooms in place.
        assertEquals(0f, doubleTapPanAxis(tapPx = 500f, containerEdgePx = 1000, targetScale = 2.5f), 0f)
        // Tapping the left edge pans right up to the limit ((500-0)*1.5 = 750 = the 2.5x max).
        assertEquals(750f, doubleTapPanAxis(tapPx = 0f, containerEdgePx = 1000, targetScale = 2.5f), 0.001f)
        // Tapping the right edge mirrors it.
        assertEquals(-750f, doubleTapPanAxis(tapPx = 1000f, containerEdgePx = 1000, targetScale = 2.5f), 0.001f)
    }
}
