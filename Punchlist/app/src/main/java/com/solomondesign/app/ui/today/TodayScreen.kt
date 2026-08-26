package com.solomondesign.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.CrewMember
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.StreamKind
import com.solomondesign.app.ui.demo.badgeColor
import com.solomondesign.app.ui.demo.statusLabel
import com.solomondesign.app.ui.designsystem.FieldCollapsibleSectionHeader
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.profile.ProfileAvatarButton
import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.collab.CollabTopic
import com.solomondesign.app.ui.demo.StreamItem
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.records.RecordSeverity
import com.solomondesign.app.ui.records.agingRfis
import com.solomondesign.app.ui.records.attentionOrder
import com.solomondesign.app.ui.records.rfiAgeLabel
import com.solomondesign.app.ui.tasks.FieldTask
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.tasks.forTrade
import com.solomondesign.app.ui.tasks.label
import com.solomondesign.app.ui.tasks.statusColor
import com.solomondesign.app.ui.theme.AvatarPalette
import com.solomondesign.app.ui.timecards.formatHours
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenVoiceLog: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onSwitchProject: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val dayStarted = DemoProjectRepository.dayStarted
    val streamItems = DemoProjectRepository.streamItems.toList()
    val crew = DemoProjectRepository.crew
    var showStartMyDay by remember { mutableStateOf(false) }
    // Same objects, reordered by persona: Crew leads with its own work, the Superintendent
    // with blockers and open records, the Project manager with aging RFIs — for those the
    // roster demotes to a collapsed "On site today" at the bottom instead of leading the
    // page. Foreman keeps the original layout: Start My Day, roster first. The Owner is the
    // one persona that OMITS objects, per the spec's "no time cards or voice log" (extended
    // 2026-08-25 to crew operations): its whole page is the confidence dashboard in
    // OwnerTodaySections.kt — progress photos, four decision topics, delays — and nothing else.
    val isCrewView = persona == FieldPersona.CREW
    val isSuperView = persona == FieldPersona.SUPERINTENDENT
    val isPmView = persona == FieldPersona.PROJECT_MANAGER
    val isOwnerView = persona == FieldPersona.OWNER
    val isSubView = persona == FieldPersona.SUBCONTRACTOR
    // Both Owner layouts stay demoable (Settings → Demo → "Owner Today layout"): the classic
    // v1 view keeps the shared Delays rows and collapsed roster the dashboard dropped.
    val isClassicOwnerView =
        isOwnerView && DemoProjectRepository.ownerTodayVariant == OwnerTodayVariant.CLASSIC
    val isForemanView = !isCrewView && !isSuperView && !isPmView && !isOwnerView && !isSubView
    var crewExpanded by remember(persona) { mutableStateOf(isForemanView) }
    val crewViewMember = DemoProjectRepository.crewViewMember
    val myTasks = crewViewMember?.let { member ->
        FieldTaskRepository.tasks.filter { it.assigneeId == member.id }
    }.orEmpty()
    // Superintendent focus: every open record, blockers and highest severity first. There is
    // no closed status in this prototype, so the whole store is the open set.
    val openRecords = if (isSuperView) RecordRepository.records.attentionOrder() else emptyList()
    // Project manager focus: RFI-type issues aging oldest-first, and decision threads.
    // The Owner shares the decision threads — approvals are the owner's half of "decisions".
    val agingRfis = if (isPmView) RecordRepository.records.agingRfis() else emptyList()
    val decisionTopics = if (isPmView || isOwnerView) CollabRepository.topics else emptyList()
    // Owner focus: progress, not operations — captured photos and videos only.
    val progressMedia = if (isOwnerView) {
        streamItems.filter { it.kind == StreamKind.PHOTO || it.kind == StreamKind.VIDEO }
    } else {
        emptyList()
    }
    // Subcontractor focus: the whole trade's tasks (assigned or not — a sub owns the trade's
    // scope), plus the Request Inspection action.
    val subMember = DemoProjectRepository.subcontractorMember
    val subTasks = subMember?.let { FieldTaskRepository.tasks.forTrade(it.trade) }.orEmpty()
    var showRequestInspection by remember { mutableStateOf(false) }

    // Issued ≠ blocked: Blockers shows only rows explicitly marked blocking (records with the
    // Blocks work toggle, dictated delays). Everything else — including logged-not-blocking
    // issues — is activity, not a stoppage.
    val blockers = streamItems.filter { it.blocking }
    val captures = streamItems.filterNot { it.blocking }

    if (showStartMyDay) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStartMyDay = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            StartMyDaySheetContent(
                crew = crew,
                onConfirm = {
                    DemoProjectRepository.confirmStartMyDay()
                    showStartMyDay = false
                },
            )
        }
    }

    if (showRequestInspection) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRequestInspection = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            RequestInspectionSheetContent(
                tasks = subTasks,
                onRequest = { task ->
                    DemoProjectRepository.requestInspection(task)
                    showRequestInspection = false
                },
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("todayScreen"),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            FieldPageHeader(
                title = "Today",
                subtitle = "${persona.displayName} · ${DemoProjectRepository.AREA}",
                projectName = DemoProjectRepository.PROJECT_NAME,
                onSwitchProject = onSwitchProject,
                onOpenSettings = onOpenSettings,
                trailing = { ProfileAvatarButton(onClick = onOpenProfile) },
            )
        }

        if (isCrewView) {
            // Crew focus: my assignment, start/end shift, take a photo. The shift clock
            // replaces the Foreman's Start My Day ritual; ending a shift logs a real time
            // entry (queued to the Outbox like any publish).
            item {
                MyShiftCard(
                    memberName = crewViewMember?.name.orEmpty(),
                    memberTrade = crewViewMember?.trade.orEmpty(),
                )
            }
            item { FieldSectionLabel("My assignment") }
            if (myTasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks assigned to you today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(myTasks, key = { it.id }) { task ->
                    // Rows open the same Field task detail the tool uses — status control,
                    // checklist and all — so the assignment never dead-ends on Today.
                    FieldWorkRow(
                        title = task.title,
                        subtitle = "${task.location} · ${task.status.label()} · ${task.dueLabel}",
                        statusColor = task.status.statusColor(),
                        enabled = true,
                        onClick = { onOpenTask(task.id) },
                        modifier = Modifier.testTag("myTask_${task.id}"),
                    )
                }
            }
        } else if (isPmView) {
            // Aging RFIs first — the Project manager's whole reason to open the app. Oldest
            // first, because an unanswered RFI gets more urgent with age, and every row
            // carries its age. Rows open the record detail the Issues tool owns.
            item { FieldSectionLabel("Aging RFIs") }
            if (agingRfis.isEmpty()) {
                item {
                    Text(
                        text = "No open RFIs. Issues filed as RFI / design clarification land here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                val nowMillis = System.currentTimeMillis()
                items(agingRfis, key = { "rfi_${it.id}" }) { record ->
                    val age = rfiAgeLabel(record.createdAtMillis, nowMillis)
                    FieldWorkRow(
                        title = record.title,
                        subtitle = "${record.location} · $age",
                        statusColor = if (age.startsWith("Opened") || age.startsWith("1 ")) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        enabled = true,
                        onClick = { onOpenRecord(record.id) },
                        modifier = Modifier.testTag("agingRfi_${record.id}"),
                    )
                }
            }
        } else if (isClassicOwnerView) {
            // The original v1 Owner layout, kept demoable for side-by-side comparison with the
            // dashboard: progress photos, then the flat Decisions & discussions topic list.
            // Delays (shared rows, relabeled) and the collapsed roster render below via the
            // shared sections, which re-include this variant.
            item { FieldSectionLabel("Progress photos") }
            if (progressMedia.isEmpty()) {
                item {
                    Text(
                        text = "No progress photos yet. Captured photos and videos land here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
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
            item { FieldSectionLabel("Decisions & discussions") }
            items(decisionTopics, key = { "topic_${it.id}" }) { topic ->
                DecisionTopicRow(topic = topic, onOpenTopic = onOpenTopic)
            }
        } else if (isOwnerView) {
            // Owner focus: confidence, not operations — progress photos, then exactly four
            // decision topics (schedule, budget, quality, decisions), then delays. The whole
            // page lives in OwnerTodaySections.kt; the shared Blockers / Recent captures /
            // roster sections below all skip the Owner view, which (per the spec's sanctioned
            // Owner removals) shows no voice logs, time cards, or crew operations at all.
            ownerTodayContent(
                progressMedia = progressMedia,
                records = RecordRepository.records,
                topics = decisionTopics,
                blockers = blockers,
                queuedOutboxCount = DemoProjectRepository.queuedOutboxCount,
                nowMillis = System.currentTimeMillis(),
                onOpenImage = onOpenImage,
                onOpenVideo = onOpenVideo,
                onOpenRecord = onOpenRecord,
                onOpenTopic = onOpenTopic,
                onOpenVoiceLog = onOpenVoiceLog,
                onOpenTask = onOpenTask,
            )
        } else if (isSubView) {
            // Subcontractor focus: assigned work plus Request Inspection. The card mirrors
            // the Crew view's My shift ritual; requesting publishes a Today row linked to the
            // task and queues one Outbox entry (offline-first, like every publish).
            item {
                RequestInspectionCard(
                    memberName = subMember?.name.orEmpty(),
                    trade = subMember?.trade.orEmpty(),
                    onRequest = { showRequestInspection = true },
                )
            }
            item { FieldSectionLabel("My work") }
            if (subTasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks for your trade today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(subTasks, key = { "subTask_${it.id}" }) { task ->
                    FieldWorkRow(
                        title = task.title,
                        subtitle = "${task.location} · ${task.status.label()} · ${task.dueLabel}",
                        statusColor = task.status.statusColor(),
                        enabled = true,
                        onClick = { onOpenTask(task.id) },
                        modifier = Modifier.testTag("subTask_${task.id}"),
                    )
                }
            }
        } else if (isForemanView && !dayStarted) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                        .testTag("startMyDayCard"),
                ) {
                    Text("Start My Day", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Crew, Area B, and weather are ready. Confirm to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    AppButton(
                        text = "Start My Day",
                        onClick = { showStartMyDay = true },
                    )
                }
            }
        } else if (isForemanView) {
            item {
                Text(
                    text = "Day started · ${DemoProjectRepository.AREA}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        // The Superintendent view adds no lead card at all: Blockers, the section that most
        // needs the super (the seeded blocker names Superintendent as resolution authority),
        // opens the page.

        if (isForemanView) {
            item {
                FieldCollapsibleSectionHeader(
                    title = "Crew",
                    count = crew.size,
                    expanded = crewExpanded,
                    onToggleExpanded = { crewExpanded = !crewExpanded },
                    modifier = Modifier.testTag("crewSectionHeader"),
                )
            }
            if (crewExpanded) {
                itemsIndexed(crew, key = { _, member -> member.name }) { index, member ->
                    FieldWorkRow(
                        title = member.name,
                        subtitle = "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
                        statusColor = member.presence.badgeColor(),
                        avatarName = member.name,
                        avatarColor = AvatarPalette.colorAt(index),
                        avatarPhotoRes = member.photoRes,
                    )
                }
            }
        }

        // Same blocker objects for every persona; the Project manager and classic Owner read
        // them as schedule delays, so only the label shifts. The dashboard Owner renders its
        // own enriched Delays section (DelayBlockerCard) inside ownerTodayContent, so this
        // shared one skips that variant only.
        if (!isOwnerView || isClassicOwnerView) {
            item {
                FieldSectionLabel(
                    when {
                        isPmView -> "Delays & blockers"
                        isClassicOwnerView -> "Delays"
                        else -> "Blockers"
                    },
                )
            }
            if (blockers.isEmpty()) {
                item {
                    Text(
                        text = "No blockers. Issues land here only when marked as blocking work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(blockers, key = { it.id }) { item ->
                    // Every blocker opens the thing behind it: record-backed rows open the
                    // record detail in its tool; voice-log rows open the daily log they came
                    // from.
                    FieldWorkRow(
                        title = item.title,
                        subtitle = item.subtitle + " · " + formatTimestamp(item.timestampMillis),
                        statusColor = if (item.kind == StreamKind.ISSUE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        enabled = item.relatedFieldRecordId != null || item.relatedRecordId != null,
                        onClick = {
                            when {
                                item.relatedFieldRecordId != null -> onOpenRecord(item.relatedFieldRecordId)
                                item.relatedRecordId != null -> onOpenVoiceLog(item.relatedRecordId)
                            }
                        },
                        modifier = Modifier.testTag("streamItem_${item.id}"),
                    )
                }
            }
        }

        if (isSuperView) {
            // Open issues / inspections first — the Superintendent's whole reason to open the
            // app. Rows open the same record detail the tools own, so nothing dead-ends.
            item { FieldSectionLabel("Open issues & inspections") }
            if (openRecords.isEmpty()) {
                item {
                    Text(
                        text = "No open records. New issues, incidents, and punch items land here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(openRecords, key = { "openRecord_${it.id}" }) { record ->
                    FieldWorkRow(
                        title = record.title,
                        subtitle = listOf(
                            record.category.label,
                            record.location,
                            record.severity.label + if (record.blocksWork) " · Blocks work" else "",
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        statusColor = when {
                            record.blocksWork -> MaterialTheme.colorScheme.error
                            record.severity == RecordSeverity.CRITICAL ||
                                record.severity == RecordSeverity.HIGH ->
                                MaterialTheme.colorScheme.error
                            record.severity == RecordSeverity.MEDIUM ->
                                MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        enabled = true,
                        onClick = { onOpenRecord(record.id) },
                        modifier = Modifier.testTag("openRecord_${record.id}"),
                    )
                }
            }
        }

        if (isPmView) {
            // Decisions live in the field's own threads: the Collaboration topics, most
            // recently active first, unread flagged. (The Owner shares this section but
            // renders it up top, before delays — see the owner lead branch.)
            item { FieldSectionLabel("Decisions & discussions") }
            items(decisionTopics, key = { "topic_${it.id}" }) { topic ->
                DecisionTopicRow(topic = topic, onOpenTopic = onOpenTopic)
            }
        }

        if (!isOwnerView) {
            item { FieldSectionLabel("Recent captures") }
            items(captures, key = { it.id }) { item ->
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

        if ((!isForemanView && !isOwnerView) || isClassicOwnerView) {
            // Same roster object the Foreman leads with, demoted for Crew and Superintendent:
            // collapsed by default at the bottom — reorder and de-emphasize, never remove.
            // The dashboard Owner is the one sanctioned exception (2026-08-25): crew
            // operations stay off its Today entirely, alongside the spec's "no time cards or
            // voice log". The classic v1 Owner variant predates that and keeps the roster.
            item {
                FieldCollapsibleSectionHeader(
                    title = "On site today",
                    count = crew.size,
                    expanded = crewExpanded,
                    onToggleExpanded = { crewExpanded = !crewExpanded },
                    modifier = Modifier.testTag("crewSectionHeader"),
                )
            }
            if (crewExpanded) {
                itemsIndexed(crew, key = { _, member -> member.name }) { index, member ->
                    FieldWorkRow(
                        title = member.name,
                        subtitle = "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
                        statusColor = member.presence.badgeColor(),
                        avatarName = member.name,
                        avatarColor = AvatarPalette.colorAt(index),
                        avatarPhotoRes = member.photoRes,
                    )
                }
            }
        }
    }
}

/**
 * The Crew view's day ritual — start/end shift instead of the Foreman's Start My Day.
 * Ending a shift logs a real time entry on the crew-view member's time card (rounded up to
 * the quarter hour) and queues it to the Outbox; the card then shows the receipt and is
 * ready to start the next shift.
 */
@Composable
private fun MyShiftCard(
    memberName: String,
    memberTrade: String,
    modifier: Modifier = Modifier,
) {
    val shiftStartedAt = DemoProjectRepository.shiftStartedAtMillis
    val lastShiftHours = DemoProjectRepository.lastShiftHours
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .testTag("myShiftCard"),
    ) {
        Text("My shift", style = MaterialTheme.typography.titleLarge)
        Text(
            text = listOf(memberName, memberTrade).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = when {
                shiftStartedAt != null -> "On shift since ${formatTimestamp(shiftStartedAt)}"
                lastShiftHours != null ->
                    "Shift logged · ${formatHours(lastShiftHours)} · queued in Outbox"
                else -> "Hours land on your time card and queue to the Outbox."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (shiftStartedAt == null) {
            AppButton(
                text = "Start shift",
                onClick = { DemoProjectRepository.startShift() },
                modifier = Modifier.testTag("startShiftButton"),
            )
        } else {
            AppButton(
                text = "End shift",
                onClick = { DemoProjectRepository.endShift() },
                modifier = Modifier.testTag("endShiftButton"),
            )
        }
    }
}

/**
 * The Subcontractor's day ritual — assigned work plus Request Inspection. The receipt line
 * mirrors the Crew view's My shift card; the button opens a Pattern C sheet to pick which
 * task the inspection covers.
 */
@Composable
private fun RequestInspectionCard(
    memberName: String,
    trade: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastRequest = DemoProjectRepository.lastInspectionRequestTitle
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .testTag("requestInspectionCard"),
    ) {
        Text("Inspections", style = MaterialTheme.typography.titleLarge)
        Text(
            text = listOf(memberName, trade).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = if (lastRequest != null) {
                "Requested · $lastRequest · queued in Outbox"
            } else {
                "Requests land on Today and queue to the Outbox."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        AppButton(
            text = "Request inspection",
            onClick = onRequest,
            modifier = Modifier.testTag("requestInspectionButton"),
        )
    }
}

/** Pattern C sheet: pick which of the trade's tasks the inspection covers — one tap. */
@Composable
private fun RequestInspectionSheetContent(
    tasks: List<FieldTask>,
    onRequest: (FieldTask) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Request inspection", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        FieldSectionLabel("Which task is ready?")
        if (tasks.isEmpty()) {
            Text(
                text = "No tasks for your trade to inspect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        tasks.forEach { task ->
            FieldWorkRow(
                title = task.title,
                subtitle = "${task.location} · ${task.status.label()}",
                statusColor = task.status.statusColor(),
                enabled = true,
                onClick = { onRequest(task) },
                modifier = Modifier.testTag("inspectTask_${task.id}"),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One capture-stream row — photo, video, voice log, or record-backed task. Photo rows carry a
 * live thumbnail and deep-link into the full-screen image viewer (deleting there removes this
 * row too, so a linked id never dangles); video rows carry a camcorder glyph and deep-link
 * into playback; record-backed rows open their tool's record detail; log rows open playback.
 * Shared by the Foreman/Super/PM "Recent captures" section and the Owner's "Progress photos"
 * (internal so OwnerTodaySections.kt reuses the exact same row).
 */
@Composable
internal fun CaptureStreamRow(
    item: StreamItem,
    onOpenVoiceLog: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkedImage = item.relatedImageId?.let { ProjectImageRepository.find(it) }
    FieldWorkRow(
        title = item.title,
        subtitle = item.subtitle + " · " + formatTimestamp(item.timestampMillis),
        statusColor = if (item.kind == StreamKind.PHOTO || item.kind == StreamKind.VIDEO) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        leading = when {
            linkedImage != null -> {
                { ImageThumbnail(image = linkedImage) }
            }

            item.kind == StreamKind.VIDEO -> {
                { VideoGlyph() }
            }

            else -> null
        },
        enabled = item.relatedFieldRecordId != null ||
            item.relatedVideoId != null ||
            linkedImage != null ||
            item.relatedRecordId != null ||
            item.relatedTaskId != null,
        onClick = {
            when {
                item.relatedFieldRecordId != null -> onOpenRecord(item.relatedFieldRecordId)
                item.relatedVideoId != null -> onOpenVideo(item.relatedVideoId)
                linkedImage != null -> onOpenImage(linkedImage.id)
                // Daily logs and voice-dictated (non-blocking) issues open their log.
                item.relatedRecordId != null -> onOpenVoiceLog(item.relatedRecordId)
                // Inspection requests open the task they were raised about.
                item.relatedTaskId != null -> onOpenTask(item.relatedTaskId)
            }
        },
        modifier = modifier.testTag("streamItem_${item.id}"),
    )
}

/**
 * One decision thread row: a Collaboration topic, unread flagged, opening the conversation
 * the Collaboration tool owns. Project manager view and the classic (v1) Owner variant; the
 * dashboard Owner reads its threads through the ranked "Decisions needed" card instead (see
 * OwnerTodaySections.kt).
 */
@Composable
private fun DecisionTopicRow(
    topic: CollabTopic,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FieldWorkRow(
        title = topic.title,
        subtitle = topic.location + if (topic.unreadCount > 0) {
            " · ${topic.unreadCount} unread"
        } else {
            ""
        },
        statusColor = if (topic.unreadCount > 0) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.outline
        },
        enabled = true,
        onClick = { onOpenTopic(topic.id) },
        modifier = modifier.testTag("decisionTopic_${topic.id}"),
    )
}

/**
 * Videos have no still frame to thumbnail (decoding one per list row is wasted work), so the
 * row leads with the same-size camcorder glyph instead — the playback screen shows the real
 * footage. Decorative: the row's own title/semantics describe the item.
 */
@Composable
private fun VideoGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StartMyDaySheetContent(
    crew: List<CrewMember>,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Start My Day", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FieldSectionLabel("Crew")
        crew.forEachIndexed { index, member ->
            FieldWorkRow(
                title = member.name,
                subtitle = "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
                statusColor = member.presence.badgeColor(),
                avatarName = member.name,
                avatarColor = AvatarPalette.colorAt(index),
                avatarPhotoRes = member.photoRes,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Weather",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            text = "Clear · 72°F · no delay",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        AppButton(text = "Confirm", onClick = onConfirm)
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
