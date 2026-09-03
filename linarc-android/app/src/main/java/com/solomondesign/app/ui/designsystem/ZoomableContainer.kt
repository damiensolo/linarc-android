package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import java.util.Locale

/*
 * Shared pinch-to-zoom container for full-screen inspection surfaces — the plan sheet viewer
 * and the photo viewer both render through this one implementation, so gesture behavior,
 * clamping, and the double-tap ritual stay identical everywhere.
 *
 * Performance: the transform is applied in [graphicsLayer], so zooming and panning never
 * re-measure, re-lay-out, or re-decode the content — only the draw layer's matrix changes.
 * Content is decoded once at its normal resolution; callers choose [maxScale] to match how
 * far their source material stays sharp (e.g. the photo viewer's 2048px decode budget).
 */

/** Furthest the content may pan on one axis at [scale] before its edge leaves the viewport. */
fun maxPanForZoom(containerEdgePx: Int, scale: Float): Float =
    (containerEdgePx * (scale - 1f)) / 2f

/** Clamps a pan candidate so zoomed content can never be dragged fully off-screen. */
fun clampPan(candidate: Float, containerEdgePx: Int, scale: Float): Float {
    val max = maxPanForZoom(containerEdgePx, scale)
    return candidate.coerceIn(-max, max)
}

/**
 * Pan that re-centers the viewport on the double-tapped point at [targetScale], clamped like
 * any other pan. Tapping dead-center yields zero pan; tapping an edge pans as far as allowed.
 */
fun doubleTapPanAxis(tapPx: Float, containerEdgePx: Int, targetScale: Float): Float =
    clampPan((containerEdgePx / 2f - tapPx) * (targetScale - 1f), containerEdgePx, targetScale)

/** Above this, a double tap resets to fit; at or below, it zooms in to the double-tap target. */
const val DOUBLE_TAP_RESET_THRESHOLD = 1.5f

/**
 * Pinch-to-zoom / pan / double-tap wrapper for one full-screen page of content.
 *
 * Gesture contract (identical for plans and photos):
 * - Pinch zooms between fit-to-screen (1×) and [maxScale].
 * - Single-finger pan engages only while zoomed in (`canPan = scale > 1`), so at rest scale
 *   the drag still belongs to whatever hosts this container (e.g. the plan pager's swipe).
 * - Double tap zooms to [doubleTapScale] centered on the tap, or resets when already zoomed
 *   past [DOUBLE_TAP_RESET_THRESHOLD].
 * - Pan is clamped so the content edge never crosses the viewport center — no losing the
 *   photo off-screen with a work glove.
 *
 * [active] resets to fit-to-screen when it flips false (a pager page swiped away);
 * [resetKey] restarts at fit when the content identity changes (a different photo id).
 * TalkBack reads the zoom state ("Fit to screen" / "Zoomed to 2.5x") via `stateDescription`.
 */
@Composable
fun ZoomableContainer(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    resetKey: Any? = null,
    maxScale: Float = 6f,
    doubleTapScale: Float = 2.5f,
    content: @Composable BoxScope.() -> Unit,
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(candidate: Offset, atScale: Float) = Offset(
        clampPan(candidate.x, containerSize.width, atScale),
        clampPan(candidate.y, containerSize.height, atScale),
    )

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, maxScale)
        offset = clampOffset(offset + panChange, scale)
    }

    LaunchedEffect(active) {
        if (!active) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .transformable(state = transformState, canPan = { scale > 1f })
            .pointerInput(resetKey) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > DOUBLE_TAP_RESET_THRESHOLD) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = doubleTapScale
                            offset = Offset(
                                doubleTapPanAxis(tap.x, size.width, doubleTapScale),
                                doubleTapPanAxis(tap.y, size.height, doubleTapScale),
                            )
                        }
                    },
                )
            }
            .semantics {
                stateDescription = if (scale > 1f) {
                    "Zoomed to ${String.format(Locale.US, "%.1f", scale)}x"
                } else {
                    "Fit to screen"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
