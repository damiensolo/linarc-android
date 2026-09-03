package com.solomondesign.app.ui.timecards

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftHoursTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun roundsUpToTheNearestQuarterHour() {
        assertEquals(8.0, shiftHoursBetween(0L, 8 * hour), 0.0)
        assertEquals(8.25, shiftHoursBetween(0L, 8 * hour + minute), 0.0)
        assertEquals(4.5, shiftHoursBetween(0L, 4 * hour + 20 * minute), 0.0)
    }

    @Test
    fun demoLengthShiftStillProducesAVisibleEntry() {
        assertEquals(0.25, shiftHoursBetween(0L, 30_000L), 0.0)
        assertEquals(0.25, shiftHoursBetween(0L, 0L), 0.0)
    }

    @Test
    fun clockSkewNeverGoesNegative() {
        assertEquals(0.25, shiftHoursBetween(hour, 0L), 0.0)
    }

    @Test
    fun capsAtMaxDailyHours() {
        assertEquals(MAX_DAILY_HOURS, shiftHoursBetween(0L, 48 * hour), 0.0)
    }
}
