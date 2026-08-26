package com.solomondesign.app.ui.voicenote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceNoteSeedsTest {

    @Test
    fun spanishNoteWithTranslation_buildsBilingualDescription_andEnglishTitle() {
        val seeds = buildVoiceNoteSeeds(
            original = "falta concreto en la pared del nivel 2",
            translation = "concrete is missing on the level 2 wall",
            spokenLanguage = VoiceNoteLanguage.SPANISH,
        )

        assertEquals(
            "falta concreto en la pared del nivel 2\n\n" +
                "English translation:\n" +
                "concrete is missing on the level 2 wall",
            seeds.description,
        )
        // Title derives from the English side so record lists read consistently.
        assertEquals("Concrete is missing on the level 2 wall", seeds.title)
        // "level 2" in the translation matches the known record location.
        assertEquals("Level 2", seeds.location)
    }

    @Test
    fun englishNoteWithTranslation_labelsTheSpanishBlockForItsReader() {
        val seeds = buildVoiceNoteSeeds(
            original = "Anchor bolts missing at column 4",
            translation = "Faltan pernos de anclaje en la columna 4",
            spokenLanguage = VoiceNoteLanguage.ENGLISH,
        )

        assertEquals(
            "Anchor bolts missing at column 4\n\n" +
                "Traducción al español:\n" +
                "Faltan pernos de anclaje en la columna 4",
            seeds.description,
        )
        assertEquals("Anchor bolts missing at column 4", seeds.title)
        assertEquals("Column 4", seeds.location)
    }

    @Test
    fun missingTranslation_neverBlocksTheSeeds_originalOnly() {
        val seeds = buildVoiceNoteSeeds(
            original = "puerta dañada en el pasillo",
            translation = null,
            spokenLanguage = VoiceNoteLanguage.SPANISH,
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
            locations = emptyList(),
        )

        assertEquals("The temporary handrail on the west stair is", seeds.title)
    }
}
