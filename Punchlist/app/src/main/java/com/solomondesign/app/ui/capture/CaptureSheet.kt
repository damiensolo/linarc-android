package com.solomondesign.app.ui.capture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.FieldWorkRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    onVoice: () -> Unit,
    onPhoto: () -> Unit,
    onIssue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Capture",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            FieldWorkRow(
                title = "Voice daily log",
                subtitle = "Dictate labor, delays, and issues",
                statusColor = MaterialTheme.colorScheme.tertiary,
                enabled = true,
                onClick = onVoice,
                modifier = Modifier.testTag("captureVoice"),
            )
            FieldWorkRow(
                title = "Photo",
                subtitle = "Tag a progress photo",
                statusColor = MaterialTheme.colorScheme.primary,
                enabled = true,
                onClick = onPhoto,
                modifier = Modifier.testTag("capturePhoto"),
            )
            FieldWorkRow(
                title = "Issue",
                subtitle = "Quick field issue",
                statusColor = MaterialTheme.colorScheme.error,
                enabled = true,
                onClick = onIssue,
                modifier = Modifier.testTag("captureIssue"),
            )
        }
    }
}
