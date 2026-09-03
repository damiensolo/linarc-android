package com.solomondesign.app.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Project engineer's "Coordination & quality" section on Today. */
class TechnicalQueueTest {

    private fun record(
        id: String,
        category: RecordCategory = RecordCategory.ISSUE,
        type: String = "Coordination",
        blocksWork: Boolean = false,
        severity: RecordSeverity = RecordSeverity.MEDIUM,
        createdAtMillis: Long = 0L,
    ) = FieldRecord(
        id = id,
        category = category,
        title = id,
        type = type,
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
    fun technicalQueue_keepsNonRfiIssuesAndPunch_dropsRfisAndIncidents() {
        val queue = listOf(
            record("coordination"),
            record("rfi", type = RFI_ISSUE_TYPE),
            record("punch", category = RecordCategory.PUNCH, type = "Deficiency"),
            record("incident", category = RecordCategory.INCIDENT, type = "Near miss"),
        ).technicalQueue()

        // RFIs live in the PE's own RFI desk section; incidents stay with the
        // Superintendent's safety oversight.
        assertEquals(setOf("coordination", "punch"), queue.map { it.id }.toSet())
    }

    @Test
    fun technicalQueue_isAttentionOrdered() {
        val queue = listOf(
            record("old-low", severity = RecordSeverity.LOW, createdAtMillis = 1L),
            record("blocker", blocksWork = true, severity = RecordSeverity.LOW, createdAtMillis = 2L),
            record("new-high", severity = RecordSeverity.HIGH, createdAtMillis = 5L),
        ).technicalQueue()

        assertEquals(listOf("blocker", "new-high", "old-low"), queue.map { it.id })
    }
}
