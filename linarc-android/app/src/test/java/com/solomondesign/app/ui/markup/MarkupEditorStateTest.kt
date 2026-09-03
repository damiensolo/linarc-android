package com.solomondesign.app.ui.markup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the markup editor's whole state machine — no Compose UI, no device. */
class MarkupEditorStateTest {

    private fun p(x: Float, y: Float) = MarkupPoint(x, y)

    private fun MarkupEditorState.drag(from: MarkupPoint, vararg through: MarkupPoint) {
        beginGesture(from)
        through.forEach { moveGesture(it) }
        endGesture()
    }

    @Test
    fun penDragCommitsAndUndoRedoRoundTrip() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.PEN)
        state.drag(p(0.1f, 0.1f), p(0.2f, 0.2f), p(0.3f, 0.25f))

        val pen = state.elements.single() as MarkupElement.Pen
        assertEquals(3, pen.points.size)
        assertTrue(state.hasMarkup)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)

        state.undo()
        assertTrue(state.elements.isEmpty())
        assertTrue(state.canRedo)

        state.redo()
        assertEquals(listOf<MarkupElement>(pen), state.elements)
    }

    @Test
    fun eachDrawingToolCreatesItsElement() {
        val state = MarkupEditorState()
        val start = p(0.2f, 0.2f)
        val end = p(0.6f, 0.5f)

        listOf(
            MarkupTool.LINE,
            MarkupTool.ARROW,
            MarkupTool.DOUBLE_ARROW,
            MarkupTool.RECT,
            MarkupTool.OVAL,
            MarkupTool.CLOUD,
        ).forEach { tool ->
            state.selectTool(tool)
            state.drag(start, end)
            when (val made = state.elements.last()) {
                is MarkupElement.Line -> {
                    assertEquals(tool.name, start, made.start)
                    assertEquals(tool.name, end, made.end)
                    assertEquals(tool.name, tool == MarkupTool.DOUBLE_ARROW, made.arrowAtStart)
                    assertEquals(tool.name, tool != MarkupTool.LINE, made.arrowAtEnd)
                }

                is MarkupElement.Shape -> {
                    assertEquals(tool.name, ShapeKind.valueOf(tool.name), made.kind)
                    assertEquals(tool.name, start, made.a)
                    assertEquals(tool.name, end, made.b)
                }

                else -> throw AssertionError("$tool made ${made::class.simpleName}")
            }
        }
        assertEquals(6, state.elements.size)
    }

    /** A sub-1%-of-image drag is an accidental touch, not an annotation. */
    @Test
    fun tinyDragsAreDiscarded() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.RECT)
        state.drag(p(0.5f, 0.5f), p(0.503f, 0.5f))
        assertTrue(state.elements.isEmpty())
        assertFalse(state.canUndo)
        assertNull(state.draft)
    }

    @Test
    fun pointsClampToTheImage() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.LINE)
        state.drag(p(-0.4f, 0.5f), p(1.6f, 2f))
        val line = state.elements.single() as MarkupElement.Line
        assertEquals(p(0f, 0.5f), line.start)
        assertEquals(p(1f, 1f), line.end)
    }

    @Test
    fun selectTapsThenDragMovesAndUndoRestores() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.RECT)
        state.drag(p(0.2f, 0.2f), p(0.4f, 0.4f))
        val original = state.elements.single() as MarkupElement.Shape

        state.selectTool(MarkupTool.SELECT)
        state.tapAt(p(0.3f, 0.3f))
        assertEquals(original.id, state.selectedId)

        state.drag(p(0.3f, 0.3f), p(0.5f, 0.6f))
        val moved = state.elements.single() as MarkupElement.Shape
        assertEquals(0.4f, moved.a.x, 1e-4f)
        assertEquals(0.5f, moved.a.y, 1e-4f)
        assertEquals(0.6f, moved.b.x, 1e-4f)
        assertEquals(0.7f, moved.b.y, 1e-4f)

        state.undo()
        assertEquals(listOf<MarkupElement>(original), state.elements)
        assertNull("undo clears selection", state.selectedId)

        // Tapping empty space clears the selection without touching history.
        state.tapAt(p(0.3f, 0.3f))
        assertNotNull(state.selectedId)
        state.tapAt(p(0.9f, 0.9f))
        assertNull(state.selectedId)
    }

    @Test
    fun gripsResizeShapesAndLineEndpoints() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.RECT)
        state.drag(p(0.2f, 0.2f), p(0.4f, 0.4f))
        state.selectTool(MarkupTool.SELECT)
        state.tapAt(p(0.3f, 0.3f))

        // Grab corner b and drag: only that corner follows.
        state.drag(p(0.4f, 0.4f), p(0.7f, 0.8f))
        val resized = state.elements.single() as MarkupElement.Shape
        assertEquals(p(0.2f, 0.2f), resized.a)
        assertEquals(p(0.7f, 0.8f), resized.b)

        state.selectTool(MarkupTool.ARROW)
        state.drag(p(0.1f, 0.9f), p(0.3f, 0.9f))
        state.selectTool(MarkupTool.SELECT)
        state.tapAt(p(0.2f, 0.9f))
        state.drag(p(0.3f, 0.9f), p(0.3f, 0.6f))
        val line = state.elements.last() as MarkupElement.Line
        assertEquals(p(0.1f, 0.9f), line.start)
        assertEquals(p(0.3f, 0.6f), line.end)
    }

    @Test
    fun textToolPlacesEditsAndIgnoresBlanks() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.TEXT)

        state.tapAt(p(0.5f, 0.5f))
        val request = state.pendingText
        assertNotNull(request)
        assertNull(request!!.existingId)
        state.commitPendingText("Check ledger")
        val label = state.elements.single() as MarkupElement.Label
        assertEquals("Check ledger", label.text)
        assertEquals(p(0.5f, 0.5f), label.at)
        assertNull(state.pendingText)

        // Tapping the existing label edits it in place.
        state.tapAt(p(0.51f, 0.52f))
        val edit = state.pendingText
        assertEquals(label.id, edit?.existingId)
        assertEquals("Check ledger", edit?.initialText)
        state.commitPendingText("Check ledger height")
        assertEquals(
            "Check ledger height",
            (state.elements.single() as MarkupElement.Label).text,
        )

        // A blank entry adds nothing and burns no history.
        state.tapAt(p(0.1f, 0.1f))
        state.commitPendingText("   ")
        assertEquals(1, state.elements.size)

        state.tapAt(p(0.1f, 0.1f))
        state.cancelPendingText()
        assertNull(state.pendingText)
        assertEquals(1, state.elements.size)
    }

    @Test
    fun deleteSelectedRemovesAndUndoRestores() {
        val state = MarkupEditorState()
        state.selectTool(MarkupTool.OVAL)
        state.drag(p(0.2f, 0.2f), p(0.5f, 0.5f))
        state.selectTool(MarkupTool.SELECT)
        state.tapAt(p(0.3f, 0.3f))

        state.deleteSelected()
        assertTrue(state.elements.isEmpty())
        assertNull(state.selectedId)

        state.undo()
        assertEquals(1, state.elements.size)
    }

    @Test
    fun colorAppliesToNewElementsAndRecolorsTheSelection() {
        val state = MarkupEditorState()
        val blue = MarkupPalette.OPTIONS.first { it.label == "Blue" }.argb
        val green = MarkupPalette.OPTIONS.first { it.label == "Green" }.argb

        state.selectTool(MarkupTool.LINE)
        state.drag(p(0.1f, 0.1f), p(0.5f, 0.1f))
        assertEquals(MarkupPalette.DEFAULT, state.elements.single().colorArgb)

        state.setColor(blue)
        state.drag(p(0.1f, 0.3f), p(0.5f, 0.3f))
        assertEquals(blue, state.elements.last().colorArgb)

        state.selectTool(MarkupTool.SELECT)
        state.tapAt(p(0.3f, 0.1f))
        state.setColor(green)
        assertEquals(green, state.elements.first().colorArgb)
        state.undo()
        assertEquals(MarkupPalette.DEFAULT, state.elements.first().colorArgb)
    }

    @Test
    fun fitRectCentersLetterboxAndPillarbox() {
        // Wider box than image: pillarbox, centered horizontally.
        assertEquals(FitRect(50f, 0f, 100f, 100f), fitRect(100, 100, 200f, 100f))
        // Taller box than image: letterbox, centered vertically.
        assertEquals(FitRect(0f, 25f, 100f, 50f), fitRect(200, 100, 100f, 100f))
        // Pointer mapping clamps to the image even outside the fit rect.
        val fit = fitRect(100, 100, 200f, 100f)
        assertEquals(MarkupPoint(0f, 0.5f), fit.normalizedPoint(10f, 50f))
        assertEquals(MarkupPoint(0.5f, 1f), fit.normalizedPoint(100f, 300f))
    }
}
