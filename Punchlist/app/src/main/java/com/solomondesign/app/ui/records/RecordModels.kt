package com.solomondesign.app.ui.records

/**
 * Unified field-record domain for the three capture-style tools — Issues, Incidents, and the
 * Punch list. One model and one create form serve all three: the categories differ only in
 * naming, type options, and how they publish (see `DemoProjectRepository.addRecord`).
 *
 * IMPORTANT: this file must stay free of `androidx.compose` imports so the model layer runs
 * under `./gradlew testDebugUnitTest` — the same JVM split as `AppChrome`/`PlanSheetModels`.
 */
enum class RecordCategory(
    /** Route-safe segment used in `records/create/{category}`. */
    val routeId: String,
    val label: String,
    /** Tool/list title, e.g. Tools → "Punch list". */
    val pluralLabel: String,
    /** Create-form title. "New issue" is pinned by `AppNavHostTest` — do not reword. */
    val screenTitle: String,
    val typeOptions: List<String>,
    /**
     * Types that block work by default — the prototype's stand-in for the admin-configurable
     * per-type policy. Narrow on purpose: only stop-work-grade conditions (imminent safety
     * hazards, failed inspections, an injury). Everything else — observations, punch items,
     * RFIs — is "officially logged and active", not "work must stop"; the reporter flips the
     * Blocks work toggle when a real constraint exists.
     */
    val blockingByDefault: Set<String> = emptySet(),
) {
    ISSUE(
        routeId = "issue",
        label = "Issue",
        pluralLabel = "Issues",
        screenTitle = "New issue",
        typeOptions = listOf(
            "Observation",
            "Quality",
            "Coordination",
            "Safety hazard",
            "Failed inspection",
            RFI_ISSUE_TYPE,
            "Damage",
        ),
        // RFIs stay off by default: they block only when the reporter ties them to a
        // scheduled task and flips the toggle.
        blockingByDefault = setOf("Safety hazard", "Failed inspection"),
    ),
    INCIDENT(
        routeId = "incident",
        label = "Incident",
        pluralLabel = "Incidents",
        screenTitle = "New incident",
        typeOptions = listOf("Near miss", "Injury", "Property damage", "Environmental"),
        blockingByDefault = setOf("Injury"),
    ),
    PUNCH(
        routeId = "punch",
        label = "Punch item",
        pluralLabel = "Punch list",
        screenTitle = "New punch item",
        typeOptions = listOf("Deficiency", "Incomplete work", "Touch-up", "Damage"),
    ),
    ;

    fun blocksByDefault(type: String): Boolean = type in blockingByDefault

    companion object {
        fun fromRouteId(routeId: String?): RecordCategory? =
            entries.firstOrNull { it.routeId == routeId }
    }
}

/**
 * The Issue type that represents an RFI in this prototype — there is no separate RFI tool
 * data model, so "aging RFIs" (the Project manager's lead section) are Issues of this type.
 */
const val RFI_ISSUE_TYPE = "RFI / design clarification"

enum class RecordSeverity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical"),
}

enum class RecordImpact(val label: String) {
    INFORMATIONAL("Informational"),
    SCHEDULE("Schedule"),
    COST("Cost"),
    QUALITY("Quality"),
    SAFETY("Safety"),
}

enum class AttachmentKind { PHOTO, FILE }

data class RecordAttachment(
    val id: String,
    val kind: AttachmentKind,
    /**
     * [AttachmentKind.PHOTO]: a `ProjectImage` id (thumbnails resolve through
     * `ProjectImageRepository`). [AttachmentKind.FILE]: the picked document's display name —
     * the prototype keeps no file contents, only the reference for the tile.
     */
    val ref: String,
)

data class FieldRecord(
    val id: String,
    val category: RecordCategory,
    val title: String,
    /** One of the category's [RecordCategory.typeOptions]. */
    val type: String,
    val description: String,
    val location: String,
    val eventDateMillis: Long,
    /** Crew ids (see `DemoProjectRepository.crew`). */
    val assigneeIds: List<String>,
    val attachments: List<RecordAttachment>,
    val createdAtMillis: Long,
    val authorName: String,
    val severity: RecordSeverity = RecordSeverity.MEDIUM,
    val impact: RecordImpact = RecordImpact.INFORMATIONAL,
    /**
     * The explicit, auditable blocking status. Creating a record never stops work by itself:
     * this is false unless the reporter flipped the toggle or the type's configured default
     * (see [RecordCategory.blockingByDefault]) turned it on. Only blocking records land on
     * Today's Blockers — and they block the scoped task/trade/work package below, never the
     * whole crew.
     */
    val blocksWork: Boolean = false,
    // Blocking scope + audit trail. Meaningful only when [blocksWork]; kept empty otherwise.
    val affectedTrade: String = "",
    val affectedTask: String = "",
    val workPackage: String = "",
    val blockingReason: String = "",
    val expectedResolutionMillis: Long? = null,
    /** Crew id of who gets escalated to if the block isn't resolved in time. */
    val escalationContactId: String = "",
    /** Who may lift the block — e.g. "Superintendent" (see [RESOLUTION_AUTHORITIES]). */
    val resolutionAuthority: String = "",
    /** Whether affected crew must acknowledge the block before starting nearby work. */
    val acknowledgementRequired: Boolean = false,
)

/**
 * The Superintendent's Today ordering for open records: blocking records first (they hold
 * work and usually name the Superintendent as resolution authority), then by severity, then
 * newest. Every record in this prototype is open — there is no closed/resolved status yet —
 * so this orders rather than filters. Pure so it is JVM-unit-testable.
 */
fun List<FieldRecord>.attentionOrder(): List<FieldRecord> =
    sortedWith(
        compareByDescending<FieldRecord> { it.blocksWork }
            .thenByDescending { it.severity.ordinal }
            .thenByDescending { it.createdAtMillis },
    )

/**
 * The Project manager's lead section: RFI-type issues, OLDEST first — an RFI gets more
 * urgent the longer it sits unanswered, the opposite of every newest-first capture list.
 * Pure so it is JVM-unit-testable.
 */
fun List<FieldRecord>.agingRfis(): List<FieldRecord> =
    filter { it.category == RecordCategory.ISSUE && it.type == RFI_ISSUE_TYPE }
        .sortedBy { it.createdAtMillis }

/** "Opened today", "1 day open", "6 days open". Pure so it is JVM-unit-testable. */
fun rfiAgeLabel(createdAtMillis: Long, nowMillis: Long): String {
    val days = ((nowMillis - createdAtMillis).coerceAtLeast(0L)) / 86_400_000L
    return when (days) {
        0L -> "Opened today"
        1L -> "1 day open"
        else -> "$days days open"
    }
}

/** The demo project's known locations — same options the old quick-issue form offered. */
val RECORD_LOCATIONS = listOf("Area B", "Column 4", "Level 2")

/** Who can clear a blocking record. In the real product this comes from project roles. */
val RESOLUTION_AUTHORITIES = listOf("Superintendent", "Safety manager", "QA/QC", "Project manager")

/** Trades a block can be scoped to; mirrors the demo crew plus general conditions. */
val AFFECTED_TRADES = listOf("Framing (Carpentry)", "Electrical", "Plumbing", "Mechanical", "General conditions")

/** Demo work packages a block can be scoped to instead of a single task. */
val WORK_PACKAGES = listOf("WP-02 Structural framing", "WP-04 MEP rough-in", "WP-07 Interior finishes")
