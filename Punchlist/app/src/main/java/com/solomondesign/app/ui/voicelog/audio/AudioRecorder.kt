package com.solomondesign.app.ui.voicelog.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Real microphone capture — abstracted so the recording flow is testable without hardware. */
interface AudioRecorder {
    fun start(outputFile: File)
    fun pause()
    fun resume()

    /** Stops and finalizes the output file. Safe to call even if never started. */
    fun stop()
    fun release()

    /** 0..32767, sampled since the last call. Real signal level, used to drive the waveform UI. */
    fun maxAmplitude(): Int
}

/** [AudioRecorder] backed by [android.media.MediaRecorder], writing real AAC/MP4 audio to disk. */
class MediaRecorderAudioRecorder(private val context: Context) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var started = false

    override fun start(outputFile: File) {
        outputFile.parentFile?.mkdirs()
        @Suppress("DEPRECATION")
        val newRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        newRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = newRecorder
        started = true
    }

    override fun pause() {
        if (Build.VERSION.SDK_INT >= 24) {
            runCatching { recorder?.pause() }
        }
    }

    override fun resume() {
        if (Build.VERSION.SDK_INT >= 24) {
            runCatching { recorder?.resume() }
        }
    }

    override fun stop() {
        val current = recorder ?: return
        if (started) {
            // stop() throws IllegalStateException if start() never produced any output (e.g.
            // stopped within milliseconds of starting) — not a real failure for a short demo take.
            runCatching { current.stop() }
        }
        current.reset()
        started = false
    }

    override fun release() {
        recorder?.release()
        recorder = null
    }

    override fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
}
