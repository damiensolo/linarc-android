package com.solomondesign.punchlist.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solomondesign.punchlist.ui.demo.DemoProjectRepository
import com.solomondesign.punchlist.ui.theme.PunchlistTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PunchlistNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDemoStore() {
        DemoProjectRepository.clear()
    }

    @Test
    fun bottomNav_showsThreeTabs_andSwitchingTabsShowsThatTabsContent() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        bottomNavTabs.forEach { tab ->
            composeTestRule.onNodeWithTag("bottomNavTab_${tab.route}").assertExists()
        }
        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.TODAY_HOME}").assertExists()
        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.PLAN_HOME}").assertExists()
        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.MORE_HOME}").assertExists()

        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.PLAN_HOME}").performClick()
        composeTestRule.onNodeWithTag("planScreen").assertExists()
    }

    @Test
    fun today_showsForemanCrewAndStartMyDay() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        composeTestRule.onNodeWithTag("todayScreen").assertExists()
        composeTestRule.onNodeWithText("Foreman · Area B").assertExists()
        composeTestRule.onNodeWithText("Hector Ortiz").assertExists()
        composeTestRule.onNodeWithTag("startMyDayCard").assertExists()
        composeTestRule.onNodeWithContentDescription("Capture").assertExists()
    }

    @Test
    fun more_showsDemoViewAs_withForemanLiveAndOthersListed() {
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${PunchlistRoutes.MORE_HOME}").performClick()
        composeTestRule.onNodeWithTag("demoViewAsRow").performClick()
        composeTestRule.onNodeWithTag("persona_FOREMAN").assertExists()
        composeTestRule.onNodeWithTag("persona_SUPERINTENDENT").assertExists()
        composeTestRule.onNodeWithTag("persona_OWNER").assertExists()
        composeTestRule.onNodeWithText("Live").assertExists()
        composeTestRule.onNodeWithText("Next — same tabs, different Today").assertCountEquals(5)
    }
}
