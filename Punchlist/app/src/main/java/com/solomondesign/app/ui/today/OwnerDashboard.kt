package com.solomondesign.app.ui.today

import com.solomondesign.app.ui.collab.CollabTopic
import com.solomondesign.app.ui.records.FieldRecord
import com.solomondesign.app.ui.records.RFI_ISSUE_TYPE
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.rfiAgeLabel
import java.util.Locale

/*
 * Pure logic behind the Owner's Today dashboard — no Compose imports, so every status
 * threshold, ranking rule, and accessible summary is unit-testable on the JVM.
 *
 * The Owner is not running crews; they open the app for confidence: is delivery on plan,
 * what is at risk, and where is a decision needed. Each of the four decision topics below
 * answers one of those questions (the design note for each visualization lives with its
 * model). OwnerTodaySections.kt renders these; OwnerDemoMetrics.kt seeds the schedule and
 * budget figures this prototype has no engine for.
 */

/** Plain three-state health language: "On track" / "At risk" / "Behind plan". */
enum class OwnerStatus { ON_TRACK, AT_RISK, BEHIND }

fun OwnerStatus.label(): String = when (this) {
    OwnerStatus.ON_TRACK -> "On track"
    OwnerStatus.AT_RISK -> "At risk"
    OwnerStatus.BEHIND -> "Behind plan"
}

// ---------------------------------------------------------------------------------------------
// Topic 1 — Schedule health
//
// User question: "Is delivery still on track?"
// Decision it supports: whether to seek a schedule update / escalate before the milestone slips.
// Chart: a two-line sparkline (planned vs actual completion by week) — the simplest form that
// shows *direction*: whether the gap to plan is opening or closing. Endpoint values are printed
// as text, so the chart carries only shape and there is no axis to truncate or decode.
// Metric: % complete · unit: percentage points · period: trailing weeks · baseline: the planned
// curve · timestamp: [ScheduleHealth.updatedAtMillis], surfaced via [freshnessLabel].
// Thresholds: within 2 pts of plan = on track; within 6 pts = at risk; beyond = behind plan.
// Data behavior: this prototype has no schedule engine, so values are seeded demo figures
// (OwnerDemoMetrics) and the card labels them "Demo data"; >24h old renders as "Stale".
// ---------------------------------------------------------------------------------------------

data class ScheduleHealth(
    val actualPercent: Int,
    val plannedPercent: Int,
    /** The milestone the % tracks toward, e.g. "Dry-in · Oct 12". */
    val milestoneLabel: String,
    /** Planned completion by week, oldest first; same length as [weeklyActual]. */
    val weeklyPlanned: List<Int>,
    val weeklyActual: List<Int>,
    val updatedAtMillis: Long,
)

/** Negative = behind plan, positive = ahead. */
val ScheduleHealth.variancePoints: Int get() = actualPercent - plannedPercent

fun ScheduleHealth.status(): OwnerStatus = when {
    variancePoints >= -2 -> OwnerStatus.ON_TRACK
    variancePoints >= -6 -> OwnerStatus.AT_RISK
    else -> OwnerStatus.BEHIND
}

/** The takeaway line — the answer first, numbers after (rendered below it). */
fun ScheduleHealth.takeaway(): String = when (status()) {
    OwnerStatus.ON_TRACK -> "Delivery on track for $milestoneLabel."
    OwnerStatus.AT_RISK -> "$milestoneLabel at risk — ${-variancePoints} pts behind plan."
    OwnerStatus.BEHIND -> "$milestoneLabel behind plan by ${-variancePoints} pts."
}

private fun variancePhrase(variancePoints: Int): String = when {
    variancePoints == 0 -> "on plan"
    variancePoints > 0 -> "$variancePoints percentage points ahead of plan"
    else -> "${-variancePoints} percentage points behind plan"
}

