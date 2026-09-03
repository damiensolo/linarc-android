package com.solomondesign.app.ui.timecards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeEntryValidationTest {

    private fun draft(
        crew: String? = "hector-ortiz",
        code: String? = COST_CODES.first(),
        hours: String = "8",
        overtime: String = "",
    ) = TimeEntryDraft(
        crewMemberId = crew,
        costCode = code,
        hoursText = hours,
        overtimeText = overtime,
    )

    @Test
    fun aCompleteDraftIsValid() {
        assertTrue(draft().isValid)
        assertTrue(draft(hours = "8.25").isValid)
        // Boundary: exactly the daily maximum is allowed.
        assertTrue(draft(hours = "16").isValid)
    }

    @Test
    fun hoursMustBePresentAndNumeric() {
        assertTrue(TimeEntryError.HoursRequired in draft(hours = "").validate())
        assertTrue(TimeEntryError.HoursNotANumber in draft(hours = "abc").validate())
    }

    /** toDoubleOrNull is locale-independent, so a comma decimal is deliberately rejected. */
    @Test
    fun commaDecimalsAreRejected() {
        assertTrue(TimeEntryError.HoursNotANumber in draft(hours = "8,25").validate())
    }

    @Test
    fun hoursMustBeInRange() {
        assertTrue(TimeEntryError.HoursOutOfRange in draft(hours = "0").validate())
        assertTrue(TimeEntryError.HoursOutOfRange in draft(hours = "16.25").validate())
    }

    @Test
    fun overtimeRequiresRegularHours() {
        val errors = draft(hours = "", overtime = "2").validate()
        assertTrue(TimeEntryError.OvertimeWithoutRegular in errors)
    }

    @Test
    fun crewMemberAndCostCodeAreRequired() {
        assertTrue(TimeEntryError.CrewMemberRequired in draft(crew = null).validate())
        assertTrue(TimeEntryError.CostCodeRequired in draft(code = null).validate())
    }

    @Test
    fun toEntryRoundTripsOnlyWhenValid() {
        val entry = draft(hours = "6.5", overtime = "1.5").toEntry("te-1", "Mon, Aug 18")
        assertEquals("te-1", entry?.id)
        assertEquals("hector-ortiz", entry?.crewMemberId)
        assertEquals(6.5, entry?.hours)
        assertEquals(1.5, entry?.overtimeHours)
        // Newly created entries are queued — the prototype's offline state.
        assertTrue(entry?.queued == true)

        assertNull(draft(hours = "nope").toEntry("te-2", "Mon, Aug 18"))
        assertFalse(draft(hours = "nope").isValid)
    }

    @Test
    fun formatHoursDropsTrailingZeroes() {
        assertEquals("8 h", formatHours(8.0))
        assertEquals("8.5 h", formatHours(8.5))
        assertEquals("0 h", formatHours(0.0))
    }
}
