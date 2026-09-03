package com.solomondesign.app.ui.voicelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.common.DetailPlaceholderScreen
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.voicelog.audio.MediaPlayerAudioPlayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Read-only detail for one submitted [DailyLogRecord] — the actual recorded audio is playable
 * here via a real [android.media.MediaPlayer], not a simulated control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogPlaybackScreen(recordId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val record = remember(recordId) { DailyLogRepository.find(recordId) }
    if (record == null) {
        DetailPlaceholderScreen(
            title = "Daily Log",
            onBack = onBack,
            subtitle = "This recording is no longer available.",
            modifier = modifier,
        )
        return
    }

    val player = remember { MediaPlayerAudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(record.id) {
        onDispose { player.release() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(record.projectName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(record.timestampMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (record.audioFilePath.isBlank()) {
                Text(
                    text = "No recording was saved for this entry — this device can't capture " +
                        "audio for playback and transcription at the same time, so transcription " +
                        "won out (see the Daily Log testing notes).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppButton(
                text = if (isPlaying) "Pause Recording" else "Play Recording",
                enabled = record.audioFilePath.isNotBlank(),
                onClick = {
                    playbackError = null
                    if (isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        try {
                            player.play(record.audioFilePath) { isPlaying = false }
                            isPlaying = true
                        } catch (e: Exception) {
                            playbackError = e.message ?: "Couldn't play this recording"
                            isPlaying = false
                        }
                    }
                },
            )
            playbackError?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()

            Text(text = "Transcript", style = MaterialTheme.typography.titleSmall)
            Text(
                text = record.transcript.ifBlank { "(no speech was captured in this recording)" },
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )

            HorizontalDivider()

            EntitySummarySection("Labor & Time Cards", record.laborCards.map { "${it.name} — ${it.trade} — ${it.hours} hrs" })
            EntitySummarySection("Materials Installed", record.materialCards.map { "${it.quantity.toInt()} ${it.unit} — ${it.description}" })
            EntitySummarySection("Site Delays & Weather", record.delayCards.map { "${it.hours} hrs — ${it.cause}" })
            EntitySummarySection("Issues", record.issueCards.map { "${it.title} — ${it.location}" })
        }
    }
}

@Composable
private fun EntitySummarySection(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Column {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        lines.forEach { line ->
            Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
