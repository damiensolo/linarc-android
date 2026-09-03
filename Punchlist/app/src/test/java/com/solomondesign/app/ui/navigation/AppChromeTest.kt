package com.solomondesign.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the route -> chrome resolver.
 *
 * These deliberately never touch `bottomNavTabs`, which is a top-level val in `AppRoutes.kt` and
 * would drag Compose icon initialisation into the test JVM.
 */
class AppChromeTest {

    @Test
    fun everyDeclaredRouteHasExplicitChrome() {
        val unregistered = AppRoutes.ALL_ROUTES.filterNot(::isChromeRegistered)
        assertEquals("routes missing explicit chrome", emptyList<String>(), unregistered)
    }

    /** Capture moved from a global FAB into the navigation bar; tab roots carry no FAB now. */
    @Test
    fun tabRootsShowBottomBarWithoutAFab() {
        listOf(AppRoutes.TODAY_HOME, AppRoutes.PLAN_HOME, AppRoutes.TOOLS_HOME).forEach { route ->
            val chrome = resolveChrome(route)
            assertEquals(route, NavPattern.TAB_ROOT, chrome.pattern)
            assertTrue(route, chrome.showBottomBar)
            assertNull(route, chrome.fab)
        }
    }

    /**
     * The bar's Capture entry is an action, not a destination: it points at the camera route
     * (which is immersive, so the bar disappears the moment it opens) and its identifiers are
     * pinned because `AppNavHostTest` and `VoiceLogFlowTest` assert them.
     */
    @Test
    fun captureNavActionOpensTheImmersiveCamera() {
        assertEquals(AppRoutes.CAMERA, CaptureNavAction.route)
        assertEquals("bottomNavCapture", CaptureNavAction.testTag)
        assertEquals("Capture", CaptureNavAction.contentDescription)
        assertEquals(ChromeIcon.PHOTO_CAMERA, CaptureNavAction.icon)
        assertEquals(NavPattern.IMMERSIVE, resolveChrome(CaptureNavAction.route).pattern)
        assertFalse(resolveChrome(CaptureNavAction.route).showBottomBar)
    }

    @Test
    fun taskFlowAndImmersiveRoutesHideBottomBarAndFab() {
        listOf(
            AppRoutes.VOICE_NOTE,
            AppRoutes.VOICE_LOG,
            AppRoutes.DAILY_LOG_DETAIL,
            AppRoutes.CAMERA,
            AppRoutes.RECORD_CREATE,
            AppRoutes.TOOL_CREATE,
            AppRoutes.IMAGE_VIEWER,
            AppRoutes.IMAGE_MARKUP,
            AppRoutes.VIDEO_PLAYBACK,
            AppRoutes.PLAN_VIEWER,
        ).forEach { route ->
            val chrome = resolveChrome(route)
            assertFalse(route, chrome.showBottomBar)
            assertNull(route, chrome.fab)
        }
    }

    /** Contextual FABs replace Capture on the screens that own an action. */
    @Test
    fun contextualFabsAreScopedToTheirOwnScreens() {
        assertEquals("timeEntryFab", resolveChrome(AppRoutes.TIME_CARD_LIST).fab?.testTag)
        assertEquals("timeEntryFab", resolveChrome(AppRoutes.TIME_CARD_DETAIL).fab?.testTag)
        assertEquals("newTopicFab", resolveChrome(AppRoutes.COLLAB_TOPIC_LIST).fab?.testTag)
        assertEquals("addImageFab", resolveChrome(AppRoutes.IMAGE_GRID).fab?.testTag)
        assertEquals("newIssueFab", resolveChrome(AppRoutes.RECORD_LIST_ISSUES).fab?.testTag)
        assertEquals("newIncidentFab", resolveChrome(AppRoutes.RECORD_LIST_INCIDENTS).fab?.testTag)
        assertEquals("newPunchItemFab", resolveChrome(AppRoutes.RECORD_LIST_PUNCH).fab?.testTag)

        // Browsing screens with no action of their own carry no FAB at all.
        listOf(
            AppRoutes.FIELD_TASK_LIST,
            AppRoutes.FIELD_TASK_DETAIL,
            AppRoutes.CREW_LIST,
            AppRoutes.CREW_DETAIL,
            AppRoutes.COLLAB_TOPIC_DETAIL,
            AppRoutes.RECORD_DETAIL,
            AppRoutes.PLAN_LIST,
        ).forEach { assertNull(it, resolveChrome(it).fab) }
    }

