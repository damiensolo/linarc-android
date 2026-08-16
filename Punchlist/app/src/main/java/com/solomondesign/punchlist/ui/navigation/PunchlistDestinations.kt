package com.solomondesign.punchlist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

/** Route string constants for the field prototype chassis. */
object PunchlistRoutes {
    const val TODAY_HOME = "today_home"
    const val PLAN_HOME = "plan_home"
    const val MORE_HOME = "more_home"

    const val VOICE_LOG = "voice_log"
    const val DAILY_LOG_HISTORY = "daily_log_history"
    const val DAILY_LOG_DETAIL = "daily_log_detail/{recordId}"

    const val PHOTO_CAPTURE = "photo_capture"
    const val QUICK_ISSUE = "quick_issue"
    const val OUTBOX = "outbox"
    const val DESIGN_SYSTEM = "design_system"
}

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(PunchlistRoutes.TODAY_HOME, "Today", Icons.Filled.Today),
    BottomNavTab(PunchlistRoutes.PLAN_HOME, "Plan", Icons.Filled.Architecture),
    BottomNavTab(PunchlistRoutes.MORE_HOME, "More", Icons.Filled.MoreHoriz),
)

fun isImmersiveRoute(route: String?): Boolean {
    if (route == null) return false
    return route == PunchlistRoutes.VOICE_LOG ||
        route == PunchlistRoutes.PHOTO_CAPTURE ||
        route == PunchlistRoutes.QUICK_ISSUE ||
        route == PunchlistRoutes.DESIGN_SYSTEM ||
        route.startsWith("daily_log_detail/") ||
        route == PunchlistRoutes.DAILY_LOG_HISTORY ||
        route == PunchlistRoutes.OUTBOX
}
