package com.solomondesign.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

/** Route string constants for the field prototype chassis. */
object AppRoutes {
    // Nested graph containers, one per bottom-nav tab. Never rendered themselves: they exist so
    // each tab owns a real back stack and so `destination.hierarchy` can identify the active tab
    // on nested (Pattern B) destinations.
    const val TODAY_GRAPH = "today_graph"
    const val PLAN_GRAPH = "plan_graph"
    const val TOOLS_GRAPH = "tools_graph"

    const val TODAY_HOME = "today_home"
    const val PLAN_HOME = "plan_home"
    const val TOOLS_HOME = "tools_home"

    const val VOICE_LOG = "voice_log"
    const val DAILY_LOG_HISTORY = "daily_log_history"
    const val DAILY_LOG_DETAIL = "daily_log_detail/{recordId}"

    const val PHOTO_CAPTURE = "photo_capture"
    const val QUICK_ISSUE = "quick_issue"
    const val OUTBOX = "outbox"

    const val TOOL_HOME = "tool/{toolId}"
    const val TOOL_CREATE = "tool/{toolId}/create"
    const val TOOL_DETAIL = "tool/{toolId}/detail/{title}"

    // Dedicated routes for the five real Tools areas. Each is Pattern B except the image viewer.
    const val FIELD_TASK_LIST = "field_task"
    const val FIELD_TASK_DETAIL = "field_task/{taskId}"

    const val TIME_CARD_LIST = "time_card"
    const val TIME_CARD_DETAIL = "time_card/{crewMemberId}"

    const val CREW_LIST = "crew"
    const val CREW_DETAIL = "crew/{crewMemberId}"

    const val COLLAB_TOPIC_LIST = "collaboration"
    const val COLLAB_TOPIC_DETAIL = "collaboration/{topicId}"

    const val IMAGE_GRID = "images"

    /**
     * Literal "viewer" segment on purpose: a bare `images/{imageId}` would be ambiguous with
     * [IMAGE_GRID]'s own children in the route matcher.
     */
    const val IMAGE_VIEWER = "images/viewer/{imageId}"

    fun fieldTaskDetail(taskId: String) = "field_task/${encode(taskId)}"

    fun timeCardDetail(crewMemberId: String) = "time_card/${encode(crewMemberId)}"

    fun crewDetail(crewMemberId: String) = "crew/${encode(crewMemberId)}"

    fun collabTopic(topicId: String) = "collaboration/${encode(topicId)}"

    fun imageViewer(imageId: String) = "images/viewer/${encode(imageId)}"

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    fun toolHome(toolId: String) = "tool/$toolId"

    fun toolCreate(toolId: String) = "tool/$toolId/create"

    fun toolDetail(toolId: String, title: String) =
        "tool/$toolId/detail/${URLEncoder.encode(title, "UTF-8")}"

    /**
     * Every navigable route pattern. Drives the completeness check in `AppChromeTest`, so a new
     * destination cannot ship without explicit chrome.
     *
     * Declared inside `object AppRoutes` (not as a top-level val) on purpose: top-level
     * declarations in this file compile into `AppRoutesKt`, whose initialiser builds
     * [bottomNavTabs] and therefore touches Compose icons. Keeping this here means JVM unit
     * tests can read it without pulling Compose into the test JVM.
     */
    val ALL_ROUTES: List<String> = listOf(
        TODAY_HOME,
        PLAN_HOME,
        TOOLS_HOME,
        VOICE_LOG,
        DAILY_LOG_HISTORY,
        DAILY_LOG_DETAIL,
        PHOTO_CAPTURE,
        QUICK_ISSUE,
        OUTBOX,
        TOOL_HOME,
        TOOL_CREATE,
        TOOL_DETAIL,
        FIELD_TASK_LIST,
        FIELD_TASK_DETAIL,
        TIME_CARD_LIST,
        TIME_CARD_DETAIL,
        CREW_LIST,
        CREW_DETAIL,
        COLLAB_TOPIC_LIST,
        COLLAB_TOPIC_DETAIL,
        IMAGE_GRID,
        IMAGE_VIEWER,
    )
}

data class BottomNavTab(
    /** The tab's root destination: the test-tag suffix and the reselect pop target. */
    val route: String,
    /** The tab's nested graph: the navigate() target and the hierarchy match for selection. */
    val graphRoute: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(AppRoutes.TODAY_HOME, AppRoutes.TODAY_GRAPH, "Today", Icons.Filled.Today),
    BottomNavTab(AppRoutes.PLAN_HOME, AppRoutes.PLAN_GRAPH, "Plan", Icons.Filled.Architecture),
    BottomNavTab(AppRoutes.TOOLS_HOME, AppRoutes.TOOLS_GRAPH, "Tools", Icons.Filled.Handyman),
)
