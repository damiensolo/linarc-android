package com.solomondesign.app.ui.video

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.solomondesign.app.ui.designsystem.DesignTokens

/**
 * Inline playback of a captured clip via the framework [VideoView] — deliberately no
 * media3/ExoPlayer dependency for a prototype playing one short local mp4. Tap toggles
 * play/pause; a centered play glyph shows whenever paused. Black letterboxing behind the video
 * is the video-surface convention, not a theme violation (same reasoning as the camera chrome).
 */
@Composable
fun VideoPlayerBox(
    videoPath: String,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember(videoPath) { mutableStateOf(false) }
    val player = remember { arrayOfNulls<VideoView>(1) }

    DisposableEffect(videoPath) {
        onDispose {
            player[0]?.stopPlayback()
            player[0] = null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.CardCornerRadius))
            .background(Color.Black)
            .clickable(onClickLabel = if (isPlaying) "Pause video" else "Play video") {
                val view = player[0] ?: return@clickable
                if (isPlaying) view.pause() else view.start()
                isPlaying = !isPlaying
            }
            .testTag("videoPlayer"),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { viewContext -> VideoView(viewContext) },
            update = { view ->
                player[0] = view
                if (view.tag != videoPath) {
                    view.stopPlayback()
                    view.tag = videoPath
                    view.setVideoPath(videoPath)
                    view.setOnPreparedListener { it.seekTo(1) }
                    view.setOnCompletionListener { isPlaying = false }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
