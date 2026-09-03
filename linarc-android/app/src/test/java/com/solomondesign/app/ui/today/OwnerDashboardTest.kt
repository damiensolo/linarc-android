package com.solomondesign.app.ui.today

import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.demo.OwnerDemoMetrics
import com.solomondesign.app.ui.records.RecordRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OwnerDashboardTest {

    @Before
    fun resetStores() {
        RecordRepository.clear()
        CollabRepository.clear()
    }

    private fun schedule(actual: Int, planned: Int) = ScheduleHealth(
        actualPercent = actual,
        plannedPercent = planned,
        milestoneLabel = "Dry-in · Oct 12",
        weeklyPlanned = listOf(planned - 4, planned),
        weeklyActual = listOf(planned - 4, actual),
        updatedAtMillis = 0L,
    )

    private fun budget(approved: Double, forecast: Double) = BudgetExposure(
        approvedMillions = approved,
        forecastMillions = forecast,
        pendingChangeCount = 1,
        pendingChangeMillions = forecast - approved,
        driverLabel = "Test change",
        driverRecordId = null,
        updatedAtMillis = 0L,
    )

    // -- Schedule health thresholds: within 2 pts = on track, within 6 = at risk, else behind --

    @Test
    fun scheduleStatus_thresholdBoundaries() {
        assertEquals(OwnerStatus.ON_TRACK, schedule(66, 66).status())
        assertEquals(OwnerStatus.ON_TRACK, schedule(64, 66).status()) // -2: boundary
        assertEquals(OwnerStatus.AT_RISK, schedule(63, 66).status()) // -3
        assertEquals(OwnerStatus.AT_RISK, schedule(60, 66).status()) // -6: boundary
        assertEquals(OwnerStatus.BEHIND, schedule(59, 66).status()) // -7
        assertEquals(OwnerStatus.ON_TRACK, schedule(70, 66).status()) // ahead is on track
    }

    // -- Budget thresholds: within the 2% contingency = on track, ≤4% = at risk, else behind --

    @Test
    fun budgetStatus_thresholdBoundaries() {
        assertEquals(OwnerStatus.ON_TRACK, budget(100.0, 100.0).status())
        assertEquals(OwnerStatus.ON_TRACK, budget(100.0, 102.0).status()) // +2%: boundary
        assertEquals(OwnerStatus.AT_RISK, budget(100.0, 103.0).status())
        assertEquals(OwnerStatus.AT_RISK, budget(100.0, 104.0).status()) // +4%: boundary
        assertEquals(OwnerStatus.BEHIND, budget(100.0, 105.0).status())
        assertEquals(OwnerStatus.ON_TRACK, budget(100.0, 96.0).status()) // under budget
    }

    /** The demo figures must tell the seeded story: schedule at risk, budget inside contingency. */
    @Test
    fun demoMetrics_matchTheSeededNarrative() {
        val now = 1_000_000_000L
        val schedule = OwnerDemoMetrics.scheduleHealth(now)
        assertEquals(OwnerStatus.AT_RISK, schedule.status())
        assertEquals(-4, schedule.variancePoints)
        assertEquals(schedule.weeklyPlanned.size, schedule.weeklyActual.size)

        val budget = OwnerDemoMetrics.budgetExposure(now)
        assertEquals(OwnerStatus.ON_TRACK, budget.status())
        // The exposure's tap-through is the seeded RFI record that drives it.
        assertEquals("rec-seed-rfi-118", budget.driverRecordId)
        assertTrue(RecordRepository.find(budget.driverRecordId!!) != null)
    }

    // -- Quality: sorted category bars + oldest-RFI exception, from live records ---------------

    @Test
    fun qualityBreakdown_sortsLargestFirst_andDropsEmptyBuckets() {
        val breakdown = ownerQualityBreakdown(RecordRepository.records)
        assertEquals(
            listOf("RFIs awaiting answer" to 2, "Open punch items" to 2, "Safety incidents" to 1),
            breakdown.map { it.label to it.count },
        )
        assertTrue(breakdown.zipWithNext().all { (a, b) -> a.count >= b.count })
    }

    @Test
    fun oldestOpenRfi_isTheSixDayHeadwallRfi_andQualityReadsAtRisk() {
        val now = System.currentTimeMillis()
        assertEquals("rec-seed-rfi-121", oldestOpenRfi(RecordRepository.records)?.id)
        // 6 days open: past the 4-day on-track line, inside the 10-day behind line.
        assertEquals(OwnerStatus.AT_RISK, ownerQualityStatus(RecordRepository.records, now))
        assertTrue(qualityTakeaway(RecordRepository.records, now).startsWith("2 RFIs awaiting answer"))
    }

    // -- Decisions needed: critical first, operational rows never rank -------------------------

    @Test
    fun ownerDecisions_ranksBlockingFirst_thenRfisOldestFirst_thenUnreadThreads() {
        val decisions =
            ownerDecisions(RecordRepository.records, CollabRepository.topics, System.currentTimeMillis())
        assertEquals(
            listOf(
                "decision-rec-seed-issue", // blocking → critical, first
                "decision-rec-seed-rfi-121", // 6 days open
                "decision-rec-seed-rfi-118", // 3 days open
                "decision-topic-topic-col4-medgas", // 2 unread
                "decision-topic-topic-headwall-heights", // 1 unread
            ),
            decisions.map { it.id },
        )
        assertTrue(decisions.first().critical)
        assertTrue(decisions.drop(1).none { it.critical })
        assertTrue(decisions.size <= MAX_OWNER_DECISIONS)
    }

    /**
     * Punch items and incidents are field operations, not Owner decisions — which is exactly
     * what keeps the Frame-inspection row (a punch record) off the Owner's Today.
     */
    @Test
    fun ownerDecisions_neverSurfaceOperationalRecords() {
        val ids = ownerDecisions(RecordRepository.records, CollabRepository.topics, System.currentTimeMillis())
            .map { it.id }
        assertFalse(ids.any { it.contains("rec-seed-inspection") })
        assertFalse(ids.any { it.contains("rec-seed-punch") })
        assertFalse(ids.any { it.contains("rec-seed-incident") })
    }

    @Test
    fun ownerDecisions_emptyStoresRankNothing() {
        assertTrue(ownerDecisions(emptyList(), emptyList(), 0L).isEmpty())
    }

    // -- Delays: the impact line an Owner actually needs ---------------------------------------

    @Test
    fun delayImpactLine_namesAreaOwnerAndExpectedResolution() {
        val record = RecordRepository.find("rec-seed-issue")!!
        val line = delayImpactLine(record, System.currentTimeMillis())
        assertTrue(line.contains("Column 4"))
        assertTrue(line.contains("with Superintendent"))
        assertTrue(line.contains("resolution expected in 2 days"))
    }

    @Test
    fun delayImpactLine_pastDueReadsToday_neverNegative() {
        val record = RecordRepository.find("rec-seed-issue")!!
            .copy(expectedResolutionMillis = 1_000L)
        assertTrue(delayImpactLine(record, 2_000_000L).contains("resolution expected today"))
    }

    // -- Freshness / staleness ------------------------------------------------------------------

    @Test
    fun freshnessLabel_coversJustNowMinutesHoursAndStale() {
        val now = 100 * 86_400_000L
        assertEquals("Updated just now", freshnessLabel(now - 60_000L, now))
        assertEquals("Updated 10 min ago", freshnessLabel(now - 10 * 60_000L, now))
        assertEquals("Updated 3 h ago", freshnessLabel(now - 3 * 3_600_000L, now))
        assertEquals("Stale · updated 2 d ago", freshnessLabel(now - 48 * 3_600_000L, now))
        assertFalse(isStale(now - 23 * 3_600_000L, now))
        assertTrue(isStale(now - 24 * 3_600_000L, now))
    }

    // -- Accessible chart summaries: the spoken sentence carries the whole chart ---------------

    @Test
    fun accessibleSummaries_speakTheNumbersAndTheVerdict() {
        val now = 1_000_000_000L
        val schedule = OwnerDemoMetrics.scheduleHealth(now).accessibleSummary(now)
        assertTrue(schedule.contains("62 percent complete"))
        assertTrue(schedule.contains("4 percentage points behind plan"))
        assertTrue(schedule.contains("at risk"))
        assertTrue(schedule.contains("Updated 10 min ago"))

        val budget = OwnerDemoMetrics.budgetExposure(now).accessibleSummary(now)
        assertTrue(budget.contains("forecast $48.9M against $48.2M approved"))
        assertTrue(budget.contains("on track"))
        assertTrue(budget.contains("1 pending change estimated at $0.7M"))

        val quality = qualityAccessibleSummary(RecordRepository.records, System.currentTimeMillis())
        assertTrue(quality.contains("2 rfis awaiting answer"))
        assertTrue(quality.contains("At risk"))
    }

    @Test
    fun onTrackScheduleAndEmptyQuality_readCalm() {
        assertEquals("Delivery on track for Dry-in · Oct 12.", schedule(66, 66).takeaway())
        assertEquals("No approvals waiting.", qualityTakeaway(emptyList(), 0L))
        assertEquals("Quality: no open items.", qualityAccessibleSummary(emptyList(), 0L))
        assertNull(oldestOpenRfi(emptyList()))
    }
}
