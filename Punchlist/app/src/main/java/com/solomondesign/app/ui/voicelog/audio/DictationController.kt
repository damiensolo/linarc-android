package com.solomondesign.app.ui.voicelog.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Turns single-utterance [SpeechTranscriber] calls into continuous, hands-free dictation:
 * stock Android's [android.speech.SpeechRecognizer] stops listening after each utterance or
 * short silence, so real dictation apps re-arm it in a loop and concatenate the results — that
 * loop lives here so it's covered by a plain unit test instead of only by a human talking to an
 * emulator.
 */
class DictationController(private val transcriber: SpeechTranscriber) {

    /** Finalized transcript accumulated across utterances so far. */
    var transcript by mutableStateOf("")
        private set

    /** The in-progress (not yet finalized) utterance, for live on-screen feedback. */
    var partial by mutableStateOf("")
        private set

    /** Set only on a non-recoverable failure; dictation has stopped when this is non-null. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Consecutive recoverable (no-match/timeout) errors with nothing recognized in between.
     * A real device with a working mic path only sees a handful of these during silence — a long,
     * unbroken streak while the user is actually talking is the signature of a device that can't
     * run this and a second audio consumer (e.g. [com.solomondesign.app.ui.voicelog.audio.AudioRecorder])
     * on the mic at once; a caller can use this to offer dropping the other consumer.
     */
    var consecutiveNoMatchCount by mutableStateOf(0)
        private set

    private var active = false

    fun start() {
        if (!transcriber.isAvailable()) {
            errorMessage = "Speech recognition isn't available on this device."
            return
        }
        active = true
        errorMessage = null
        listenOnce()
    }

    fun stop() {
        active = false
        partial = ""
        transcriber.stop()
    }

    private fun listenOnce() {
        transcriber.start(
            onPartial = { text ->
                if (text.isNotBlank()) consecutiveNoMatchCount = 0
                partial = text
            },
            onFinal = { text ->
                if (text.isNotBlank()) {
                    transcript = if (transcript.isBlank()) text else "$transcript $text"
                    consecutiveNoMatchCount = 0
                }
                partial = ""
                if (active) listenOnce()
            },
            onError = { error ->
                partial = ""
                if (active && error.recoverable) {
                    consecutiveNoMatchCount++
                    listenOnce()
                } else if (active) {
                    active = false
                    errorMessage = error.message
                }
            },
        )
    }
}
