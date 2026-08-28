package com.solomondesign.app.ui.capture.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.video.IssueDraftParser
import com.solomondesign.app.ui.video.VideoPlayerBox
import com.solomondesign.app.ui.video.formatVideoDuration

/**
 * Post-recording review: playback, a title drafted by [IssueDraftParser] from the spoken
 * description, the description itself (editable), and a "file an issue" switch the parser
 * preselects when the description mentions a defect. Mirrors [PhotoReviewScreen]'s contract —
 * the recorded-but-unsaved video counts as an unsaved edit, so Close/back warn before discard.
 */
@Composable
fun VideoReviewScreen(
    videoPath: String,
    durationSeconds: Int,
    transcript: String,
    onRetake: () -> Unit,
    onSave: (title: String, note: String, location: String?, fileIssue: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(transcript) { IssueDraftParser.parse(transcript) }
    var title by remember(transcript) { mutableStateOf(parsed.title) }
    var note by remember(transcript) { mutableStateOf(transcript) }
    var fileIssue by remember(transcript) { mutableStateOf(parsed.looksLikeIssue) }

    val save = { onSave(title.trim(), note.trim(), parsed.location, fileIssue) }

    TaskFlowScaffold(
        title = "New video",
        onClose = onRetake,
        modifier = modifier,
        hasUnsavedChanges = true,
        onConfirm = save,
        confirmLabel = "Save",
        confirmEnabled = title.isNotBlank(),
        discardTitle = "Discard video?",
        discardMessage = "This video hasn't been saved and will be lost.",
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("videoReviewScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VideoPlayerBox(
                videoPath = videoPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
            Text(
                text = "Video · ${formatVideoDuration(durationSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("videoTitleField"),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Description (from your dictation)") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("videoNoteField"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("File an issue from this", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "After saving, opens a new issue prefilled from the description.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = fileIssue,
                    onCheckedChange = { fileIssue = it },
                    modifier = Modifier.testTag("videoFileIssueSwitch"),
                )
            }
            AppButton(
                text = "Retake",
                type = AppButtonType.Secondary,
                onClick = onRetake,
                modifier = Modifier.testTag("videoRetake"),
            )
        }
    }
}
