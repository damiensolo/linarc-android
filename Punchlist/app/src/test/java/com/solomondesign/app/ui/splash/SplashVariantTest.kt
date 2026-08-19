package com.solomondesign.app.ui.splash

import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SplashVariantTest {

    @Before
    fun reset() {
        DemoSession.reset()
        DemoProjectRepository.splashVariant = SplashVariant.DEPTH
    }

    @Test
    fun sevenVariantsExistWithDistinctTitlesAndNoGlass() {
        val titles = SplashVariant.entries.map { it.title }.toSet()
        assertEquals(7, SplashVariant.entries.size)
        assertEquals(7, titles.size)
        assertFalse(titles.contains("Glass"))
        assertTrue(titles.contains("Split"))
    }

    @Test
    fun depthIsDefaultAndRevealsHomeAfterLogo() {
        assertEquals(SplashVariant.DEPTH, SplashVariant.entries.first())
        assertTrue(SplashVariant.DEPTH.revealsHome)
        assertTrue(SplashVariant.MORPH.revealsHome)
        assertTrue(SplashVariant.BLOOM.revealsHome)
        assertFalse(SplashVariant.SPLIT.revealsHome)
        assertFalse(SplashVariant.ASSEMBLE.revealsHome)
        assertFalse(SplashVariant.SIGNATURE.revealsHome)
        assertFalse(SplashVariant.IRIS.revealsHome)
    }

    @Test
    fun lockupMatchesFigmaPrimaryLogoViewBoxAndIsHalfScale() {
        assertEquals(400f, LockupViewBox)
        assertEquals(100.390625f, LockupContentMinX)
        assertEquals(71.09375f, LockupContentMinY)
        assertEquals(199.21875f, LockupContentWidth)
        assertEquals(257.81247f, LockupContentHeight, 0.0001f)
        assertEquals(108, LockupDisplayWidthDp)
        assertTrue(LockupDisplayWidthDp <= 120)
    }

    @Test
    fun selectedVariantSurvivesSessionReset() {
        DemoProjectRepository.splashVariant = SplashVariant.IRIS
        DemoSession.reset()
        assertEquals(SplashVariant.IRIS, DemoProjectRepository.splashVariant)
    }

    @Test
    fun segmentClampsOutsideTheRange() {
        assertEquals(0f, segment(0.1f, 0.2f, 0.4f))
        assertEquals(1f, segment(0.9f, 0.2f, 0.4f), 0f)
        assertEquals(0.5f, segment(0.3f, 0.2f, 0.4f), 0.001f)
    }

    @Test
    fun easeOutCurvesLeaveTheOriginAndArriveAtOne() {
        assertEquals(0f, easeOutCubic(0f), 0.0001f)
        assertEquals(1f, easeOutCubic(1f), 0.0001f)
        assertTrue(easeOutCubic(0.5f) > 0.5f)
        assertEquals(0f, easeOutQuint(0f), 0.0001f)
        assertEquals(1f, easeOutQuint(1f), 0.0001f)
        assertEquals(0f, easeOutBack(0f), 0.05f)
        assertEquals(1f, easeOutBack(1f), 0.0001f)
    }

    @Test
    fun lerpInterpolatesEndpoints() {
        assertEquals(10f, lerp(10f, 20f, 0f), 0f)
        assertEquals(20f, lerp(10f, 20f, 1f), 0f)
        assertEquals(15f, lerp(10f, 20f, 0.5f), 0f)
    }

    @Test
    fun staggeredEaseDelaysLaterLetters() {
        val first = staggeredEase(0.40f, 0, start = 0.30f, duration = 0.20f, step = 0.05f)
        val last = staggeredEase(0.40f, 5, start = 0.30f, duration = 0.20f, step = 0.05f)
        assertTrue(first > last)
        assertEquals(1f, staggeredEase(1f, 5, start = 0.30f, duration = 0.20f), 0.0001f)
    }

    @Test
    fun scrambleEnvelopeStartsHighAndLocksToZero() {
        assertEquals(1f, scrambleEnvelope(0.32f, 0.30f, 0.60f), 0.0001f)
        assertEquals(0f, scrambleEnvelope(0.60f, 0.30f, 0.60f), 0.0001f)
        assertEquals(0f, scrambleEnvelope(0.10f, 0.30f, 0.60f), 0.0001f)
    }
}
