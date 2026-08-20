package com.solomondesign.app.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.tasks.TaskStatus
import com.solomondesign.app.ui.theme.AppTheme
import com.solomondesign.app.ui.timecards.TimeCardRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDemoStore() {
        DemoSession.reset()
    }

    private fun launchAndOpenTool(toolTestTag: String) {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag(toolTestTag).performClick()
    }

    @Test
    fun crew_listDrillsIntoDetail_withBottomNavVisible() {
        launchAndOpenTool("toolCard_crew")

        composeTestRule.onNodeWithTag("crewListScreen").assertExists()
        composeTestRule.onNodeWithTag("crewRow_maria-chen").performClick()

        composeTestRule.onNodeWithTag("crewDetailScreen").assertExists()
        composeTestRule.onNodeWithText("Electrical").assertExists()
        // Pattern B: bottom navigation survives two levels of drill-down.
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").assertExists()
    }

    @Test
    fun fieldTask_detailShowsRealDataAndStatusIsEditable() {
        launchAndOpenTool("toolCard_field_task")

        composeTestRule.onNodeWithTag("taskRow_task-med-gas-col4").performClick()
        composeTestRule.onNodeWithTag("fieldTaskDetailScreen").assertExists()

        composeTestRule.onNodeWithTag("taskStatus_DONE").performClick()
        check(FieldTaskRepository.find("task-med-gas-col4")?.status == TaskStatus.DONE) {
            "Tapping a status segment should update the task"
        }
    }

    @Test
    fun fieldTask_filterCanProduceAnEmptyState() {
        launchAndOpenTool("toolCard_field_task")

        composeTestRule.onNodeWithTag("taskFilter_MINE").performClick()
        composeTestRule.onNodeWithText("No tasks match this filter.").assertExists()
    }

    /** Pattern C: the contextual FAB replaces Capture and opens a validating sheet. */
    @Test
    fun timeCards_contextualFabOpensSheet_andValidatesBeforeSaving() {
        launchAndOpenTool("toolCard_time_card")

        composeTestRule.onNodeWithTag("timeCardListScreen").assertExists()
        composeTestRule.onNodeWithTag("captureFab").assertDoesNotExist()
        composeTestRule.onNodeWithTag("timeEntryFab").performClick()

        composeTestRule.onNodeWithTag("newTimeEntrySheet").assertExists()
        // Hours start empty, so Save is blocked.
        composeTestRule.onNodeWithTag("timeEntrySave").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("timeEntryHoursField").performTextInput("6.5")
        composeTestRule.onNodeWithTag("timeEntrySave").assertIsEnabled().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            TimeCardRepository.entries.any { it.id.startsWith("te-new-") }
        }
    }

    @Test
    fun timeCards_memberWithNoHoursShowsEmptyState() {
        launchAndOpenTool("toolCard_time_card")

        composeTestRule.onNodeWithTag("timeCardRow_sam-reyes").performClick()
        composeTestRule.onNodeWithText("No hours logged for this week.").assertExists()
    }

    @Test
    fun collaboration_topicOpensConversation_andPostingIsBlankGuarded() {
        launchAndOpenTool("toolCard_collaboration")

        composeTestRule.onNodeWithTag("collabTopicListScreen").assertExists()
        composeTestRule.onNodeWithTag("topicRow_topic-col4-medgas").performClick()

        composeTestRule.onNodeWithTag("collabTopicScreen").assertExists()
        composeTestRule.onNodeWithText("Copy. Crew is on the level 2 restroom group until then.")
            .assertExists()
        // Blank composer keeps Send disabled; the FAB is absent inside a conversation.
        composeTestRule.onNodeWithTag("collabSend").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("newTopicFab").assertDoesNotExist()

        composeTestRule.onNodeWithTag("collabComposer").performTextInput("On my way")
        composeTestRule.onNodeWithTag("collabSend").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("On my way").assertExists()
    }

    /** Images grid is Pattern B; the viewer is Pattern A with a floating footer toolbar. */
    @Test
    fun images_gridOpensFullScreenViewer_thatHidesChromeAndCanDelete() {
        launchAndOpenTool("toolCard_images")

        composeTestRule.onNodeWithTag("imageGridScreen").assertExists()
        composeTestRule.onNodeWithTag("addImageFab").assertExists()
        composeTestRule.onNodeWithTag("imageTile_img-yesterday").performClick()

        composeTestRule.onNodeWithTag("imageViewerScreen").assertExists()
        composeTestRule.onNodeWithTag("imageViewerToolbar").assertExists()
        composeTestRule.onNodeWithTag("addImageFab").assertDoesNotExist()
        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("viewerDelete").performClick()
        composeTestRule.onNodeWithTag("viewerDeleteDialog").assertExists()
        composeTestRule.onNodeWithTag("viewerDeleteConfirm").performClick()

        // Back on the grid, and the photo is gone.
        composeTestRule.onNodeWithTag("imageGridScreen").assertExists()
        composeTestRule.onNodeWithTag("imageTile_img-yesterday").assertDoesNotExist()
    }

    @Test
    fun bottomNav_showsThreeTabs_andSwitchingTabsShowsThatTabsContent() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertExists()
        }
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TODAY_HOME}").assertExists()
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.PLAN_HOME}").assertExists()
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").assertExists()

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.PLAN_HOME}").performClick()
        composeTestRule.onNodeWithTag("planScreen").assertExists()
    }

    @Test
    fun today_showsForemanCrewAndStartMyDay() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        composeTestRule.onNodeWithText("Foreman · Area B").assertExists()
        composeTestRule.onNodeWithText("Hector Ortiz").assertExists()
        composeTestRule.onNodeWithTag("startMyDayCard").assertExists()
        composeTestRule.onNodeWithContentDescription("Capture").assertExists()
    }

    @Test
    fun today_crewSection_collapsesAndExpands() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithText("Hector Ortiz").assertExists()

        composeTestRule.onNodeWithTag("crewSectionHeader").performClick()
        composeTestRule.onNodeWithText("Hector Ortiz").assertDoesNotExist()

        composeTestRule.onNodeWithTag("crewSectionHeader").performClick()
        composeTestRule.onNodeWithText("Hector Ortiz").assertExists()
    }

    /** Startup flow: with the picker opted in, Project List gates the chassis until a row is tapped. */
    @Test
    fun projectPicker_selectingAProjectRevealsToday() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost(showProjectPicker = true)
            }
        }

        composeTestRule.onNodeWithTag("projectListScreen").assertExists()
        composeTestRule.onNodeWithTag("todayScreen").assertDoesNotExist()

        composeTestRule.onNodeWithTag("projectRow_riverside-medical").performClick()

        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        composeTestRule.onNodeWithTag("projectListScreen").assertDoesNotExist()
    }

    /** The Accounts tab sits beside Projects on the picker's own footer nav (not the chassis one). */
    @Test
    fun projectPicker_accountsTabShowsPlaceholder() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost(showProjectPicker = true)
            }
        }

        composeTestRule.onNodeWithTag("pickerNavTab_ACCOUNTS").performClick()
        composeTestRule.onNodeWithText("Accounts isn't part of this prototype yet.").assertExists()

        composeTestRule.onNodeWithTag("pickerNavTab_PROJECTS").performClick()
        composeTestRule.onNodeWithTag("projectRow_riverside-medical").assertExists()
    }

    /** Profile → Switch project is the way back to the picker once a project is loaded. */
    @Test
    fun profile_switchProject_returnsToProjectList() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost(showProjectPicker = true)
            }
        }

        composeTestRule.onNodeWithTag("projectRow_riverside-medical").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()

        composeTestRule.onNodeWithTag("profileAvatarButton").performClick()
        composeTestRule.onNodeWithTag("profileSwitchProject").performClick()

        composeTestRule.onNodeWithTag("projectListScreen").assertExists()
        composeTestRule.onNodeWithTag("todayScreen").assertDoesNotExist()
    }

    /** The header chip is the fast path — no detour through Profile. */
    @Test
    fun header_projectChip_returnsToProjectList() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost(showProjectPicker = true)
            }
        }

        composeTestRule.onNodeWithTag("projectRow_riverside-medical").performClick()
        composeTestRule.onNodeWithTag("headerProjectChip").performClick()

        composeTestRule.onNodeWithTag("projectListScreen").assertExists()
        composeTestRule.onNodeWithTag("todayScreen").assertDoesNotExist()
    }

    /** The header overflow menu is the second, spec-unambiguous shortcut to the same action. */
    @Test
    fun header_overflowMenu_switchProject_returnsToProjectList() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost(showProjectPicker = true)
            }
        }

        composeTestRule.onNodeWithTag("projectRow_riverside-medical").performClick()
        composeTestRule.onNodeWithTag("headerOverflowMenu").performClick()
        composeTestRule.onNodeWithTag("headerSwitchProjectMenuItem").performClick()

        composeTestRule.onNodeWithTag("projectListScreen").assertExists()
        composeTestRule.onNodeWithTag("todayScreen").assertDoesNotExist()
    }

    @Test
    fun tools_showsDemoViewAs_withForemanLiveAndOthersListed() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        // Tools is a LazyColumn: the demo controls sit below the catalog and aren't composed
        // until scrolled into view.
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasTestTag("demoViewAsRow"))
        composeTestRule.onNodeWithTag("demoViewAsRow").performClick()
        composeTestRule.onNodeWithTag("persona_FOREMAN").assertExists()
        composeTestRule.onNodeWithTag("persona_SUPERINTENDENT").assertExists()
        composeTestRule.onNodeWithTag("persona_OWNER").assertExists()
        composeTestRule.onNodeWithText("Live").assertExists()
        composeTestRule.onAllNodesWithText("Next — same tabs, different Today").assertCountEquals(5)
    }

    @Test
    fun tools_showsThemeToggle_andHidesDesignSystem() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasTestTag("themeToggle"))
        composeTestRule.onNodeWithTag("themeToggle").assertExists()
        composeTestRule.onNodeWithText("Dark theme").assertExists()
        composeTestRule.onNodeWithText("Design system").assertDoesNotExist()
    }

    @Test
    fun tools_showsCatalog_andTogglesGridAndList() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolsScreen").assertExists()
        composeTestRule.onNodeWithText("Field task").assertExists()
        composeTestRule.onNodeWithText("Collaboration").assertExists()
        composeTestRule.onNodeWithText("Hours on site").assertDoesNotExist()

        composeTestRule.onNodeWithTag("toolsViewList").performClick()
        composeTestRule.onNodeWithText("Hours on site").assertExists()
        composeTestRule.onNodeWithText("Who's on this job").assertExists()

        composeTestRule.onNodeWithTag("toolsViewGrid").performClick()
        composeTestRule.onNodeWithText("Hours on site").assertDoesNotExist()
    }

    @Test
    fun tools_openAndQuickCreateReachPlaceholders() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        // punch_list is still a placeholder tool, so it keeps the generic list/detail behaviour.
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_punch_list").performClick()
        composeTestRule.onNodeWithText("Punch list · sample 1").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithTag("toolQuickCreate_rfis").performClick()
        composeTestRule.onNodeWithText("New RFIs").assertExists()
        composeTestRule.onNodeWithText("Quick create is a placeholder in this build.").assertExists()
    }

    /** Pattern B: nested browsing keeps the bottom navigation visible. */
    @Test
    fun patternB_nestedToolList_keepsBottomNavigationVisible() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_field_task").performClick()

        composeTestRule.onNodeWithText("Frame corridor C partitions").assertExists()
        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertExists()
        }
    }

    /** Pattern A: full-screen task flows hide both the bottom navigation and the FAB. */
    @Test
    fun patternA_quickIssue_hidesBottomNavigationAndFab() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithContentDescription("Capture").performClick()
        composeTestRule.onNodeWithTag("captureIssue").performClick()

        composeTestRule.onNodeWithText("New issue").assertExists()
        composeTestRule.onNodeWithTag("captureFab").assertDoesNotExist()
        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertDoesNotExist()
        }
    }

    /** Pattern A: a clean task flow closes immediately, with no discard prompt. */
    @Test
    fun patternA_closeWithNoEdits_exitsWithoutWarning() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithContentDescription("Capture").performClick()
        composeTestRule.onNodeWithTag("captureIssue").performClick()
        composeTestRule.onNodeWithText("New issue").assertExists()

        composeTestRule.onNodeWithTag("taskFlowClose").performClick()

        composeTestRule.onNodeWithTag("discardDialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()
    }

    /** Pattern A: warn before discard only when unsaved edits exist. */
    @Test
    fun patternA_closeWithUnsavedEdits_warnsAndCanKeepEditingOrDiscard() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithContentDescription("Capture").performClick()
        composeTestRule.onNodeWithTag("captureIssue").performClick()
        composeTestRule.onNodeWithText("Title").performTextInput("Missing guardrail")

        // Closing with edits warns rather than exiting.
        composeTestRule.onNodeWithTag("taskFlowClose").performClick()
        composeTestRule.onNodeWithTag("discardDialog").assertExists()

        // Keep editing returns to the flow with the edit intact.
        composeTestRule.onNodeWithTag("discardDismiss").performClick()
        composeTestRule.onNodeWithTag("discardDialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("Missing guardrail").assertExists()

        // Discard actually leaves, and nothing was submitted.
        composeTestRule.onNodeWithTag("taskFlowClose").performClick()
        composeTestRule.onNodeWithTag("discardConfirm").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        check(DemoProjectRepository.streamItems.none { it.title == "Missing guardrail" }) {
            "Discarding an issue must not publish it to Today"
        }
    }

    /** Pattern B: reselecting the active tab returns that tab to its root screen. */
    @Test
    fun reselectingActiveTab_returnsThatTabToItsRoot() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_field_task").performClick()
        composeTestRule.onNodeWithText("Frame corridor C partitions").assertExists()

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()

        composeTestRule.onNodeWithTag("toolsScreen").assertExists()
        composeTestRule.onNodeWithText("Frame corridor C partitions").assertDoesNotExist()
    }

    /** Nested graphs give each tab its own back stack, preserved across tab switches. */
    @Test
    fun switchingTabs_preservesEachTabsOwnBackStack() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_field_task").performClick()
        composeTestRule.onNodeWithText("Frame corridor C partitions").assertExists()

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TODAY_HOME}").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()

        // Returning to Tools restores the drill-down rather than resetting to the catalog.
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithText("Frame corridor C partitions").assertExists()
    }
}
