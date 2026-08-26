package com.solomondesign.app.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

class AgingRfiTest {

    private val day = 86_400_000L

    private fun record(
        id: String,
        category: RecordCategory = RecordCategory.ISSUE,
        type: String = RFI_ISSUE_TYPE,
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
    )

    @Test
    fun agingRfis_keepsOnlyRfiTypeIssues_oldestFirst() {
        val rfis = listOf(
            record("rfi-new", createdAtMillis = 3 * day),
            record("observation", type = "Observation", createdAtMillis = 1L),
            record("punch", category = RecordCategory.PUNCH, createdAtMillis = 1L),
            record("rfi-old", createdAtMillis = 1 * day),
        ).agingRfis()

        assertEquals(listOf("rfi-old", "rfi-new"), rfis.map { it.id })
    }

    @Test
    fun rfiAgeLabel_countsWholeDaysOpen() {
        assertEquals("Opened today", rfiAgeLabel(createdAtMillis = 0L, nowMillis = day - 1))
        assertEquals("1 day open", rfiAgeLabel(createdAtMillis = 0L, nowMillis = day))
        assertEquals("6 days open", rfiAgeLabel(createdAtMillis = 0L, nowMillis = 6 * day))
        // Clock skew never shows a negative age.
        assertEquals("Opened today", rfiAgeLabel(createdAtMillis = day, nowMillis = 0L))
    }

    @Test
    fun rfiIssueType_isARealIssueTypeOption() {
        // The PM view depends on the Issue create form offering this exact type.
        assertEquals(true, RFI_ISSUE_TYPE in RecordCategory.ISSUE.typeOptions)
    }
}
