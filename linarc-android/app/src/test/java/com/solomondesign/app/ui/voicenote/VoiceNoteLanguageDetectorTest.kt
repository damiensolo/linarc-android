package com.solomondesign.app.ui.voicenote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceNoteLanguageDetectorTest {

    @Test
    fun detect_typicalFieldSpanish_returnsSpanish() {
        assertEquals(
            VoiceNoteLanguage.SPANISH,
            VoiceNoteLanguageDetector.detect("falta concreto en el nivel dos y la pared no está lista"),
        )
    }

    @Test
    fun detect_accentedWordsAlone_carryEnoughSignal() {
        // A single word like "tubería" has an accent plus a stopword hit — enough to call it.
        assertEquals(VoiceNoteLanguage.SPANISH, VoiceNoteLanguageDetector.detect("tubería rota"))
    }

    @Test
    fun detect_typicalFieldEnglish_returnsEnglish() {
        assertEquals(
            VoiceNoteLanguage.ENGLISH,
            VoiceNoteLanguageDetector.detect("the drywall crew is missing anchors on level two"),
        )
    }

    @Test
    fun detect_blankOrAmbiguousText_returnsNull() {
        assertNull(VoiceNoteLanguageDetector.detect(""))
        assertNull(VoiceNoteLanguageDetector.detect("   "))
        // Proper nouns and numbers common to both languages shouldn't force a call.
        assertNull(VoiceNoteLanguageDetector.detect("area 4 columna b wall"))
    }

    @Test
    fun detect_evenlyMixedText_returnsNull() {
        // Neither language dominates 2:1, so the detector must not flip-flop the toggle.
        assertNull(VoiceNoteLanguageDetector.detect("the wall está en el floor with door"))
    }
}
