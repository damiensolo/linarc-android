package com.solomondesign.app.ui.voicenote

import com.solomondesign.app.ui.records.RecordCategory

/** An existing task or field record the transcript may be naming. */
data class VoiceNoteMatch(
    val id: String,
    val title: String,
    val kind: Kind,
) {
    enum class Kind { TASK, RECORD }
}

/**
 * Cheap keyword routing over a voice-note transcript — the same "basic local model" approach
 * as [com.solomondesign.app.ui.video.IssueDraftParser]. No Whisper/LLM.
 */
data class VoiceNoteIntent(
    val category: RecordCategory,
    /** True when a defect / incident / punch keyword selected [category], not the Issue default. */
    val categoryFromKeywords: Boolean,
    val blocksWork: Boolean,
    val blockingReason: String?,
    val existing: VoiceNoteMatch?,
)

fun inferVoiceNoteIntent(
    text: String,
    existing: List<VoiceNoteMatch> = emptyList(),
): VoiceNoteIntent {
    val trimmed = text.trim()
    if (trimmed.isBlank()) {
        return VoiceNoteIntent(
            category = RecordCategory.ISSUE,
            categoryFromKeywords = false,
            blocksWork = false,
            blockingReason = null,
            existing = null,
        )
    }
    val lower = trimmed.lowercase()

    val incident = incidentKeywords.any { lower.contains(it) }
    val punch = punchKeywords.any { lower.contains(it) }
    val issue = issueKeywords.any { lower.contains(it) }
    val (category, fromKeywords) = when {
        incident -> RecordCategory.INCIDENT to true
        punch -> RecordCategory.PUNCH to true
        issue -> RecordCategory.ISSUE to true
        else -> RecordCategory.ISSUE to false
    }

    val blocksWork = blockingKeywords.any { lower.contains(it) }
    val blockingReason = trimmed.takeIf { blocksWork }

    val existingMatch = matchExisting(lower, existing)

    return VoiceNoteIntent(
        category = category,
        categoryFromKeywords = fromKeywords,
        blocksWork = blocksWork,
        blockingReason = blockingReason,
        existing = existingMatch,
    )
}

private fun matchExisting(lower: String, existing: List<VoiceNoteMatch>): VoiceNoteMatch? {
    rfiIdRegex.findAll(lower).forEach { hit ->
        val spaced = hit.value
        val dashed = hit.value.replace(Regex("""\s+"""), "-")
        existing.firstOrNull { candidate ->
            candidate.title.contains(spaced, ignoreCase = true) ||
                candidate.title.contains(dashed, ignoreCase = true)
        }?.let { return it }
    }
    existing.firstOrNull { lower.contains(it.title.lowercase()) }?.let { return it }

    val haystack = lower.replace("-", " ")
    return existing.mapNotNull { candidate ->
        val tokens = significantTokens(candidate.title)
        if (tokens.isEmpty()) return@mapNotNull null
        val hits = tokens.count { token -> haystack.contains(token) }
        if (hits >= minOf(2, tokens.size)) {
            candidate to hits.toDouble() / tokens.size
        } else {
            null
        }
    }.maxByOrNull { it.second }?.first
}

private fun significantTokens(title: String): List<String> =
    title.lowercase()
        .split(Regex("""[\s,./—\-]+"""))
        .map { it.trim().trimEnd(':') }
        .filter { it.length >= 3 && it !in tokenStopwords }

private val tokenStopwords = setOf(
    "the", "and", "for", "at", "from", "with", "this", "that", "into",
)

private val rfiIdRegex = Regex("""rfi[-\s]?\d+""", RegexOption.IGNORE_CASE)

private val issueKeywords = listOf(
    "spalling", "cracked", "crack", "leaking", "leak", "damaged", "damage",
    "missing", "exposed", "broken", "unsafe", "hazard", "loose",
    "grieta", "filtración", "filtracion", "daño", "dano", "dañada", "danada",
    "falta", "suelto", "peligro",
)

private val incidentKeywords = listOf(
    "injury", "injured", "near miss", "accident", "fell", "falling", "lesión",
    "lesion", "accidente", "se cayo", "se cayó",
)

private val punchKeywords = listOf(
    "punch list", "punchlist", "punch item", "punch", "touch-up", "touch up",
    "deficiency", "incomplete work", "repaso",
)

private val blockingKeywords = listOf(
    "blocks work", "blocking", "blocked", "can't work", "cannot work",
    "can't proceed", "cannot proceed", "stop work", "work stoppage",
    "unsafe", "bloquea", "bloqueando", "bloqueado", "no podemos trabajar",
    "no se puede trabajar", "peligro",
)
