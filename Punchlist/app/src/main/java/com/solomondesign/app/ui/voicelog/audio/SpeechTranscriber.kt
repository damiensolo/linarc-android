package com.solomondesign.app.ui.voicelog.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/** A speech-recognition error. [recoverable] errors (silence/no-match) should just restart listening. */
data class SpeechError(val message: String, val recoverable: Boolean)

/**
 * Converts live microphone speech to text — one *single utterance* per [start] call, matching
 * the real Android [SpeechRecognizer] contract. [com.solomondesign.app.ui.voicelog.audio.DictationController]
 * builds continuous, hands-free dictation out of repeated single-utterance calls to this.
 * Abstracted so the dictation loop is unit-testable without the Android speech framework.
 */
interface SpeechTranscriber {
    /** Whether this device actually has a speech-recognition service installed. */
    fun isAvailable(): Boolean

    /** [languageTag] is a BCP-47 hint (e.g. "es-US"); null keeps the device's default language. */
    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (SpeechError) -> Unit,
        languageTag: String? = null,
    )

    /**
     * A continuous dictation session is beginning: the next [start] is the user-visible
     * "recording started" moment and may play the OS recognizer tone. Default no-op.
     */
    fun sessionStarted() {}

    /**
     * The dictation session is over (stopped, reset, or dead on error) — restore any global
     * audio state changed during it. Default no-op.
     */
    fun sessionEnded() {}

    fun stop()
    fun destroy()
}

/** Real [SpeechTranscriber] backed by the OS [SpeechRecognizer] (on-device or Google's cloud recognizer). */
class AndroidSpeechTranscriber(private val context: Context) : SpeechTranscriber {

    private var recognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** True once this session's first utterance armed — everything after must stay silent. */
    private var chimedThisSession = false

    /** Streams this class muted itself (never ones the user already had muted). */
    private val mutedStreams = mutableListOf<Int>()

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun sessionStarted() {
        // Fresh session: make sure a leaked mute from a crashed session can't swallow the one
        // legitimate "recording started" tone.
        unmuteChimes()
        chimedThisSession = false
    }

    override fun sessionEnded() {
        unmuteChimes()
        chimedThisSession = false
    }

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (SpeechError) -> Unit,
        languageTag: String?,
    ) {
        // Reuse one SpeechRecognizer for the whole dictation session instead of creating a new
        // one per utterance — DictationController re-arms this every few seconds, and creating
        // (without destroying) a fresh SpeechRecognizer each time leaks a bound connection to the
        // recognition service. Left unfixed, those leaked bindings pile up over a single
        // recording and the service stops returning real results — "Listening…" forever with
        // nothing ever transcribed, even though audio is genuinely reaching the device.
        val activeRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        activeRecognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // The OS start tone has fired by now. Continuous dictation re-arms the
                    // recognizer every few seconds (see DictationController), and each re-arm
                    // replays that tone mid-recording — a fragmented capture experience. So the
                    // session's first tone plays as the "recording started" cue, then the tone
                    // streams are muted until sessionEnded()/destroy() restores them.
                    if (!chimedThisSession) {
                        chimedThisSession = true
                        muteChimes()
                    }
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    val speechError = toSpeechError(error)
                    Log.d("VoiceLogSpeech", "onError: code=$error -> ${speechError.message} (recoverable=${speechError.recoverable})")
                    onError(speechError)
                }

                override fun onResults(results: Bundle?) {
                    val text = results.firstTranscript()
                    Log.d("VoiceLogSpeech", "onResults: \"$text\"")
                    onFinal(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults.firstTranscript()
                    Log.d("VoiceLogSpeech", "onPartialResults: \"$text\"")
                    if (text.isNotBlank()) onPartial(text)
                }
            },
        )
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (languageTag != null) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            }
        }
        // Re-arms after the session's first tone must stay silent even if a callback ordering
        // quirk skipped onReadyForSpeech.
        if (chimedThisSession) muteChimes()
        activeRecognizer.startListening(intent)
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
        // Last line of defense: the screen's dispose path must never leave media muted.
        unmuteChimes()
        chimedThisSession = false
    }

    /**
     * The recognizer tone plays on media (and on some OEMs, system) audio; those are the safe
     * streams to touch — ring/notification mutes can throw SecurityException without
     * Do-Not-Disturb access, so they're deliberately left alone.
     */
    private fun muteChimes() {
        if (mutedStreams.isNotEmpty()) return
        listOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_SYSTEM).forEach { stream ->
            runCatching {
                if (!audioManager.isStreamMute(stream)) {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                    mutedStreams.add(stream)
                }
            }
        }
    }

    private fun unmuteChimes() {
        mutedStreams.forEach { stream ->
            runCatching { audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) }
        }
        mutedStreams.clear()
    }

    private fun Bundle?.firstTranscript(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    private fun toSpeechError(error: Int): SpeechError = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechError("No speech recognized", recoverable = true)
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError("No speech detected", recoverable = true)
        SpeechRecognizer.ERROR_AUDIO -> SpeechError("Audio recording error", recoverable = false)
        SpeechRecognizer.ERROR_CLIENT -> SpeechError("Speech recognition client error", recoverable = false)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SpeechError("Missing microphone permission", recoverable = false)
        SpeechRecognizer.ERROR_NETWORK -> SpeechError("Network error", recoverable = false)
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError("Network timeout", recoverable = false)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError("Speech recognizer is busy", recoverable = false)
        SpeechRecognizer.ERROR_SERVER -> SpeechError("Speech recognition server error", recoverable = false)
        else -> SpeechError("Unknown speech recognition error", recoverable = false)
    }
}
