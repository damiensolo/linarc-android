package com.solomondesign.punchlist.ui.voicelog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solomondesign.punchlist.ui.designsystem.PunchlistButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Daily Log" home: past voice-to-log submissions (real recordings, real transcripts, real
 * extracted entities — see [DailyLogRepository]) plus the entry point into a new recording.
 */
@Composable
fun DailyLogHomeScreen(
    onRecordNew: () -> Unit,
    onOpenRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val records = DailyLogRepository.records

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Daily Log", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Dictate a site update — labor, materials, delays, and issues are " +
                        "pulled from what you actually say.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                PunchlistButton(text = "Record New Voice Log", onClick = onRecordNew)
            }
        }
        if (records.isEmpty()) {
            item {
                Text(
                    text = "No voice logs recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            items(records, key = { it.id }) { record ->
                ListItem(
                    headlineContent = { Text(record.projectName) },
                    supportingContent = { Text(formatTimestamp(record.timestampMillis) + entitySummary(record)) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onOpenRecord(record.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

private fun entitySummary(record: DailyLogRecord): String {
    val total = record.laborCards.size + record.materialCards.size + record.delayCards.size + record.issueCards.size
    return if (total == 0) " · no entries detected" else " · $total entries logged"
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(millis))
