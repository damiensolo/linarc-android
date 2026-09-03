package com.solomondesign.app.ui.voicelog.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A scriptable [SpeechTranscriber] test double. [start] is real Android's `SpeechRecognizer`
 * contract: one utterance per call — the caller (here, [DictationController]) is expected to
 * call it again to keep listening.
 */
private class FakeSpeechTranscriber(private val available: Boolean = true) : SpeechTranscriber {
    var startCount = 0
        private set

    /** The most recent onFinal callback handed to [start] — lets a test fire a "late" result. */
    var lastOnFinal: ((String) -> Unit)? = null
        private set

    /** Language tag of every [start] call, in order — how a test proves a mid-take switch. */
    val startLanguageTags = mutableListOf<String?>()

    /** Session brackets — the chime-control contract: one start per take, always one end. */
    var sessionStartedCount = 0
        private set
    var sessionEndedCount = 0
        private set

    override fun sessionStarted() {
        sessionStartedCount++
    }

    override fun sessionEnded() {
        sessionEndedCount++
    }

    /** Set while an utterance is "in flight"; [stop] finalizes it, like the real recognizer. */
    private var pendingOnFinal: ((String) -> Unit)? = null

    private val script = ArrayDeque<(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (SpeechError) -> Unit) -> Unit>()

    fun enqueueFinal(text: String) {
        script.add { _, onFinal, _ -> onFinal(text) }
    }

    fun enqueueError(error: SpeechError) {
        script.add { _, _, onError -> onError(error) }
    }

    /** Leaves the next utterance open (no scripted result) so stop() has something to finalize. */
    fun enqueueOpenUtterance() {
        script.add { _, onFinal, _ -> pendingOnFinal = onFinal }
    }

    override fun isAvailable(): Boolean = available

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (SpeechError) -> Unit,
        languageTag: String?,
    ) {
        startCount++
        lastOnFinal = onFinal
        startLanguageTags.add(languageTag)
        script.removeFirstOrNull()?.invoke(onPartial, onFinal, onError)
    }

    override fun stop() {
        pendingOnFinal?.invoke("")
        pendingOnFinal = null
    }

    override fun destroy() = Unit
}

class DictationControllerTest {

    @Test
    fun start_unavailableDevice_setsErrorAndNeverListens() {
        val transcriber = FakeSpeechTranscriber(available = false)
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals(0, transcriber.startCount)
        assertEquals("Speech recognition isn't available on this device.", controller.errorMessage)
    }

    @Test
    fun start_singleUtterance_accumulatesTranscript() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("hello world")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("hello world", controller.transcript)
        assertNull(controller.errorMessage)
    }

    @Test
    fun recoverableTimeout_restartsListeningAndKeepsAccumulating() {
        // Mirrors real continuous dictation: SpeechRecognizer times out during a pause, then
        // picks back up once the user keeps talking.
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("first sentence")
        transcriber.enqueueError(SpeechError("No speech detected", recoverable = true))
        transcriber.enqueueFinal("second sentence")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("first sentence second sentence", controller.transcript)
        // 3 scripted turns, plus one more listenOnce() after the last result — dictation keeps
        // listening indefinitely until stop() is called, which is exactly the point of this test.
        assertEquals(4, transcriber.startCount)
        assertNull(controller.errorMessage)
    }

    @Test
    fun nonRecoverableError_stopsListeningAndSurfacesMessage() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("partial take")
        transcriber.enqueueError(SpeechError("Speech recognizer is busy", recoverable = false))
        transcriber.enqueueFinal("this should never be heard")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("partial take", controller.transcript)
        assertEquals("Speech recognizer is busy", controller.errorMessage)
        // The loop must have actually stopped instead of continuing to call start().
        assertEquals(2, transcriber.startCount)
    }

    @Test
    fun repeatedRecoverableErrors_incrementStreak_andRealSpeechResetsIt() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueError(SpeechError("No speech detected", recoverable = true))
        transcriber.enqueueError(SpeechError("No speech detected", recoverable = true))
        transcriber.enqueueError(SpeechError("No speech detected", recoverable = true))
        transcriber.enqueueFinal("finally heard something")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("finally heard something", controller.transcript)
        assertEquals(0, controller.consecutiveNoMatchCount)
    }

    @Test
    fun startAndStop_bracketTheSession_forChimeControl() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("hello")
        val controller = DictationController(transcriber)

        controller.start()
        assertEquals(1, transcriber.sessionStartedCount)
        assertEquals(0, transcriber.sessionEndedCount)

        controller.stop()
        assertEquals(1, transcriber.sessionEndedCount)
    }

    @Test
    fun nonRecoverableError_alsoEndsTheSession_soMutedAudioIsRestored() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueError(SpeechError("Speech recognizer is busy", recoverable = false))
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals(1, transcriber.sessionStartedCount)
        assertEquals(1, transcriber.sessionEndedCount)
    }

    @Test
    fun unavailableDevice_neverOpensASession() {
        val transcriber = FakeSpeechTranscriber(available = false)
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals(0, transcriber.sessionStartedCount)
    }

    @Test
    fun setLanguage_midDictation_rearmsTheRecognizerInTheNewLanguage() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueOpenUtterance()
        val controller = DictationController(transcriber)
        controller.setLanguage("en-US")
        controller.start()
        assertEquals(listOf<String?>("en-US"), transcriber.startLanguageTags)

        controller.setLanguage("es-US")

        // The in-flight utterance was stopped and finalized, so the loop re-armed — in Spanish.
        assertEquals(listOf<String?>("en-US", "es-US"), transcriber.startLanguageTags)
    }

    @Test
    fun reset_clearsAccumulatedState_forAFreshTake() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("first take")
        val controller = DictationController(transcriber)
        controller.start()
        assertEquals("first take", controller.transcript)

        controller.reset()

        assertEquals("", controller.transcript)
        assertNull(controller.errorMessage)
        assertEquals(0, controller.consecutiveNoMatchCount)
    }

    @Test
    fun stop_thenLateResultArrives_doesNotRestartListening() {
        // A real async race: the recognizer is mid-utterance when the user taps Stop & Parse,
        // and its result callback fires after stop() was already requested.
        val transcriber = FakeSpeechTranscriber()
        val controller = DictationController(transcriber)
        controller.start()
        assertEquals(1, transcriber.startCount)

        controller.stop()
        transcriber.lastOnFinal?.invoke("late result")

        assertEquals("late result", controller.transcript)
        assertEquals(1, transcriber.startCount)
    }

    @Test
    fun start_whileAlreadyListening_isANoOp() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueOpenUtterance()
        val controller = DictationController(transcriber)
        controller.start()
        assertEquals(1, transcriber.startCount)

        controller.start()

        assertEquals(1, transcriber.startCount)
        assertEquals(1, transcriber.sessionStartedCount)
    }

    @Test
    fun stopThenStart_keepsTranscript_forPauseResume() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("first pass")
        transcriber.enqueueOpenUtterance()
        val controller = DictationController(transcriber)
        controller.start()
        assertEquals("first pass", controller.transcript)

        controller.stop()
        transcriber.enqueueFinal("after the truck passed")
        controller.start()

        assertEquals("first pass after the truck passed", controller.transcript)
        assertEquals(2, transcriber.sessionStartedCount)
    }
}
