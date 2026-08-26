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
 * Turns a captured note into record-form seeds. The description is bilingual on purpose
 * (decided 2026-08-25): a Spanish-speaking crew member's issue stays readable to
 * English-speaking reviewers and vice versa, so the translation is appended under a label in
 * the *other* language's reader's terms. The title derives from the English text (records are
 * office-facing), falling back to the original when no translation is available.
 */
fun buildVoiceNoteSeeds(
    original: String,
    translation: String?,
    spokenLanguage: VoiceNoteLanguage,
    locations: List<String> = RECORD_LOCATIONS,
): VoiceNoteSeeds {
    val note = original.trim()
    val translated = translation?.trim()?.takeIf { it.isNotBlank() }

    val englishText = if (spokenLanguage == VoiceNoteLanguage.ENGLISH) note else translated ?: note

    val description = if (translated == null) {
        note
    } else {
        val label = if (spokenLanguage == VoiceNoteLanguage.SPANISH) {
            "English translation:"
        } else {
            "Traducción al español:"
        }
        "$note\n\n$label\n$translated"
    }

    val location = locations.firstOrNull { loc ->
        note.contains(loc, ignoreCase = true) || translated?.contains(loc, ignoreCase = true) == true
    }

    return VoiceNoteSeeds(title = deriveTitle(englishText), description = description, location = location)
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
