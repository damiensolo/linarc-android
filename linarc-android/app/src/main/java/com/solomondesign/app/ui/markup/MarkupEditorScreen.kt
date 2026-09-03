package com.solomondesign.app.ui.markup

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Same hard-coded convention exception as the camera chrome: markup happens over a photo, and
// a themed light surface behind one is unreadable. Scoped to this file.
private val EditorBackground = Color.Black
private val OnEditor = Color.White

private data class ToolSpec(val tool: MarkupTool, val icon: ImageVector, val label: String)

// The two most-used tools lead the rail: pen, then text right beside it.
private val toolSpecs = listOf(
    ToolSpec(MarkupTool.SELECT, Icons.Filled.PanTool, "Select and move"),
    ToolSpec(MarkupTool.PEN, Icons.Filled.Gesture, "Pen"),
    ToolSpec(MarkupTool.TEXT, Icons.Filled.TextFields, "Text"),
    ToolSpec(MarkupTool.LINE, Icons.Filled.HorizontalRule, "Line"),
    ToolSpec(MarkupTool.ARROW, Icons.AutoMirrored.Filled.TrendingFlat, "Arrow"),
    ToolSpec(MarkupTool.DOUBLE_ARROW, Icons.Filled.SyncAlt, "Double arrow"),
    ToolSpec(MarkupTool.RECT, Icons.Filled.CropSquare, "Box"),
    ToolSpec(MarkupTool.OVAL, Icons.Filled.RadioButtonUnchecked, "Oval"),
    ToolSpec(MarkupTool.CLOUD, Icons.Filled.Cloud, "Cloud"),
)

/**
 * Full-screen annotation editor over one photo. Owns no persistence: [onDone] hands back a
 * bitmap with the annotations baked in (or the untouched original when nothing was drawn), and
 * the caller decides what a save means — the camera writes it before review, the Images entry
 * offers copy-or-replace. Draw tools drag to create; Select taps, drags to move, and drags a
 * grip to resize; Text taps to place or edit.
 */
@Composable
fun MarkupEditorScreen(
    photo: Bitmap,
    onCancel: () -> Unit,
    onDone: (result: Bitmap, hasMarkup: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    doneLabel: String = "Done",
    doneRequiresMarkup: Boolean = false,
) {
    val state = remember(photo) { MarkupEditorState() }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val requestCancel = {
        if (state.hasMarkup) showDiscardDialog = true else onCancel()
    }
    BackHandler(onBack = requestCancel)

    fun finish() {
        if (exporting) return
        if (!state.hasMarkup) {
            onDone(photo, false)
            return
        }
        exporting = true
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                renderMarkup(photo, state.elements, textMeasurer, density)
            }
            onDone(result, true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .testTag("markupEditorScreen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorIconButton(Icons.Filled.Close, "Close markup", "markupClose", onClick = requestCancel)
            Text(
                text = "Markup",
                style = MaterialTheme.typography.titleMedium,
                color = OnEditor,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.selectedId != null) {
                EditorIconButton(
                    Icons.Filled.Delete,
                    "Delete selection",
                    "markupDelete",
                    onClick = state::deleteSelected,
                )
            }
            EditorIconButton(
                Icons.AutoMirrored.Filled.Undo,
                "Undo",
                "markupUndo",
                onClick = state::undo,
                enabled = state.canUndo,
            )
            EditorIconButton(
                Icons.AutoMirrored.Filled.Redo,
                "Redo",
                "markupRedo",
                onClick = state::redo,
                enabled = state.canRedo,
            )
            TextButton(
                onClick = ::finish,
                enabled = !exporting && (!doneRequiresMarkup || state.hasMarkup),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = OnEditor,
                    disabledContentColor = OnEditor.copy(alpha = 0.4f),
                ),
                modifier = Modifier.testTag("markupDone"),
            ) {
                if (exporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = OnEditor,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(doneLabel)
                }
            }
        }

        var boxSize by remember { mutableStateOf(IntSize.Zero) }
        val fit = if (boxSize.width > 0 && boxSize.height > 0) {
            fitRect(photo.width, photo.height, boxSize.width.toFloat(), boxSize.height.toFloat())
        } else {
            null
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { boxSize = it },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Photo being marked up",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("markupCanvas")
                    .pointerInput(state.tool, fit) {
                        val f = fit ?: return@pointerInput
                        detectDragGestures(
                            onDragStart = { state.beginGesture(f.normalizedPoint(it.x, it.y)) },
                            onDrag = { change, _ ->
                                change.consume()
                                state.moveGesture(
                                    f.normalizedPoint(change.position.x, change.position.y),
                                )
                            },
                            onDragEnd = { state.endGesture() },
                            onDragCancel = { state.endGesture() },
                        )
                    }
                    .pointerInput(state.tool, fit) {
                        val f = fit ?: return@pointerInput
                        detectTapGestures { state.tapAt(f.normalizedPoint(it.x, it.y)) }
                    },
            ) {
                val f = fit ?: return@Canvas
                translate(f.left, f.top) {
                    clipRect(0f, 0f, f.width, f.height) {
                        drawMarkupElements(
                            area = Size(f.width, f.height),
                            elements = state.elements,
                            draft = state.draft,
                            selectedId = state.selectedId,
                            textMeasurer = textMeasurer,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarkupPalette.OPTIONS.forEach { option ->
                    val selected = state.colorArgb == option.argb
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(option.argb))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) OnEditor else OnEditor.copy(alpha = 0.4f),
                                shape = CircleShape,
                            )
                            .clickable(role = Role.Button) { state.setColor(option.argb) }
                            .semantics { contentDescription = "${option.label} markup color" }
                            .testTag("markupColor_${option.label}"),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                toolSpecs.forEach { spec ->
                    val selected = state.tool == spec.tool
                    IconButton(
                        onClick = { state.selectTool(spec.tool) },
                        modifier = Modifier
                            .background(
                                if (selected) OnEditor else Color.Transparent,
                                CircleShape,
                            )
                            .testTag("markupTool_${spec.tool.name}"),
                    ) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = spec.label,
                            tint = if (selected) Color.Black else OnEditor,
                        )
                    }
                }
            }
        }
    }

    val pending = state.pendingText
    if (pending != null) {
        var text by remember(pending) { mutableStateOf(pending.initialText) }
        val editing = pending.existingId != null
        AlertDialog(
            onDismissRequest = state::cancelPendingText,
            title = { Text(if (editing) "Edit text" else "Add text") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markupTextField"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { state.commitPendingText(text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.testTag("markupTextOk"),
                ) {
                    Text(if (editing) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelPendingText) { Text("Cancel") }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard markup?") },
            text = { Text("Your annotations haven't been saved and will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onCancel()
                    },
                    modifier = Modifier.testTag("markupDiscardConfirm"),
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun EditorIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(testTag)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) OnEditor else OnEditor.copy(alpha = 0.4f),
        )
    }
}
