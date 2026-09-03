package com.solomondesign.app.ui.voicenote

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonSize
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.voicelog.audio.AndroidSpeechTranscriber
import com.solomondesign.app.ui.voicelog.audio.DictationController
import com.solomondesign.app.ui.voicelog.audio.FieldDictationBroker

/**
 * Long-text field with an explicit **Speak** control. Keyboard voice typing still works if the
 * user focuses the field; Speak is the gloves-sized path. Only one take runs at a time (see
 * [FieldDictationBroker]); words append into this field and stay editable after Stop.
 */
@Composable
fun SpeakableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    fieldTestTag: String,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
    singleLine: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    fieldModifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val transcriber = remember { AndroidSpeechTranscriber(context) }
    val dictation = remember {
        DictationController(transcriber).apply { setLanguage(VoiceNoteLanguage.ENGLISH.speechTag) }
    }
    var language by remember { mutableStateOf(VoiceNoteLanguage.ENGLISH) }
    var prefix by remember { mutableStateOf("") }
    var permissionDenied by remember { mutableStateOf(false) }

    val halt = remember {
        lateinit var stop: () -> Unit
        stop = {
            dictation.stop()
            FieldDictationBroker.release(stop)
        }
        stop
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) beginSpeak(dictation, value, onPrefix = { prefix = it }, halt = halt)
    }

    DisposableEffect(dictation, transcriber) {
        onDispose {
            FieldDictationBroker.release(halt)
            dictation.stop()
            transcriber.destroy()
        }
    }

    LaunchedEffect(dictation.isListening, dictation.transcript, dictation.partial) {
        if (!dictation.isListening) return@LaunchedEffect
        val spoken = "${dictation.transcript} ${dictation.partial}".trim()
        val combined = when {
            prefix.isBlank() -> spoken
            spoken.isBlank() -> prefix
            else -> "$prefix $spoken"
        }
        if (combined != value) onValueChange(combined)
    }

    val speak: () -> Unit = {
        if (dictation.isListening) {
            halt()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                permissionDenied = false
                beginSpeak(dictation, value, onPrefix = { prefix = it }, halt = halt)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val fieldInputModifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(fieldModifier)
            .testTag(fieldTestTag)

        OutlinedTextField(
            value = value,
            onValueChange = { typed ->
                if (dictation.isListening) halt()
                onValueChange(typed)
            },
            label = label,
            placeholder = placeholder,
            minLines = minLines,
            singleLine = singleLine,
            isError = isError,
            supportingText = when {
                permissionDenied -> {
                    { Text("Microphone permission is needed to speak into this field.") }
                }
                dictation.errorMessage != null -> {
                    { Text(dictation.errorMessage!!) }
                }
                dictation.isListening -> {
                    {
                        Text(
                            "Listening… tap Stop when you’re done.",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                else -> supportingText
            },
            enabled = enabled,
            readOnly = dictation.isListening,
            modifier = fieldInputModifier,
        )

        val speakButton = @Composable {
            AppButton(
                text = if (dictation.isListening) "Stop" else "Speak",
                onClick = speak,
                type = if (dictation.isListening) AppButtonType.Primary else AppButtonType.Secondary,
                size = if (compact) AppButtonSize.Small else AppButtonSize.Large,
                enabled = enabled,
                modifier = Modifier.testTag("${fieldTestTag}_speak"),
            )
        }

        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                speakButton()
                VoiceLanguageToggle(
                    selected = language,
                    onSelect = { next ->
                        language = next
                        dictation.setLanguage(next.speechTag)
                    },
                    testTagPrefix = "${fieldTestTag}_lang",
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            VoiceLanguageToggle(
                selected = language,
                onSelect = { next ->
                    language = next
                    dictation.setLanguage(next.speechTag)
                },
                testTagPrefix = "${fieldTestTag}_lang",
            )
            speakButton()
        }
    }
}

private fun beginSpeak(
    dictation: DictationController,
    currentValue: String,
    onPrefix: (String) -> Unit,
    halt: () -> Unit,
) {
    FieldDictationBroker.claim(halt)
    onPrefix(currentValue.trimEnd())
    dictation.reset()
    dictation.start()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLanguageToggle(
    selected: VoiceNoteLanguage,
    onSelect: (VoiceNoteLanguage) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "voiceNoteLang",
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        VoiceNoteLanguage.entries.forEachIndexed { index, language ->
            SegmentedButton(
                selected = selected == language,
                onClick = { onSelect(language) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = VoiceNoteLanguage.entries.size,
                ),
                modifier = Modifier.testTag("${testTagPrefix}_${language.name}"),
            ) {
                Text(language.displayName)
            }
        }
    }
}
