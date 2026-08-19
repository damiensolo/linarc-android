package com.solomondesign.app.ui.navigation

/**
 * Per-destination chrome for the field prototype shell.
 *
 * IMPORTANT: this file must stay free of `androidx.compose` imports so it runs under
 * `./gradlew testDebugUnitTest` (JVM only — the project has no Robolectric). Icons are modelled
 * as [ChromeIcon] and mapped to `ImageVector` at the Compose boundary in `ChromeIcons.kt`.
 *
 * Replaces the old single `isImmersiveRoute()` boolean, which coupled bottom-bar and FAB
 * visibility together and could not express Pattern B (nested browsing keeps the bottom bar).
 */

/** The three navigation-contract patterns, plus fully chrome-less flows. */
enum class NavPattern {
    /** Bottom bar; no Material top app bar (title lives in content via `FieldPageHeader`). */
    TAB_ROOT,

    /** Pattern B: nested browsing. Bottom bar STAYS VISIBLE; back-arrow + title top bar. */
    BROWSE,

    /** Pattern A: full-screen task flow. No bottom bar; Close top-left / Save top-right. */
    TASK_FLOW,

    /** No bottom bar and no top bar (voice recording, media playback). */
    IMMERSIVE,
}

/** Icon identity, resolved to an `ImageVector` only at the Compose boundary. */
enum class ChromeIcon { ADD, ADD_A_PHOTO, MORE_TIME }

/** Modal bottom sheets hoisted in `AppNavHost`. Pattern C is never a nav destination. */
enum class AppSheet { CAPTURE, PROFILE, TIME_ENTRY, NEW_TOPIC, IMAGE_SOURCE }

/** What the shared FAB does. Data rather than a lambda, so the resolver stays pure. */
sealed interface FabAction {
    /** Pattern A: navigate to a static, argument-free route. */
    data class Navigate(val route: String) : FabAction

    /** Pattern C: open a hoisted modal bottom sheet. */
    data class OpenSheet(val sheet: AppSheet) : FabAction
}

data class FabConfig(
    val icon: ChromeIcon,
    val contentDescription: String,
    val action: FabAction,
    val testTag: String,
    /** Non-null renders an ExtendedFloatingActionButton. */
    val label: String? = null,
)

data class ScreenChrome(
    val pattern: NavPattern,
    val fab: FabConfig? = null,
)

/** Derived, never stored, so bottom-bar visibility can't drift from [NavPattern]. */
val ScreenChrome.showBottomBar: Boolean
    get() = pattern == NavPattern.TAB_ROOT || pattern == NavPattern.BROWSE

/**
 * The Capture FAB shown on the three tab roots.
 *
 * `contentDescription` and `testTag` are asserted by existing instrumented tests
 * (`AppNavHostTest`) — do not rename them.
 */
private val CAPTURE_FAB = FabConfig(
    icon = ChromeIcon.ADD,
    contentDescription = "Capture",
    action = FabAction.OpenSheet(AppSheet.CAPTURE),
    testTag = "captureFab",
)

private val TIME_ENTRY_FAB = FabConfig(
    icon = ChromeIcon.MORE_TIME,
    contentDescription = "New time entry",
    action = FabAction.OpenSheet(AppSheet.TIME_ENTRY),
    testTag = "timeEntryFab",
)

private val NEW_TOPIC_FAB = FabConfig(
    icon = ChromeIcon.ADD,
    contentDescription = "New topic",
    action = FabAction.OpenSheet(AppSheet.NEW_TOPIC),
    testTag = "newTopicFab",
)

private val ADD_IMAGE_FAB = FabConfig(
    icon = ChromeIcon.ADD_A_PHOTO,
    contentDescription = "Add image",
    action = FabAction.OpenSheet(AppSheet.IMAGE_SOURCE),
    testTag = "addImageFab",
)

/**
 * Explicit route -> chrome table.
 *
 * Keys are route *patterns* (`"tool/{toolId}"`), never concrete paths (`"tool/field_task"`),
 * because `NavDestination.route` returns the pattern. `resolverKeysOnPatternsNotPaths` in
 * `AppChromeTest` guards this.
 */
private val chromeByRoute: Map<String, ScreenChrome> = mapOf(
    AppRoutes.TODAY_HOME to ScreenChrome(NavPattern.TAB_ROOT, CAPTURE_FAB),
    AppRoutes.PLAN_HOME to ScreenChrome(NavPattern.TAB_ROOT, CAPTURE_FAB),
    AppRoutes.TOOLS_HOME to ScreenChrome(NavPattern.TAB_ROOT, CAPTURE_FAB),

    AppRoutes.VOICE_LOG to ScreenChrome(NavPattern.IMMERSIVE),
    AppRoutes.DAILY_LOG_DETAIL to ScreenChrome(NavPattern.IMMERSIVE),
    AppRoutes.PHOTO_CAPTURE to ScreenChrome(NavPattern.TASK_FLOW),
    AppRoutes.QUICK_ISSUE to ScreenChrome(NavPattern.TASK_FLOW),
    AppRoutes.TOOL_CREATE to ScreenChrome(NavPattern.TASK_FLOW),

    // Pattern B: nested browsing inside the Tools tab, so the bottom bar stays visible. These
    // were immersive before the graphs were nested.
    AppRoutes.DAILY_LOG_HISTORY to ScreenChrome(NavPattern.BROWSE),
    AppRoutes.OUTBOX to ScreenChrome(NavPattern.BROWSE),
    AppRoutes.TOOL_HOME to ScreenChrome(NavPattern.BROWSE),
    AppRoutes.TOOL_DETAIL to ScreenChrome(NavPattern.BROWSE),

    // Field Tasks — Pattern B, no contextual FAB.
    AppRoutes.FIELD_TASK_LIST to ScreenChrome(NavPattern.BROWSE),
    AppRoutes.FIELD_TASK_DETAIL to ScreenChrome(NavPattern.BROWSE),

    // Time Cards — Pattern B with a contextual FAB on both list and detail.
    AppRoutes.TIME_CARD_LIST to ScreenChrome(NavPattern.BROWSE, TIME_ENTRY_FAB),
    AppRoutes.TIME_CARD_DETAIL to ScreenChrome(NavPattern.BROWSE, TIME_ENTRY_FAB),

    // Crew — Pattern B, no contextual FAB.
    AppRoutes.CREW_LIST to ScreenChrome(NavPattern.BROWSE),
    AppRoutes.CREW_DETAIL to ScreenChrome(NavPattern.BROWSE),

    // Collaboration — FAB only on the topic list; the conversation composes inline.
    AppRoutes.COLLAB_TOPIC_LIST to ScreenChrome(NavPattern.BROWSE, NEW_TOPIC_FAB),
    AppRoutes.COLLAB_TOPIC_DETAIL to ScreenChrome(NavPattern.BROWSE),

    // Images — grid is Pattern B; the viewer is Pattern A with a floating footer toolbar.
    AppRoutes.IMAGE_GRID to ScreenChrome(NavPattern.BROWSE, ADD_IMAGE_FAB),
    AppRoutes.IMAGE_VIEWER to ScreenChrome(NavPattern.TASK_FLOW),
)

/** Fail closed: an unregistered route hides all chrome rather than showing a wrong FAB. */
private val UNREGISTERED = ScreenChrome(NavPattern.IMMERSIVE, fab = null)

fun resolveChrome(route: String?): ScreenChrome = chromeByRoute[route] ?: UNREGISTERED

/** Exposed so `AppChromeTest` can prove every declared route has explicit chrome. */
fun isChromeRegistered(route: String): Boolean = chromeByRoute.containsKey(route)
