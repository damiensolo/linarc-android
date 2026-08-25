package com.solomondesign.app.ui.capture.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which physical camera the capture screen is driving. */
enum class CameraLens { REAR, FRONT }

/** What the shutter does: still photo, or a video clip (capped, then described). */
enum class CameraMode { PHOTO, VIDEO }

data class CameraUiState(
    val lens: CameraLens = CameraLens.REAR,
    val hasRearCamera: Boolean = true,
    val hasFrontCamera: Boolean = false,
    val torchOn: Boolean = false,
    /** Whether the *active* lens has a flash unit — resolved from CameraInfo after binding. */
    val torchAvailable: Boolean = false,
    val mode: CameraMode = CameraMode.PHOTO,
    /** Turn on before shooting to land in the markup editor right after the shutter. */
    val markupAfterCapture: Boolean = false,
    /** True from shutter press until the file is written; blocks re-entrant captures. */
    val isCapturing: Boolean = false,
    /** True from record press until the recording finalizes; blocks mode/lens changes. */
    val isRecording: Boolean = false,
    /** Elapsed recording time, fed from the recorder's own status events. */
    val recordedSeconds: Int = 0,
) {
    val canFlip: Boolean get() = hasRearCamera && hasFrontCamera && !isRecording
}

/**
 * Pure control rules for the camera screen, kept free of CameraX types so they run under
 * `./gradlew testDebugUnitTest` — the same JVM-testability split `AppChrome` uses. Snapshot state
 * rather than a raw var so Compose recomposes on every transition.
 */
class CameraStateHolder(initial: CameraUiState = CameraUiState()) {

    var state: CameraUiState by mutableStateOf(initial)
        private set

    /**
     * Reported once camera initialization finishes. If the current lens doesn't exist on this
     * device (e.g. a rear-only rig, or an emulator with only a front webcam), falls over to the
     * one that does.
     */
    fun setAvailableLenses(hasRear: Boolean, hasFront: Boolean) {
        val lens = when {
            state.lens == CameraLens.REAR && !hasRear && hasFront -> CameraLens.FRONT
            state.lens == CameraLens.FRONT && !hasFront && hasRear -> CameraLens.REAR
            else -> state.lens
        }
        state = state.copy(lens = lens, hasRearCamera = hasRear, hasFrontCamera = hasFront)
    }

    /**
     * Torch state never carries across a lens swap: the new camera reports its own flash unit
     * asynchronously, so until it does the torch is off and unavailable.
     */
    fun toggleLens() {
        if (!state.canFlip) return
        val next = if (state.lens == CameraLens.REAR) CameraLens.FRONT else CameraLens.REAR
        state = state.copy(lens = next, torchOn = false, torchAvailable = false)
    }

    fun setTorchAvailable(available: Boolean) {
        state = state.copy(
            torchAvailable = available,
            torchOn = state.torchOn && available,
        )
    }

    fun toggleTorch() {
        if (!state.torchAvailable) return
        state = state.copy(torchOn = !state.torchOn)
    }

    fun toggleMarkupAfterCapture() {
        state = state.copy(markupAfterCapture = !state.markupAfterCapture)
    }

    /** Ignored mid-capture/mid-recording: the active use case can't be swapped under itself. */
    fun selectMode(mode: CameraMode) {
        if (state.isCapturing || state.isRecording) return
        state = state.copy(mode = mode)
    }

    /** Returns false when a capture is already in flight, so the shutter can't double-fire. */
    fun beginCapture(): Boolean {
        if (state.isCapturing || state.isRecording) return false
        state = state.copy(isCapturing = true)
        return true
    }

    fun endCapture() {
        state = state.copy(isCapturing = false)
    }

    /** Returns false when a recording is already in flight, so record can't double-fire. */
    fun beginRecording(): Boolean {
        if (state.isCapturing || state.isRecording) return false
        state = state.copy(isRecording = true, recordedSeconds = 0)
        return true
    }

    fun setRecordingElapsed(seconds: Int) {
        if (!state.isRecording) return
        state = state.copy(recordedSeconds = seconds)
    }

    fun endRecording() {
        state = state.copy(isRecording = false, recordedSeconds = 0)
    }
}
