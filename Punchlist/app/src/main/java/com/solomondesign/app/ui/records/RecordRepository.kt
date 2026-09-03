package com.solomondesign.app.ui.records

import androidx.compose.runtime.mutableStateListOf

/**
 * In-memory store behind the Issues / Incidents / Punch list tools, mirroring
 * `ProjectImageRepository`: process-scoped snapshot state, seeded so the tool lists demo
 * without first creating anything. Publishing side effects (Today, pins, outbox) live in
 * `DemoProjectRepository.addRecord`, not here.
 */
object RecordRepository {

    private val _records = mutableStateListOf<FieldRecord>()

    /** Newest first — [add] prepends, like every capture surface in this prototype. */
    val records: List<FieldRecord> get() = _records

    fun byCategory(category: RecordCategory): List<FieldRecord> =
        _records.filter { it.category == category }

    fun find(id: String): FieldRecord? = _records.firstOrNull { it.id == id }

    fun add(record: FieldRecord) {
        _records.add(0, record)
    }

    fun clear() {
        _records.clear()
        seed()
    }

    init {
        seed()
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        _records.addAll(
            listOf(
                // The one seeded blocker: the reporter marked it blocks-work (a dependency that
                // must resolve before close-in), so it also shows on Today's Blockers.
                FieldRecord(
                    id = "rec-seed-issue",
                    category = RecordCategory.ISSUE,
                    title = "Med-gas conflict at Column 4",
                    type = "Coordination",
                    description = "Branch line clashes with the med-gas drop; needs re-route before close-in.",
                    location = "Column 4",
                    eventDateMillis = now - 4 * hour,
                    assigneeIds = listOf("maria-chen"),
                    attachments = listOf(
                        RecordAttachment("att-seed-issue", AttachmentKind.PHOTO, "img-col4-conflict"),
                    ),
                    createdAtMillis = now - 4 * hour,
                    authorName = "Sam Reyes",
                    severity = RecordSeverity.HIGH,
                    impact = RecordImpact.SCHEDULE,
                    blocksWork = true,
                    affectedTrade = "Electrical",
                    // A real FieldTaskRepository task: the block scopes to this one task.
                    affectedTask = "Rough-in branch circuits, exam 5–8",
                    workPackage = "WP-04 MEP rough-in",
                    blockingReason = "Close-in can't proceed until the branch line re-routes around the med-gas drop.",
                    expectedResolutionMillis = now + 48 * hour,
                    escalationContactId = "sam-reyes",
                    resolutionAuthority = "Superintendent",
                    acknowledgementRequired = true,
                ),
                // Logged, not blocking: the near miss is documented and the area is taped off,
                // but no scheduled work is stopped — it stays off Today's Blockers.
                FieldRecord(
                    id = "rec-seed-incident",
                    category = RecordCategory.INCIDENT,
                    title = "Near miss — falling conduit",
                    type = "Near miss",
                    description = "A conduit stick slid off the lift deck; nobody below, area taped off.",
                    location = "Level 2",
                    eventDateMillis = now - 26 * hour,
                    assigneeIds = listOf("sam-reyes"),
                    attachments = emptyList(),
                    createdAtMillis = now - 26 * hour,
                    authorName = "Dave Miller",
                    severity = RecordSeverity.HIGH,
                    impact = RecordImpact.SAFETY,
                ),
                // The seeded Outbox's "Issue: Missing guardrail" entry deep-links here — the
                // queue must tap through to detail from first launch. Logged, not blocking:
                // Safety hazard defaults to blocks-work on the create form, but the reporter
                // cleared it because the opening is barricaded and no scheduled work stops —
                // same documented-and-contained story as the near-miss seed above. (Blocking
                // would also rank it in the Owner's Decisions needed, which the dashboard
                // seeds pin to exactly one critical row.)
                FieldRecord(
                    id = "rec-seed-guardrail",
                    category = RecordCategory.ISSUE,
                    title = "Missing guardrail",
                    type = "Safety hazard",
                    description = "Guardrail section removed at the slab edge for material " +
                        "hoisting; opening is taped off and coned. Reinstall before deck work resumes.",
                    location = "Level 2",
                    eventDateMillis = now - 6 * hour,
                    assigneeIds = listOf("dave-miller"),
                    attachments = emptyList(),
                    createdAtMillis = now - 6 * hour,
                    authorName = "Alex Rivera",
                    severity = RecordSeverity.HIGH,
                    impact = RecordImpact.SAFETY,
                ),
                // Today's seeded "Frame inspection" row deep-links to this record
                // (DemoProjectRepository.seed sets relatedFieldRecordId = this id).
                FieldRecord(
                    id = "rec-seed-inspection",
                    category = RecordCategory.PUNCH,
                    title = "Frame inspection",
                    type = "Incomplete work",
                    description = "Gridline C walk: add blocking at the exam 4 header and " +
                        "re-nail two shear panels before drywall.",
                    location = "Area B",
                    eventDateMillis = now - hour,
                    assigneeIds = listOf("hector-ortiz"),
                    attachments = emptyList(),
                    createdAtMillis = now - hour,
                    authorName = "Hector Ortiz",
                ),
                // Aging RFIs (logged-not-blocking, per the RFI default): the Project manager
                // view leads with these, oldest first. Both tie into the existing demo
                // narrative — RFI-118 is named in the med-gas collab thread and the Column 4
                // task note; RFI-121 answers the headwall-heights question Dave asked.
                FieldRecord(
                    id = "rec-seed-rfi-118",
                    category = RecordCategory.ISSUE,
                    title = "RFI-118 — Med-gas re-route at Column 4",
                    type = RFI_ISSUE_TYPE,
                    description = "Requested routing direction for the med-gas drop vs the " +
                        "4\" storm conflict. Awaiting A/E response.",
                    location = "Column 4",
                    eventDateMillis = now - 72 * hour,
                    assigneeIds = listOf("sam-reyes"),
                    attachments = emptyList(),
                    createdAtMillis = now - 72 * hour,
                    authorName = "Alex Rivera",
                    severity = RecordSeverity.HIGH,
                    impact = RecordImpact.SCHEDULE,
                ),
                FieldRecord(
                    id = "rec-seed-rfi-121",
                    category = RecordCategory.ISSUE,
                    title = "RFI-121 — Headwall backing heights",
                    type = RFI_ISSUE_TYPE,
                    description = "Drawings show 60\" AFF, submittal shows 54\". Need " +
                        "direction before the backing install in rooms 5–8.",
                    location = "Level 2",
                    eventDateMillis = now - 144 * hour,
                    assigneeIds = listOf("dave-miller"),
                    attachments = emptyList(),
                    createdAtMillis = now - 144 * hour,
                    authorName = "Alex Rivera",
                    severity = RecordSeverity.MEDIUM,
                    impact = RecordImpact.QUALITY,
                ),
                FieldRecord(
                    id = "rec-seed-punch",
                    category = RecordCategory.PUNCH,
                    title = "Touch up drywall at exam 6",
                    type = "Touch-up",
                    description = "Corner bead scuffed during rough-in; patch and repaint.",
                    location = "Level 2",
                    eventDateMillis = now - 20 * hour,
                    assigneeIds = listOf("dave-miller"),
                    attachments = listOf(
                        RecordAttachment("att-seed-punch", AttachmentKind.PHOTO, "img-conduit-exam6"),
                    ),
                    createdAtMillis = now - 20 * hour,
                    authorName = "Hector Ortiz",
                ),
            ),
        )
    }
}
