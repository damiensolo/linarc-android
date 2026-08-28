package com.solomondesign.app.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.collab.CollabTopic
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.OwnerDemoMetrics
import com.solomondesign.app.ui.demo.StreamItem
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.records.FieldRecord
import com.solomondesign.app.ui.theme.LightPresenceOnSite
import com.solomondesign.app.ui.theme.PresenceOnSite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * The Owner's Today — the "OwnerTodayScreen" of the design brief, expressed as a LazyListScope
 * extension so it plugs into the one persona-branched TodayScreen this app uses instead of a
 * parallel screen hierarchy. Order (per the Owner spec):
 *
 *   1. Sync/staleness row (header date + offline state)
 *   2. Progress photos — the Owner's window into the site, newest capture first
 *   3. Exactly four decision topics: Schedule health, Budget & change exposure,
 *      Quality & approvals, Decisions needed
 *   4. Delays — live work stoppages (the Med-gas blocker), enriched with impact/owner/next step
 *
 * No voice logs, no time cards, no crew roster, no Foreman operational rows — the Owner is the
 * one persona sanctioned to drop surfaces ("no time cards or voice log", extended 2026-08-25 to
 * the roster and operational stream). All thresholds, rankings, and accessible summaries are
 * pure functions in OwnerDashboard.kt; demo-seeded figures are isolated in OwnerDemoMetrics.
 */
fun LazyListScope.ownerTodayContent(
    progressMedia: List<StreamItem>,
    records: List<FieldRecord>,
    topics: List<CollabTopic>,
    blockers: List<StreamItem>,
    queuedOutboxCount: Int,
    nowMillis: Long,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenVoiceLog: (String) -> Unit,
    onOpenTask: (String) -> Unit,
) {
    item { OwnerSyncStateRow(queuedOutboxCount = queuedOutboxCount, nowMillis = nowMillis) }

    // -- 1. Progress photos -----------------------------------------------------------------
    item { FieldSectionLabel("Progress photos") }
    if (progressMedia.isEmpty()) {
        item { FieldEmptyState("No progress photos yet. Captured photos and videos land here.") }
    } else {
        items(progressMedia, key = { it.id }) { item ->
            CaptureStreamRow(
                item = item,
                onOpenVoiceLog = onOpenVoiceLog,
                onOpenImage = onOpenImage,
                onOpenVideo = onOpenVideo,
                onOpenRecord = onOpenRecord,
                onOpenTask = onOpenTask,
            )
        }
    }

    // -- 2. Schedule health -----------------------------------------------------------------
    item {
        val schedule = OwnerDemoMetrics.scheduleHealth(nowMillis)
        // The variance's driver is the blocking issue record; with no schedule tool in this
        // prototype, that record detail IS the milestone detail behind the risk.
        val driver = records.firstOrNull { it.blocksWork }
        OwnerDecisionTopicCard(
            title = "Schedule health",
            status = schedule.status(),
            takeaway = schedule.takeaway(),
            metaLine = "Plan ${schedule.plannedPercent}% vs actual ${schedule.actualPercent}% · " +
                "Demo data · ${freshnessLabel(schedule.updatedAtMillis, nowMillis)}",
            onOpen = driver?.let { { onOpenRecord(it.id) } },
            openLabel = driver?.let { "Open ${it.title}" },
            testTag = "ownerTopic_schedule",
        ) {
            OwnerTrendChart(
                planned = schedule.weeklyPlanned,
                actual = schedule.weeklyActual,
                statusColor = schedule.status().statusColor(),
                summary = schedule.accessibleSummary(nowMillis),
            )
        }
    }

    // -- 3. Budget and change exposure --------------------------------------------------------
    item {
        val budget = OwnerDemoMetrics.budgetExposure(nowMillis)
        OwnerDecisionTopicCard(
            title = "Budget & changes",
            status = budget.status(),
            takeaway = budget.takeaway(),
            metaLine = "${budget.pendingChangeCount} pending " +
                "change${if (budget.pendingChangeCount == 1) "" else "s"} · " +
                "${formatMillions(budget.pendingChangeMillions)} est. · ${budget.driverLabel} · " +
                "Demo data · ${freshnessLabel(budget.updatedAtMillis, nowMillis)}",
            onOpen = budget.driverRecordId?.let { id -> { onOpenRecord(id) } },
            openLabel = "Open ${budget.driverLabel}",
            testTag = "ownerTopic_budget",
        ) {
            OwnerBudgetComparison(
                budget = budget,
                statusColor = budget.status().statusColor(),
                summary = budget.accessibleSummary(nowMillis),
            )
        }
    }

    // -- 4. Quality and approvals -------------------------------------------------------------
    item {
        val oldestRfi = oldestOpenRfi(records)
        OwnerDecisionTopicCard(
            title = "Quality & approvals",
            status = ownerQualityStatus(records, nowMillis),
            takeaway = qualityTakeaway(records, nowMillis),
            metaLine = "Live from project records · counts update as records open and close",
            onOpen = oldestRfi?.let { { onOpenRecord(it.id) } },
            openLabel = oldestRfi?.let { "Open ${it.title}" },
            testTag = "ownerTopic_quality",
        ) {
            OwnerCategoryBars(
                breakdown = ownerQualityBreakdown(records),
                summary = qualityAccessibleSummary(records, nowMillis),
            )
        }
    }

    // -- 5. Decisions needed --------------------------------------------------------------------
    item {
        val decisions = ownerDecisions(records, topics, nowMillis)
        OwnerDecisionTopicCard(
            title = "Decisions needed",
            status = null,
            takeaway = when {
                decisions.isEmpty() -> "Nothing waiting on you today."
                decisions.any { it.critical } ->
                    "${decisions.size} items — work is stopped on ${decisions.count { it.critical }}."
                else -> "${decisions.size} item${if (decisions.size == 1) "" else "s"} waiting on a decision or reply."
            },
            metaLine = "Critical first · each row opens its detail",
            testTag = "ownerTopic_decisions",
        ) {
            OwnerExceptionList(
                items = decisions,
                onOpenRecord = onOpenRecord,
                onOpenTopic = onOpenTopic,
            )
        }
    }

    // -- 6. Delays ------------------------------------------------------------------------------
    item { FieldSectionLabel("Delays") }
    if (blockers.isEmpty()) {
        item { FieldEmptyState("No active delays. Blocking issues land here.") }
    } else {
        items(blockers, key = { "ownerDelay_${it.id}" }) { item ->
            DelayBlockerCard(
                item = item,
                record = item.relatedFieldRecordId?.let { id -> records.firstOrNull { it.id == id } },
                nowMillis = nowMillis,
                onOpen = when {
                    item.relatedFieldRecordId != null -> {
                        { onOpenRecord(item.relatedFieldRecordId) }
                    }
                    item.relatedRecordId != null -> {
                        { onOpenVoiceLog(item.relatedRecordId) }
                    }
                    else -> null
                },
            )
        }
    }
}

