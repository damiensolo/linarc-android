package com.solomondesign.app.ui.voicelog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.designsystem.AppButton
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
            Column {
                FieldPageHeader(
                    title = "Daily Log",
                    subtitle = "Dictate a site update — labor, materials, delays, and issues are " +
                        "pulled from what you actually say.",
                )
                AppButton(
                    text = "Record New Voice Log",
                    onClick = onRecordNew,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        if (records.isEmpty()) {
            item {
                Text(
                    text = "No voice logs recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        } else {
            items(records, key = { it.id }) { record ->
                FieldWorkRow(
                    title = record.projectName,
                    subtitle = formatTimestamp(record.timestampMillis) + entitySummary(record),
                    statusColor = MaterialTheme.colorScheme.tertiary,
                    enabled = true,
                    onClick = { onOpenRecord(record.id) },
                )
            }
        }
    }
}

private fun entitySummary(record: DailyLogRecord): String {
    val total = record.laborCards.size + record.materialCards.size + record.delayCards.size + record.issueCards.size
    return if (total == 0) " · no entries detected" else " · $total entries logged"
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
