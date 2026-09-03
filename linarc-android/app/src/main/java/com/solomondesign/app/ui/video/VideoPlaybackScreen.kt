package com.solomondesign.app.ui.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen playback of one [CapturedVideo], reached from its Today row: the clip, its
 * title/metadata, and the described note. Pattern A / immersive like the daily-log playback —
 * the route draws no shell chrome, this scaffold is the chrome.
 */
@Composable
fun VideoPlaybackScreen(
    videoId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val video = VideoRepository.find(videoId)

    TaskFlowScaffold(
        title = "Video",
        onClose = onClose,
        modifier = modifier,
    ) { padding ->
        if (video == null) {
            FieldEmptyState(
                message = "This video isn't available anymore.",
                modifier = Modifier
                    .padding(padding)
                    .testTag("videoMissing"),
            )
            return@TaskFlowScaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("videoPlaybackScreen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VideoPlayerBox(
                videoPath = video.videoPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
            val capturedAtLabel = remember(video.capturedAtMillis) {
                formatCapturedAt(video.capturedAtMillis)
            }
            Text(video.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Video · ${formatVideoDuration(video.durationSeconds)} · " +
                    "$capturedAtLabel · ${video.authorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (video.note.isNotBlank()) {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = video.note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("videoNote"),
                )
            }
        }
    }
}

private fun formatCapturedAt(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