/**
 * Header meta row: today's date plus the sync state. Offline is a first-class condition on a
 * job site, so queued publishes read as "waiting for signal — showing device data" rather
 * than pretending to be synced.
 */
@Composable
internal fun OwnerSyncStateRow(
    queuedOutboxCount: Int,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    val syncState = if (queuedOutboxCount > 0) {
        "$queuedOutboxCount queued · waiting for signal — showing device data"
    } else {
        "All changes synced"
    }
    Text(
        text = "$date · $syncState",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(horizontal = 20.dp)
            .testTag("ownerSyncState"),
    )
}

/** Status → semantic color. Never the only signal: every use pairs it with a label and icon. */
@Composable
internal fun OwnerStatus.statusColor(): Color = when (this) {
    OwnerStatus.ON_TRACK ->
        if (DemoProjectRepository.darkTheme) PresenceOnSite else LightPresenceOnSite
    OwnerStatus.AT_RISK -> MaterialTheme.colorScheme.tertiary
    OwnerStatus.BEHIND -> MaterialTheme.colorScheme.error
}

/**
 * The status chip on each topic card: icon + plain-language label + color — three channels,
 * so the state survives bright sunlight, color-vision differences, and grayscale.
 */
@Composable
internal fun OwnerStatusSummary(status: OwnerStatus, modifier: Modifier = Modifier) {
    val color = status.statusColor()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                OwnerStatus.ON_TRACK -> Icons.Filled.CheckCircle
                OwnerStatus.AT_RISK -> Icons.Filled.Warning
                OwnerStatus.BEHIND -> Icons.Filled.Error
            },
            contentDescription = null, // The label text right beside it carries the meaning.
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * One Owner decision topic. Layout: takeaway first (the answer), visualization second, source
 * numbers and freshness last — a 3–5 second glance, with the whole card tapping through to the
 * supporting detail ([openLabel] names the destination for TalkBack).
 */
