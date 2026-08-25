package com.solomondesign.app.ui.capture.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.voicelog.audio.AndroidSpeechTranscriber
import com.solomondesign.app.ui.voicelog.audio.DictationController

/**
 * Spoken description right after a video is recorded. Dictation is sequential with recording:
 * SpeechRecognizer and the recorder can't share the microphone on this project's devices
 * (see `VoiceRecordingScreen`), so the mic is free by the time this screen starts listening.
 *
 * Always skippable — a denied mic or a quiet jobsite must never trap the video. Close discards
 * the clip (guarded by the scaffold's discard dialog).
 */
@Composable
fun VideoDescribeScreen(
    onCancel: () -> Unit,
    onDone: (transcript: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    TaskFlowScaffold(
        title = "Describe what you saw",
        onClose = onCancel,
        modifier = modifier,
        hasUnsavedChanges = true,
        discardTitle = "Discard video?",
        discardMessage = "The video and its description haven't been saved and will be lost.",
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("videoDescribeScreen"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Talk through what you saw — where it is, what's wrong, who needs to know. " +
                    "A title is drafted from what you say.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (hasMicPermission) {
                DescribeDictation(onSkip = { onDone("") }, onUse = onDone)
            } else {
                Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Microphone access is needed to dictate a description.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                AppButton(
                    text = "Allow microphone",
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
                Spacer(Modifier.height(12.dp))
                AppButton(
                    text = "Skip description",
                    type = AppButtonType.Secondary,
                    onClick = { onDone("") },
                    modifier = Modifier.testTag("videoSkipDescription"),
                )
            }
        }
    }
}

@Composable
private fun DescribeDictation(
    onSkip: () -> Unit,
    onUse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val transcriber = remember { AndroidSpeechTranscriber(context) }
    val dictation = remember { DictationController(transcriber) }

    DisposableEffect(Unit) {
        onDispose {
            dictation.stop()
            transcriber.destroy()
        }
    }
    LaunchedEffect(Unit) { dictation.start() }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        dictation.errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }

        val liveText = if (dictation.partial.isNotBlank()) {
            "${dictation.transcript} ${dictation.partial}".trim()
        } else {
            dictation.transcript
        }
        Text(
            text = if (liveText.isBlank()) "Listening…" else "\"$liveText\"",
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("videoLiveTranscript"),
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(
                text = "Skip",
                type = AppButtonType.Secondary,
                onClick = {
                    dictation.stop()
                    onSkip()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("videoSkipDescription"),
            )
            AppButton(
                text = "Use description",
                onClick = {
                    dictation.stop()
                    onUse(dictation.transcript)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("videoUseDescription"),
            )
        }
    }
}