/** TalkBack summary for the trend chart, e.g. spoken instead of the two drawn lines. */
fun ScheduleHealth.accessibleSummary(nowMillis: Long): String =
    "Schedule: $actualPercent percent complete, ${variancePhrase(variancePoints)}, " +
        "${status().label().lowercase(Locale.US)}. Target $milestoneLabel. " +
        freshnessLabel(updatedAtMillis, nowMillis) + "."

// ---------------------------------------------------------------------------------------------
// Topic 2 — Budget and change exposure
//
// User question: "Is the project staying within the approved financial plan?"
// Decision it supports: whether to ask for a cost review before pending changes are approved.
// Chart: two horizontal bars from a shared zero baseline (approved vs forecast) — the simplest
// honest way to compare two magnitudes; explicitly not a gauge/donut, and bars starting at zero
// cannot exaggerate the variance. Values are printed directly on each bar's label.
// Metric: contract value · unit: $ millions · baseline: approved budget · forecast includes
// pending changes and is labeled an estimate · timestamp via [freshnessLabel].
// Thresholds: forecast within the 2% contingency = on track; ≤4% over = at risk; beyond =
// behind plan. Under-budget is always on track.
// Data behavior: seeded demo figures (no cost engine exists); labeled "Demo data" on the card.
// ---------------------------------------------------------------------------------------------

data class BudgetExposure(
    val approvedMillions: Double,
    /** Forecast at completion, including pending changes — an estimate, labeled as such. */
    val forecastMillions: Double,
    val pendingChangeCount: Int,
    val pendingChangeMillions: Double,
    /** What is driving the exposure, e.g. "Med-gas re-route (RFI-118)". */
    val driverLabel: String,
    /** The [FieldRecord] behind the driver — the card's tap-through, when one exists. */
    val driverRecordId: String?,
    val updatedAtMillis: Long,
)

val BudgetExposure.variancePercent: Double
    get() = (forecastMillions - approvedMillions) / approvedMillions * 100.0

fun BudgetExposure.status(): OwnerStatus = when {
    variancePercent <= 2.0 -> OwnerStatus.ON_TRACK
    variancePercent <= 4.0 -> OwnerStatus.AT_RISK
    else -> OwnerStatus.BEHIND
}

fun formatMillions(value: Double): String = String.format(Locale.US, "$%.1fM", value)

fun BudgetExposure.takeaway(): String {
    val direction = if (variancePercent >= 0) "+" else "−"
    val magnitude = String.format(Locale.US, "%.1f", kotlin.math.abs(variancePercent))
    val verdict = when (status()) {
        OwnerStatus.ON_TRACK -> "within the 2% contingency"
        OwnerStatus.AT_RISK -> "over budget, inside the 4% review line"
        OwnerStatus.BEHIND -> "over the review line"
    }
    return "Forecast ${formatMillions(forecastMillions)} vs " +
        "${formatMillions(approvedMillions)} approved ($direction$magnitude%) — $verdict."
}

fun BudgetExposure.accessibleSummary(nowMillis: Long): String =
    "Budget: forecast ${formatMillions(forecastMillions)} against " +
        "${formatMillions(approvedMillions)} approved, " +
        "${status().label().lowercase(Locale.US)}. " +
        "$pendingChangeCount pending change${if (pendingChangeCount == 1) "" else "s"} " +
        "estimated at ${formatMillions(pendingChangeMillions)}. " +
        freshnessLabel(updatedAtMillis, nowMillis) + "."

// ---------------------------------------------------------------------------------------------
// Topic 3 — Quality and approvals
//
// User question: "Are inspections, approvals, or quality items likely to affect the project?"
// Decision it supports: whether to push the design team (or the GC) for an overdue answer.
// Chart: sorted horizontal bars of open items by category — category comparison, so bars, not
// a pie; sorted descending so the biggest bucket reads first; counts printed on each bar.
// The exception line names only the single most urgent item (the oldest open RFI); routine
// operational rows (e.g. the Frame-inspection punch item) are aggregated into counts, never
// surfaced as rows on the Owner's Today.
// Metric: open record count · baseline: zero open · derived live from RecordRepository (real
// data, not demo) · staleness does not apply — counts are computed at render time.
// Thresholds: oldest open RFI ≤4 days = on track; ≤10 days = at risk; older = behind plan.
// Empty behavior: no open records at all reads "No approvals waiting", on track.
// ---------------------------------------------------------------------------------------------

