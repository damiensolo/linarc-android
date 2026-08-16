package com.solomondesign.app.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDemoStore() {
        DemoProjectRepository.clear()
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
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.MORE_HOME}").assertExists()

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
    fun more_showsDemoViewAs_withForemanLiveAndOthersListed() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.MORE_HOME}").performClick()
        composeTestRule.onNodeWithTag("demoViewAsRow").performClick()
        composeTestRule.onNodeWithTag("persona_FOREMAN").assertExists()
        composeTestRule.onNodeWithTag("persona_SUPERINTENDENT").assertExists()
        composeTestRule.onNodeWithTag("persona_OWNER").assertExists()
        composeTestRule.onNodeWithText("Live").assertExists()
        composeTestRule.onAllNodesWithText("Next — same tabs, different Today").assertCountEquals(5)
    }

    @Test
    fun more_showsThemeToggle_andHidesDesignSystem() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.MORE_HOME}").performClick()
        composeTestRule.onNodeWithTag("themeToggle").assertExists()
        composeTestRule.onNodeWithText("Dark theme").assertExists()
        composeTestRule.onNodeWithText("Design system").assertDoesNotExist()
    }
}
