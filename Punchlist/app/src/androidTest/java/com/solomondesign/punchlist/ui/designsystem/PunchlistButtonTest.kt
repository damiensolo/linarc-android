package com.solomondesign.punchlist.ui.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solomondesign.punchlist.ui.theme.PunchlistTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PunchlistButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enabledButton_click_invokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistButton(text = "Label", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Label").performClick()

        assertTrue(clicked)
    }

    @Test
    fun disabledButton_click_doesNotInvokeCallback() {
        var clicked = false
        composeTestRule.setContent {
            PunchlistTheme {
                PunchlistButton(
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
