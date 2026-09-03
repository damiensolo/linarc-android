package com.solomondesign.app.ui.capture

/**
 * Dictated seeds for an Issue create form: location and note only — never a title (decided
 * 2026-09-03: transcript-derived titles were junk the reporter had to delete, so the reporter
 * names the record on the form). Null/blank pieces leave the form field at its default.
 */
data class IssueDraft(
    val location: String?,
    val note: String,
)

/**
 * One-shot hand-off of an [IssueDraft] into the Issue create form
 * ([com.solomondesign.app.ui.records.RecordDraft.begin] drains it). Navigation-compose route
 * arguments would force URL-encoding free-dictated text, so the draft travels in memory instead:
 * the producer (video review's File an issue) calls [set] right before navigating, and the form
 * consumes it with [take] — which clears it, so a later plain visit starts blank.
 */
object IssueDraftHolder {

    private var draft: IssueDraft? = null

    fun set(value: IssueDraft) {
        draft = value
    }

    fun take(): IssueDraft? = draft.also { draft = null }
}
