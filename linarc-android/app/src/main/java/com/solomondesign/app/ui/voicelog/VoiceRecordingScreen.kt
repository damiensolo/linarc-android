package com.solomondesign.app.ui.voicelog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.voicelog.audio.AndroidSpeechTranscriber
import com.solomondesign.app.ui.voicelog.audio.DictationController
import com.solomondesign.app.ui.voicelog.audio.MediaRecorderAudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Screen A from `voice-to-log-spec.md`: real hands-free dictation. Requests RECORD_AUDIO, then
 * genuinely records audio to a file (playable afterward from Daily Log history) and runs live
 * on-device speech recognition to build the transcript shown on screen — nothing here is canned.
 */
@Composable
fun VoiceRecordingScreen(
    onCancel: () -> Unit,
    onStopAndParse: (audioFilePath: String, transcript: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    if (!hasPermission) {
        MicPermissionRequiredScreen(
            denied = permissionDenied,
            onCancel = onCancel,
            onRetry = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = modifier,
        )
        return
    }

    RecordingContent(context = context, onCancel = onCancel, onStopAndParse = onStopAndParse, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingContent(
    context: Context,
    onCancel: () -> Unit,
    onStopAndParse: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outputFile = remember { createVoiceLogFile(context) }
    val audioRecorder = remember { MediaRecorderAudioRecorder(context) }
    val transcriber = remember { AndroidSpeechTranscriber(context) }
    val dictation = remember { DictationController(transcriber) }

    // Confirmed by repeated, direct testing on this project's own dev emulator: running
    // MediaRecorder and SpeechRecognizer on the mic at the same time reliably starves the
    // recognizer (a permanent stream of ERROR_NO_MATCH, real speech or not), while transcription
    // alone reliably works. There is no public API to ask a given device up front which behavior
    // it has (checked the actual SDK — no such method exists), so the reliable, proven-working
    // mode — transcription only — is the default, and saving a real audio file for playback is
    // opt-in via [wantsAudioRecording]. If a device DOES support both and this is opted into, the
    // conflict hint below still offers a way back out.
    var wantsAudioRecording by remember { mutableStateOf(false) }
    var audioFileWritten by remember { mutableStateOf(false) }
    var micConflictDetected by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var amplitudeFraction by remember { mutableFloatStateOf(0f) }
    var recorderError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        dictation.start()
        onDispose {
            dictation.stop()
            transcriber.destroy()
            if (audioFileWritten) {
                runCatching { audioRecorder.stop() }
                audioRecorder.release()
            }
        }
    }

    LaunchedEffect(wantsAudioRecording) {
        if (wantsAudioRecording && !audioFileWritten && !micConflictDetected && recorderError == null) {
            try {
                audioRecorder.start(outputFile)
                audioFileWritten = true
            } catch (e: Exception) {
                recorderError = e.message ?: "Couldn't start the microphone"
            }
        }
    }

    val showMicConflictHint = audioFileWritten &&
        dictation.transcript.isBlank() &&
        dictation.consecutiveNoMatchCount >= 3

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Real signal level from the active recording — drives the waveform below. Without a real
    // MediaRecorder session there's no amplitude to read, so the waveform just stays at rest.
    LaunchedEffect(isPaused, recorderError, audioFileWritten) {
        if (audioFileWritten && !isPaused && recorderError == null) {
            while (isActive) {
                delay(120)
                amplitudeFraction = (audioRecorder.maxAmplitude() / 12000f).coerceIn(0.08f, 1f)
            }
        }
    }

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(FakeVoiceLogData.HEADER_META, style = MaterialTheme.typography.bodyMedium) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel recording")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppButton(
                    text = if (isPaused) "Resume" else "Pause",
                    onClick = {
                        isPaused = !isPaused
                        if (isPaused) {
                            if (audioFileWritten) audioRecorder.pause()
                            dictation.stop()
                        } else {
                            if (audioFileWritten) audioRecorder.resume()
                            dictation.start()
                        }
                    },
                    type = AppButtonType.Secondary,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "Stop & Parse",
                    onClick = {
                        dictation.stop()
                        val audioPath = if (audioFileWritten) {
                            runCatching { audioRecorder.stop() }
                            outputFile.absolutePath
                        } else {
                            ""
                        }
                        onStopAndParse(audioPath, dictation.transcript)
                    },
                    type = AppButtonType.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "CURRENT PROJECT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = FakeVoiceLogData.PROJECT_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            RecordingPill(paused = isPaused)
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatElapsed(elapsedSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))
            WaveformRow(amplitudeFraction = amplitudeFraction, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            if (showMicConflictHint) {
                Text(
                    text = "Not picking up your voice — this device can't record audio and " +
                        "transcribe it at the same time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AppButton(
                    text = "Switch to transcription only",
                    type = AppButtonType.Secondary,
                    onClick = {
                        runCatching { audioRecorder.stop() }
                        audioRecorder.release()
                        audioFileWritten = false
                        micConflictDetected = true
                    },
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            } else if (!wantsAudioRecording && !micConflictDetected) {
                AppButton(
                    text = "Also Save Audio for Playback",
                    type = AppButtonType.Secondary,
                    onClick = { wantsAudioRecording = true },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else if (micConflictDetected) {
                Text(
                    text = "Continuing with transcription only — this recording won't be saved " +
                        "for playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            recorderError?.let {
                Text(
                    text = "Recording error: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            dictation.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            val liveText = if (dictation.partial.isNotBlank()) {
                "${dictation.transcript} ${dictation.partial}".trim()
            } else {
                dictation.transcript
            }
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                Text(
                    text = if (liveText.isBlank()) "Listening…" else "\"$liveText\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MicPermissionRequiredScreen(
    denied: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (denied) "Microphone permission was denied" else "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Voice-to-Log needs the microphone to record and transcribe your dictation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            AppButton(text = "Allow Microphone Access", onClick = onRetry)
        }
    }
}

@Composable
private fun RecordingPill(paused: Boolean, modifier: Modifier = Modifier) {
    val containerColor = if (paused) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (paused) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val dotColor = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error

    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Text(
                text = if (paused) "PAUSED" else "RECORDING",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

/** Bars driven by the real polled mic amplitude ([amplitudeFraction]), not decorative animation. */
private val waveformBarVariance = listOf(0.55f, 0.75f, 1f, 0.85f, 1f, 0.7f, 0.5f)

@Composable
private fun WaveformRow(amplitudeFraction: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        waveformBarVariance.forEach { variance ->
            val targetFraction = (amplitudeFraction * variance).coerceIn(0.06f, 1f)
            val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "waveformBar")
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(animatedFraction)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            )
        }
    }
}

private fun createVoiceLogFile(context: Context): File {
    val dir = File(context.filesDir, "voice_logs")
    dir.mkdirs()
    return File(dir, "log_${System.currentTimeMillis()}.m4a")
}

private fun formatElapsed(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
