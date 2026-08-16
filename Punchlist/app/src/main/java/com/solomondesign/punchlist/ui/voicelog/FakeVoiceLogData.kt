package com.solomondesign.punchlist.ui.voicelog

/**
 * Canned data standing in for the AI parsing pipeline described in
 * `voice-to-log-spec.md`. Lifted directly from that spec's own example transcript and
 * JSON payload — this demo proves the recording -> parsing -> review UI/state flow,
 * not real transcription or entity extraction.
 */
object FakeVoiceLogData {
    const val PROJECT_NAME = "Riverside Medical"
    const val HEADER_META = "10:04 AM · Area B · Foreman"

    const val TRANSCRIPT = "Me, Dave, and the crew worked on the structural framing " +
        "in Area B today. We used about 40 studs, but we ran out of the 12-footers " +
        "because the delivery got delayed. Rain started at 2 PM, so we had to tarp " +
        "everything and call it a day early. Also, we noticed some minor spalling on " +
        "the concrete slab near column 4 — needs an inspector to look at it."

    val parsingSteps = listOf(
        "Labor & Personnel Detected (2 Trades)",
        "Material quantities mapped (40 studs)",
        "Mapping cost codes and locations...",
    )

    fun seedLaborCards() = listOf(
        LaborCard(id = "labor-1", name = "Hector Ortiz", trade = "Framing (Carpentry)", hours = 8.0),
        LaborCard(id = "labor-2", name = "Dave Miller", trade = "Framing (Carpentry)", hours = 8.0),
    )

    fun seedMaterialCards() = listOf(
        MaterialCard(id = "material-1", quantity = 40.0, unit = "Units", description = "2x4 Wood Stud, 10ft"),
    )

    fun seedDelayCards() = listOf(
        DelayCard(id = "delay-1", hours = 2.5, cause = "Delivery Delay: Studs"),
        DelayCard(id = "delay-2", hours = 2.0, cause = "Rain Delay: Site Tarped"),
    )

    fun seedIssueCards() = listOf(
        IssueCard(id = "issue-1", title = "Concrete Spalling", location = "Slab, Column 4"),
    )
}
