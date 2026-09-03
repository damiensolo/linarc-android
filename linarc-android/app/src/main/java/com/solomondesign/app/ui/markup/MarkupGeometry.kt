package com.solomondesign.app.ui.markup

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Pure geometry for the markup editor: hit-testing, element bounds, and the ContentScale.Fit
 * rect that maps screen pixels to image fractions. No Compose imports, so all of it runs under
 * JVM unit tests — the drawing code and the editor screen are thin layers over these functions.
 */

/** Strokes register a tap within ~3% of the image; forgiving enough for gloved fingers. */
const val HIT_TOLERANCE = 0.03f

/** Resize grips get a larger target than strokes; grabbing beats precision. */
const val HANDLE_TOLERANCE = 0.05f

/** Drags smaller than 1% of the image are discarded as accidental touches, not annotations. */
const val MIN_DRAG_EXTENT = 0.01f

// A text label's tap target, estimated from its length. Rendering measures real text; the hit
// box only needs to be roughly right, and generous beats exact for selection.
internal const val LABEL_CHAR_WIDTH = 0.022f
internal const val LABEL_HEIGHT = 0.06f
internal const val LABEL_MAX_WIDTH = 0.9f

data class MarkupBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val maxDimension: Float get() = max(width, height)

    fun contains(p: MarkupPoint): Boolean = p.x in minX..maxX && p.y in minY..maxY

    fun expandedBy(d: Float) = MarkupBounds(minX - d, minY - d, maxX + d, maxY + d)
}

fun MarkupElement.bounds(): MarkupBounds = when (this) {
    is MarkupElement.Pen -> boundsOf(points)
    is MarkupElement.Line -> boundsOf(listOf(start, end))
    is MarkupElement.Shape -> boundsOf(listOf(a, b))
    is MarkupElement.Label -> MarkupBounds(
        minX = at.x,
        minY = at.y,
        maxX = at.x + min(LABEL_MAX_WIDTH, LABEL_CHAR_WIDTH * text.length),
        maxY = at.y + LABEL_HEIGHT,
    )
}

private fun boundsOf(points: List<MarkupPoint>): MarkupBounds {
    if (points.isEmpty()) return MarkupBounds(0f, 0f, 0f, 0f)
    return MarkupBounds(
        minX = points.minOf { it.x },
        minY = points.minOf { it.y },
        maxX = points.maxOf { it.x },
        maxY = points.maxOf { it.y },
    )
}

fun MarkupElement.hitTest(p: MarkupPoint, tolerance: Float = HIT_TOLERANCE): Boolean =
    when (this) {
        is MarkupElement.Pen ->
            if (points.size < 2) {
                points.isNotEmpty() && distance(p, points.first()) <= tolerance
            } else {
                points.zipWithNext().any { (a, b) -> distanceToSegment(p, a, b) <= tolerance }
            }

        is MarkupElement.Line -> distanceToSegment(p, start, end) <= tolerance

        // Shapes and labels select from anywhere inside — outline-only hit-testing would make
        // a big cloud nearly impossible to grab.
        is MarkupElement.Shape -> bounds().expandedBy(tolerance).contains(p)
        is MarkupElement.Label -> bounds().expandedBy(tolerance).contains(p)
    }

/** The grip positions a selected element renders and responds to. */
fun MarkupElement.handlePoints(): List<Pair<MarkupHandle, MarkupPoint>> = when (this) {
    is MarkupElement.Line -> listOf(MarkupHandle.A to start, MarkupHandle.B to end)
    is MarkupElement.Shape -> listOf(MarkupHandle.A to a, MarkupHandle.B to b)
    is MarkupElement.Pen, is MarkupElement.Label -> emptyList()
}

fun MarkupElement.handleAt(p: MarkupPoint, tolerance: Float = HANDLE_TOLERANCE): MarkupHandle? =
    handlePoints().firstOrNull { (_, at) -> distance(p, at) <= tolerance }?.first

/** How far a drawing drag travelled — its bounds' larger side; gates against accidental dots. */
fun MarkupElement.dragExtent(): Float = bounds().maxDimension

fun distance(a: MarkupPoint, b: MarkupPoint): Float = hypot(a.x - b.x, a.y - b.y)

fun distanceToSegment(p: MarkupPoint, a: MarkupPoint, b: MarkupPoint): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSquared = abx * abx + aby * aby
    if (lengthSquared == 0f) return distance(p, a)
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSquared).coerceIn(0f, 1f)
    return distance(p, MarkupPoint(a.x + t * abx, a.y + t * aby))
}

/** Where a ContentScale.Fit image actually lands inside its container, in container pixels. */
data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

fun fitRect(imageWidth: Int, imageHeight: Int, boxWidth: Float, boxHeight: Float): FitRect {
    if (imageWidth <= 0 || imageHeight <= 0 || boxWidth <= 0f || boxHeight <= 0f) {
        return FitRect(0f, 0f, max(0f, boxWidth), max(0f, boxHeight))
    }
    val scale = min(boxWidth / imageWidth, boxHeight / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return FitRect((boxWidth - width) / 2f, (boxHeight - height) / 2f, width, height)
}

/** Maps a pointer position in container pixels to an image fraction, clamped to the image. */
fun FitRect.normalizedPoint(px: Float, py: Float): MarkupPoint {
    if (width <= 0f || height <= 0f) return MarkupPoint(0f, 0f)
    return MarkupPoint(((px - left) / width).coerceIn(0f, 1f), ((py - top) / height).coerceIn(0f, 1f))
}
