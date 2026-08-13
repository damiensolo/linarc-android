package com.solomondesign.punchlist.ui.voicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves [VoiceLogParser] actually extracts real structured data from real transcript text —
 * this is what makes the voice-to-log pipeline genuine rather than a scripted playthrough.
 */
class VoiceLogParserTest {

    /** The exact example transcript from `voice-to-log-spec.md`. */
    private val specTranscript = "Hey, it's Hector. Me, Dave, and the crew worked on the structural " +
        "framing in Area B today. We used about 40 studs, but we ran out of the 12-footers because " +
        "the delivery got delayed. Rain started at 2 PM, so we had to tarp everything and call it a " +
        "day early. Also, we noticed some minor spalling on the concrete slab near column 4 — needs " +
        "an inspector to look at it."

    @Test
    fun parse_specTranscript_extractsBothRosterWorkersAtDefaultEightHours() {
        val result = VoiceLogParser.parse(specTranscript)

        assertEquals(2, result.labor.size)
        assertTrue(result.labor.any { it.name == "Hector Ortiz" && it.hours == 8.0 })
        assertTrue(result.labor.any { it.name == "Dave Miller" && it.hours == 8.0 })
    }

    @Test
    fun parse_specTranscript_extractsStudQuantity() {
        val result = VoiceLogParser.parse(specTranscript)

        assertEquals(1, result.materials.size)
        val stud = result.materials.single()
        assertEquals(40.0, stud.quantity, 0.0)
        assertEquals("Stud", stud.description)
    }

    @Test
    fun parse_specTranscript_extractsDeliveryAndWeatherDelaysOnce() {
        val result = VoiceLogParser.parse(specTranscript)

        assertEquals(2, result.delays.size)
        assertTrue(result.delays.any { it.cause.startsWith("Material Delivery") })
        assertTrue(result.delays.any { it.cause.startsWith("Weather Event") })
    }

    @Test
    fun parse_specTranscript_extractsSpallingIssueNearColumnFour() {
        val result = VoiceLogParser.parse(specTranscript)

        assertEquals(1, result.issues.size)
        val issue = result.issues.single()
        assertEquals("Spalling", issue.title)
        assertEquals("Column 4", issue.location)
    }

    @Test
    fun parse_blankTranscript_returnsNothing() {
        val result = VoiceLogParser.parse("")

        assertTrue(result.labor.isEmpty())
        assertTrue(result.materials.isEmpty())
        assertTrue(result.delays.isEmpty())
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun parse_unrelatedTranscript_matchesNoRosterWorkersOrKeywords() {
        val result = VoiceLogParser.parse("The weather was nice and we had lunch at noon.")

        assertTrue(result.labor.isEmpty())
        assertTrue(result.materials.isEmpty())
        assertTrue(result.issues.isEmpty())
        // "weather" alone still legitimately maps to a Weather Event delay category.
        assertEquals(1, result.delays.size)
    }

    @Test
    fun parse_explicitHours_overridesTheEightHourDefault() {
        val result = VoiceLogParser.parse("Hector worked 6.5 hours on site today.")

        assertEquals(6.5, result.labor.single().hours, 0.0)
    }
}
