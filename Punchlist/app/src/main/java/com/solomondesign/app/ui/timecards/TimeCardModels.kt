package com.solomondesign.app.ui.timecards

data class TimeEntry(
    val id: String,
    val crewMemberId: String,
    val dateLabel: String,
    val costCode: String,
    val hours: Double,
    val overtimeHours: Double = 0.0,
    val note: String = "",
    /** Created this session and not synced — the prototype's offline state. */
    val queued: Boolean = false,
)

/** "8 h", "8.5 h", "0 h". Pure so it is JVM-unit-testable. */
fun formatHours(hours: Double): String {
    val text = if (hours % 1.0 == 0.0) hours.toInt().toString() else hours.toString()
    return "$text h"
}

val COST_CODES = listOf(
    "09-2216 Non-structural metal framing",
    "08-1113 Hollow metal doors & frames",
    "26-0526 Grounding & bonding",
    "22-6200 Medical gas systems",
    "07-8400 Firestopping",
    "01-5000 Temporary facilities",
)

const val MAX_DAILY_HOURS = 16.0

enum class TimeEntryError {
    CrewMemberRequired,
    CostCodeRequired,
    HoursRequired,
    HoursNotANumber,
    HoursOutOfRange,
    OvertimeNotANumber,
    OvertimeOutOfRange,
    OvertimeWithoutRegular,
}

fun TimeEntryError.message(): String = when (this) {
    TimeEntryError.CrewMemberRequired -> "Choose a crew member"
    TimeEntryError.CostCodeRequired -> "Choose a cost code"
    TimeEntryError.HoursRequired -> "Enter hours"
    TimeEntryError.HoursNotANumber -> "Hours must be a number, e.g. 8 or 8.5"
    TimeEntryError.HoursOutOfRange -> "Hours must be between 0 and ${MAX_DAILY_HOURS.toInt()}"
    TimeEntryError.OvertimeNotANumber -> "Overtime must be a number"
    TimeEntryError.OvertimeOutOfRange -> "Overtime must be between 0 and ${MAX_DAILY_HOURS.toInt()}"
    TimeEntryError.OvertimeWithoutRegular -> "Enter regular hours before overtime"
}

/**
 * Raw form state straight off the sheet's fields. Validation lives here rather than in the
 * composable so it can be unit tested without an emulator.
 */
data class TimeEntryDraft(
    val crewMemberId: String? = null,
    val costCode: String? = null,
    val hoursText: String = "",
    val overtimeText: String = "",
    val note: String = "",
) {
    /** Empty means valid. Ordered so the first error is the one worth surfacing. */
    fun validate(): List<TimeEntryError> {
        val errors = mutableListOf<TimeEntryError>()
        if (crewMemberId.isNullOrBlank()) errors += TimeEntryError.CrewMemberRequired
        if (costCode.isNullOrBlank()) errors += TimeEntryError.CostCodeRequired

        val hoursRaw = hoursText.trim()
        // toDoubleOrNull is locale-independent, so "8,25" is deliberately rejected. Pair the
        // field with KeyboardType.Decimal.
        val hours = hoursRaw.toDoubleOrNull()
        when {
            hoursRaw.isEmpty() -> errors += TimeEntryError.HoursRequired
            hours == null -> errors += TimeEntryError.HoursNotANumber
            hours <= 0.0 || hours > MAX_DAILY_HOURS -> errors += TimeEntryError.HoursOutOfRange
        }

        val overtimeRaw = overtimeText.trim()
        if (overtimeRaw.isNotEmpty()) {
            val overtime = overtimeRaw.toDoubleOrNull()
            when {
                overtime == null -> errors += TimeEntryError.OvertimeNotANumber
                overtime < 0.0 || overtime > MAX_DAILY_HOURS ->
                    errors += TimeEntryError.OvertimeOutOfRange
                overtime > 0.0 && (hours == null || hours <= 0.0) ->
                    errors += TimeEntryError.OvertimeWithoutRegular
            }
        }
        return errors
    }

    val isValid: Boolean get() = validate().isEmpty()

    fun toEntry(id: String, dateLabel: String): TimeEntry? {
        if (!isValid) return null
        return TimeEntry(
            id = id,
            crewMemberId = crewMemberId.orEmpty(),
            dateLabel = dateLabel,
            costCode = costCode.orEmpty(),
            hours = hoursText.trim().toDouble(),
            overtimeHours = overtimeText.trim().toDoubleOrNull() ?: 0.0,
            note = note.trim(),
            queued = true,
        )
    }
}
