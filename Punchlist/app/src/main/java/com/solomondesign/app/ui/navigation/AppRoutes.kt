package com.solomondesign.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

/** Route string constants for the field prototype chassis. */
object AppRoutes {
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

    fun toolHome(toolId: String) = "tool/$toolId"

    fun toolCreate(toolId: String) = "tool/$toolId/create"

    fun toolDetail(toolId: String, title: String) =
        "tool/$toolId/detail/${URLEncoder.encode(title, "UTF-8")}"
}

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(AppRoutes.TODAY_HOME, "Today", Icons.Filled.Today),
    BottomNavTab(AppRoutes.PLAN_HOME, "Plan", Icons.Filled.Architecture),
    BottomNavTab(AppRoutes.TOOLS_HOME, "Tools", Icons.Filled.Handyman),
)

fun isImmersiveRoute(route: String?): Boolean {
    if (route == null) return false
    return route == AppRoutes.VOICE_LOG ||
        route == AppRoutes.PHOTO_CAPTURE ||
        route == AppRoutes.QUICK_ISSUE ||
        route.startsWith("daily_log_detail/") ||
        route == AppRoutes.DAILY_LOG_HISTORY ||
        route == AppRoutes.OUTBOX ||
        route.startsWith("tool/")
}
