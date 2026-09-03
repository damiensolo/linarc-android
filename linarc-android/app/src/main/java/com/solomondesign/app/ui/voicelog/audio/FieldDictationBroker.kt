package com.solomondesign.app.ui.voicelog.audio

/**
 * Only one in-app dictation take runs at a time (form Speak, collab, pin comment). Starting a
 * new take, or arming the camera, stops the current one — the same sequential-mic rule as
 * Voice note + photo.
 */
object FieldDictationBroker {
    private var activeStop: (() -> Unit)? = null

    fun claim(stop: () -> Unit) {
        val previous = activeStop
        activeStop = stop
        if (previous != null && previous !== stop) previous()
    }

    fun release(stop: () -> Unit) {
        if (activeStop === stop) activeStop = null
    }

    fun stopActive() {
        val current = activeStop
        activeStop = null
        current?.invoke()
    }
}
