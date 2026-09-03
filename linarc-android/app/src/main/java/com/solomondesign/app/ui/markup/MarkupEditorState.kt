package com.solomondesign.app.ui.markup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A requested text entry: where the label goes, or which existing label is being edited. */
data class PendingText(
    val at: MarkupPoint,
    val existingId: Long? = null,
    val initialText: String = "",
)

/**
 * The markup editor's whole state machine — tool choice, gesture handling, selection,
 * move/resize, and undo/redo — with no Compose UI dependencies (only snapshot state, same as
 * [CameraStateHolder][com.solomondesign.app.ui.capture.camera.CameraStateHolder]), so every
 * behaviour here is provable in plain JVM unit tests.
 *
 * Gestures arrive already normalised to image fractions (the screen maps pixels through
 * [FitRect.normalizedPoint]): drags draw or move, taps select or place text, depending on [tool].
 */
class MarkupEditorState {

    var tool by mutableStateOf(MarkupTool.PEN)
        private set
    var colorArgb by mutableLongStateOf(MarkupPalette.DEFAULT)
        private set
    var elements by mutableStateOf(listOf<MarkupElement>())
        private set

    /** The in-progress drag, rendered on top but not yet committed (or undoable). */
    var draft by mutableStateOf<MarkupElement?>(null)
        private set
    var selectedId by mutableStateOf<Long?>(null)
        private set
    var pendingText by mutableStateOf<PendingText?>(null)
        private set

    // Undo/redo hold whole-list snapshots: elements are tiny value objects, and snapshots make
    // move/resize (arbitrary intermediate states) trivially reversible.
    private var undoStack by mutableStateOf(listOf<List<MarkupElement>>())
    private var redoStack by mutableStateOf(listOf<List<MarkupElement>>())

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val hasMarkup: Boolean get() = elements.isNotEmpty()

    private var nextId = 1L
    private var gesture: Gesture = Gesture.None

    private sealed interface Gesture {
        data object None : Gesture
        data object Draw : Gesture
        data class Move(val before: List<MarkupElement>, val last: MarkupPoint) : Gesture
        data class Resize(
            val before: List<MarkupElement>,
            val elementId: Long,
            val handle: MarkupHandle,
        ) : Gesture
    }

    fun selectTool(newTool: MarkupTool) {
        tool = newTool
        pendingText = null
        if (newTool != MarkupTool.SELECT) selectedId = null
    }

    /** Sets the drawing color; with a selection active it recolors that element (undoable). */
    fun setColor(argb: Long) {
        colorArgb = argb
        val id = selectedId ?: return
        val current = elements.firstOrNull { it.id == id } ?: return
        if (current.colorArgb == argb) return
        pushHistory(elements)
        elements = elements.map { if (it.id == id) it.withColor(argb) else it }
    }

    fun beginGesture(raw: MarkupPoint) {
        val p = raw.clamped()
        pendingText = null
        when (tool) {
            MarkupTool.SELECT -> beginSelectGesture(p)
            // Text places on tap; a drag with the text tool does nothing.
            MarkupTool.TEXT -> Unit
            else -> {
                selectedId = null
                draft = newElementAt(p)
                gesture = Gesture.Draw
            }
        }
    }

    fun moveGesture(raw: MarkupPoint) {
        val p = raw.clamped()
        when (val g = gesture) {
            Gesture.Draw -> draft = draft?.draggedTo(p)

            is Gesture.Move -> {
                val id = selectedId ?: return
                elements = elements.map {
                    if (it.id == id) it.translated(p.x - g.last.x, p.y - g.last.y) else it
                }
                gesture = g.copy(last = p)
            }

            is Gesture.Resize ->
                elements = elements.map {
                    if (it.id == g.elementId) it.withHandleAt(g.handle, p) else it
                }

            Gesture.None -> Unit
        }
    }

