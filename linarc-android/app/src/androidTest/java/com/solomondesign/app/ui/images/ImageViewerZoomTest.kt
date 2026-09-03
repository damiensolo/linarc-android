package com.solomondesign.app.ui.images

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.doubleClick
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.navigation.AppNavHost
import com.solomondesign.app.ui.navigation.AppRoutes
import com.solomondesign.app.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pinch-to-zoom in the full-screen photo viewer (the shared ZoomableContainer): pinch zooms
 * for close inspection, double tap resets to fit. Zoom state is asserted through the same
 * stateDescription TalkBack reads, so the accessibility contract is pinned too.
 */
class ImageViewerZoomTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDemoStore() {
        DemoSession.reset()
    }

    private fun hasZoomState(prefix: String) = SemanticsMatcher(
        "stateDescription starts with \"$prefix\"",
    ) { node ->
        node.config.getOrNull(SemanticsProperties.StateDescription)?.startsWith(prefix) == true
    }

    @Test
    fun imageViewer_pinchZoomsIn_andDoubleTapResetsToFit() {
        composeTestRule.setContent {
            AppTheme {
                AppNavHost()
            }
        }
        composeTestRule.onNodeWithTag("bottomNavTab_${AppRoutes.TOOLS_HOME}").performClick()
        composeTestRule.onNodeWithTag("toolCard_images").performClick()
        composeTestRule.onNodeWithTag("imageTile_img-yesterday").performClick()

        val container = composeTestRule.onNodeWithTag("imageZoomContainer")
        container.assert(hasZoomState("Fit to screen"))

        // Two fingers moving apart from the center — a standard zoom-in pinch.
        container.performTouchInput {
            pinch(
                start0 = center - Offset(60f, 0f),
                end0 = center - Offset(240f, 0f),
                start1 = center + Offset(60f, 0f),
                end1 = center + Offset(240f, 0f),
            )
        }
        container.assert(hasZoomState("Zoomed to"))

        // Double tap while zoomed past the reset threshold returns to fit.
        container.performTouchInput { doubleClick() }
        container.assert(hasZoomState("Fit to screen"))
    }
}
