package com.solomondesign.app.ui.records

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solomondesign.app.ui.capture.IssueDraftHolder

/**
 * The one in-progress record form, held outside the composition on purpose: attaching a photo
 * mid-form navigates away to the full-screen camera, and a singleton draft survives that round
 * trip with every field intact — no Savers, no ViewModel, matching this prototype's holder
 * pattern (`IssueDraftHolder`). Only one create form ever runs at a time.
 *
 * Entry points that seed the form (photo viewer's Create, review's Save & create) call [begin]
 * and leave [consumeStaged] true; plain entries (tool FAB, camera Issue chip) navigate without
 * calling anything and the form begins itself. Compose snapshot state throughout so the form
 * recomposes as fields change.
 */
object RecordDraft {

    var category by mutableStateOf(RecordCategory.ISSUE)
        private set
    var title by mutableStateOf("")
    var type by mutableStateOf(RecordCategory.ISSUE.typeOptions.first())
        private set
    var description by mutableStateOf("")
    var location by mutableStateOf(RECORD_LOCATIONS.first())
    var eventDateMillis by mutableLongStateOf(0L)
    var assigneeIds by mutableStateOf(listOf<String>())
        private set
    var attachments by mutableStateOf(listOf<RecordAttachment>())
        private set

    var severity by mutableStateOf(RecordSeverity.MEDIUM)
    var impact by mutableStateOf(RecordImpact.INFORMATIONAL)
    var blocksWork by mutableStateOf(false)
        private set
    var affectedTrade by mutableStateOf("")
    var affectedTask by mutableStateOf("")
    var workPackage by mutableStateOf("")
    var blockingReason by mutableStateOf("")
    var expectedResolutionMillis by mutableStateOf<Long?>(null)
    var escalationContactId by mutableStateOf("")
    var resolutionAuthority by mutableStateOf(RESOLUTION_AUTHORITIES.first())
    var acknowledgementRequired by mutableStateOf(false)

    // Progressive validation: a required field shows its error only after it loses focus or a
    // save is attempted — never on first load — and typed values are never cleared by failure.
    var titleTouched by mutableStateOf(false)
    var blockingReasonTouched by mutableStateOf(false)
    var submitAttempted by mutableStateOf(false)

    private var staged = false

    /** Drives the discard warning: a photo already attached counts as an edit. */
    val hasEdits: Boolean
        get() = title.isNotBlank() || description.isNotBlank() || attachments.isNotEmpty() ||
            blocksWork

    /**
     * Blocking must be auditable, so a record marked "blocks work" cannot be submitted without
     * saying why. Everything else about blocking (dates, scope) is optional in the prototype.
     */
    val canSubmit: Boolean
        get() = title.isNotBlank() && (!blocksWork || blockingReason.isNotBlank())

    val titleError: String?
        get() = "Enter a title".takeIf {
            title.isBlank() && (titleTouched || submitAttempted)
        }

    val blockingReasonError: String?
        get() = "Describe why work is blocked".takeIf {
            blocksWork && blockingReason.isBlank() && (blockingReasonTouched || submitAttempted)
        }

    /** Required fields still empty; drives the footer's "Complete N required fields" summary. */
    val missingRequiredCount: Int
        get() = (if (title.isBlank()) 1 else 0) +
            (if (blocksWork && blockingReason.isBlank()) 1 else 0)

    /**
     * Selecting a type re-applies the configured per-type default (the admin policy): picking
     * "Safety hazard" turns blocking on, switching back to "Observation" turns it off again.
     * The reporter can still flip the toggle afterwards — the default is a starting point,
     * not a lock.
     */
    fun selectType(next: String) {
        type = next
        setBlocking(category.blocksByDefault(next))
    }

    /** Turning blocking on also requires acknowledgement by default; off clears the flag.
     * (Named to avoid a JVM clash with [blocksWork]'s generated private setter.) */
    fun setBlocking(next: Boolean) {
        blocksWork = next
        acknowledgementRequired = next
    }

    /**
     * Resets the draft for [newCategory] and applies any seeds. For [RecordCategory.ISSUE] the
     * dictated-video hand-off ([IssueDraftHolder]) is drained too — explicit seed arguments win
     * over it. [nowMillis] is a parameter (not read from a clock here) so tests can pin it.
     */
    fun begin(
        newCategory: RecordCategory,
        nowMillis: Long,
        seedTitle: String = "",
        seedDescription: String = "",
        seedLocation: String? = null,
        seedPhotoImageIds: List<String> = emptyList(),
        seedBlocksWork: Boolean = false,
        seedBlockingReason: String = "",
        /** Pre-selects a type (must be one of the category's [RecordCategory.typeOptions],
         * ignored otherwise) — e.g. the Project engineer's Draft RFI opens the Issue form
         * already on [RFI_ISSUE_TYPE]. Applies the type's blocking default like a manual pick. */
        seedType: String? = null,
    ) {
        val dictated = if (newCategory == RecordCategory.ISSUE) IssueDraftHolder.take() else null
        category = newCategory
        // Only an explicit seed (e.g. the photo viewer's image title) fills the title — the
        // dictated hand-off carries no title at all (see IssueDraft, decided 2026-09-03).
        title = seedTitle
        description = seedDescription.ifBlank { dictated?.note.orEmpty() }
        location = (seedLocation ?: dictated?.location)
            ?.takeIf { it in RECORD_LOCATIONS }
            ?: RECORD_LOCATIONS.first()
        eventDateMillis = nowMillis
        assigneeIds = emptyList()
        attachments = seedPhotoImageIds.map { imageId ->
            RecordAttachment(id = "att-$imageId", kind = AttachmentKind.PHOTO, ref = imageId)
        }
        severity = RecordSeverity.MEDIUM
        impact = RecordImpact.INFORMATIONAL
        affectedTrade = ""
        affectedTask = ""
        workPackage = ""
        blockingReason = ""
        expectedResolutionMillis = null
        escalationContactId = ""
        resolutionAuthority = RESOLUTION_AUTHORITIES.first()
        titleTouched = false
        blockingReasonTouched = false
        submitAttempted = false
        // Last: applies the type's configured blocking default (first option is never blocking,
        // so a fresh form always starts with the toggle off). A valid seedType then wins,
        // through the same path as a manual pick so its blocking default applies too.
        selectType(newCategory.typeOptions.first())
        seedType?.takeIf { it in newCategory.typeOptions }?.let(::selectType)
        if (seedBlocksWork) {
            setBlocking(true)
            blockingReason = seedBlockingReason
        }
        staged = true
        CameraAttachmentInbox.reset()
    }