    fun endGesture() {
        when (val g = gesture) {
            Gesture.Draw -> {
                val finished = draft
                draft = null
                if (finished != null && finished.dragExtent() >= MIN_DRAG_EXTENT) {
                    pushHistory(elements)
                    elements = elements + finished
                }
            }

            is Gesture.Move -> if (elements != g.before) pushHistory(g.before)
            is Gesture.Resize -> if (elements != g.before) pushHistory(g.before)
            Gesture.None -> Unit
        }
        gesture = Gesture.None
    }

    fun tapAt(raw: MarkupPoint) {
        val p = raw.clamped()
        when (tool) {
            MarkupTool.SELECT -> selectedId = elements.lastOrNull { it.hitTest(p) }?.id

            MarkupTool.TEXT -> {
                val hit = elements.lastOrNull { it is MarkupElement.Label && it.hitTest(p) }
                    as? MarkupElement.Label
                pendingText = if (hit != null) {
                    PendingText(at = hit.at, existingId = hit.id, initialText = hit.text)
                } else {
                    PendingText(at = p)
                }
            }

            else -> Unit
        }
    }

    fun commitPendingText(text: String) {
        val pending = pendingText ?: return
        pendingText = null
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (pending.existingId != null) {
            if (trimmed == pending.initialText) return
            pushHistory(elements)
            elements = elements.map {
                if (it.id == pending.existingId && it is MarkupElement.Label) {
                    it.copy(text = trimmed)
                } else {
                    it
                }
            }
        } else {
            pushHistory(elements)
            elements = elements + MarkupElement.Label(nextId++, colorArgb, pending.at, trimmed)
        }
    }

    fun cancelPendingText() {
        pendingText = null
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        if (elements.none { it.id == id }) return
        pushHistory(elements)
        elements = elements.filterNot { it.id == id }
        selectedId = null
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = redoStack + listOf(elements)
        restore(previous)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = undoStack + listOf(elements)
        restore(next)
    }

    // Selection may reference an element the restored snapshot doesn't have, so it clears.
    private fun restore(snapshot: List<MarkupElement>) {
        elements = snapshot
        selectedId = null
        draft = null
        gesture = Gesture.None
    }

    private fun pushHistory(before: List<MarkupElement>) {
        undoStack = (undoStack + listOf(before)).takeLast(MAX_HISTORY)
        redoStack = emptyList()
    }

    private fun beginSelectGesture(p: MarkupPoint) {
        // A grip on the already-selected element wins over selecting whatever is under it.
        val selected = elements.firstOrNull { it.id == selectedId }
        val handle = selected?.handleAt(p)
        if (selected != null && handle != null) {
            gesture = Gesture.Resize(elements, selected.id, handle)
            return
        }
        val hit = elements.lastOrNull { it.hitTest(p) }
        selectedId = hit?.id
        gesture = if (hit != null) Gesture.Move(elements, p) else Gesture.None
    }

    private fun newElementAt(p: MarkupPoint): MarkupElement = when (tool) {
        MarkupTool.PEN -> MarkupElement.Pen(nextId++, colorArgb, listOf(p))
        MarkupTool.LINE ->
            MarkupElement.Line(nextId++, colorArgb, p, p, arrowAtStart = false, arrowAtEnd = false)
        MarkupTool.ARROW ->
            MarkupElement.Line(nextId++, colorArgb, p, p, arrowAtStart = false, arrowAtEnd = true)
        MarkupTool.DOUBLE_ARROW ->
            MarkupElement.Line(nextId++, colorArgb, p, p, arrowAtStart = true, arrowAtEnd = true)
        MarkupTool.RECT -> MarkupElement.Shape(nextId++, colorArgb, p, p, ShapeKind.RECT)
        MarkupTool.OVAL -> MarkupElement.Shape(nextId++, colorArgb, p, p, ShapeKind.OVAL)
        MarkupTool.CLOUD -> MarkupElement.Shape(nextId++, colorArgb, p, p, ShapeKind.CLOUD)
        MarkupTool.SELECT, MarkupTool.TEXT -> error("not a drawing tool: $tool")
    }

    private companion object {
        const val MAX_HISTORY = 50
    }
}
