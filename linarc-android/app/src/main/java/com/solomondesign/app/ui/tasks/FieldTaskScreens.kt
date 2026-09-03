package com.solomondesign.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow

/** Pattern B — field task list with a filter row. Bottom navigation stays visible. */
@Composable
fun FieldTaskListScreen(
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // "Mine" means the borrowed view member while the Crew or Subcontractor persona is
    // demoed (Demo: view as is a lens, not a login), and the signed-in user otherwise.
    // Both of those personas open on their own work.
    val viewMember = DemoProjectRepository.crewViewMember
        ?: DemoProjectRepository.subcontractorMember
    val myId = viewMember?.id ?: CurrentUser.ID
    var filter by rememberSaveable {
        mutableStateOf(if (viewMember != null) TaskFilter.MINE else TaskFilter.ALL)
    }
    val visible = FieldTaskRepository.tasks.applyFilter(filter, myId)

    BrowseScaffold(
        title = "Field task",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().testTag("fieldTaskListScreen")) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label()) },
                        modifier = Modifier.testTag("taskFilter_${option.name}"),
                    )
                }
            }

            if (visible.isEmpty()) {
                FieldEmptyState(message = "No tasks match this filter.")
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { task ->
                    val assignee = task.assigneeId?.let(DemoProjectRepository::crewMember)
                    FieldWorkRow(
                        title = task.title,
                        subtitle = "${task.location} · ${assignee?.name ?: "Unassigned"} · ${task.dueLabel}",
                        statusColor = task.status.statusColor(),
                        avatarName = assignee?.name,
                        avatarColor = DemoProjectRepository.avatarColorFor(task.assigneeId),
                        avatarPhotoRes = assignee?.photoRes,
                        enabled = true,
                        onClick = { onOpenTask(task.id) },
                        modifier = Modifier.testTag("taskRow_${task.id}"),
                    )
                }
            }
        }
    }
}

/** Pattern B — one task: status control, assignee, checklist, notes. */
@Composable
fun FieldTaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCrewMember: ((String) -> Unit)? = null,
) {
    val task = FieldTaskRepository.find(taskId)

    if (task == null) {
        BrowseScaffold(title = "Field task", onBack = onBack, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This task is no longer available.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val assignee = task.assigneeId?.let(DemoProjectRepository::crewMember)

    BrowseScaffold(
        title = task.title,
        subtitle = task.location,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("fieldTaskDetailScreen"),
        ) {
            FieldSectionLabel("STATUS")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                TaskStatus.entries.forEachIndexed { index, status ->
                    SegmentedButton(
                        selected = task.status == status,
                        onClick = { FieldTaskRepository.setStatus(task.id, status) },
                        shape = SegmentedButtonDefaults.itemShape(index, TaskStatus.entries.size),
                        label = { Text(status.label()) },
                        modifier = Modifier.testTag("taskStatus_${status.name}"),
                    )
                }
            }

            FieldSectionLabel("ASSIGNED TO")
            if (assignee == null) {
                FieldWorkRow(
                    title = "Unassigned",
                    subtitle = "No one is on this task yet",
                    statusColor = MaterialTheme.colorScheme.outline,
                    enabled = false,
                )
            } else {
                FieldWorkRow(
                    title = assignee.name,
                    subtitle = "${assignee.trade} · due ${task.dueLabel}",
                    statusColor = task.status.statusColor(),
                    avatarName = assignee.name,
                    avatarColor = DemoProjectRepository.avatarColorFor(assignee.id),
                    avatarPhotoRes = assignee.photoRes,
                    enabled = onOpenCrewMember != null,
                    onClick = { onOpenCrewMember?.invoke(assignee.id) },
                )
            }

            if (task.checklist.isNotEmpty()) {
                FieldSectionLabel("CHECKLIST")
                task.checklist.forEach { item ->
                    FieldWorkRow(
                        title = item.label,
                        subtitle = null,
                        statusColor = if (item.done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        enabled = true,
                        onClick = { FieldTaskRepository.toggleCheckItem(task.id, item.id) },
                        trailing = {
                            Checkbox(
                                checked = item.done,
                                onCheckedChange = {
                                    FieldTaskRepository.toggleCheckItem(task.id, item.id)
                                },
                                modifier = Modifier.testTag("taskCheck_${item.id}"),
                            )
                        },
                    )
                }
            }

            FieldSectionLabel("NOTES")
            if (task.note.isBlank()) {
                FieldEmptyState(message = "No notes on this task.")
            } else {
                Text(
                    text = task.note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}
