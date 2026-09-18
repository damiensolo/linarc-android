package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * One message in a discussion — the reader's own messages sit right in `primaryContainer`,
 * everyone else's sit left with an avatar in `surfaceContainerHigh`. Shared by the
 * Collaboration conversation screen and every object's inline discussion (records, tasks),
 * so a thread reads the same wherever it appears. Pure presentation: callers resolve the
 * author's avatar color and photo.
 */
@Composable
fun DiscussionBubble(
    authorName: String,
    body: String,
    isMine: Boolean,
    avatarColor: Color,
    modifier: Modifier = Modifier,
    avatarPhotoRes: Int? = null,
    queued: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            PersonAvatar(
                name = authorName,
                color = avatarColor,
                photoRes = avatarPhotoRes,
                size = 32.dp,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                if (!isMine) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (queued) {
                    Text(
                        text = "Queued · waiting for signal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
