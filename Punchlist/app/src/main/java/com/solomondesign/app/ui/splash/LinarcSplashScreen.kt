package com.solomondesign.app.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

internal const val SplashDurationMs = 2800
private const val ReducedMotionHoldMs = 700L

/**
 * LINARC wordmark timing. `t` is 0–1 over [SplashDurationMs] (2800ms),
 * so 0.10 = 280ms. Lower values = sooner / faster.
 *
 * Hand-edit here for the shared speed of each letter, then the `start =`
 * on each `splitType(` / `scrambleType(` call below for when LINARC begins
 * in that variant.
 */
private object WordmarkTiming {
    /** How long one letter takes to rise into its mask. */
    const val LETTER_DURATION = 0.13f
    /** Delay between L → I → N → A → R → C. */
    const val LETTER_STAGGER = 0.028f
    /** Tagline wipe starts this far after the first letter, plus stagger. */
    const val TAGLINE_GAP = 0.10f
    /** How long glyphs cycle before they lock (Signature / Bloom). */
    const val SCRAMBLE_WINDOW = 0.18f
}

/**
 * Depth's logo-to-Today handoff. `t` is 0–1 over [SplashDurationMs].
 * Black holds until BLACK_FADE_START; home unblurs only after the lockup has played.
 */
private object DepthTransition {
    const val BLACK_FADE_START = 0.68f
    const val BLACK_FADE_END = 0.82f
    const val LOCKUP_FADE_START = 0.70f
    const val LOCKUP_FADE_END = 0.86f
    /** Unblur only after black has cleared, so the frost is actually visible. */
    const val HOME_UNBLUR_START = 0.84f
    const val HOME_UNBLUR_END = 1.00f
}

@Composable
fun LinarcSplashScreen(
    variant: SplashVariant,
    playbackId: Int,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onRevealProgress: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val progress = remember(playbackId, variant) { Animatable(0f) }
    val reduceMotion = remember(playbackId) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    LaunchedEffect(playbackId, variant) {
        if (reduceMotion) {
            progress.snapTo(1f)
            onRevealProgress(0f)
            delay(ReducedMotionHoldMs)
        } else {
            progress.snapTo(0f)
            onRevealProgress(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SplashDurationMs, easing = LinearEasing),
            )
        }
        onRevealProgress(1f)
        onFinished()
    }

    val t = progress.value
    val dissolve = if (reduceMotion) {
        1f
    } else {
        when (variant) {
            // Depth fades its own black + lockup; keep the overlay opaque so
            // the scaffold blur can take over underneath.
            SplashVariant.DEPTH -> 1f
            else -> 1f - easeInOutCubic(segment(t, 0.88f, 1f))
        }
    }
    SideEffect {
        if (!reduceMotion) {
            onRevealProgress(homeRevealProgress(t, variant))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = dissolve }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
            .testTag("splashScreen")
            .semantics { contentDescription = "Linarc" },
        contentAlignment = Alignment.Center,
    ) {
        when (variant) {
            SplashVariant.MORPH -> MorphSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.SPLIT -> SplitSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.DEPTH -> DepthSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.BLOOM -> BloomSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.ASSEMBLE -> AssembleSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.SIGNATURE -> SignatureSplash(t = t, reduceMotion = reduceMotion)
            SplashVariant.IRIS -> IrisSplash(t = t, reduceMotion = reduceMotion)
        }
    }
}

