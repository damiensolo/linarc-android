package com.solomondesign.app.ui.capture

/** Prefill for [QuickIssueScreen]'s fields; null/blank pieces leave the field at its default. */
data class IssueDraft(
    val title: String,
    val location: String?,
    val note: String,
)

/**
 * One-shot hand-off of an [IssueDraft] into the Quick issue route. Navigation-compose route
 * arguments would force URL-encoding free-dictated text, so the draft travels in memory instead:
 * the producer (video review) calls [set] right before navigating, and the screen
 * consumes it with [take] — which clears it, so a later plain visit to Quick issue starts blank.
 */
object IssueDraftHolder {

    private var draft: IssueDraft? = null

    fun set(value: IssueDraft) {
        draft = value
    }

    fun take(): IssueDraft? = draft.also { draft = null }
}
