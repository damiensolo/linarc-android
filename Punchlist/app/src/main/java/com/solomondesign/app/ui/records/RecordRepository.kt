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
