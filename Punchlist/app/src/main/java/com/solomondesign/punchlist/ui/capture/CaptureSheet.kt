package com.solomondesign.punchlist.ui.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

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
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Capture",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Voice daily log") },
                supportingContent = { Text("Dictate labor, delays, and issues") },
                leadingContent = { Icon(Icons.Filled.Mic, contentDescription = null) },
                modifier = Modifier
                    .clickable(onClick = onVoice)
                    .testTag("captureVoice"),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Photo") },
                supportingContent = { Text("Tag a progress photo") },
                leadingContent = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                modifier = Modifier
                    .clickable(onClick = onPhoto)
                    .testTag("capturePhoto"),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Issue") },
                supportingContent = { Text("Quick field issue") },
                leadingContent = { Icon(Icons.Filled.Report, contentDescription = null) },
                modifier = Modifier
                    .clickable(onClick = onIssue)
                    .testTag("captureIssue"),
            )
        }
    }
}
