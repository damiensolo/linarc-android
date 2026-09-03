package com.solomondesign.app.ui.voicenote

/**
 * The two languages Voice notes captures and translates between. A closed enum (not raw locale
 * tags) so the recognition, detection, translation, and UI layers all agree on exactly one pair.
 */
enum class VoiceNoteLanguage(
    /** ISO 639-1 code — what ML Kit's translator keys on. */
    val code: String,
    /** BCP-47 tag handed to the speech recognizer. es-US: the demo persona's crews are US-based. */
    val speechTag: String,
    /** Name shown in the language toggle, in the language itself so it's findable by its speaker. */
    val displayName: String,
) {
    ENGLISH("en", "en-US", "English"),
    SPANISH("es", "es-US", "Español"),
    ;

    fun other(): VoiceNoteLanguage = if (this == ENGLISH) SPANISH else ENGLISH
}

/**
 * Best-effort spoken-language detection from recognizer text. Character/stopword scoring rather
 * than a real language-ID model because the input is tiny (one utterance) and only two languages
 * are ever in play; the one-tap toggle remains the authoritative control when this guesses wrong.
 */
object VoiceNoteLanguageDetector {

    private const val SPANISH_MARKER_CHARS = "áéíóúñü¿¡"

    // Stopwords chosen to be common in field speech AND unambiguous between the two languages
    // (so no "no", "a", or "hospital"). Accented words also earn marker-character points.
    private val SPANISH_WORDS = setOf(
        "el", "la", "los", "las", "un", "una", "de", "del", "al", "que", "en", "es", "está",
        "están", "hay", "para", "con", "por", "se", "pero", "como", "más", "muy", "todo",
        "hoy", "mañana", "aquí", "necesito", "necesita", "necesitamos", "falta", "faltan",
        "trabajo", "trabajando", "terminado", "terminamos", "listo", "problema", "pared",
        "techo", "piso", "puerta", "tubería", "concreto", "hormigón", "obra", "cuadrilla",
        "nivel", "columna", "tenemos", "tiene", "hacer", "esta", "este", "esto", "nosotros",
    )

    private val ENGLISH_WORDS = setOf(
        "the", "is", "are", "was", "were", "we", "i", "you", "they", "it", "this", "that",
        "these", "and", "of", "to", "in", "on", "at", "for", "with", "from", "need", "needs",
        "have", "has", "do", "does", "done", "work", "working", "finished", "wall", "ceiling",
        "floor", "door", "pipe", "concrete", "crew", "level", "column", "today", "tomorrow",
        "here", "there", "missing", "broken", "install", "installed", "not",
    )

    /**
     * Returns the language [text] appears to be in, or null when there isn't enough signal to
     * call it (empty text, mixed text, or words common to both languages).
     */
    fun detect(text: String): VoiceNoteLanguage? {
        if (text.isBlank()) return null
        val lower = text.lowercase()
        val words = lower.split(WORD_BOUNDARY).filter { it.isNotBlank() }
        val accentPoints = lower.count { it in SPANISH_MARKER_CHARS } * 2
        val spanishScore = words.count { it in SPANISH_WORDS } + accentPoints
        val englishScore = words.count { it in ENGLISH_WORDS }
        return when {
            spanishScore >= 2 && spanishScore >= englishScore * 2 -> VoiceNoteLanguage.SPANISH
            englishScore >= 2 && englishScore >= spanishScore * 2 -> VoiceNoteLanguage.ENGLISH
            else -> null
        }
    }

    private val WORD_BOUNDARY = Regex("[^\\p{L}]+")
}
