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
        assertEquals("falta concreto en la pared del nivel 2", seeds.description)
        assertEquals("Falta concreto en la pared del nivel 2", seeds.title)
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
        assertEquals("Concrete is missing on the level 2 wall", seeds.title)
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
        assertEquals("Puerta dañada en el pasillo", seeds.title)
        assertNull(seeds.location)
    }

    @Test
    fun title_capsAtEightWords_withoutCuttingMidWord() {
        val seeds = buildVoiceNoteSeeds(
            original = "the temporary handrail on the west stair is loose and needs to be re-anchored today",
            translation = null,
            spokenLanguage = VoiceNoteLanguage.ENGLISH,
            displayLanguage = VoiceNoteLanguage.ENGLISH,
            locations = emptyList(),
        )

        assertEquals("The temporary handrail on the west stair is", seeds.title)
    }
}
