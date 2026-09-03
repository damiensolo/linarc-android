package com.solomondesign.app.ui.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueDraftParserTest {

    @Test
    fun defectWithKnownLocation_titlesFromBoth_andPreselectsIssue() {
        val parsed = IssueDraftParser.parse(
            "There's a crack in the slab near column 4, needs a structural look",
        )

        assertEquals("Crack — Column 4", parsed.title)
        assertEquals("known project locations map onto the issue form", "Column 4", parsed.location)
        assertTrue(parsed.looksLikeIssue)
    }

    @Test
    fun spokenLocationPhrase_isUsedWhenNoKnownLocationMatches() {
        val parsed = IssueDraftParser.parse("Water is leaking near the loading dock")

        assertTrue(parsed.looksLikeIssue)
        assertEquals("Loading dock", parsed.location)
        assertEquals("Leaking — Loading dock", parsed.title)
    }

    @Test
    fun plainNarration_fallsBackToALeadingWordsTitle_withoutAnIssue() {
        val parsed = IssueDraftParser.parse(
            "walked the second floor with the electrical crew this morning, all rough-in done",
        )

        assertFalse("no defect keyword means no preselected issue", parsed.looksLikeIssue)
        assertEquals("Walked the second floor with the", parsed.title)
    }

    @Test
    fun blankTranscript_yieldsTheDefaultTitle() {
        val parsed = IssueDraftParser.parse("   ")

        assertEquals("Video", parsed.title)
        assertEquals(IssueDraftParser.DEFAULT_TITLE, parsed.title)
        assertNull(parsed.location)
        assertFalse(parsed.looksLikeIssue)
    }

    @Test
    fun longerKeywordFormsWin_soTheTitleUsesTheSpokenWord() {
        val parsed = IssueDraftParser.parse("The conduit strap is cracked at Level 2")

        assertEquals("Cracked — Level 2", parsed.title)
    }
}