@Composable
internal fun OwnerDecisionTopicCard(
    title: String,
    status: OwnerStatus?,
    takeaway: String,
    metaLine: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    openLabel: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (onOpen != null) {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
                } else {
                    Modifier
                },
            )
            .padding(16.dp)
            .testTag(testTag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (status != null) OwnerStatusSummary(status)
        }
        Text(
            text = takeaway,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )
        content()
        Text(
            text = metaLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * Planned-vs-actual completion sparkline (see the design note on [ScheduleHealth]). Two
 * direct-labeled series — no legend, no gridlines, no axis (endpoint values are printed as
 * text, so the drawing carries shape only). The whole drawing is replaced for TalkBack by
 * [summary] via [accessibleChartSummary].
 */
@Composable
internal fun OwnerTrendChart(
    planned: List<Int>,
    actual: List<Int>,
    statusColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val plannedColor = MaterialTheme.colorScheme.outline
    Column(modifier = modifier.accessibleChartSummary(summary)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            // One shared linear scale across both series (with 1-pt headroom) so the gap
            // between the lines is true; values are read from the labels, not the axis.
            val all = planned + actual
            val min = (all.minOrNull() ?: 0) - 1
            val max = (all.maxOrNull() ?: 100) + 1
            fun y(value: Int): Float =
                size.height - (value - min).toFloat() / (max - min).toFloat() * size.height

            fun pathFor(series: List<Int>): Path = Path().apply {
                if (series.isEmpty()) return@apply
                val stepX = if (series.size > 1) size.width / (series.size - 1) else 0f
                moveTo(0f, y(series.first()))
                series.forEachIndexed { index, value -> lineTo(index * stepX, y(value)) }
            }
            drawPath(pathFor(planned), color = plannedColor, style = Stroke(width = 2.dp.toPx()))
            drawPath(pathFor(actual), color = statusColor, style = Stroke(width = 3.dp.toPx()))
            actual.lastOrNull()?.let { last ->
                drawCircle(color = statusColor, radius = 4.dp.toPx(), center = Offset(size.width, y(last)))
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = "Plan ${planned.lastOrNull() ?: 0}%",
                style = MaterialTheme.typography.labelMedium,
                color = plannedColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Actual ${actual.lastOrNull() ?: 0}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Approved-vs-forecast bars from a shared zero baseline (see the design note on
 * [BudgetExposure]) — two magnitudes, directly labeled, deliberately not a gauge or donut.
 */
@Composable
internal fun OwnerBudgetComparison(
    budget: BudgetExposure,
    statusColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val maxValue = maxOf(budget.approvedMillions, budget.forecastMillions)
    Column(modifier = modifier.accessibleChartSummary(summary)) {
        OwnerBarRow(
            label = "Approved",
            valueLabel = formatMillions(budget.approvedMillions),
            fraction = (budget.approvedMillions / maxValue).toFloat(),
            color = MaterialTheme.colorScheme.outline,
        )
        OwnerBarRow(
            label = "Forecast (est.)",
            valueLabel = formatMillions(budget.forecastMillions),
            fraction = (budget.forecastMillions / maxValue).toFloat(),
            color = statusColor,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Sorted category bars for Quality & approvals (see the design note on [ownerQualityBreakdown]). */
@Composable
internal fun OwnerCategoryBars(
    breakdown: List<QualityCategoryCount>,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val maxCount = breakdown.maxOfOrNull { it.count } ?: 0
    Column(modifier = modifier.accessibleChartSummary(summary)) {
        if (breakdown.isEmpty()) {
            Text(
                text = "No open items.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        breakdown.forEachIndexed { index, category ->
            OwnerBarRow(
                label = category.label,
                valueLabel = category.count.toString(),
                fraction = if (maxCount == 0) 0f else category.count.toFloat() / maxCount,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (index == 0) Modifier else Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** One labeled horizontal bar: text label + printed value, track from zero. Purely comparative. */
@Composable
private fun OwnerBarRow(
    label: String,
    valueLabel: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

/**
 * The Decisions-needed exception list: urgency-ranked rows, critical first, each carrying its
 * one-line impact and a printed next action, tapping through to the record or thread.
 */
@Composable
internal fun OwnerExceptionList(
    items: List<OwnerDecisionItem>,
    onOpenRecord: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (items.isEmpty()) {
            Text(
                text = "Nothing needs a decision right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp) // One-handed, gloved-thumb target.
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = item.nextAction, role = Role.Button) {
                        when {
                            item.recordId != null -> onOpenRecord(item.recordId)
                            item.topicId != null -> onOpenTopic(item.topicId)
                        }
                    }
                    .padding(vertical = 6.dp)
                    .testTag("ownerDecision_${item.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (item.critical) Icons.Filled.Error else Icons.Filled.Warning,
                    contentDescription = if (item.critical) "Critical" else "Needs attention",
                    tint = if (item.critical) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = item.impactLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${item.nextAction} ›",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * One live delay, Owner-legible: what stopped, where, why, who owns lifting it, and when
 * resolution is expected — pulled from the backing [FieldRecord] when there is one. Urgency
 * is carried by the "Blocking" label + icon + color together, never color alone.
 */
@Composable
internal fun DelayBlockerCard(
    item: StreamItem,
    record: FieldRecord?,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    val impact = record?.let { delayImpactLine(it, nowMillis) } ?: item.subtitle
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (onOpen != null) {
                    Modifier.clickable(onClickLabel = "Open delay detail", role = Role.Button, onClick = onOpen)
                } else {
                    Modifier
                },
            )
            .padding(16.dp)
            .testTag("ownerDelay_${item.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null, // "Blocking" text beside it carries the state.
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Blocking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = impact,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * AccessibleChartSummary: replaces a drawing's contents with one spoken sentence for
 * TalkBack (e.g. "Schedule: 62 percent complete, 4 percentage points behind plan…"), so a
 * chart is never silent and never read shape-by-shape.
 */
internal fun Modifier.accessibleChartSummary(summary: String): Modifier =
    semantics(mergeDescendants = true) { contentDescription = summary }
