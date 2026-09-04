package com.solomondesign.app.ui.voicelog.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    /**
     * Leaves the next utterance open (no scripted result) so stop() has something to finalize.
     * [partial] is delivered first, like a recognizer that has heard words but not finalized yet.
     */
    fun enqueueOpenUtterance(partial: String? = null) {
        script.add { onPartial, onFinal, _ ->
            if (partial != null) onPartial(partial)
            pendingOnFinal = onFinal
        }
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

    /** Real cancel drops the in-flight utterance without delivering anything. */
    override fun cancel() {
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

    /** CLIENT/BUSY are transient: the loop re-arms instead of dying (the old toggle-kill bug). */
    @Test
    fun clientRetryError_rearmsAndRecoversWhenSpeechArrives() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueError(
            SpeechError("Restarting the microphone…", recoverable = true, clientRetry = true),
        )
        transcriber.enqueueFinal("still listening")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("still listening", controller.transcript)
        assertNull(controller.errorMessage)
    }

    /** …but a service that only ever errors gives up with a plain-language message, no spin. */
    @Test
    fun clientRetryErrors_capOut_withAFriendlyMessage() {
        val transcriber = FakeSpeechTranscriber()
        repeat(10) {
            transcriber.enqueueError(
                SpeechError("Restarting the microphone…", recoverable = true, clientRetry = true),
            )
        }
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals(
            "The microphone stopped responding — tap Resume to try again.",
            controller.errorMessage,
        )
        // Initial arm + 5 retries, then the cap trips — never all 10 scripted errors.
        assertEquals(6, transcriber.startCount)
        assertEquals(1, transcriber.sessionEndedCount)
    }

    /** Silence no-matches between client retries keep the retry budget fresh. */
    @Test
    fun silenceBetweenClientErrors_resetsTheRetryStreak() {
        val transcriber = FakeSpeechTranscriber()
        repeat(4) {
            transcriber.enqueueError(
                SpeechError("Restarting the microphone…", recoverable = true, clientRetry = true),
            )
        }
        transcriber.enqueueError(SpeechError("No speech detected", recoverable = true))
        repeat(4) {
            transcriber.enqueueError(
                SpeechError("Restarting the microphone…", recoverable = true, clientRetry = true),
            )
        }
        transcriber.enqueueFinal("made it through")
        val controller = DictationController(transcriber)

        controller.start()

        assertEquals("made it through", controller.transcript)
        assertNull(controller.errorMessage)
    }

    /** Re-record must actually discard: a late result from the canceled take is ignored. */
    @Test
    fun reset_dropsTheInFlightUtterance_soLateResultsAreIgnored() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueOpenUtterance()
        val controller = DictationController(transcriber)
        controller.start()

        controller.reset()
        transcriber.lastOnFinal?.invoke("stale words from the thrown-away take")

        assertEquals("", controller.transcript)
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

        // Re-armed in Spanish immediately — without waiting on the old utterance to call back.
        assertEquals(listOf<String?>("en-US", "es-US"), transcriber.startLanguageTags)
        assertTrue(controller.isListening)
    }

    @Test
    fun setLanguage_midDictation_keepsTheWordsAlreadyOnScreen() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueFinal("first sentence")
        transcriber.enqueueOpenUtterance(partial = "segunda frase")
        val controller = DictationController(transcriber)
        controller.setLanguage("en-US")
        controller.start()
        assertEquals("segunda frase", controller.partial)

        controller.setLanguage("es-US")

        // The in-flight partial is committed rather than dropped with the canceled utterance.
        assertEquals("first sentence segunda frase", controller.transcript)
        assertEquals("", controller.partial)
    }

    @Test
    fun setLanguage_midDictation_ignoresTheOldLanguageUtteranceWhenItFinallyCallsBack() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueOpenUtterance()
        val controller = DictationController(transcriber)
        controller.setLanguage("en-US")
        controller.start()
        val staleOnFinal = transcriber.lastOnFinal!!

        controller.setLanguage("es-US")
        staleOnFinal("late english words")

        // No words from the disowned arm, and no second re-arm racing the Spanish one.
        assertEquals("", controller.transcript)
        assertEquals(2, transcriber.startCount)
    }

    @Test
    fun setLanguage_whileStopped_appliesToTheNextStart() {
        val transcriber = FakeSpeechTranscriber()
        val controller = DictationController(transcriber)
        controller.setLanguage("en-US")

        controller.setLanguage("es-US")
        controller.start()

        assertEquals(listOf<String?>("es-US"), transcriber.startLanguageTags)
    }

    @Test
    fun setLanguage_sameLanguage_doesNotDisturbTheTake() {
        val transcriber = FakeSpeechTranscriber()
        transcriber.enqueueOpenUtterance(partial = "still talking")
        val controller = DictationController(transcriber)
        controller.setLanguage("en-US")
        controller.start()

        controller.setLanguage("en-US")

        assertEquals(1, transcriber.startCount)
        assertEquals("still talking", controller.partial)
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
