package com.solomondesign.app.ui.voicelog.audio

import android.media.MediaPlayer

/** Plays back a real recorded audio file — abstracted so playback screens are testable. */
interface AudioPlayer {
    val isPlaying: Boolean
    fun play(filePath: String, onCompletion: () -> Unit)
    fun pause()
    fun release()
}

/** [AudioPlayer] backed by [android.media.MediaPlayer], for real playback of a recorded take. */
class MediaPlayerAudioPlayer : AudioPlayer {
    private var player: MediaPlayer? = null

    override var isPlaying: Boolean = false
        private set

    override fun play(filePath: String, onCompletion: () -> Unit) {
        val existing = player
        if (existing != null) {
            existing.start()
            isPlaying = true
            return
        }
        player = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                this@MediaPlayerAudioPlayer.isPlaying = false
                onCompletion()
            }
            prepare()
            start()
        }
        isPlaying = true
    }

    override fun pause() {
        runCatching { player?.pause() }
        isPlaying = false
    }

    override fun release() {
        player?.release()
        player = null
        isPlaying = false
    }
}
