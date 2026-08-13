package com.solomondesign.punchlist.ui.voicelog

import android.Manifest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.solomondesign.punchlist.ui.navigation.PunchlistNavHost
import com.solomondesign.punchlist.ui.navigation.PunchlistRoutes
import com.solomondesign.punchlist.ui.theme.PunchlistTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * End-to-end pass through the real recording -> parsing -> review -> submit -> history pipeline.
 * RECORD_AUDIO is pre-granted so the test exercises the actual [android.media.MediaRecorder] /
 * [android.speech.SpeechRecognizer] wiring instead of stopping at the permission screen. This
 * can't assert on transcribed *words* — there's no real speech to feed it here — but it proves a
 * submitted record actually lands in [DailyLogRepository] and stays reachable from history.
 *
 * Recording a playable audio file is opt-in (see [VoiceRecordingScreen]) because running
 * MediaRecorder and SpeechRecognizer on the mic at once was proven, by direct testing on this
 * project's own dev emulator, to reliably break transcription — so the default path here has no
 * audio file, and a second test exercises the opt-in "Also Save Audio for Playback" path.
 */
class VoiceLogFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetRepository() {
        // DailyLogRepository is a process-wide singleton — without this, a record left over from
        // a previous test method makes history-screen text matches ambiguous (two identical rows).
        DailyLogRepository.clear()
    }

    @Test
    fun voiceLogFlow_defaultTranscriptionOnly_recordSubmitAndReachHistory_withNoAudioFile() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        goToRecordingScreen()
        composeTestRule.onNodeWithText("Stop & Parse").performClick()
        waitForReviewScreen()
        submitAndReturnToHistory()

        val record = DailyLogRepository.records.firstOrNull()
        checkNotNull(record) { "Submitting should add a record to DailyLogRepository" }
        check(record.audioFilePath.isBlank()) {
            "Default recording flow shouldn't save an audio file (it's opt-in): ${record.audioFilePath}"
        }

        composeTestRule.onNodeWithText(record.projectName).performClick()
        composeTestRule.onNodeWithText(
            "No recording was saved for this entry — this device can't capture audio for " +
                "playback and transcription at the same time, so transcription won out " +
                "(see the Daily Log testing notes).",
        ).assertExists()
    }

    @Test
    fun voiceLogFlow_optIntoAudioRecording_savesARealPlayableFile() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        goToRecordingScreen()
        composeTestRule.onNodeWithText("Also Save Audio for Playback").performClick()
        composeTestRule.onNodeWithText("Stop & Parse").performClick()
        waitForReviewScreen()
        submitAndReturnToHistory()

        val record = DailyLogRepository.records.firstOrNull()
        checkNotNull(record) { "Submitting should add a record to DailyLogRepository" }
        check(File(record.audioFilePath).exists()) {
            "Opting into audio recording should write a real file at ${record.audioFilePath}"
        }

        composeTestRule.onNodeWithText(record.projectName).performClick()
        composeTestRule.onNodeWithText("Play Recording").assertExists().performClick()
    }

    private fun goToRecordingScreen() {
        // Capture -> Daily Log -> Record New Voice Log launches real recording (Screen A).
        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.CAPTURE_HOME}").performClick()
        composeTestRule.onNodeWithText("Daily Log").performClick()
        composeTestRule.onNodeWithText("Record New Voice Log").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Stop & Parse").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForReviewScreen() {
        // Parsing (Screen B) runs the real VoiceLogParser and auto-advances to Review (Screen C).
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithText("Proposed Site Logs").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun submitAndReturnToHistory() {
        composeTestRule.onNodeWithText("Submit").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.onNodeWithText("Record New Voice Log").assertExists()
    }
}
