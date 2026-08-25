package com.solomondesign.app.ui.markup

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * One renderer for both surfaces: the live editor overlay and the baked JPEG export both call
 * [drawMarkupElements], and every stroke/text size is a fraction of the drawn area — so the
 * export is WYSIWYG by construction, not by keeping two drawing routines in sync.
 */

private const val STROKE_FRACTION = 0.008f
private const val LABEL_TEXT_FRACTION = 0.045f

fun DrawScope.drawMarkupElements(
    area: Size,
    elements: List<MarkupElement>,
    draft: MarkupElement?,
    selectedId: Long?,
    textMeasurer: TextMeasurer,
) {
    if (area.width <= 0f || area.height <= 0f) return
    val strokeWidth = max(2f, area.minDimension * STROKE_FRACTION)
    elements.forEach { drawElement(area, it, strokeWidth, textMeasurer) }
    draft?.let { drawElement(area, it, strokeWidth, textMeasurer) }
    val selected = selectedId?.let { id -> elements.firstOrNull { it.id == id } } ?: return
    drawSelection(area, selected, strokeWidth)
}

/**
 * Bakes annotations into a copy of [source] at full stored resolution.
 *
 * [density] must be the same density the editor composed [textMeasurer] with: the text measurer
 * converts sp back to pixels with its own density, so using it here makes the px-fraction font
 * sizing cancel exactly and match the overlay regardless of canvas size.
 */
fun renderMarkup(
    source: Bitmap,
    elements: List<MarkupElement>,
    textMeasurer: TextMeasurer,
    density: Density,
): Bitmap {
    if (elements.isEmpty()) return source
    val out = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(out.asImageBitmap()),
        size = Size(out.width.toFloat(), out.height.toFloat()),
    ) {
        drawMarkupElements(size, elements, draft = null, selectedId = null, textMeasurer = textMeasurer)
    }
    return out
}

private fun Size.pointAt(p: MarkupPoint) = Offset(p.x * width, p.y * height)

private fun DrawScope.drawElement(
    area: Size,
    element: MarkupElement,
    strokeWidth: Float,
    textMeasurer: TextMeasurer,
) {
    val color = Color(element.colorArgb)
    when (element) {
        is MarkupElement.Pen -> drawPen(area, element, color, strokeWidth)

        is MarkupElement.Line -> {
            val start = area.pointAt(element.start)
            val end = area.pointAt(element.end)
            drawLine(color, start, end, strokeWidth, cap = StrokeCap.Round)
            if (element.arrowAtEnd) drawArrowHead(tip = end, from = start, color = color, strokeWidth = strokeWidth)
            if (element.arrowAtStart) drawArrowHead(tip = start, from = end, color = color, strokeWidth = strokeWidth)
        }

        is MarkupElement.Shape -> {
            val b = element.bounds()
            val rect = Rect(
                left = b.minX * area.width,
                top = b.minY * area.height,
                right = b.maxX * area.width,
                bottom = b.maxY * area.height,
            )
            val style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
            when (element.kind) {
                ShapeKind.RECT -> drawRect(color, rect.topLeft, rect.size, style = style)
                ShapeKind.OVAL -> drawOval(color, rect.topLeft, rect.size, style = style)
                ShapeKind.CLOUD -> drawPath(cloudPath(rect), color, style = style)
            }
        }

        is MarkupElement.Label -> {
            val topLeft = area.pointAt(element.at)
            drawText(
                textMeasurer = textMeasurer,
                text = element.text,
                topLeft = topLeft,
                style = TextStyle(
                    color = color,
                    fontSize = (area.minDimension * LABEL_TEXT_FRACTION).toSp(),
                    fontWeight = FontWeight.SemiBold,
                    // Legible over both bright concrete and dark interiors.
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = strokeWidth),
                ),
                size = Size(
                    max(48f, area.width - topLeft.x),
                    max(32f, area.height - topLeft.y),
                ),
            )
        }
    }
}

private fun DrawScope.drawPen(area: Size, pen: MarkupElement.Pen, color: Color, strokeWidth: Float) {
    val points = pen.points.map { area.pointAt(it) }
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = strokeWidth * 1.5f, center = points.first())
        return
    }
    // Midpoint-quadratic smoothing: raw touch samples as straight segments look jagged.
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val current = points[i]
            quadraticTo(prev.x, prev.y, (prev.x + current.x) / 2f, (prev.y + current.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawArrowHead(tip: Offset, from: Offset, color: Color, strokeWidth: Float) {
    val angle = atan2(tip.y - from.y, tip.x - from.x)
    val wingLength = strokeWidth * 4.5f
    val spread = 0.5f
    listOf(angle + Math.PI.toFloat() - spread, angle + Math.PI.toFloat() + spread).forEach { a ->
        drawLine(
            color = color,
            start = tip,
            end = tip + Offset(cos(a), sin(a)) * wingLength,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Scalloped revision cloud: half-circle bumps marching around the dragged rectangle. */
private fun cloudPath(rect: Rect): Path {
    val path = Path()
    val r = max(4f, min(rect.width, rect.height) / 6f)
    path.moveTo(rect.left, rect.top)
    // Top, left → right: bumps bulge upward.
    edgeBumps(rect.width, r) { start, end ->
        path.arcTo(Rect(rect.left + start, rect.top - r, rect.left + end, rect.top + r), 180f, 180f, false)
    }
    // Right, top → bottom: bulge rightward.
    edgeBumps(rect.height, r) { start, end ->
        path.arcTo(Rect(rect.right - r, rect.top + start, rect.right + r, rect.top + end), 270f, 180f, false)
    }
    // Bottom, right → left: bulge downward.
    edgeBumps(rect.width, r) { start, end ->
        path.arcTo(Rect(rect.right - end, rect.bottom - r, rect.right - start, rect.bottom + r), 0f, 180f, false)
    }
    // Left, bottom → top: bulge leftward.
    edgeBumps(rect.height, r) { start, end ->
        path.arcTo(Rect(rect.left - r, rect.bottom - end, rect.left + r, rect.bottom - start), 90f, 180f, false)
    }
    path.close()
    return path
}

private inline fun edgeBumps(edgeLength: Float, r: Float, draw: (start: Float, end: Float) -> Unit) {
    val bumps = max(1, (edgeLength / (2f * r)).roundToInt())
    val step = edgeLength / bumps
    repeat(bumps) { i -> draw(i * step, (i + 1) * step) }
}

private fun DrawScope.drawSelection(area: Size, element: MarkupElement, strokeWidth: Float) {
    val b = element.bounds()
    val pad = strokeWidth * 1.5f
    drawRect(
        color = Color.White,
        topLeft = Offset(b.minX * area.width - pad, b.minY * area.height - pad),
        size = Size(b.width * area.width + pad * 2f, b.height * area.height + pad * 2f),
        style = Stroke(
            width = max(1.5f, strokeWidth * 0.6f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 2.5f, strokeWidth * 1.8f)),
        ),
    )
    element.handlePoints().forEach { (_, point) ->
        val center = area.pointAt(point)
        drawCircle(Color.White, radius = strokeWidth * 2.2f, center = center)
        drawCircle(Color(element.colorArgb), radius = strokeWidth * 1.3f, center = center)
    }
}