    /** Each record tool's FAB creates that tool's own category — never a sibling's. */
    @Test
    fun recordToolFabsTargetTheirOwnCreateForm() {
        assertEquals(
            FabAction.Navigate("records/create/issue"),
            resolveChrome(AppRoutes.RECORD_LIST_ISSUES).fab?.action,
        )
        assertEquals(
            FabAction.Navigate("records/create/incident"),
            resolveChrome(AppRoutes.RECORD_LIST_INCIDENTS).fab?.action,
        )
        assertEquals(
            FabAction.Navigate("records/create/punch"),
            resolveChrome(AppRoutes.RECORD_LIST_PUNCH).fab?.action,
        )
    }

    /** The global Capture FAB is gone for good — no route may quietly resurrect it. */
    @Test
    fun noRouteCarriesACaptureFab() {
        val withCaptureFab = AppRoutes.ALL_ROUTES
            .filter { resolveChrome(it).fab?.testTag == "captureFab" }
        assertEquals(emptyList<String>(), withCaptureFab)
    }

    @Test
    fun unknownRouteAndNullFailClosed() {
        val expected = ScreenChrome(NavPattern.IMMERSIVE)
        assertEquals(expected, resolveChrome("who_knows"))
        assertEquals(expected, resolveChrome(null))
        assertFalse(resolveChrome("who_knows").showBottomBar)
    }

    /**
     * `NavDestination.route` yields the route *pattern*, so the resolver must key on the pattern
     * constants. A concrete path is unregistered and must fail closed rather than silently
     * inheriting a sibling's chrome.
     */
    @Test
    fun resolverKeysOnPatternsNotConcretePaths() {
        assertEquals(NavPattern.IMMERSIVE, resolveChrome("tool/field_task").pattern)
        assertFalse(isChromeRegistered("tool/field_task"))
        assertTrue(isChromeRegistered(AppRoutes.TOOL_HOME))
    }

    /**
     * Explicit expectation table for bottom-bar visibility, replacing the deleted
     * `isImmersiveRoute()`. The Pattern B rows are the contract this refactor exists to deliver:
     * nested browsing keeps the bottom bar.
     */
    @Test
    fun bottomBarVisibilityMatchesTheExpectedTable() {
        val expected = mapOf(
            AppRoutes.TODAY_HOME to true,
            AppRoutes.PLAN_HOME to true,
            AppRoutes.TOOLS_HOME to true,
            AppRoutes.VOICE_NOTE to false,
            AppRoutes.VOICE_LOG to false,
            AppRoutes.DAILY_LOG_DETAIL to false,
            AppRoutes.CAMERA to false,
            AppRoutes.RECORD_CREATE to false,
            AppRoutes.TOOL_CREATE to false,
            // Record tool lists browse inside the Tools tab; the detail stays in the stack too.
            AppRoutes.RECORD_LIST_ISSUES to true,
            AppRoutes.RECORD_LIST_INCIDENTS to true,
            AppRoutes.RECORD_LIST_PUNCH to true,
            AppRoutes.RECORD_DETAIL to true,
            // Pattern B: bottom nav stays visible while browsing inside the Tools tab.
            AppRoutes.DAILY_LOG_HISTORY to true,
            AppRoutes.OUTBOX to true,
            AppRoutes.SETTINGS to true,
            AppRoutes.TOOL_HOME to true,
            AppRoutes.TOOL_DETAIL to true,
            AppRoutes.FIELD_TASK_LIST to true,
            AppRoutes.FIELD_TASK_DETAIL to true,
            AppRoutes.TIME_CARD_LIST to true,
            AppRoutes.TIME_CARD_DETAIL to true,
            AppRoutes.CREW_LIST to true,
            AppRoutes.CREW_DETAIL to true,
            AppRoutes.COLLAB_TOPIC_LIST to true,
            AppRoutes.COLLAB_TOPIC_DETAIL to true,
            AppRoutes.IMAGE_GRID to true,
            AppRoutes.PLAN_LIST to true,
            // Pattern A: the viewers are full-screen; the markup editor is immersive.
            AppRoutes.IMAGE_VIEWER to false,
            AppRoutes.IMAGE_MARKUP to false,
            AppRoutes.VIDEO_PLAYBACK to false,
            AppRoutes.PLAN_VIEWER to false,
        )
        assertEquals(
            "expectation table must cover every declared route",
            AppRoutes.ALL_ROUTES.toSet(),
            expected.keys,
        )
        expected.forEach { (route, showsBar) ->
            assertEquals(route, showsBar, resolveChrome(route).showBottomBar)
        }
    }
}
