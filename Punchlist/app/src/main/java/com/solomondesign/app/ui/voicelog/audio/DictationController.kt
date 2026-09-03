package com.solomondesign.app.ui.voicelog.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    var consecutiveNoMatchCount by mutableIntStateOf(0)
        private set

    /** BCP-47 hint passed to every (re-)arm of the recognizer; null = device default. */
    var languageTag: String? = null
        private set

    /** True while the recognizer loop is armed — Compose snapshot so the Speak UI recomposes. */
    var isListening by mutableStateOf(false)
        private set

    private var active = false

    /**
     * Increments on [reset]; utterance callbacks armed under an older take are ignored, so a
     * late result from a canceled take can never resurrect discarded words into the fresh one.
     * [stop] deliberately does NOT bump it — a Pause's in-flight words should still land.
     */
    private var takeId = 0

    /**
     * Consecutive CLIENT/BUSY retries with nothing recognized in between. Unlike silence
     * no-matches (fine forever), these mean the recognizer service itself is struggling — after
     * [MAX_CLIENT_RETRIES] the loop gives up with a plain-language message instead of spinning.
     */
    private var clientRetryStreak = 0

    /**
     * Switches the recognition language mid-dictation. The in-flight utterance is stopped so it
     * finalizes in its old language; the loop then re-arms with [tag]. Already-accumulated
     * transcript is kept — dictation may legitimately mix languages.
     */
    fun setLanguage(tag: String?) {
        if (tag == languageTag) return
        languageTag = tag
        if (active) transcriber.stop()
    }

    /**
     * Clears all accumulated state for a fresh take (re-record). Doesn't start listening.
     * Cancels rather than stops the in-flight utterance — its result must be dropped, not
     * finalized into the take the user just threw away.
     */
    fun reset() {
        active = false
        isListening = false
        partial = ""
        takeId++
        transcriber.cancel()
        transcriber.sessionEnded()
        transcript = ""
        errorMessage = null
        consecutiveNoMatchCount = 0
        clientRetryStreak = 0
    }

    fun start() {
        if (isListening) return
        if (!transcriber.isAvailable()) {
            errorMessage = "Speech recognition isn't available on this device."
            return
        }
        // Brackets the whole take for the transcriber: one audible "recording started" tone,
        // then silent re-arms until the matching sessionEnded (stop, reset, or fatal error).
        transcriber.sessionStarted()
        active = true
        isListening = true
        errorMessage = null
        // A fresh Resume gets a fresh retry budget — a capped-out take shouldn't poison it.
        clientRetryStreak = 0
        listenOnce()
    }

    fun stop() {
        active = false
        isListening = false
        partial = ""
        transcriber.stop()
        transcriber.sessionEnded()
    }

    private fun listenOnce() {
        val take = takeId
        transcriber.start(
            onPartial = { text ->
                if (take != takeId) return@start
                if (text.isNotBlank()) {
                    consecutiveNoMatchCount = 0
                    clientRetryStreak = 0
                }
                partial = text
            },
            onFinal = { text ->
                if (take != takeId) return@start
                if (text.isNotBlank()) {
                    transcript = if (transcript.isBlank()) text else "$transcript $text"
                    consecutiveNoMatchCount = 0
                    clientRetryStreak = 0
                }
                partial = ""
                if (active) listenOnce()
            },
            onError = { error ->
                if (take != takeId) return@start
                partial = ""
                when {
                    !active -> Unit
                    error.recoverable && error.clientRetry -> {
                        clientRetryStreak++
                        if (clientRetryStreak > MAX_CLIENT_RETRIES) {
                            fail("The microphone stopped responding — tap Resume to try again.")
                        } else {
                            listenOnce()
                        }
                    }
                    error.recoverable -> {
                        clientRetryStreak = 0
                        consecutiveNoMatchCount++
                        listenOnce()
                    }
                    else -> fail(error.message)
                }
            },
            languageTag = languageTag,
        )
    }

    private fun fail(message: String) {
        active = false
        isListening = false
        errorMessage = message
        transcriber.sessionEnded()
    }

    private companion object {
        const val MAX_CLIENT_RETRIES = 5
    }
}
