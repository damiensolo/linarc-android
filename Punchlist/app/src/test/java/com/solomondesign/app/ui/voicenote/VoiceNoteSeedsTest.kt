package com.solomondesign.app.ui.voicenote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceNoteSeedsTest {

    @Test
    fun displayingTheOriginal_seedsOnlyTheOriginalText() {
        val seeds = buildVoiceNoteSeeds(
            original = "falta concreto en la pared del nivel 2",
            translation = "concrete is missing on the level 2 wall",
            spokenLanguage = VoiceNoteLanguage.SPANISH,
            displayLanguage = VoiceNoteLanguage.SPANISH,
        )

        // Exactly what was on screen — no translation block appended (simplified 2026-08-25).
        // No title is derived (2026-09-03): dictated text seeds only the description.
        assertEquals("falta concreto en la pared del nivel 2", seeds.description)
        // The unused translation still helps: "level 2" matches the known record location.
        assertEquals("Level 2", seeds.location)
    }

    @Test
    fun displayingTheTranslation_seedsOnlyTheTranslation() {
        val seeds = buildVoiceNoteSeeds(
            original = "falta concreto en la pared del nivel 2",
            translation = "concrete is missing on the level 2 wall",
            spokenLanguage = VoiceNoteLanguage.SPANISH,
            displayLanguage = VoiceNoteLanguage.ENGLISH,
        )

        assertEquals("concrete is missing on the level 2 wall", seeds.description)
        assertEquals("Level 2", seeds.location)
    }

    @Test
    fun missingTranslation_fallsBackToTheOriginal_neverBlocksTheCreate() {
        val seeds = buildVoiceNoteSeeds(
            original = "puerta dañada en el pasillo",
            translation = null,
            spokenLanguage = VoiceNoteLanguage.SPANISH,
            displayLanguage = VoiceNoteLanguage.ENGLISH,
        )

        assertEquals("puerta dañada en el pasillo", seeds.description)
        assertNull(seeds.location)
    }

    @Test
    fun photoIds_passThroughToTheDraftSeeds() {
        val seeds = buildVoiceNoteSeeds(
            original = "crack at column 4",
            translation = null,
            spokenLanguage = VoiceNoteLanguage.ENGLISH,
            displayLanguage = VoiceNoteLanguage.ENGLISH,
            photoImageIds = listOf("photo-1"),
        )
        assertEquals(listOf("photo-1"), seeds.photoImageIds)
        assertEquals(true, seeds.categoryFromKeywords)
        assertEquals("Column 4", seeds.location)
    }
}
