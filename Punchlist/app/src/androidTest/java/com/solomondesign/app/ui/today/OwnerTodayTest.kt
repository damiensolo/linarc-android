package com.solomondesign.app.ui.today

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.navigation.AppNavHost
import com.solomondesign.app.ui.navigation.AppRoutes
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.theme.AppTheme
import com.solomondesign.app.ui.voicelog.DailyLogRecord
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Acceptance tests for the Owner persona's confidence dashboard (OwnerTodaySections.kt):
 * layout order, the four decision topics, persona isolation, and the Owner's sanctioned
 * removals (no time card, no voice logs, no crew operations).
 */
class OwnerTodayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDemoStore() {
        DemoSession.reset()
    }

    private fun launch() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }
    }

    private fun scrollTodayTo(matcher: SemanticsMatcher) {
        composeTestRule.onNodeWithTag("todayScreen").performScrollToNode(matcher)
    }

    @Test
    fun owner_showsPhotosThenFourTopicsThenDelays_andNoOperationalRows() {
        DemoProjectRepository.selectPersona(FieldPersona.OWNER)
        launch()

        // 1. Header sync state and Progress photos first, with the seeded thumbnail row.
        composeTestRule.onNodeWithTag("ownerSyncState").assertExists()
        composeTestRule.onNodeWithText("Progress photos").assertExists()
        composeTestRule.onNodeWithText("Yesterday progress").assertExists()

        // 2. Exactly the four decision topics, in order, then Delays with the med-gas blocker.
        scrollTodayTo(hasTestTag("ownerTopic_schedule"))
        composeTestRule.onNodeWithTag("ownerTopic_schedule").assertExists()
        scrollTodayTo(hasTestTag("ownerTopic_budget"))
        scrollTodayTo(hasTestTag("ownerTopic_quality"))
        scrollTodayTo(hasTestTag("ownerTopic_decisions"))
        scrollTodayTo(hasTestTag("ownerDelay_stream-rec-seed-issue"))
        composeTestRule.onNodeWithTag("ownerDelay_stream-rec-seed-issue").assertExists()

        // 3. Nothing operational anywhere on the page (the full list has been composed above).
        composeTestRule.onAllNodesWithText("Frame inspection").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Voice daily log").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Recent captures").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("On site today").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Start My Day").assertCountEquals(0)
    }

    @Test
    fun owner_photoCapturedThisSession_leadsProgressPhotos() {
        DemoProjectRepository.selectPersona(FieldPersona.OWNER)
        launch()

        composeTestRule.runOnIdle {
            DemoProjectRepository.addPhoto(
                title = "Level 3 window walk",
                subtitle = "Area B",
                createIssue = false,
            )
        }

        val newPhotoY = composeTestRule.onNodeWithText("Level 3 window walk")
            .fetchSemanticsNode().positionInRoot.y
        val seededPhotoY = composeTestRule.onNodeWithText("Yesterday progress")
            .fetchSemanticsNode().positionInRoot.y
        check(newPhotoY < seededPhotoY) {
            "A photo captured as Owner must appear above the seeded progress photo"
        }
    }

    @Test
    fun ownerTools_dropTimeCardAndVoiceLogs_foremanGetsThemBack() {
        DemoProjectRepository.selectPersona(FieldPersona.OWNER)
        launch()
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()

        // Grid view: Activity Center is Outbox only, no Time card tile.
        composeTestRule.onNodeWithTag("toolsScreen").performScrollToNode(hasText("Outbox"))
        composeTestRule.onAllNodesWithTag("toolCard_time_card").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Voice logs").assertCountEquals(0)
        composeTestRule.onNodeWithText("Outbox").assertExists()

        // List view: same rules.
        composeTestRule.onNodeWithTag("toolsViewToggle").performClick()
        composeTestRule.onNodeWithTag("toolsScreen").performScrollToNode(hasText("Outbox"))
        composeTestRule.onAllNodesWithTag("toolCard_time_card").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Voice logs").assertCountEquals(0)

        // Back as Foreman, both return.
        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.FOREMAN) }
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasTestTag("toolCard_time_card"))
        composeTestRule.onNodeWithTag("toolCard_time_card").assertExists()
        composeTestRule.onNodeWithTag("toolsScreen").performScrollToNode(hasText("Voice logs"))
        composeTestRule.onNodeWithText("Voice logs").assertExists()
    }

    /**
     * Persona isolation: a Foreman voice log never leaks onto the Owner's Today, while a photo
     * from the same session does — photo visibility and voice-log visibility are independent.
     */
    @Test
    fun foremanVoiceLog_staysOffOwnerToday_butSessionPhotoShows() {
        launch() // Foreman is the default persona.

        composeTestRule.runOnIdle {
            DemoProjectRepository.publishVoiceLog(
                DailyLogRecord(
                    id = "log-owner-isolation",
                    timestampMillis = System.currentTimeMillis(),
                    projectName = DemoProjectRepository.PROJECT_NAME,
                    audioFilePath = "",
                    transcript = "Crew of six on framing, no delays",
                    laborCards = emptyList(),
                    materialCards = emptyList(),
                    delayCards = emptyList(),
                    issueCards = emptyList(),
                ),
            )
            DemoProjectRepository.addPhoto(
                title = "Same-session photo",
                subtitle = "Area B",
                createIssue = false,
            )
        }
        scrollTodayTo(hasText("Voice daily log"))
        composeTestRule.onNodeWithText("Voice daily log").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.OWNER) }
        composeTestRule.onAllNodesWithText("Voice daily log").assertCountEquals(0)
        scrollTodayTo(hasText("Same-session photo"))
        composeTestRule.onNodeWithText("Same-session photo").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.FOREMAN) }
        scrollTodayTo(hasText("Voice daily log"))
        composeTestRule.onNodeWithText("Voice daily log").assertExists()
    }

    /**
     * Both Owner layouts stay demoable from the one picker: Demo: view as lists Owner as two
     * rows — the v2 decision dashboard (default) and the original v1 photos-and-discussions
     * view — and tapping one sets the layout and switches to the Owner view together.
     */
    @Test
    fun viewAsPicker_switchesBetweenOwnerDashboardAndClassic() {
        DemoProjectRepository.selectPersona(FieldPersona.OWNER)
        launch()
        composeTestRule.onNodeWithTag("ownerTopic_schedule").assertExists()

        // Pick the classic v1 Owner row through the real demo flow.
        composeTestRule.onNodeWithTag("headerOverflowMenu").performClick()
        composeTestRule.onNodeWithTag("headerSettingsMenuItem").performClick()
        composeTestRule.onNodeWithTag("settingsScreen")
            .performScrollToNode(hasTestTag("demoViewAsRow"))
        composeTestRule.onNodeWithTag("demoViewAsRow").performClick()
        composeTestRule.onNodeWithTag("persona_OWNER_CLASSIC").performClick()
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TODAY_HOME}").performClick()

        // v1: photos, flat topic list, shared Delays rows, collapsed roster — no dashboard.
        composeTestRule.onNodeWithText("Yesterday progress").assertExists()
        scrollTodayTo(hasText("Decisions & discussions"))
        composeTestRule.onNodeWithText("Decisions & discussions").assertExists()
        scrollTodayTo(hasText("On site today"))
        composeTestRule.onNodeWithText("On site today").assertExists()
        composeTestRule.onAllNodesWithTag("ownerTopic_schedule").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ownerDelay_stream-rec-seed-issue").assertCountEquals(0)

        // And back to the dashboard.
        composeTestRule.runOnIdle {
            DemoProjectRepository.ownerTodayVariant = OwnerTodayVariant.DASHBOARD
        }
        scrollTodayTo(hasTestTag("ownerTopic_schedule"))
        composeTestRule.onNodeWithTag("ownerTopic_schedule").assertExists()
        composeTestRule.onAllNodesWithText("Decisions & discussions").assertCountEquals(0)
    }

    /** Cycling every live persona swaps the layout cleanly, with no stale Owner content. */
    @Test
    fun personaCycle_swapsLayoutsWithoutStaleContent() {
        launch()
        composeTestRule.onNodeWithTag("startMyDayCard").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.CREW) }
        scrollTodayTo(hasTestTag("myShiftCard"))
        composeTestRule.onNodeWithTag("myShiftCard").assertExists()
        composeTestRule.onAllNodesWithTag("ownerTopic_schedule").assertCountEquals(0)

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.SUPERINTENDENT) }
        scrollTodayTo(hasText("Open issues & inspections"))
        composeTestRule.onNodeWithText("Open issues & inspections").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.PROJECT_MANAGER) }
        scrollTodayTo(hasText("Aging RFIs"))
        composeTestRule.onNodeWithText("Aging RFIs").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.OWNER) }
        scrollTodayTo(hasTestTag("ownerTopic_schedule"))
        composeTestRule.onNodeWithTag("ownerTopic_schedule").assertExists()

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.SUBCONTRACTOR) }
        scrollTodayTo(hasTestTag("requestInspectionCard"))
        composeTestRule.onNodeWithTag("requestInspectionCard").assertExists()
        composeTestRule.onAllNodesWithTag("ownerTopic_schedule").assertCountEquals(0)

        composeTestRule.runOnIdle { DemoProjectRepository.selectPersona(FieldPersona.FOREMAN) }
        scrollTodayTo(hasTestTag("startMyDayCard"))
        composeTestRule.onNodeWithTag("startMyDayCard").assertExists()
        composeTestRule.onAllNodesWithTag("ownerDelay_stream-rec-seed-issue").assertCountEquals(0)
    }
}
