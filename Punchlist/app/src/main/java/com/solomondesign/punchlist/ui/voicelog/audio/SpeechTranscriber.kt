package com.solomondesign.punchlist.ui.voicelog.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/** A speech-recognition error. [recoverable] errors (silence/no-match) should just restart listening. */
data class SpeechError(val message: String, val recoverable: Boolean)

/**
 * Converts live microphone speech to text — one *single utterance* per [start] call, matching
 * the real Android [SpeechRecognizer] contract. [com.solomondesign.punchlist.ui.voicelog.audio.DictationController]
 * builds continuous, hands-free dictation out of repeated single-utterance calls to this.
 * Abstracted so the dictation loop is unit-testable without the Android speech framework.
 */
interface SpeechTranscriber {
    /** Whether this device actually has a speech-recognition service installed. */
    fun isAvailable(): Boolean

    fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (SpeechError) -> Unit)
    fun stop()
    fun destroy()
}

/** Real [SpeechTranscriber] backed by the OS [SpeechRecognizer] (on-device or Google's cloud recognizer). */
class AndroidSpeechTranscriber(private val context: Context) : SpeechTranscriber {

    private var recognizer: SpeechRecognizer? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (SpeechError) -> Unit) {
        // Reuse one SpeechRecognizer for the whole dictation session instead of creating a new
        // one per utterance — DictationController re-arms this every few seconds, and creating
        // (without destroying) a fresh SpeechRecognizer each time leaks a bound connection to the
        // recognition service. Left unfixed, those leaked bindings pile up over a single
        // recording and the service stops returning real results — "Listening…" forever with
        // nothing ever transcribed, even though audio is genuinely reaching the device.
        val activeRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        activeRecognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
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
        }
        activeRecognizer.startListening(intent)
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
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
