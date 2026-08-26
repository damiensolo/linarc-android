package com.solomondesign.app.ui.voicelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solomondesign.app.ui.demo.DemoProjectRepository

/**
 * Voice-to-Log, demoed from Settings → Demo → "Voice daily log (demo)" since 2026-08-25 (the
 * camera quick chip that used to open this now opens the bilingual Voice note). Recording and
 * transcription are real (`ui/voicelog/audio`); [VoiceLogParser] is a deterministic on-device
 * pass over the transcript. On submit, the record is stored in [DailyLogRepository] and published
 * to [DemoProjectRepository] so Today and Plan update in the same session.
 */
@Composable
fun VoiceLogScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf<VoiceLogUiState>(VoiceLogUiState.Recording) }
    var audioFilePath by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }

    var laborCards by remember { mutableStateOf(emptyList<LaborCard>()) }
    var materialCards by remember { mutableStateOf(emptyList<MaterialCard>()) }
    var delayCards by remember { mutableStateOf(emptyList<DelayCard>()) }
    var issueCards by remember { mutableStateOf(emptyList<IssueCard>()) }

    when (val state = uiState) {
        VoiceLogUiState.Recording -> VoiceRecordingScreen(
            onCancel = onExit,
            onStopAndParse = { path, text ->
                audioFilePath = path
                transcript = text
                uiState = VoiceLogUiState.Parsing(text)
            },
            modifier = modifier,
        )

        is VoiceLogUiState.Parsing -> VoiceParsingScreen(
            transcript = state.transcript,
            onAbort = onExit,
            onParsingComplete = { parsed ->
                laborCards = parsed.labor
                materialCards = parsed.materials
                delayCards = parsed.delays
                issueCards = parsed.issues
                uiState = VoiceLogUiState.Review
            },
            modifier = modifier,
        )

        VoiceLogUiState.Review -> VoiceReviewScreen(
            laborCards = laborCards,
            materialCards = materialCards,
            delayCards = delayCards,
            issueCards = issueCards,
            onBack = { uiState = VoiceLogUiState.Recording },
            onDeleteLabor = { id -> laborCards = laborCards.filterNot { it.id == id } },
            onDeleteMaterial = { id -> materialCards = materialCards.filterNot { it.id == id } },
            onDeleteDelay = { id -> delayCards = delayCards.filterNot { it.id == id } },
            onDeleteIssue = { id -> issueCards = issueCards.filterNot { it.id == id } },
            onEditLaborHours = { id, hours ->
                laborCards = laborCards.map { if (it.id == id) it.copy(hours = hours) else it }
            },
            onEditDelayHours = { id, hours ->
                delayCards = delayCards.map { if (it.id == id) it.copy(hours = hours) else it }
            },
            onSubmit = {
                val record = DailyLogRecord(
                    id = "log-${System.currentTimeMillis()}",
                    timestampMillis = System.currentTimeMillis(),
                    projectName = FakeVoiceLogData.PROJECT_NAME,
                    audioFilePath = audioFilePath,
                    transcript = transcript,
                    laborCards = laborCards,
                    materialCards = materialCards,
                    delayCards = delayCards,
                    issueCards = issueCards,
                )
                DailyLogRepository.add(record)
                DemoProjectRepository.publishVoiceLog(record)
                uiState = VoiceLogUiState.Submitted
            },
            modifier = modifier,
        )

        VoiceLogUiState.Submitted -> VoiceLogSubmittedScreen(onDone = onExit, modifier = modifier)
    }
}
