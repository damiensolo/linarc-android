package com.solomondesign.app.ui.voicelog.audio

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FieldDictationBrokerTest {

    @Before
    fun reset() {
        FieldDictationBroker.stopActive()
    }

    @Test
    fun claim_stopsThePreviousTake() {
        var firstStopped = 0
        var secondStopped = 0
        FieldDictationBroker.claim { firstStopped++ }
        FieldDictationBroker.claim { secondStopped++ }
        assertEquals(1, firstStopped)
        assertEquals(0, secondStopped)
    }

    @Test
    fun stopActive_runsTheCurrentTakeOnce() {
        var stopped = 0
        FieldDictationBroker.claim { stopped++ }
        FieldDictationBroker.stopActive()
        FieldDictationBroker.stopActive()
        assertEquals(1, stopped)
    }
}
