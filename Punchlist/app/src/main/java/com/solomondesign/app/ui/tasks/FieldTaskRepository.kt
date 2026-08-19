package com.solomondesign.app.ui.tasks

import androidx.compose.runtime.mutableStateListOf

/**
 * In-memory demo store for field tasks. Follows the existing feature-scoped store pattern
 * (see `DailyLogRepository`): a Compose snapshot-state singleton, no ViewModel, so screens
 * recompose automatically when it mutates.
 */
object FieldTaskRepository {
    private val _tasks = mutableStateListOf<FieldTask>()
    val tasks: List<FieldTask> get() = _tasks

    init {
        seed()
    }

    fun find(id: String): FieldTask? = _tasks.firstOrNull { it.id == id }

    fun setStatus(id: String, status: TaskStatus) {
        val index = _tasks.indexOfFirst { it.id == id }
        if (index >= 0) _tasks[index] = _tasks[index].copy(status = status)
    }

    fun toggleCheckItem(taskId: String, itemId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index < 0) return
        val task = _tasks[index]
        _tasks[index] = task.copy(
            checklist = task.checklist.map { item ->
                if (item.id == itemId) item.copy(done = !item.done) else item
            },
        )
    }

    fun clear() {
        _tasks.clear()
        seed()
    }

    private fun seed() {
        _tasks.addAll(
            listOf(
                FieldTask(
                    id = "task-frame-corridor-c",
                    title = "Frame corridor C partitions",
                    trade = "Framing (Carpentry)",
                    location = "Area B · Level 2 · gridline C",
                    status = TaskStatus.IN_PROGRESS,
                    assigneeId = "hector-ortiz",
                    dueLabel = "Today",
                    note = "Track is snapped through the nurse station. Hold the head of wall " +
                        "for the deflection clip detail.",
                    checklist = listOf(
                        TaskCheckItem("chk-layout", "Layout snapped", done = true),
                        TaskCheckItem("chk-track", "Bottom track anchored", done = true),
                        TaskCheckItem("chk-studs", "Studs at 16\" o.c.", done = false),
                        TaskCheckItem("chk-inspect", "Inspection requested", done = false),
                    ),
                ),
                FieldTask(
                    id = "task-door-bucks",
                    title = "Set door bucks 210–218",
                    trade = "Framing (Carpentry)",
                    location = "Area B · Level 2",
                    status = TaskStatus.NOT_STARTED,
                    assigneeId = "dave-miller",
                    dueLabel = "Today",
                    note = "Frames staged at the level 2 lay-down area.",
                ),
                FieldTask(
                    id = "task-branch-circuits",
                    title = "Rough-in branch circuits, exam 5–8",
                    trade = "Electrical",
                    location = "Area B · Level 2",
                    status = TaskStatus.IN_PROGRESS,
                    assigneeId = "maria-chen",
                    dueLabel = "Today",
                    note = "Home runs to panel 2LB. Keep clear of the med gas rack.",
                ),
                FieldTask(
                    id = "task-med-gas-col4",
                    title = "Med gas rough-in at column 4",
                    trade = "Plumbing",
                    location = "Area B · Column 4",
                    status = TaskStatus.BLOCKED,
                    assigneeId = "sam-reyes",
                    dueLabel = "Today",
                    note = "Med gas line conflicts with the 4\" storm at column 4. " +
                        "RFI-118 outstanding.",
                ),
                FieldTask(
                    id = "task-headwall-backing",
                    title = "Backing for wall-mounted headwalls",
                    trade = "Framing (Carpentry)",
                    location = "Area B · rooms 5–8",
                    status = TaskStatus.NOT_STARTED,
                    assigneeId = null,
                    dueLabel = "Tomorrow",
                    note = "Needs an owner before Thursday's headwall delivery.",
                ),
                FieldTask(
                    id = "task-firecaulk-l2n",
                    title = "Fire-caulk penetrations, level 2 north",
                    trade = "Framing (Carpentry)",
                    location = "Area B · Level 2 north",
                    status = TaskStatus.DONE,
                    assigneeId = "dave-miller",
                    dueLabel = "Yesterday",
                ),
            ),
        )
    }
}