data class QualityCategoryCount(val label: String, val count: Int)

/** Open items by category, largest first (stable, so ties keep RFIs — the approval bucket — first). */
fun ownerQualityBreakdown(records: List<FieldRecord>): List<QualityCategoryCount> =
    listOf(
        QualityCategoryCount(
            "RFIs awaiting answer",
            records.count { it.category == RecordCategory.ISSUE && it.type == RFI_ISSUE_TYPE },
        ),
        QualityCategoryCount("Open punch items", records.count { it.category == RecordCategory.PUNCH }),
        QualityCategoryCount("Safety incidents", records.count { it.category == RecordCategory.INCIDENT }),
    ).filter { it.count > 0 }.sortedByDescending { it.count }

fun oldestOpenRfi(records: List<FieldRecord>): FieldRecord? =
    records.filter { it.category == RecordCategory.ISSUE && it.type == RFI_ISSUE_TYPE }
        .minByOrNull { it.createdAtMillis }

private fun ageDays(createdAtMillis: Long, nowMillis: Long): Long =
    ((nowMillis - createdAtMillis).coerceAtLeast(0L)) / DAY_MILLIS

fun ownerQualityStatus(records: List<FieldRecord>, nowMillis: Long): OwnerStatus {
    val oldest = oldestOpenRfi(records) ?: return OwnerStatus.ON_TRACK
    return when {
        ageDays(oldest.createdAtMillis, nowMillis) <= 4 -> OwnerStatus.ON_TRACK
        ageDays(oldest.createdAtMillis, nowMillis) <= 10 -> OwnerStatus.AT_RISK
        else -> OwnerStatus.BEHIND
    }
}

fun qualityTakeaway(records: List<FieldRecord>, nowMillis: Long): String {
    val rfiCount = records.count { it.category == RecordCategory.ISSUE && it.type == RFI_ISSUE_TYPE }
    val oldest = oldestOpenRfi(records)
        ?: return "No approvals waiting."
    return "$rfiCount RFI${if (rfiCount == 1) "" else "s"} awaiting answer — oldest " +
        rfiAgeLabel(oldest.createdAtMillis, nowMillis).lowercase(Locale.US) + "."
}

fun qualityAccessibleSummary(records: List<FieldRecord>, nowMillis: Long): String {
    val breakdown = ownerQualityBreakdown(records)
    if (breakdown.isEmpty()) return "Quality: no open items."
    val parts = breakdown.joinToString(", ") { "${it.count} ${it.label.lowercase(Locale.US)}" }
    return "Quality: $parts. ${ownerQualityStatus(records, nowMillis).label()}."
}

// ---------------------------------------------------------------------------------------------
// Topic 4 — Decisions needed
//
// User question: "What requires my attention or decision today?"
// Decision it supports: each row IS a decision — the next action is printed on the row.
// Presentation: an urgency-ranked exception list, not a chart — the items are few and each
// needs reading, so a list is the simplest effective form.
// Ranking: blocking records first (critical — work is stopped), then open RFIs oldest first
// (a decision is pending elsewhere; age = urgency), then unread decision threads (awareness).
// Punch items and incidents are field operations, not Owner decisions, so they never rank —
// which is also what keeps the Frame-inspection row off the Owner's Today.
// Data: live records + Collaboration topics; capped at [MAX_OWNER_DECISIONS] so the card stays
// a 3–5 second glance (anything beyond the cap is reachable through the tools).
// ---------------------------------------------------------------------------------------------

const val MAX_OWNER_DECISIONS = 5

