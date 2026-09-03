package com.solomondesign.app.ui.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solomondesign.app.ui.theme.AppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enabledButton_click_invokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                AppButton(text = "Label", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Label").performClick()

        assertTrue(clicked)
    }

    @Test
    fun disabledButton_click_doesNotInvokeCallback() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                AppButton(
                    text = "Label",
                    onClick = { clicked = true },
                    enabled = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Label").performClick()

        assertFalse(clicked)
    }
}
