package com.solomondesign.app.ui.capture.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraStateHolderTest {

    @Test
    fun lensFlip_alwaysDropsTorchStateAndAvailability() {
        val holder = CameraStateHolder()
        holder.setAvailableLenses(hasRear = true, hasFront = true)
        holder.setTorchAvailable(true)
        holder.toggleTorch()
        assertTrue(holder.state.torchOn)

        holder.toggleLens()

        assertEquals(CameraLens.FRONT, holder.state.lens)
        assertFalse("torch must not carry across a lens swap", holder.state.torchOn)
        assertFalse("new lens hasn't reported a flash unit yet", holder.state.torchAvailable)
    }

    @Test
    fun lensFlip_isANoOpOnSingleCameraDevices() {
        val holder = CameraStateHolder()
        holder.setAvailableLenses(hasRear = true, hasFront = false)

        holder.toggleLens()

        assertEquals(CameraLens.REAR, holder.state.lens)
        assertFalse(holder.state.canFlip)
    }

    @Test
    fun frontOnlyDevice_fallsOverToTheFrontLens() {
        val holder = CameraStateHolder()

        holder.setAvailableLenses(hasRear = false, hasFront = true)

        assertEquals(CameraLens.FRONT, holder.state.lens)
        assertFalse(holder.state.canFlip)
    }

    @Test
    fun torchToggle_isANoOpWhenNoFlashUnitWasReported() {
        val holder = CameraStateHolder()

        holder.toggleTorch()

        assertFalse(holder.state.torchOn)
    }

    @Test
    fun losingTheFlashUnit_forcesTheTorchOff() {
        val holder = CameraStateHolder()
        holder.setTorchAvailable(true)
        holder.toggleTorch()
        assertTrue(holder.state.torchOn)

        holder.setTorchAvailable(false)

        assertFalse(holder.state.torchOn)
        assertFalse(holder.state.torchAvailable)
    }

    @Test
    fun beginCapture_latchesUntilEndCapture_soTheShutterCannotDoubleFire() {
        val holder = CameraStateHolder()

        assertTrue(holder.beginCapture())
        assertFalse("a capture is already in flight", holder.beginCapture())

        holder.endCapture()
        assertTrue(holder.beginCapture())
    }

    @Test
    fun beginRecording_latchesUntilEndRecording_andResetsTheClock() {
        val holder = CameraStateHolder()

        assertTrue(holder.beginRecording())
        assertFalse("a recording is already in flight", holder.beginRecording())
        assertFalse("photo capture can't start under an active recording", holder.beginCapture())

        holder.setRecordingElapsed(42)
        assertEquals(42, holder.state.recordedSeconds)

        holder.endRecording()
        assertFalse(holder.state.isRecording)
        assertEquals("the clock must not leak into the next recording", 0, holder.state.recordedSeconds)
    }

    @Test
    fun recordingElapsed_isIgnoredOnceTheRecordingFinalized() {
        val holder = CameraStateHolder()
        holder.beginRecording()
        holder.endRecording()

        // A late Status event from the recorder's executor must not resurrect the timer.
        holder.setRecordingElapsed(7)

        assertEquals(0, holder.state.recordedSeconds)
    }

    @Test
    fun modeSelection_isBlockedWhileRecordingOrCapturing() {
        val holder = CameraStateHolder()

        holder.selectMode(CameraMode.VIDEO)
        assertEquals(CameraMode.VIDEO, holder.state.mode)

        holder.beginRecording()
        holder.selectMode(CameraMode.PHOTO)
        assertEquals("the active use case can't be swapped mid-recording", CameraMode.VIDEO, holder.state.mode)
        holder.endRecording()

        holder.selectMode(CameraMode.PHOTO)
        holder.beginCapture()
        holder.selectMode(CameraMode.VIDEO)
        assertEquals(CameraMode.PHOTO, holder.state.mode)
    }

    @Test
    fun lensFlip_isBlockedWhileRecording() {
        val holder = CameraStateHolder()
        holder.setAvailableLenses(hasRear = true, hasFront = true)
        holder.beginRecording()

        assertFalse(holder.state.canFlip)
        holder.toggleLens()

        assertEquals(CameraLens.REAR, holder.state.lens)
    }

    @Test
    fun markupAfterCapture_togglesForTheUpcomingEditorIteration() {
        val holder = CameraStateHolder()
        assertFalse(holder.state.markupAfterCapture)

        holder.toggleMarkupAfterCapture()
        assertTrue(holder.state.markupAfterCapture)

        holder.toggleMarkupAfterCapture()
        assertFalse(holder.state.markupAfterCapture)
    }
}