data class OwnerDecisionItem(
    val id: String,
    val title: String,
    /** One line: impact · location · age. */
    val impactLine: String,
    /** The direct next action, printed on the row, e.g. "Push for the A/E answer". */
    val nextAction: String,
    /** Critical = work is stopped; sorts first and renders with the error state. */
    val critical: Boolean,
    val recordId: String? = null,
    val topicId: String? = null,
)

fun ownerDecisions(
    records: List<FieldRecord>,
    topics: List<CollabTopic>,
    nowMillis: Long,
): List<OwnerDecisionItem> {
    val blocking = records.filter { it.blocksWork }
        .sortedByDescending { it.severity.ordinal }
        .map { record ->
            OwnerDecisionItem(
                id = "decision-${record.id}",
                title = record.title,
                impactLine = listOf(
                    "${record.impact.label} impact",
                    record.location,
                    rfiAgeLabel(record.createdAtMillis, nowMillis),
                ).filter { it.isNotBlank() }.joinToString(" · "),
                nextAction = "Review with " +
                    record.resolutionAuthority.ifBlank { "the field team" },
                critical = true,
                recordId = record.id,
            )
        }
    val rfis = records
        .filter { it.category == RecordCategory.ISSUE && it.type == RFI_ISSUE_TYPE && !it.blocksWork }
        .sortedBy { it.createdAtMillis }
        .map { record ->
            OwnerDecisionItem(
                id = "decision-${record.id}",
                title = record.title,
                impactLine = listOf(
                    "${record.impact.label} impact",
                    record.location,
                    rfiAgeLabel(record.createdAtMillis, nowMillis),
                ).filter { it.isNotBlank() }.joinToString(" · "),
                nextAction = "Push for the A/E answer",
                critical = false,
                recordId = record.id,
            )
        }
    val unreadThreads = topics.filter { it.unreadCount > 0 }
        .map { topic ->
            OwnerDecisionItem(
                id = "decision-topic-${topic.id}",
                title = topic.title,
                impactLine = "${topic.location} · ${topic.unreadCount} unread",
                nextAction = "Read and reply",
                critical = false,
                topicId = topic.id,
            )
        }
    return (blocking + rfis + unreadThreads).take(MAX_OWNER_DECISIONS)
}

// ---------------------------------------------------------------------------------------------
// Delays — the Med-gas blocker and any live work stoppage, rendered after the four topics.
// ---------------------------------------------------------------------------------------------

/**
 * One line making a delay's impact legible to an Owner: affected area, why work is stopped,
 * who owns lifting it, and when resolution is expected. Falls back gracefully when the
 * blocker has no backing record (e.g. a voice-dictated delay).
 */
fun delayImpactLine(record: FieldRecord, nowMillis: Long): String {
    val expected = record.expectedResolutionMillis?.let { millis ->
        val days = ((millis - nowMillis + DAY_MILLIS - 1) / DAY_MILLIS).coerceAtLeast(0L)
        if (days == 0L) "resolution expected today" else "resolution expected in $days day${if (days == 1L) "" else "s"}"
    }
    return listOfNotNull(
        record.location.ifBlank { null },
        record.blockingReason.ifBlank { record.description }.ifBlank { null },
        record.resolutionAuthority.ifBlank { null }?.let { "with $it" },
        expected,
    ).joinToString(" · ")
}

// ---------------------------------------------------------------------------------------------
// Freshness — every metric card prints when its data was last updated; >24h reads as stale.
// ---------------------------------------------------------------------------------------------

private const val DAY_MILLIS = 86_400_000L

fun isStale(updatedAtMillis: Long, nowMillis: Long): Boolean =
    nowMillis - updatedAtMillis >= DAY_MILLIS

fun freshnessLabel(updatedAtMillis: Long, nowMillis: Long): String {
    val minutes = ((nowMillis - updatedAtMillis).coerceAtLeast(0L)) / 60_000L
    return when {
        minutes < 2 -> "Updated just now"
        minutes < 60 -> "Updated $minutes min ago"
        minutes < 24 * 60 -> "Updated ${minutes / 60} h ago"
        else -> "Stale · updated ${minutes / (24 * 60)} d ago"
    }
}
