package com.solomondesign.app.ui.timecards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.crew.CrewMemberRow
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow

/** Pattern B — crew list showing hours to date. Contextual FAB adds a time entry. */
@Composable
fun TimeCardCrewListScreen(
    onOpenCrewMember: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrowseScaffold(
        title = "Time card",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · week of Aug 18",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("timeCardListScreen"),
        ) {
            items(DemoProjectRepository.crew, key = { it.id }) { member ->
                val total = TimeCardRepository.totalHours(member.id)
                CrewMemberRow(
                    member = member,
                    enabled = true,
                    onClick = { onOpenCrewMember(member.id) },
                    modifier = Modifier.testTag("timeCardRow_${member.id}"),
                    trailing = {
                        Text(
                            text = if (total > 0.0) formatHours(total) else "No hours",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (total > 0.0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
            }
        }
    }
}

/** Pattern B — one crew member's entries, grouped by day. */
@Composable
fun TimeCardDetailScreen(
    crewMemberId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val member = DemoProjectRepository.crewMember(crewMemberId)

    if (member == null) {
        BrowseScaffold(title = "Time card", onBack = onBack, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This crew member is no longer on the project.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val entries = TimeCardRepository.entriesFor(crewMemberId)
    val byDate = entries.groupBy { it.dateLabel }

    BrowseScaffold(
        title = member.name,
        subtitle = "Total ${formatHours(TimeCardRepository.totalHours(crewMemberId))}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("timeCardDetailScreen"),
        ) {
            if (entries.isEmpty()) {
                FieldEmptyState(message = "No hours logged for this week.")
                return@Column
            }
            byDate.forEach { (date, dayEntries) ->
                val dayTotal = dayEntries.sumOf { it.hours + it.overtimeHours }
                FieldSectionLabel("${date.uppercase()} · ${formatHours(dayTotal)}")
                dayEntries.forEach { entry ->
                    val overtime = if (entry.overtimeHours > 0.0) {
                        " · OT ${formatHours(entry.overtimeHours)}"
                    } else {
                        ""
                    }
                    val queued = if (entry.queued) " · Queued · waiting for signal" else ""
                    FieldWorkRow(
                        title = entry.costCode,
                        subtitle = "${formatHours(entry.hours)}$overtime$queued",
                        statusColor = if (entry.queued) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        enabled = false,
                        trailing = {
                            Text(
                                text = formatHours(entry.hours + entry.overtimeHours),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
            Text(
                text = "Queued on this device. Sync is not part of this prototype.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}
