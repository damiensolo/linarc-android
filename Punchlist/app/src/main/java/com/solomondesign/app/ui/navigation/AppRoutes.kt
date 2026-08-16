package com.solomondesign.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

/** Route string constants for the field prototype chassis. */
object AppRoutes {
    const val TODAY_HOME = "today_home"
    const val PLAN_HOME = "plan_home"
    const val MORE_HOME = "more_home"

    const val VOICE_LOG = "voice_log"
    const val DAILY_LOG_HISTORY = "daily_log_history"
    const val DAILY_LOG_DETAIL = "daily_log_detail/{recordId}"

    const val PHOTO_CAPTURE = "photo_capture"
    const val QUICK_ISSUE = "quick_issue"
    const val OUTBOX = "outbox"
}

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(AppRoutes.TODAY_HOME, "Today", Icons.Filled.Today),
    BottomNavTab(AppRoutes.PLAN_HOME, "Plan", Icons.Filled.Architecture),
    BottomNavTab(AppRoutes.MORE_HOME, "More", Icons.Filled.MoreHoriz),
)

fun isImmersiveRoute(route: String?): Boolean {
    if (route == null) return false
    return route == AppRoutes.VOICE_LOG ||
        route == AppRoutes.PHOTO_CAPTURE ||
        route == AppRoutes.QUICK_ISSUE ||
        route.startsWith("daily_log_detail/") ||
        route == AppRoutes.DAILY_LOG_HISTORY ||
        route == AppRoutes.OUTBOX
}
