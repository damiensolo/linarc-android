package com.solomondesign.app.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

class AttentionOrderTest {

    private fun record(
        id: String,
        blocksWork: Boolean = false,
        severity: RecordSeverity = RecordSeverity.MEDIUM,
        createdAtMillis: Long = 0L,
    ) = FieldRecord(
        id = id,
        category = RecordCategory.ISSUE,
        title = id,
        type = "Observation",
        description = "",
        location = "Area B",
        eventDateMillis = createdAtMillis,
        assigneeIds = emptyList(),
        attachments = emptyList(),
        createdAtMillis = createdAtMillis,
        authorName = "Test",
        severity = severity,
        blocksWork = blocksWork,
    )

    @Test
    fun blockersLead_thenSeverity_thenNewest() {
        val ordered = listOf(
            record("old-low", severity = RecordSeverity.LOW, createdAtMillis = 1L),
            record("new-high", severity = RecordSeverity.HIGH, createdAtMillis = 5L),
            record("blocker", blocksWork = true, severity = RecordSeverity.LOW, createdAtMillis = 2L),
            record("new-medium", severity = RecordSeverity.MEDIUM, createdAtMillis = 9L),
        ).attentionOrder()

        assertEquals(
            listOf("blocker", "new-high", "new-medium", "old-low"),
            ordered.map { it.id },
        )
    }

    @Test
    fun equalPriorityFallsBackToNewestFirst() {
        val ordered = listOf(
            record("older", createdAtMillis = 1L),
            record("newer", createdAtMillis = 2L),
        ).attentionOrder()

        assertEquals(listOf("newer", "older"), ordered.map { it.id })
    }
}
