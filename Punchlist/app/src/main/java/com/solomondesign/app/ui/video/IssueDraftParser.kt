package com.solomondesign.app.ui.video

/** Result of running [IssueDraftParser] over a spoken video description. */
data class ParsedIssueDraft(
    /** Short human title drafted from the transcript; never blank. */
    val title: String,
    /** A location phrase when one was spoken, normalized to a known area when possible. */
    val location: String?,
    /** True when the description mentions a defect — used to preselect "file an issue". */
    val looksLikeIssue: Boolean,
)

/**
 * Deterministic keyword/regex extraction over the spoken description of a captured video —
 * the same "basic local model" approach as [com.solomondesign.app.ui.voicelog.VoiceLogParser],
 * and swappable for a real on-device LLM later without touching the flow: callers depend only
 * on the [ParsedIssueDraft] shape.
 */
object IssueDraftParser {

    /** Fallback title for the video when nothing was dictated (the describe step is skippable). */
    const val DEFAULT_TITLE = "Video"

    private const val TITLE_WORD_LIMIT = 6

    /**
     * Field-defect vocabulary; a hit titles the video draft and preselects issue filing.
     * (It never titles the issue record itself — dictated text seeds only the issue's
     * note/location, decided 2026-09-03.) Extends `VoiceLogParser`'s issue keywords with
     * common walkthrough callouts.
     */
    private val defectKeywords = listOf(
        "spalling", "cracked", "crack", "leaking", "leak", "damaged", "damage",
        "missing", "exposed", "broken", "unsafe", "hazard", "loose",
    )

    /**
     * The demo project's known locations (mirrors the Quick issue location options). Kept local
     * so this parser stays importable from JVM tests without touching any Compose-adjacent file.
     */
    private val knownLocations = listOf("Area B", "Column 4", "Level 2")

    private val locationRegex =
        Regex("""\b(?:near|at|by)\s+(?:the\s+)?([a-zA-Z][a-zA-Z0-9 ]{1,30})""", RegexOption.IGNORE_CASE)

    fun parse(transcript: String): ParsedIssueDraft {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) {
            return ParsedIssueDraft(title = DEFAULT_TITLE, location = null, looksLikeIssue = false)
        }
        val lower = trimmed.lowercase()

        // Keywords are ordered longest-form-first ("cracked" before "crack") so the title uses
        // the word actually spoken rather than a stem of it.
        val defect = defectKeywords.firstOrNull { lower.contains(it) }

        // A known project location mentioned anywhere wins (it maps onto the issue form's
        // dropdown); otherwise fall back to the spoken "near/at/by <place>" phrase.
        val location = knownLocations.firstOrNull { lower.contains(it.lowercase()) }
            ?: locationRegex.find(trimmed)?.groupValues?.get(1)?.trim()
                ?.replaceFirstChar { it.uppercase() }

        val title = when {
            defect != null && location != null ->
                "${defect.replaceFirstChar { it.uppercase() }} — $location"
            defect != null -> defect.replaceFirstChar { it.uppercase() }
            else -> trimmed.split(Regex("""\s+"""))
                .take(TITLE_WORD_LIMIT)
                .joinToString(" ")
                .trimEnd('.', ',', ';')
                .replaceFirstChar { it.uppercase() }
        }

        return ParsedIssueDraft(
            title = title.ifBlank { DEFAULT_TITLE },
            location = location,
            looksLikeIssue = defect != null,
        )
    }

}
