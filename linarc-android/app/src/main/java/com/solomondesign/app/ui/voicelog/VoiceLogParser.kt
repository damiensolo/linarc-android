package com.solomondesign.app.ui.voicelog

/** Result of running [VoiceLogParser] on a real transcript. */
data class ParsedVoiceLog(
    val labor: List<LaborCard>,
    val materials: List<MaterialCard>,
    val delays: List<DelayCard>,
    val issues: List<IssueCard>,
)

/**
 * A small local roster the parser matches against — the "small local data model" standing in
 * for a real project directory / material catalog lookup.
 */
private object LocalRoster {
    data class Worker(val name: String, val trade: String)

    val workers = listOf(
        Worker("Hector Ortiz", "Framing (Carpentry)"),
        Worker("Dave Miller", "Framing (Carpentry)"),
        Worker("Maria Chen", "Electrical"),
        Worker("Sam Reyes", "Plumbing"),
    )

    val materialKeywords = listOf("stud", "lumber", "concrete", "rebar", "drywall", "conduit", "pipe")

    /** keyword -> delay category. Multiple keywords can share a category (deduped on parse). */
    val delayKeywords = linkedMapOf(
        "delivery" to "Material Delivery",
        "delayed" to "Material Delivery",
        "stockout" to "Material Delivery",
        "rain" to "Weather Event",
        "weather" to "Weather Event",
        "tarp" to "Weather Event",
    )

    val issueKeywords = listOf("spalling", "crack", "leak", "damage")
}

/**
 * Deterministic, on-device keyword/regex extraction over a real transcript — explicitly NOT a
 * neural model. This is the "basic local model" called for as a proof of concept without pulling
 * in an actual bundled LLM runtime (e.g. MediaPipe LLM Inference or Gemini Nano/AICore), which
 * would be a much larger dependency + device-support surface and needs an explicit go-ahead per
 * this project's "ask before adding a new dependency" rule. Swapping this for a real on-device
 * LLM later is a drop-in replacement — nothing else in the voice-log flow depends on how
 * [parse] gets its answer, only on the [ParsedVoiceLog] shape it returns.
 */
object VoiceLogParser {

    private val hoursRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:hours|hour|hrs|hr)\b""", RegexOption.IGNORE_CASE)
    private val quantityRegex = Regex("""\b(\d+)\s+([a-zA-Z][a-zA-Z-]*)""")
    private val nearRegex = Regex("""near\s+([a-zA-Z0-9 ]{2,30})""", RegexOption.IGNORE_CASE)

    fun parse(transcript: String): ParsedVoiceLog {
        if (transcript.isBlank()) {
            return ParsedVoiceLog(emptyList(), emptyList(), emptyList(), emptyList())
        }
        val lower = transcript.lowercase()

        // Labor: any roster worker mentioned (by first or full name) is logged at the first
        // hours figure stated anywhere in the transcript, defaulting to a standard 8-hour day
        // when none is stated — real field dictation often doesn't restate the obvious.
        val sharedHours = hoursRegex.find(transcript)?.groupValues?.get(1)?.toDoubleOrNull() ?: 8.0
        val labor = LocalRoster.workers
            .filter { worker ->
                lower.contains(worker.name.lowercase()) ||
                    lower.contains(worker.name.substringBefore(' ').lowercase())
            }
            .mapIndexed { index, worker ->
                LaborCard(id = "labor-${index + 1}", name = worker.name, trade = worker.trade, hours = sharedHours)
            }

        // Materials: "<quantity> <material keyword>", e.g. "40 studs".
        val materials = quantityRegex.findAll(transcript)
            .mapNotNull { match ->
                val quantity = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
                val word = match.groupValues[2].lowercase().trimEnd('s')
                val keyword = LocalRoster.materialKeywords.firstOrNull { it == word }
                if (keyword == null) null else quantity to keyword
            }
            .distinct()
            .toList()
            .mapIndexed { index, (quantity, keyword) ->
                MaterialCard(
                    id = "material-${index + 1}",
                    quantity = quantity,
                    unit = "pcs",
                    description = keyword.replaceFirstChar { it.uppercase() },
                )
            }

        // Delays: any matched keyword's category is logged once, paired with the nearest hours
        // figure mentioned after the triggering keyword (falls back to 1.0 if none is stated).
        val delays = LocalRoster.delayKeywords.entries
            .filter { (keyword, _) -> lower.contains(keyword) }
            .groupBy({ it.value }, { it.key })
            .entries
            .mapIndexed { index, (category, keywords) ->
                val keyword = keywords.first()
                val fromKeyword = transcript.substring(lower.indexOf(keyword).coerceAtLeast(0))
                val hours = hoursRegex.find(fromKeyword)?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
                DelayCard(
                    id = "delay-${index + 1}",
                    hours = hours,
                    cause = "$category: ${keyword.replaceFirstChar { it.uppercase() }}",
                )
            }

        // Issues: a matched defect keyword becomes a card; "near <place>" is captured as a rough
        // location when present, since that pattern shows up often in field dictation.
        val location = nearRegex.find(transcript)?.groupValues?.get(1)?.trim()?.replaceFirstChar { it.uppercase() }
        val issues = LocalRoster.issueKeywords
            .filter { lower.contains(it) }
            .mapIndexed { index, keyword ->
                IssueCard(
                    id = "issue-${index + 1}",
                    title = keyword.replaceFirstChar { it.uppercase() },
                    location = location ?: "Mentioned in dictation",
                )
            }

        return ParsedVoiceLog(labor, materials, delays, issues)
    }
}
