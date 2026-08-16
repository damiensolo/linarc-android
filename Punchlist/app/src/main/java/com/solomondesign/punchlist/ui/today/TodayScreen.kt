package com.solomondesign.punchlist.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.punchlist.ui.demo.CrewMember
import com.solomondesign.punchlist.ui.demo.DemoProjectRepository
import com.solomondesign.punchlist.ui.demo.StreamItem
import com.solomondesign.punchlist.ui.demo.StreamKind
import com.solomondesign.punchlist.ui.designsystem.PunchlistButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenVoiceLog: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val dayStarted = DemoProjectRepository.dayStarted
    val streamItems = DemoProjectRepository.streamItems.toList()
    val crew = DemoProjectRepository.crew
    var showStartMyDay by remember { mutableStateOf(false) }

    val blockers = streamItems.filter { it.kind == StreamKind.BLOCKER || it.kind == StreamKind.ISSUE }
    val captures = streamItems.filter {
        it.kind == StreamKind.DAILY_LOG || it.kind == StreamKind.PHOTO || it.kind == StreamKind.TASK
    }

    if (showStartMyDay) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStartMyDay = false },
            sheetState = sheetState,
        ) {
            StartMyDaySheetContent(
                crew = crew,
                onConfirm = {
                    DemoProjectRepository.confirmStartMyDay()
                    showStartMyDay = false
                },
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("todayScreen"),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = "Today", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${persona.displayName} · ${DemoProjectRepository.AREA}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!dayStarted) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .testTag("startMyDayCard"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Start My Day", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Crew, Area B, and weather are ready. Confirm to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        PunchlistButton(
                            text = "Start My Day",
                            onClick = { showStartMyDay = true },
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Day started · ${DemoProjectRepository.AREA}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        item { SectionHeader("Crew") }
        items(crew, key = { it.name }) { member ->
            CrewRow(member)
        }

        item { SectionHeader("Blockers") }
        if (blockers.isEmpty()) {
            item {
                Text(
                    text = "No blockers yet. Dictate a voice log if something is in the way.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            items(blockers, key = { it.id }) { item ->
                StreamRow(item = item, onClick = { item.relatedRecordId?.let(onOpenVoiceLog) })
            }
        }

        item { SectionHeader("Recent captures") }
        items(captures, key = { it.id }) { item ->
            StreamRow(
                item = item,
                onClick = {
                    if (item.kind == StreamKind.DAILY_LOG) {
                        item.relatedRecordId?.let(onOpenVoiceLog)
                    }
                },
            )
        }
    }
}

@Composable
private fun StartMyDaySheetContent(
    crew: List<CrewMember>,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Start My Day", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Text("Crew", style = MaterialTheme.typography.titleSmall)
        crew.forEach { member ->
            Text(
                text = "${member.name} · ${member.trade}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Weather", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Clear · 72°F · no delay",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        PunchlistButton(text = "Confirm", onClick = onConfirm)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CrewRow(member: CrewMember) {
    ListItem(
        headlineContent = { Text(member.name) },
        supportingContent = { Text("${member.trade} · ${member.status}") },
        leadingContent = {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
    HorizontalDivider()
}

@Composable
private fun StreamRow(item: StreamItem, onClick: () -> Unit) {
    val clickable = item.relatedRecordId != null && item.kind == StreamKind.DAILY_LOG
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = { Text(item.subtitle + " · " + formatTimestamp(item.timestampMillis)) },
        leadingContent = {
            Icon(iconFor(item.kind), contentDescription = null, tint = tintFor(item.kind))
        },
        trailingContent = {
            if (clickable) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(enabled = clickable, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
    HorizontalDivider()
}

@Composable
private fun tintFor(kind: StreamKind) = when (kind) {
    StreamKind.BLOCKER, StreamKind.ISSUE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

private fun iconFor(kind: StreamKind): ImageVector = when (kind) {
    StreamKind.BLOCKER -> Icons.Filled.WarningAmber
    StreamKind.ISSUE -> Icons.Filled.Report
    StreamKind.PHOTO -> Icons.Filled.PhotoCamera
    StreamKind.DAILY_LOG -> Icons.Filled.TaskAlt
    StreamKind.TASK -> Icons.Filled.TaskAlt
    StreamKind.CREW -> Icons.Filled.Groups
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
