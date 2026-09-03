package com.solomondesign.app.ui.markup

/**
 * Markup domain model. Deliberately free of `androidx.compose.ui` imports (same rule as
 * `AppChrome.kt`) so the editor state machine runs under plain JVM unit tests. All coordinates
 * are image fractions in 0..1 — the same convention `DemoMarkup` and `PlanPin` already use —
 * so annotations are independent of both the on-screen fit rect and the export bitmap size.
 */

enum class MarkupTool { SELECT, PEN, LINE, ARROW, DOUBLE_ARROW, RECT, OVAL, CLOUD, TEXT }

/** Outline shapes share one element class; the kind only changes how the outline is drawn. */
enum class ShapeKind { RECT, OVAL, CLOUD }

/** The two draggable grips of a resizable element: a line's ends or a shape's two corners. */
enum class MarkupHandle { A, B }

data class MarkupPoint(val x: Float, val y: Float)

fun MarkupPoint.clamped() = MarkupPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))

internal fun MarkupPoint.offsetBy(dx: Float, dy: Float) = MarkupPoint(x + dx, y + dy)

/**
 * One annotation. The requested tool set collapses to four element classes: line, arrow, and
 * double arrow are a [Line] with arrowhead flags; square, oval, and cloud are a [Shape] with a
 * [ShapeKind] — fewer classes, identical capability.
 */
sealed interface MarkupElement {
    val id: Long
    val colorArgb: Long

    data class Pen(
        override val id: Long,
        override val colorArgb: Long,
        val points: List<MarkupPoint>,
    ) : MarkupElement

    data class Line(
        override val id: Long,
        override val colorArgb: Long,
        val start: MarkupPoint,
        val end: MarkupPoint,
        val arrowAtStart: Boolean,
        val arrowAtEnd: Boolean,
    ) : MarkupElement

    /** [a] and [b] are opposite corners exactly as dragged; consumers normalise via bounds. */
    data class Shape(
        override val id: Long,
        override val colorArgb: Long,
        val a: MarkupPoint,
        val b: MarkupPoint,
        val kind: ShapeKind,
    ) : MarkupElement

    data class Label(
        override val id: Long,
        override val colorArgb: Long,
        val at: MarkupPoint,
        val text: String,
    ) : MarkupElement
}

fun MarkupElement.withColor(argb: Long): MarkupElement = when (this) {
    is MarkupElement.Pen -> copy(colorArgb = argb)
    is MarkupElement.Line -> copy(colorArgb = argb)
    is MarkupElement.Shape -> copy(colorArgb = argb)
    is MarkupElement.Label -> copy(colorArgb = argb)
}

fun MarkupElement.translated(dx: Float, dy: Float): MarkupElement = when (this) {
    is MarkupElement.Pen -> copy(points = points.map { it.offsetBy(dx, dy) })
    is MarkupElement.Line -> copy(start = start.offsetBy(dx, dy), end = end.offsetBy(dx, dy))
    is MarkupElement.Shape -> copy(a = a.offsetBy(dx, dy), b = b.offsetBy(dx, dy))
    is MarkupElement.Label -> copy(at = at.offsetBy(dx, dy))
}

/** Extends an in-progress drag: pens accumulate points, two-point elements move their far end. */
internal fun MarkupElement.draggedTo(p: MarkupPoint): MarkupElement = when (this) {
    is MarkupElement.Pen -> copy(points = points + p)
    is MarkupElement.Line -> copy(end = p)
    is MarkupElement.Shape -> copy(b = p)
    is MarkupElement.Label -> this
}

internal fun MarkupElement.withHandleAt(handle: MarkupHandle, p: MarkupPoint): MarkupElement =
    when (this) {
        is MarkupElement.Line ->
            if (handle == MarkupHandle.A) copy(start = p) else copy(end = p)
        is MarkupElement.Shape ->
            if (handle == MarkupHandle.A) copy(a = p) else copy(b = p)
        is MarkupElement.Pen, is MarkupElement.Label -> this
    }

/** The construction-markup palette. ARGB longs, not `Color`, to stay JVM-testable. */
object MarkupPalette {
    data class Option(val label: String, val argb: Long)

    val OPTIONS = listOf(
        Option("Red", 0xFFE53935),
        Option("Yellow", 0xFFFDD835),
        Option("Blue", 0xFF1E88E5),
        Option("Green", 0xFF43A047),
        Option("White", 0xFFFFFFFF),
        Option("Black", 0xFF000000),
    )

    val DEFAULT = OPTIONS.first().argb
}
