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
) {
    ISSUE(
        routeId = "issue",
        label = "Issue",
        pluralLabel = "Issues",
        screenTitle = "New issue",
        typeOptions = listOf("Quality", "Safety", "Coordination", "Damage"),
    ),
    INCIDENT(
        routeId = "incident",
        label = "Incident",
        pluralLabel = "Incidents",
        screenTitle = "New incident",
        typeOptions = listOf("Near miss", "Injury", "Property damage", "Environmental"),
    ),
    PUNCH(
        routeId = "punch",
        label = "Punch item",
        pluralLabel = "Punch list",
        screenTitle = "New punch item",
        typeOptions = listOf("Deficiency", "Incomplete work", "Touch-up", "Damage"),
    ),
    ;

    companion object {
        fun fromRouteId(routeId: String?): RecordCategory? =
            entries.firstOrNull { it.routeId == routeId }
    }
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
)

/** The demo project's known locations — same options the old quick-issue form offered. */
val RECORD_LOCATIONS = listOf("Area B", "Column 4", "Level 2")
