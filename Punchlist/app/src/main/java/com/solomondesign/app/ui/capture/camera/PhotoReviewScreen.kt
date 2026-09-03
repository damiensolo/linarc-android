package com.solomondesign.app.ui.capture.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.DesignTokens
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.markup.MarkupEditorScreen
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordChooserSheet

/** Same scripted suggestions the pre-camera photo flow used — a demo, not a vision model. */
private val suggestedTags = listOf("Framing", "Area B", "Progress")

/**
 * Post-capture review: the shot plus title / description / tags, then Save (or Save & create
 * issue). A captured-but-unsaved photo always counts as an unsaved edit, so Close and system
 * back warn before discarding — per the Pattern A contract in `TaskFlowScaffold`.
 *
 * Markup stays one tap away even when the viewfinder toggle was off: the pill on the photo (or
 * tapping the photo itself) opens the editor *in place*, and Done returns here with the
 * title/description/tags untouched — the field states live above the branch, so swapping the
 * rendered surface never resets them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoReviewScreen(
    photo: Bitmap,
    onRetake: () -> Unit,
    /** [createRecord] non-null continues into that record's create form with the photo attached. */
    onSave: (
        title: String,
        description: String,
        tags: List<String>,
        createRecord: RecordCategory?,
        continueToVoice: Boolean,
    ) -> Unit,
    onAnnotated: (Bitmap) -> Unit,
    /** Hide when the camera was opened to attach a photo (voice review or a record form). */
    allowContinueToVoice: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("Progress photo") }
    var description by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(suggestedTags.toSet()) }
    var showMarkup by remember { mutableStateOf(false) }
    var showCreateChooser by remember { mutableStateOf(false) }

    val save = { createRecord: RecordCategory?, continueToVoice: Boolean ->
        onSave(title.trim(), description.trim(), selectedTags.toList(), createRecord, continueToVoice)
    }

    if (showMarkup) {
        MarkupEditorScreen(
            photo = photo,
            onCancel = { showMarkup = false },
            onDone = { result, hasMarkup ->
                showMarkup = false
                if (hasMarkup) onAnnotated(result)
            },
            modifier = modifier,
        )
        return
    }

    TaskFlowScaffold(
        title = "New photo",
        onClose = onRetake,
        modifier = modifier,
        // The photo itself is the unsaved edit — discarding returns to the viewfinder.
        hasUnsavedChanges = true,
        onConfirm = { save(null, false) },
        confirmLabel = "Save",
        confirmEnabled = title.isNotBlank(),
        discardTitle = "Discard photo?",
        discardMessage = "This photo hasn't been saved and will be lost.",
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("photoReviewScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Captured photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(DesignTokens.CardCornerRadius))
                        .clickable(onClickLabel = "Mark up photo") { showMarkup = true },
                )
                // The pill is the discoverable route; tapping the photo is the fast one.
                Surface(
                    onClick = { showMarkup = true },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .testTag("photoMarkup"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("Markup", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photoTitleField"),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photoDescriptionField"),
            )
            Text("Suggested tags", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestedTags.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            selectedTags = if (tag in selectedTags) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        },
                        label = { Text(tag) },
                    )
                }
            }
            AppButton(
                text = "Save & create…",
                type = AppButtonType.Secondary,
                enabled = title.isNotBlank(),
                onClick = { showCreateChooser = true },
                modifier = Modifier.testTag("photoSaveCreate"),
            )
            if (allowContinueToVoice) {
                AppButton(
                    text = "Add voice note",
                    type = AppButtonType.Secondary,
                    enabled = title.isNotBlank(),
                    onClick = { save(null, true) },
                    modifier = Modifier.testTag("photoAddVoiceNote"),
                )
            }
            AppButton(
                text = "Retake",
                type = AppButtonType.Secondary,
                onClick = onRetake,
                modifier = Modifier.testTag("photoRetake"),
            )
        }
    }

    if (showCreateChooser) {
        RecordChooserSheet(
            title = "Save photo & create",
            onPick = { category ->
                showCreateChooser = false
                save(category, false)
            },
            onDismiss = { showCreateChooser = false },
        )
    }
}
