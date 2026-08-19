package com.solomondesign.app.ui.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Floating footer toolbar for the full-screen image viewer. No existing primitive covers this —
 * the app has no bottom action bar distinct from the main navigation.
 */
@Composable
fun ImageViewerToolbar(
    onShare: () -> Unit,
    onMarkup: () -> Unit,
    onDelete: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("imageViewerToolbar"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarAction(Icons.Filled.Share, "Share", "viewerShare", onShare)
            ToolbarAction(Icons.Filled.Draw, "Markup", "viewerMarkup", onMarkup)
            ToolbarAction(
                icon = Icons.Filled.DeleteOutline,
                label = "Delete",
                testTag = "viewerDelete",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
            ToolbarAction(Icons.Filled.AddCircleOutline, "Create", "viewerCreate", onCreate)
        }
    }
}

@Composable
private fun ToolbarAction(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.sizeIn(minWidth = 64.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}
