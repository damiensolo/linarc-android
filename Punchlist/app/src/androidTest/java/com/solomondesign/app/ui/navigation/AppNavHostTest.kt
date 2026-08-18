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

    @Test
    fun tools_showsDemoViewAs_withForemanLiveAndOthersListed() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
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

        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_field_task").performClick()
        composeTestRule.onNodeWithText("Field task").assertExists()
        composeTestRule.onNodeWithText("Field task · sample 1").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithTag("toolQuickCreate_collaboration").performClick()
        composeTestRule.onNodeWithText("New Collaboration").assertExists()
        composeTestRule.onNodeWithText("Quick create is a placeholder in this build.").assertExists()
    }
}
