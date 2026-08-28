package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.today.BudgetExposure
import com.solomondesign.app.ui.today.ScheduleHealth

/**
 * DEMO DATA — this prototype has no schedule or cost engine, so the Owner dashboard's
 * schedule and budget figures are frozen fictions seeded here, isolated from the pure
 * derivation logic in OwnerDashboard.kt (which is what real data would flow through).
 * The UI labels every value from this object "Demo data"; the numbers are chosen to tell
 * the seeded story coherently: the Column 4 med-gas conflict (RFI-118) is what has the
 * schedule 4 pts behind plan and is the sole pending change against the budget.
 *
 * Quality and Decisions are NOT seeded here — they derive live from RecordRepository and
 * CollabRepository, so records the user creates during a demo move those cards.
 */
object OwnerDemoMetrics {

    /** 62% built vs 66% planned → "At risk" per the ±2/±6 pt thresholds. Updated 10 min ago. */
    fun scheduleHealth(nowMillis: Long): ScheduleHealth = ScheduleHealth(
        actualPercent = 62,
        plannedPercent = 66,
        milestoneLabel = "Dry-in · Oct 12",
        weeklyPlanned = listOf(38, 45, 52, 58, 62, 66),
        weeklyActual = listOf(38, 44, 50, 55, 59, 62),
        updatedAtMillis = nowMillis - 10 * 60_000L,
    )

    /** +1.5% forecast over approved → "On track" (within the 2% contingency). Updated 1 h ago. */
    fun budgetExposure(nowMillis: Long): BudgetExposure = BudgetExposure(
        approvedMillions = 48.2,
        forecastMillions = 48.9,
        pendingChangeCount = 1,
        pendingChangeMillions = 0.7,
        driverLabel = "Med-gas re-route (RFI-118)",
        // The RecordRepository seed behind the exposure — the card's tap-through.
        driverRecordId = "rec-seed-rfi-118",
        updatedAtMillis = nowMillis - 60 * 60_000L,
    )
}
