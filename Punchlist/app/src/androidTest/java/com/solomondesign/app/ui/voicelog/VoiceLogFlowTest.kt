package com.solomondesign.app.ui.voicelog

import android.Manifest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.rule.GrantPermissionRule
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.StreamKind
import com.solomondesign.app.ui.navigation.AppNavHost
import com.solomondesign.app.ui.navigation.AppRoutes
import com.solomondesign.app.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * End-to-end pass through recording -> parsing -> review -> submit -> Today/history.
 * Entry is the bottom bar's Capture action -> camera -> Voice log quick chip; RECORD_AUDIO and
 * CAMERA are pre-granted so no system dialog blocks the pass.
 */
class VoiceLogFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    )

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetRepository() {
        DailyLogRepository.clear()
        DemoProjectRepository.clear()
    }

    @Test
    fun voiceLogFlow_defaultTranscriptionOnly_recordSubmitAndReachHistory_withNoAudioFile() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        goToRecordingScreen()
        composeTestRule.onNodeWithText("Stop & Parse").performClick()
        waitForReviewScreen()
        submitAndReturnToToday()

        val record = DailyLogRepository.records.firstOrNull()
        checkNotNull(record) { "Submitting should add a record to DailyLogRepository" }
        check(record.audioFilePath.isBlank()) {
            "Default recording flow shouldn't save an audio file (it's opt-in): ${record.audioFilePath}"
        }
        check(DemoProjectRepository.streamItems.any { it.kind == StreamKind.DAILY_LOG && it.relatedRecordId == record.id }) {
            "Submitted voice log should appear on Today"
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        // Tools is a LazyColumn: "Voice logs" sits below the catalog and isn't composed until
        // scrolled into view.
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasText("Voice logs"))
        composeTestRule.onNodeWithText("Voice logs").performClick()
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
            AppTheme {
                AppNavHost()
            }
        }

        goToRecordingScreen()
        composeTestRule.onNodeWithText("Also Save Audio for Playback").performClick()
        composeTestRule.onNodeWithText("Stop & Parse").performClick()
        waitForReviewScreen()
        submitAndReturnToToday()

        val record = DailyLogRepository.records.firstOrNull()
        checkNotNull(record) { "Submitting should add a record to DailyLogRepository" }
        check(File(record.audioFilePath).exists()) {
            "Opting into audio recording should write a real file at ${record.audioFilePath}"
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        // Tools is a LazyColumn: "Voice logs" sits below the catalog and isn't composed until
        // scrolled into view.
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasText("Voice logs"))
        composeTestRule.onNodeWithText("Voice logs").performClick()
        composeTestRule.onNodeWithText(record.projectName).performClick()
        composeTestRule.onNodeWithText("Play Recording").assertExists().performClick()
    }

    private fun goToRecordingScreen() {
        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()
        composeTestRule.onNodeWithTag("cameraQuickVoice").performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Stop & Parse").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // 15s to match AppNavHostTest's camera waits: the transition is synchronous UI work, so the
    // budget only absorbs a cold or heavily loaded emulator, and 8s proved too tight there.
    private fun waitForReviewScreen() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Proposed Site Logs").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun submitAndReturnToToday() {
        composeTestRule.onNodeWithText("Submit").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()
    }
}
