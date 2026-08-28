package com.solomondesign.app.ui.voicenote

import com.solomondesign.app.ui.records.RECORD_LOCATIONS

/** What a finished voice note contributes to a new record's form. */
data class VoiceNoteSeeds(
    val title: String,
    val description: String,
    val location: String?,
)

private const val TITLE_MAX_WORDS = 8
private const val TITLE_MAX_CHARS = 56

/**
 * Turns a captured note into record-form seeds. The description carries exactly the text the
 * user had showing when they tapped Create — the original or the translation, never both
 * (simplified 2026-08-25: an earlier dual-language block meant extra editing before every
 * save). If the translation view is showing but the translation isn't available yet, the
 * original fills in — a create is never blocked on translation. The title derives from the
 * same selected text; the location is matched against both languages since either may name it.
 */
fun buildVoiceNoteSeeds(
    original: String,
    translation: String?,
    spokenLanguage: VoiceNoteLanguage,
    displayLanguage: VoiceNoteLanguage,
    locations: List<String> = RECORD_LOCATIONS,
): VoiceNoteSeeds {
    val note = original.trim()
    val translated = translation?.trim()?.takeIf { it.isNotBlank() }

    val selected = if (displayLanguage == spokenLanguage) note else translated ?: note

    val location = locations.firstOrNull { loc ->
        note.contains(loc, ignoreCase = true) || translated?.contains(loc, ignoreCase = true) == true
    }

    return VoiceNoteSeeds(title = deriveTitle(selected), description = selected, location = location)
}

/** First few words of the note, capped so it reads as a list-row title, never a paragraph. */
private fun deriveTitle(text: String): String {
    val head = text.trim().split(Regex("\\s+")).take(TITLE_MAX_WORDS).joinToString(" ")
    val capped = if (head.length <= TITLE_MAX_CHARS) {
        head
    } else {
        head.take(TITLE_MAX_CHARS).substringBeforeLast(' ')
    }
    return capped.replaceFirstChar { it.titlecase() }
}
