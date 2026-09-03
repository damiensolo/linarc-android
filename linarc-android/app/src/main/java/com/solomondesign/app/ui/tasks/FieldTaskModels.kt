package com.solomondesign.app.ui.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, BLOCKED, DONE }

/**
 * Semantic-token colour for a status dot. Deliberately `@Composable` — unlike the older
 * `CrewPresence.badgeColor()`, which reads raw palette hexes — so this maps onto
 * `MaterialTheme.colorScheme` roles per the design-system rules.
 */
@Composable
@ReadOnlyComposable
fun TaskStatus.statusColor(): Color = when (this) {
    TaskStatus.NOT_STARTED -> MaterialTheme.colorScheme.outline
    TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
    TaskStatus.BLOCKED -> MaterialTheme.colorScheme.error
    TaskStatus.DONE -> MaterialTheme.colorScheme.primary
}

fun TaskStatus.label(): String = when (this) {
    TaskStatus.NOT_STARTED -> "Not started"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.BLOCKED -> "Blocked"
    TaskStatus.DONE -> "Done"
}

data class TaskCheckItem(
    val id: String,
    val label: String,
    val done: Boolean,
)

data class FieldTask(
    val id: String,
    val title: String,
    val trade: String,
    val location: String,
    val status: TaskStatus,
    /** [com.solomondesign.app.ui.demo.CrewMember.id], or null when unassigned. */
    val assigneeId: String?,
    val dueLabel: String,
    val note: String = "",
    val checklist: List<TaskCheckItem> = emptyList(),
)

enum class TaskFilter { ALL, MINE, BLOCKED }

fun TaskFilter.label(): String = when (this) {
    TaskFilter.ALL -> "All"
    TaskFilter.MINE -> "Mine"
    TaskFilter.BLOCKED -> "Blocked"
}

/**
 * The Subcontractor's "assigned work" scope: every task for their trade, assigned or not —
 * a sub owns the trade's scope, not just the names already on tasks. Pure so it is
 * JVM-unit-testable.
 */
fun List<FieldTask>.forTrade(trade: String): List<FieldTask> = filter { it.trade == trade }

/** Pulled out of the composable so list filtering is JVM-unit-testable. */
fun List<FieldTask>.applyFilter(filter: TaskFilter, currentUserId: String): List<FieldTask> =
    when (filter) {
        TaskFilter.ALL -> this
        TaskFilter.MINE -> filter { it.assigneeId == currentUserId }
        TaskFilter.BLOCKED -> filter { it.status == TaskStatus.BLOCKED }
    }
