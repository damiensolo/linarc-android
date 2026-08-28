package com.solomondesign.app.ui.splash

/** Maps a 0–1 timeline onto a sub-range, clamped. */
internal fun segment(t: Float, start: Float, end: Float): Float {
    if (end <= start) return if (t >= start) 1f else 0f
    return ((t - start) / (end - start)).coerceIn(0f, 1f)
}

internal fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

internal fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

internal fun easeOutQuint(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u * u * u
}

/** Slight overshoot — used when the blades snap into the mark. */
internal fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

internal fun easeInOutCubic(t: Float): Float =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f

/** GSAP SplitText-style stagger: each index starts [step] later. */
internal fun staggeredEase(
    t: Float,
    index: Int,
    start: Float,
    duration: Float,
    step: Float = 0.048f,
): Float {
    val s = start + index * step
    return easeOutCubic(segment(t, s, s + duration))
}

/**
 * ScrambleText envelope: full glyph cycling, then a hard lock.
 * Returns 1 at the start of the window and 0 at the end.
 */
internal fun scrambleEnvelope(t: Float, start: Float, end: Float): Float {
    if (t < start) return 0f
    val p = segment(t, start, end)
    return if (p < 0.62f) 1f else 1f - easeOutCubic((p - 0.62f) / 0.38f)
}
