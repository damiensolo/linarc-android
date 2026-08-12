package com.solomondesign.punchlist.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solomondesign.punchlist.ui.theme.PunchlistTheme
import org.junit.Rule
import org.junit.Test

class PunchlistNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNav_showsAllFiveTabs_andSwitchingTabsShowsThatTabsContent() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertExists()
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.CAPTURE_HOME}").performClick()
        composeTestRule.onNodeWithText("New Issue / Punch").assertExists()
    }

    @Test
    fun today_drillDownToTaskList_andBack_returnsToTodayHome() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        composeTestRule.onNodeWithText("My Tasks").performClick()
        composeTestRule.onNodeWithText("Task List").assertExists()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("My Tasks").assertExists()
    }
}
