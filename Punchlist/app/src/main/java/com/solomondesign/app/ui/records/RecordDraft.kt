package com.solomondesign.app.ui.records

import androidx.compose.runtime.getValue
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
    var description by mutableStateOf("")
    var location by mutableStateOf(RECORD_LOCATIONS.first())
    var eventDateMillis by mutableStateOf(0L)
    var assigneeIds by mutableStateOf(listOf<String>())
        private set
    var attachments by mutableStateOf(listOf<RecordAttachment>())
        private set

    private var staged = false

    /** Drives the discard warning: a photo already attached counts as an edit. */
    val hasEdits: Boolean
        get() = title.isNotBlank() || description.isNotBlank() || attachments.isNotEmpty()

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
    ) {
        val dictated = if (newCategory == RecordCategory.ISSUE) IssueDraftHolder.take() else null
        category = newCategory
        title = seedTitle.ifBlank { dictated?.title.orEmpty() }
        type = newCategory.typeOptions.first()
        description = seedDescription.ifBlank { dictated?.note.orEmpty() }
        location = (seedLocation ?: dictated?.location)
            ?.takeIf { it in RECORD_LOCATIONS }
            ?: RECORD_LOCATIONS.first()
        eventDateMillis = nowMillis
        assigneeIds = emptyList()
        attachments = seedPhotoImageIds.map { imageId ->
            RecordAttachment(id = "att-$imageId", kind = AttachmentKind.PHOTO, ref = imageId)
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