    /**
     * True exactly once after [begin] — the form calls this on a fresh entry to decide whether
     * a caller already staged the draft or it should [begin] itself.
     */
    fun consumeStaged(forCategory: RecordCategory): Boolean {
        val usable = staged && category == forCategory
        staged = false
        return usable
    }

    /** Adds a photo attachment; a photo already attached is not attached twice. */
    fun addPhoto(imageId: String) {
        if (attachments.any { it.kind == AttachmentKind.PHOTO && it.ref == imageId }) return
        attachments = attachments +
            RecordAttachment(id = "att-$imageId", kind = AttachmentKind.PHOTO, ref = imageId)
    }

    fun addFile(displayName: String) {
        attachments = attachments + RecordAttachment(
            id = "att-file-${attachments.size}-${displayName.hashCode()}",
            kind = AttachmentKind.FILE,
            ref = displayName,
        )
    }

    fun removeAttachment(attachmentId: String) {
        attachments = attachments.filterNot { it.id == attachmentId }
    }

    fun toggleAssignee(crewId: String) {
        assigneeIds = if (crewId in assigneeIds) assigneeIds - crewId else assigneeIds + crewId
    }

    fun toRecord(id: String, nowMillis: Long, authorName: String): FieldRecord = FieldRecord(
        id = id,
        category = category,
        title = title.trim(),
        type = type,
        description = description.trim(),
        location = location,
        eventDateMillis = eventDateMillis,
        assigneeIds = assigneeIds,
        attachments = attachments,
        createdAtMillis = nowMillis,
        authorName = authorName,
        severity = severity,
        impact = impact,
        blocksWork = blocksWork,
        // Blocking scope only travels with an actual block — a toggled-off record stays clean
        // even if the reporter filled these in before changing their mind.
        affectedTrade = if (blocksWork) affectedTrade else "",
        affectedTask = if (blocksWork) affectedTask else "",
        workPackage = if (blocksWork) workPackage else "",
        blockingReason = if (blocksWork) blockingReason.trim() else "",
        expectedResolutionMillis = if (blocksWork) expectedResolutionMillis else null,
        escalationContactId = if (blocksWork) escalationContactId else "",
        resolutionAuthority = if (blocksWork) resolutionAuthority else "",
        acknowledgementRequired = blocksWork && acknowledgementRequired,
    )

    /**
     * Back to a blank ISSUE draft; called after save/discard and by `DemoSession.reset`.
     * Deliberately not via [begin], which would wrongly drain a pending dictation hand-off.
     */
    fun clear() {
        category = RecordCategory.ISSUE
        title = ""
        type = RecordCategory.ISSUE.typeOptions.first()
        description = ""
        location = RECORD_LOCATIONS.first()
        eventDateMillis = 0L
        assigneeIds = emptyList()
        attachments = emptyList()
        severity = RecordSeverity.MEDIUM
        impact = RecordImpact.INFORMATIONAL
        blocksWork = false
        affectedTrade = ""
        affectedTask = ""
        workPackage = ""
        blockingReason = ""
        expectedResolutionMillis = null
        escalationContactId = ""
        resolutionAuthority = RESOLUTION_AUTHORITIES.first()
        acknowledgementRequired = false
        titleTouched = false
        blockingReasonTouched = false
        submitAttempted = false
        staged = false
        CameraAttachmentInbox.reset()
    }
}

/**
 * Hand-off for "attach from camera": the form arms the inbox, navigates to the regular camera
 * route, and the nav host deposits the saved photo's id on the way back. Arming makes deposits
 * opt-in, so ordinary captures (nobody listening) never leak into a later form.
 */
object CameraAttachmentInbox {

    var pending by mutableStateOf<String?>(null)
        private set

    private var armed = false

    /** True after [arm] until a [deposit] or [reset] — used to hide photo-review's voice chip. */
    fun isArmed(): Boolean = armed

    fun arm() {
        armed = true
        pending = null
    }

    fun deposit(imageId: String) {
        if (!armed) return
        pending = imageId
        armed = false
    }

    fun take(): String? = pending.also { pending = null }

    fun reset() {
        armed = false
        pending = null
    }
}
