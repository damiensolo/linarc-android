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

    /**
     * Full-screen sheet viewer (Pattern A). Lives at the nav-graph root like [IMAGE_VIEWER]
     * because it hides the bottom bar.
     */
    const val PLAN_VIEWER = "plan_viewer/{sheetId}"

    const val VOICE_LOG = "voice_log"
    const val DAILY_LOG_HISTORY = "daily_log_history"
    const val DAILY_LOG_DETAIL = "daily_log_detail/{recordId}"

    /**
     * Full-screen in-app camera (Pattern A / immersive), opened by the bottom bar's Capture
     * action and by every "take photo" entry point. Replaced the old `photo_capture` system-
     * camera stub.
     */
    const val CAMERA = "camera"

    const val OUTBOX = "outbox"

    /** App settings (Appearance + demo controls), reached from the Tools header overflow. */
    const val SETTINGS = "settings"

    // Record tools (Issues / Incidents / Punch list): Pattern B lists inside the Tools tab,
    // one shared Pattern A create form, and a Pattern B detail. Replaced the old quick_issue
    // route — the Issue category of the record form is its successor.
    const val RECORD_LIST_ISSUES = "records/issues"
    const val RECORD_LIST_INCIDENTS = "records/incidents"
    const val RECORD_LIST_PUNCH = "records/punch"
    const val RECORD_CREATE = "records/create/{category}"
    const val RECORD_DETAIL = "records/detail/{recordId}"

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

    /** Markup editor over one captured image — immersive, like the camera it grew out of. */
    const val IMAGE_MARKUP = "images/markup/{imageId}"

    /** Playback of one captured video — immersive, like daily-log playback. */
    const val VIDEO_PLAYBACK = "video/{videoId}"

    fun planViewer(sheetId: String) = "plan_viewer/${encode(sheetId)}"

    fun fieldTaskDetail(taskId: String) = "field_task/${encode(taskId)}"

    fun timeCardDetail(crewMemberId: String) = "time_card/${encode(crewMemberId)}"

    fun crewDetail(crewMemberId: String) = "crew/${encode(crewMemberId)}"

    fun collabTopic(topicId: String) = "collaboration/${encode(topicId)}"

    fun imageViewer(imageId: String) = "images/viewer/${encode(imageId)}"

    fun imageMarkup(imageId: String) = "images/markup/${encode(imageId)}"

    fun videoPlayback(videoId: String) = "video/${encode(videoId)}"

    /** [categoryRouteId] comes from `RecordCategory.routeId` — already route-safe. */
    fun recordCreate(categoryRouteId: String) = "records/create/$categoryRouteId"

    fun recordDetail(recordId: String) = "records/detail/${encode(recordId)}"

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
        PLAN_VIEWER,
        TOOLS_HOME,
        VOICE_LOG,
        DAILY_LOG_HISTORY,
        DAILY_LOG_DETAIL,
        CAMERA,
        OUTBOX,
        SETTINGS,
        RECORD_LIST_ISSUES,
        RECORD_LIST_INCIDENTS,
        RECORD_LIST_PUNCH,
        RECORD_CREATE,
        RECORD_DETAIL,
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
        IMAGE_MARKUP,
        VIDEO_PLAYBACK,
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
    BottomNavTab(AppRoutes.PLAN_HOME, AppRoutes.PLAN_GRAPH, "Plans", Icons.Filled.Architecture),
    BottomNavTab(AppRoutes.TOOLS_HOME, AppRoutes.TOOLS_GRAPH, "Tools", Icons.Filled.Handyman),
)
