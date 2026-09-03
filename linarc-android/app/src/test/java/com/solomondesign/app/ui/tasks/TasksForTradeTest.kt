package com.solomondesign.app.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

class TasksForTradeTest {

    private fun task(id: String, trade: String, assigneeId: String? = null) = FieldTask(
        id = id,
        title = id,
        trade = trade,
        location = "Area B",
        status = TaskStatus.NOT_STARTED,
        assigneeId = assigneeId,
        dueLabel = "Today",
    )

    @Test
    fun forTrade_keepsTheWholeTrade_assignedOrNot() {
        val tasks = listOf(
            task("plumbing-assigned", "Plumbing", assigneeId = "sam-reyes"),
            task("framing", "Framing (Carpentry)", assigneeId = "hector-ortiz"),
            // Unassigned but still the sub's scope — a sub owns the trade, not just names.
            task("plumbing-unassigned", "Plumbing"),
        ).forTrade("Plumbing")

        assertEquals(
            listOf("plumbing-assigned", "plumbing-unassigned"),
            tasks.map { it.id },
        )
    }

    @Test
    fun seededPlumbingScope_hasTheSubDemoTasks() {
        // The Subcontractor view's "My work": the blocked med-gas task (RFI-118) and the
        // fixture-carrier task that motivates Request Inspection.
        assertEquals(
            listOf("task-fixture-carriers", "task-med-gas-col4").sorted(),
            FieldTaskRepository.tasks.forTrade("Plumbing").map { it.id }.sorted(),
        )
    }
}
