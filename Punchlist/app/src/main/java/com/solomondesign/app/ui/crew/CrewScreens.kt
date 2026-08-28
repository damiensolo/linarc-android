package com.solomondesign.app.ui.crew

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.statusLabel
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.designsystem.PersonAvatar
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.tasks.label
import com.solomondesign.app.ui.tasks.statusColor
import com.solomondesign.app.ui.timecards.TimeCardRepository
import com.solomondesign.app.ui.timecards.formatHours

/** Pattern B — crew list, grouped by trade. Bottom navigation stays visible. */
@Composable
fun CrewListScreen(
    onOpenCrewMember: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val crew = DemoProjectRepository.crew
    val byTrade = crew.groupBy { it.trade }

    BrowseScaffold(
        title = "Crew",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (crew.isEmpty()) {
            FieldEmptyState(
                message = "No crew assigned to this area yet.",
                modifier = Modifier.padding(padding),
            )
            return@BrowseScaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("crewListScreen"),
        ) {
            byTrade.forEach { (trade, members) ->
                item(key = "trade-$trade") {
                    FieldSectionLabel("${trade.uppercase()} · ${members.size}")
                }
                items(members, key = { it.id }) { member ->
                    CrewMemberRow(
                        member = member,
                        enabled = true,
                        onClick = { onOpenCrewMember(member.id) },
                        modifier = Modifier.testTag("crewRow_${member.id}"),
                    )
                }
            }
        }
    }
}

/** Pattern B — one crew member: profile, assigned work, and hours to date. */
@Composable
fun CrewDetailScreen(
    crewMemberId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTask: ((String) -> Unit)? = null,
    onOpenTimeCard: ((String) -> Unit)? = null,
) {
    val member = DemoProjectRepository.crewMember(crewMemberId)

    if (member == null) {
        BrowseScaffold(title = "Crew", onBack = onBack, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This crew member is no longer on the project.",
                modifier = Modifier.padding(padding),
                action = {
                    AppButton(text = "Back", type = AppButtonType.Secondary, onClick = onBack)
                },
            )
        }
        return
    }

    val assigned = FieldTaskRepository.tasks.filter { it.assigneeId == crewMemberId }
    val hours = TimeCardRepository.totalHours(crewMemberId)

    BrowseScaffold(
        title = member.name,
        subtitle = member.trade,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("crewDetailScreen"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(
                    name = member.name,
                    color = DemoProjectRepository.avatarColorFor(member.id),
                    photoRes = member.photoRes,
                    size = 72.dp,
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(member.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = member.presence.statusLabel(DemoProjectRepository.AREA),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FieldSectionLabel("HOURS THIS WEEK")
            FieldWorkRow(
                title = formatHours(hours),
                subtitle = if (onOpenTimeCard == null) "Time card" else "View time card",
                statusColor = MaterialTheme.colorScheme.primary,
                enabled = onOpenTimeCard != null,
                onClick = { onOpenTimeCard?.invoke(member.id) },
                modifier = Modifier.testTag("crewDetailHours"),
            )

            FieldSectionLabel("ASSIGNED WORK · ${assigned.size}")
            if (assigned.isEmpty()) {
                FieldEmptyState(message = "No tasks assigned right now.")
            } else {
                assigned.forEach { task ->
                    FieldWorkRow(
                        title = task.title,
                        subtitle = "${task.location} · ${task.status.label()}",
                        statusColor = task.status.statusColor(),
                        enabled = onOpenTask != null,
                        onClick = { onOpenTask?.invoke(task.id) },
                    )
                }
            }
        }
    }
}
