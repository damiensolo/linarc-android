package com.solomondesign.app.ui.navigation

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.demo.StreamKind
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.tasks.TaskStatus
import com.solomondesign.app.ui.theme.AppTheme
import com.solomondesign.app.ui.timecards.TimeCardRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

    /** Pre-granted so opening the camera never blocks a test behind the system dialog. */
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

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
        // The contextual FAB coexists with the bar's Capture action (Pattern B keeps the bar).
        composeTestRule.onNodeWithTag("bottomNavCapture").assertExists()
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
        // Plus the Capture action, between Today and Plans.
        composeTestRule.onNodeWithTag("bottomNavCapture").assertExists()

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
        composeTestRule.onNodeWithTag("bottomNavCapture").assertExists()
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
    fun settings_showsDemoViewAs_withForemanLiveAndOthersListed() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        // Demo controls moved off the Tools catalog: they live on Settings, reached from the
        // Tools header's overflow menu.
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("headerOverflowMenu").performClick()
        composeTestRule.onNodeWithTag("headerSettingsMenuItem").performClick()
        composeTestRule.onNodeWithTag("settingsScreen")
            .performScrollToNode(hasTestTag("demoViewAsRow"))
        composeTestRule.onNodeWithTag("demoViewAsRow").performClick()
        composeTestRule.onNodeWithTag("persona_FOREMAN").assertExists()
        composeTestRule.onNodeWithTag("persona_SUPERINTENDENT").assertExists()
        composeTestRule.onNodeWithTag("persona_OWNER").assertExists()
        composeTestRule.onNodeWithText("Live").assertExists()
        composeTestRule.onAllNodesWithText("Next — same tabs, different Today").assertCountEquals(5)
    }

    @Test
    fun settings_showsThemeToggle_andToolsShowsActivityCenter() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        // Product activity stays on Tools…
        composeTestRule.onNodeWithTag("toolsScreen")
            .performScrollToNode(hasText("Voice logs"))
        composeTestRule.onNodeWithText("Activity Center").assertExists()
        composeTestRule.onNodeWithText("Outbox").assertExists()
        composeTestRule.onNodeWithText("Voice logs").assertExists()
        composeTestRule.onNodeWithText("Dark theme").assertDoesNotExist()

        // …while appearance settings live behind the overflow's Settings item.
        composeTestRule.onNodeWithTag("headerOverflowMenu").performClick()
        composeTestRule.onNodeWithTag("headerSettingsMenuItem").performClick()
        composeTestRule.onNodeWithTag("settingsScreen")
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

        composeTestRule.onNodeWithTag("toolsViewToggle").performClick()
        composeTestRule.onNodeWithText("Hours on site").assertExists()
        composeTestRule.onNodeWithText("Who's on this job").assertExists()

        composeTestRule.onNodeWithTag("toolsViewToggle").performClick()
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

    /** The bar's Capture action opens the full-screen camera; closing lands back on Today. */
    @Test
    fun captureNavAction_opensFullScreenCamera_andCloseReturnsToToday() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()

        composeTestRule.onNodeWithTag("cameraCaptureScreen").assertExists()
        composeTestRule.onNodeWithTag("cameraShutter").assertExists()
        // Immersive: the whole bar is gone, the Capture action included.
        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertDoesNotExist()
        }
        composeTestRule.onNodeWithTag("bottomNavCapture").assertDoesNotExist()

        composeTestRule.onNodeWithTag("cameraClose").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()
    }

    /** Recent captures: the seeded photo row deep-links into the full-screen image viewer. */
    @Test
    fun today_seededPhotoRow_opensFullScreenImageViewer() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("todayScreen")
            .performScrollToNode(hasTestTag("streamItem_stream-img-yesterday"))
        composeTestRule.onNodeWithTag("streamItem_stream-img-yesterday").performClick()

        composeTestRule.onNodeWithTag("imageViewerScreen").assertExists()
        composeTestRule.onNodeWithTag("imageViewerToolbar").assertExists()

        composeTestRule.onNodeWithTag("taskFlowClose").performClick()
        composeTestRule.onNodeWithTag("todayScreen").assertExists()
    }

    /**
     * The full capture loop the demo tells: shoot on the (virtual) camera → save → the photo
     * appears in Today's Recent captures as a thumbnail row → the row opens the full-screen
     * viewer with the share / markup / delete / create toolbar → deleting there removes the
     * Today row again. End-to-end, no shortcuts.
     */
    @Test
    fun captureToViewer_endToEnd_photoLandsOnTodayAndDeletesCleanly() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()
        // The shutter stays disabled until CameraX finishes binding the camera.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodes(hasTestTag("cameraShutter") and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("cameraShutter").performClick()
        // Capture + JPEG processing finish on the review step.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag("photoReviewScreen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("taskFlowConfirm").performClick()

        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        // Let the "Photo saved" snackbar time out first: it floats over the bottom of the list,
        // exactly where the scrolled-to row lands, and would swallow the tap. The budget is
        // generous because the ~4s auto-dismiss lags badly on a loaded emulator.
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText("Photo saved — on Today, Plans, and Images")
                .fetchSemanticsNodes().isEmpty()
        }
        // Newest stream item is the capture (addPhoto inserts at the top).
        val streamItem = DemoProjectRepository.streamItems.first { it.kind == StreamKind.PHOTO }
        check(streamItem.relatedImageId != null) { "captured photo row must link to its image" }
        composeTestRule.onNodeWithTag("todayScreen")
            .performScrollToNode(hasTestTag("streamItem_${streamItem.id}"))
        composeTestRule.onNodeWithTag("streamItem_${streamItem.id}").performClick()

        composeTestRule.onNodeWithTag("imageViewerScreen").assertExists()
        composeTestRule.onNodeWithTag("imageViewerToolbar").assertExists()

        composeTestRule.onNodeWithTag("viewerDelete").performClick()
        composeTestRule.onNodeWithTag("viewerDeleteConfirm").performClick()

        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        composeTestRule.onNodeWithTag("streamItem_${streamItem.id}").assertDoesNotExist()
    }

    /** Pattern A: full-screen task flows hide the bottom navigation (Capture action included). */
    @Test
    fun patternA_quickIssue_hidesBottomNavigationAndFab() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()
        composeTestRule.onNodeWithTag("cameraQuickIssue").performClick()

        composeTestRule.onNodeWithText("New issue").assertExists()
        composeTestRule.onNodeWithTag("bottomNavCapture").assertDoesNotExist()
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

        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()
        composeTestRule.onNodeWithTag("cameraQuickIssue").performClick()
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

        composeTestRule.onNodeWithTag("bottomNavCapture").performClick()
        composeTestRule.onNodeWithTag("cameraQuickIssue").performClick()
        composeTestRule.onNodeWithText("Title *").performTextInput("Missing guardrail")

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
