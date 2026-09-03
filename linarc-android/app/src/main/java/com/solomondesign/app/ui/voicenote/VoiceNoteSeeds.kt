package com.solomondesign.app.ui.voicenote

import com.solomondesign.app.ui.records.RECORD_LOCATIONS
import com.solomondesign.app.ui.records.RecordCategory

/**
 * What a finished voice note contributes to a new record's form. Deliberately no title:
 * dictated text seeds only the description (decided 2026-09-03 — a transcript head made a
 * junk title the reporter had to delete before typing the real one; the naive derivation
 * stays out until parsing is genuinely smarter). The reporter types the title on the form.
 */
data class VoiceNoteSeeds(
    val description: String,
    val location: String?,
    val photoImageIds: List<String> = emptyList(),
    val blocksWork: Boolean = false,
    val blockingReason: String = "",
    val category: RecordCategory = RecordCategory.ISSUE,
    val categoryFromKeywords: Boolean = false,
    val existing: VoiceNoteMatch? = null,
)

/**
 * Turns a captured note into record-form seeds. The description carries exactly the text the
 * user had showing when they tapped Create — the original or the translation, never both
 * (simplified 2026-08-25: an earlier dual-language block meant extra editing before every
 * save). If the translation view is showing but the translation isn't available yet, the
 * original fills in — a create is never blocked on translation. No title is derived (see
 * [VoiceNoteSeeds]); the location is matched against both languages since either may name it.
 */
fun buildVoiceNoteSeeds(
    original: String,
    translation: String?,
    spokenLanguage: VoiceNoteLanguage,
    displayLanguage: VoiceNoteLanguage,
    locations: List<String> = RECORD_LOCATIONS,
    photoImageIds: List<String> = emptyList(),
    existingCandidates: List<VoiceNoteMatch> = emptyList(),
): VoiceNoteSeeds {
    val note = original.trim()
    val translated = translation?.trim()?.takeIf { it.isNotBlank() }

    val selected = if (displayLanguage == spokenLanguage) note else translated ?: note

    val location = locations.firstOrNull { loc ->
        note.contains(loc, ignoreCase = true) || translated?.contains(loc, ignoreCase = true) == true
    }

    val intent = inferVoiceNoteIntent(selected, existingCandidates)

    return VoiceNoteSeeds(
        description = selected,
        location = location,
        photoImageIds = photoImageIds,
        blocksWork = intent.blocksWork,
        blockingReason = intent.blockingReason.orEmpty(),
        category = intent.category,
        categoryFromKeywords = intent.categoryFromKeywords,
        existing = intent.existing,
    )
}

/**
 * Photo-then-voice hand-off: the camera deposits the saved photo id, then replaces itself with
 * Voice note. Arming [com.solomondesign.app.ui.records.CameraAttachmentInbox] is the inverse
 * (voice already open, camera stacks and pops back).
 */
object VoiceNotePhotoInbox {
    private val ids = mutableListOf<String>()

    fun deposit(imageId: String) {
        if (imageId.isBlank() || imageId in ids) return
        ids += imageId
    }

    fun takeAll(): List<String> = ids.toList().also { ids.clear() }

    fun reset() {
        ids.clear()
    }
}
