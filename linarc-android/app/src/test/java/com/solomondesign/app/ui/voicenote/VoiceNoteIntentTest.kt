package com.solomondesign.app.ui.voicenote

import com.solomondesign.app.ui.records.RecordCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNoteIntentTest {

    private val candidates = listOf(
        VoiceNoteMatch("rec-seed-rfi-118", "RFI-118 — Med-gas re-route at Column 4", VoiceNoteMatch.Kind.RECORD),
        VoiceNoteMatch("task-med-gas-col4", "Med gas rough-in at column 4", VoiceNoteMatch.Kind.TASK),
    )

    @Test
    fun defectAndLocation_isIssueWithMatchedLocation() {
        val intent = inferVoiceNoteIntent("There's a crack at column 4")
        assertEquals(RecordCategory.ISSUE, intent.category)
        assertTrue(intent.categoryFromKeywords)
        assertFalse(intent.blocksWork)
        val seeds = buildVoiceNoteSeeds(
            original = "There's a crack at column 4",
            translation = null,
            spokenLanguage = VoiceNoteLanguage.ENGLISH,
            displayLanguage = VoiceNoteLanguage.ENGLISH,
        )
        assertEquals("Column 4", seeds.location)
    }

    @Test
    fun blockingLanguage_preselectsBlocksWork() {
        val intent = inferVoiceNoteIntent("This is blocking plumbing at column 4, crack in the slab")
        assertTrue(intent.blocksWork)
        assertNotNull(intent.blockingReason)
        assertEquals(RecordCategory.ISSUE, intent.category)
    }

    @Test
    fun injury_preselectsIncident() {
        val intent = inferVoiceNoteIntent("Near miss — a conduit stick fell off the lift")
        assertEquals(RecordCategory.INCIDENT, intent.category)
        assertTrue(intent.categoryFromKeywords)
    }

    @Test
    fun punchKeyword_preselectsPunch() {
        val intent = inferVoiceNoteIntent("Punch item: touch-up drywall at exam 6")
        assertEquals(RecordCategory.PUNCH, intent.category)
    }

    @Test
    fun rfiId_matchesExistingRecord() {
        val intent = inferVoiceNoteIntent("RFI-118 still waiting", existing = candidates)
        assertEquals("rec-seed-rfi-118", intent.existing?.id)
        assertEquals(VoiceNoteMatch.Kind.RECORD, intent.existing?.kind)
    }

    @Test
    fun medGasTokens_matchTask() {
        val intent = inferVoiceNoteIntent(
            "med gas still waiting at column 4",
            existing = candidates,
        )
        assertEquals("task-med-gas-col4", intent.existing?.id)
    }

    @Test
    fun plainNarration_selectsNoCategoryFromKeywords() {
        val intent = inferVoiceNoteIntent("walked the second floor with the crew this morning")
        assertFalse(intent.categoryFromKeywords)
    }

    @Test
    fun spanishFalta_isAnIssueKeyword() {
        val intent = inferVoiceNoteIntent("falta concreto en la pared del nivel dos")
        assertTrue(intent.categoryFromKeywords)
        assertEquals(RecordCategory.ISSUE, intent.category)
    }
}