@Composable
private fun MorphSplash(t: Float, reduceMotion: Boolean) {
    val lock = if (reduceMotion) 1f else easeOutQuint(segment(t, 0.04f, 0.40f))
    val type = splitType(t, start = 0.16f, reduceMotion = reduceMotion)
    val field = if (reduceMotion) 0.2f else (1f - easeInOutCubic(segment(t, 0.62f, 0.90f)))

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AmbientField(
            t = t,
            alpha = field,
            base = Color(0xFF070707),
            opaque = false,
        )
        CenteredLockup {
            LinarcLockup(
                top = BladeMotion(
                    alpha = lock,
                    offsetY = lerp(-28f, 0f, lock),
                    scale = lerp(1.55f, 1f, lock),
                ),
                right = BladeMotion(
                    alpha = lock,
                    offsetX = lerp(22f, 0f, lock),
                    offsetY = lerp(16f, 0f, lock),
                    scale = lerp(1.55f, 1f, lock),
                ),
                left = BladeMotion(
                    alpha = lock,
                    offsetX = lerp(-22f, 0f, lock),
                    offsetY = lerp(16f, 0f, lock),
                    scale = lerp(1.55f, 1f, lock),
                ),
                markColor = LinarcOrange,
                ink = Color.White,
                glowAlpha = (1f - lock) * 0.7f + 0.15f,
                markScale = 1f,
                markBlur = lerp(18f, 0f, lock),
                letterAlphas = type.alphas,
                letterOffsetYs = type.offsetYs,
                letterClips = type.clips,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun SplitSplash(t: Float, reduceMotion: Boolean) {
    val appear = if (reduceMotion) 1f else easeOutQuint(segment(t, 0.04f, 0.32f))
    val type = splitType(t, start = 0.12f, reduceMotion = reduceMotion, step = 0.036f, duration = 0.16f)

    BrandCanvas(background = Color(0xFF050505)) {
        CenteredLockup {
            LinarcLockup(
                top = RestingBlade.copy(alpha = appear),
                right = RestingBlade.copy(alpha = appear),
                left = RestingBlade.copy(alpha = appear),
                markColor = LinarcOrange,
                ink = Color.White,
                glowAlpha = (1f - appear) * 0.35f,
                markScale = lerp(0.92f, 1f, appear),
                letterAlphas = type.alphas,
                letterOffsetYs = type.offsetYs,
                letterClips = type.clips,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun DepthSplash(t: Float, reduceMotion: Boolean) {
    val settle = if (reduceMotion) 1f else easeOutCubic(segment(t, 0.06f, 0.48f))
    val type = splitType(
        t,
        start = 0.14f,
        reduceMotion = reduceMotion,
        step = WordmarkTiming.LETTER_STAGGER,
        duration = WordmarkTiming.LETTER_DURATION,
    )
    val tiltX = lerp(9f, 0f, settle)
    val tiltY = lerp(-7f, 0f, settle)
    val density = LocalDensity.current
    val blackAlpha = if (reduceMotion) {
        1f
    } else {
        1f - easeInOutCubic(
            segment(t, DepthTransition.BLACK_FADE_START, DepthTransition.BLACK_FADE_END),
        )
    }
    val lockupAlpha = if (reduceMotion) {
        1f
    } else {
        1f - easeInOutCubic(
            segment(t, DepthTransition.LOCKUP_FADE_START, DepthTransition.LOCKUP_FADE_END),
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer { alpha = blackAlpha },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = lockupAlpha },
            contentAlignment = Alignment.Center,
        ) {
            CenteredLockup {
                Box {
                    LinarcLockup(
                        top = RestingBlade,
                        right = RestingBlade,
                        left = RestingBlade,
                        markColor = LinarcOrange,
                        ink = Color.Transparent,
                        glowAlpha = 0.35f,
                        markBlur = 16f,
                        letterAlphas = List(6) { 0f },
                        taglineAlpha = 0f,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.4f
                            translationY = lerp(14f, 6f, settle)
                        },
                    )
                    LinarcLockup(
                        top = BladeMotion(alpha = settle, offsetY = lerp(-12f, 0f, settle)),
                        right = BladeMotion(alpha = settle, offsetX = lerp(10f, 0f, settle)),
                        left = BladeMotion(alpha = settle, offsetX = lerp(-10f, 0f, settle)),
                        markColor = LinarcOrange,
                        ink = Color.White,
                        glowAlpha = (1f - settle) * 0.45f,
                        letterAlphas = type.alphas,
                        letterOffsetYs = type.offsetYs,
                        letterClips = type.clips,
                        taglineAlpha = type.taglineAlpha,
                        taglineClip = type.taglineClip,
                        modifier = Modifier.graphicsLayer {
                            rotationX = tiltX
                            rotationY = tiltY
                            cameraDistance = 18f * density.density
                            transformOrigin = TransformOrigin(0.5f, 0.38f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BloomSplash(t: Float, reduceMotion: Boolean) {
    val appear = if (reduceMotion) 1f else easeOutQuint(segment(t, 0.06f, 0.40f))
    val bloom = if (reduceMotion) 0.35f else sin(segment(t, 0.12f, 0.55f) * Math.PI).toFloat()
    val type = scrambleType(t, start = 0.28f, reduceMotion = reduceMotion)
    val sweep = if (reduceMotion) 1f else easeOutCubic(segment(t, 0.36f, 0.62f))
    val field = if (reduceMotion) 0.2f else (1f - easeInOutCubic(segment(t, 0.64f, 0.90f)))

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LinarcOrange.copy(alpha = bloom * 0.55f * field),
                        Color.Transparent,
                    ),
                    center = c,
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = c,
            )
            val sweepX = lerp(-size.width * 0.3f, size.width * 1.1f, sweep)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f * field),
                        Color.Transparent,
                    ),
                    start = Offset(sweepX - 80f, 0f),
                    end = Offset(sweepX + 80f, size.height),
                ),
            )
        }
        CenteredLockup {
            LinarcLockup(
                top = RestingBlade.copy(alpha = appear),
                right = RestingBlade.copy(alpha = appear),
                left = RestingBlade.copy(alpha = appear),
                markColor = LinarcOrange,
                ink = Color.White,
                glowAlpha = 0.25f + bloom * 0.7f,
                markScale = lerp(0.90f, 1f, appear),
                letterAlphas = type.alphas,
                letterClips = type.clips,
                letterScramble = type.scramble,
                scrambleClock = t,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun AssembleSplash(t: Float, reduceMotion: Boolean) {
    val topT = if (reduceMotion) 1f else easeOutBack(segment(t, 0.04f, 0.32f))
    val rightT = if (reduceMotion) 1f else easeOutBack(segment(t, 0.10f, 0.38f))
    val leftT = if (reduceMotion) 1f else easeOutBack(segment(t, 0.16f, 0.44f))
    val glow = if (reduceMotion) 0f else sin(segment(t, 0.28f, 0.52f) * Math.PI).toFloat()
    val settle = if (reduceMotion) 1f else easeOutCubic(segment(t, 0.34f, 0.52f))
    val type = splitType(t, start = 0.40f, reduceMotion = reduceMotion, step = 0.045f)

    BrandCanvas(background = Color.Black) {
        CenteredLockup {
            LinarcLockup(
                top = BladeMotion(
                    alpha = topT,
                    offsetY = lerp(-56f, 0f, topT),
                    rotationDeg = lerp(-16f, 0f, topT),
                    scale = lerp(0.72f, 1f, topT),
                ),
                right = BladeMotion(
                    alpha = rightT,
                    offsetX = lerp(48f, 0f, rightT),
                    offsetY = lerp(32f, 0f, rightT),
                    rotationDeg = lerp(14f, 0f, rightT),
                    scale = lerp(0.72f, 1f, rightT),
                ),
                left = BladeMotion(
                    alpha = leftT,
                    offsetX = lerp(-48f, 0f, leftT),
                    offsetY = lerp(32f, 0f, leftT),
                    rotationDeg = lerp(-14f, 0f, leftT),
                    scale = lerp(0.72f, 1f, leftT),
                ),
                markColor = LinarcOrange,
                ink = Color.White,
                glowAlpha = glow * 0.85f,
                markScale = lerp(1.05f, 1f, settle),
                letterAlphas = type.alphas,
                letterOffsetYs = type.offsetYs,
                letterClips = type.clips,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun SignatureSplash(t: Float, reduceMotion: Boolean) {
    val appear = if (reduceMotion) 1f else easeOutQuint(segment(t, 0.06f, 0.40f))
    val bloom = if (reduceMotion) 0f else sin(segment(t, 0.16f, 0.48f) * Math.PI).toFloat()
    val type = scrambleType(t, start = 0.30f, reduceMotion = reduceMotion)

    BrandCanvas(background = Color(0xFF050505)) {
        CenteredLockup {
            LinarcLockup(
                top = RestingBlade.copy(alpha = appear),
                right = RestingBlade.copy(alpha = appear),
                left = RestingBlade.copy(alpha = appear),
                markColor = LinarcOrange,
                ink = Color.White,
                glowAlpha = bloom * 0.9f,
                markScale = lerp(0.86f, 1f, appear),
                letterAlphas = type.alphas,
                letterClips = type.clips,
                letterScramble = type.scramble,
                scrambleClock = t,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun IrisSplash(t: Float, reduceMotion: Boolean) {
    val circleT = if (reduceMotion) 1f else easeOutQuint(segment(t, 0.00f, 0.38f))
    val appear = if (reduceMotion) 1f else easeOutCubic(segment(t, 0.18f, 0.48f))
    val type = splitType(t, start = 0.36f, reduceMotion = reduceMotion, step = 0.055f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val coverPx = hypot(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()) * 1.08f
        val diameterDp = with(density) { (coverPx * circleT).toDp() }

        Box(
            modifier = Modifier
                .size(diameterDp)
                .clip(CircleShape)
                .background(LinarcOrange),
        )
        CenteredLockup {
            LinarcLockup(
                top = RestingBlade.copy(alpha = appear),
                right = RestingBlade.copy(alpha = appear),
                left = RestingBlade.copy(alpha = appear),
                markColor = Color.White,
                ink = Color.White,
                markScale = lerp(1.08f, 1f, appear),
                letterAlphas = type.alphas,
                letterOffsetYs = type.offsetYs,
                letterClips = type.clips,
                taglineAlpha = type.taglineAlpha,
                taglineClip = type.taglineClip,
            )
        }
    }
}

@Composable
private fun AmbientField(
    t: Float,
    alpha: Float,
    base: Color,
    modifier: Modifier = Modifier,
    ember: Color = LinarcOrange,
    opaque: Boolean = true,
) {
    Canvas(
        modifier.then(
            Modifier
                .fillMaxSize()
                .then(if (opaque) Modifier.background(base) else Modifier),
        ),
    ) {
        if (alpha <= 0.01f) return@Canvas
        val w = size.width
        val h = size.height
        val drift = t * 6.28f
        fun blob(cx: Float, cy: Float, radius: Float, color: Color, a: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = a * alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
        blob(
            cx = w * (0.35f + 0.08f * sin(drift)),
            cy = h * (0.32f + 0.06f * cos(drift * 0.7f)),
            radius = w * 0.62f,
            color = ember,
            a = 0.42f,
        )
        blob(
            cx = w * (0.72f + 0.07f * cos(drift * 0.9f)),
            cy = h * (0.58f + 0.08f * sin(drift * 0.6f)),
            radius = w * 0.55f,
            color = Color(0xFFEA580C),
            a = 0.28f,
        )
        blob(
            cx = w * (0.50f + 0.10f * sin(drift * 0.5f)),
            cy = h * (0.78f + 0.04f * cos(drift)),
            radius = w * 0.48f,
            color = ember,
            a = 0.18f,
        )
    }
}

@Composable
private fun BrandCanvas(
    background: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun CenteredLockup(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.width(LockupDisplayWidthDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private data class TypeMotion(
    val alphas: List<Float>,
    val clips: List<Float>,
    val offsetYs: List<Float>,
    val scramble: Float,
    val taglineClip: Float,
    val taglineAlpha: Float,
)

private val RestingType = TypeMotion(
    alphas = List(6) { 1f },
    clips = List(6) { 1f },
    offsetYs = List(6) { 0f },
    scramble = 0f,
    taglineClip = 1f,
    taglineAlpha = 0.92f,
)

private fun homeRevealProgress(t: Float, variant: SplashVariant): Float {
    if (!variant.revealsHome) return 0f
    return if (variant == SplashVariant.DEPTH) {
        easeInOutCubic(
            segment(t, DepthTransition.HOME_UNBLUR_START, DepthTransition.HOME_UNBLUR_END),
        )
    } else {
        easeOutCubic(segment(t, 0.68f, 0.92f))
    }
}

/** Overflow-hidden rise, one letter at a time — SplitText / Zeit / Lumio. */
private fun splitType(
    t: Float,
    start: Float,
    reduceMotion: Boolean,
    step: Float = WordmarkTiming.LETTER_STAGGER,
    duration: Float = WordmarkTiming.LETTER_DURATION,
): TypeMotion {
    if (reduceMotion) return RestingType
    val clips = List(6) { staggeredEase(t, it, start, duration, step) }
    val offsetYs = List(6) { lerp(26f, 0f, clips[it]) }
    val alphas = List(6) { if (clips[it] > 0.03f) 1f else 0f }
    val tagStart = start + WordmarkTiming.TAGLINE_GAP + step * 5f
    val tagClip = easeOutCubic(segment(t, tagStart, tagStart + 0.18f))
    return TypeMotion(
        alphas = alphas,
        clips = clips,
        offsetYs = offsetYs,
        scramble = 0f,
        taglineClip = tagClip,
        taglineAlpha = tagClip * 0.92f,
    )
}

/** Random glyphs cycle inside each Figma slot, then snap to LINARC. */
private fun scrambleType(
    t: Float,
    start: Float,
    reduceMotion: Boolean,
): TypeMotion {
    if (reduceMotion) return RestingType
    val appear = easeOutCubic(segment(t, start, start + 0.05f))
    val scramble = scrambleEnvelope(t, start, start + WordmarkTiming.SCRAMBLE_WINDOW)
    val tagStart = start + WordmarkTiming.SCRAMBLE_WINDOW
    val tagClip = easeOutCubic(segment(t, tagStart, tagStart + 0.16f))
    return TypeMotion(
        alphas = List(6) { appear },
        clips = List(6) { 1f },
        offsetYs = List(6) { 0f },
        scramble = scramble,
        taglineClip = tagClip,
        taglineAlpha = tagClip * 0.92f,
    )
}
