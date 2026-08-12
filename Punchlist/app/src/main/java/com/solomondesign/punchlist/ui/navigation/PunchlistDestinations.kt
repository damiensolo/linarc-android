package com.solomondesign.punchlist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

/** Route string constants for every screen in the app's information architecture. */
object PunchlistRoutes {
    // Today
    const val TODAY_HOME = "today_home"
    const val TASK_LIST = "task_list"
    const val TASK_DETAIL = "task_detail"
    const val START_WORK = "start_work"
    const val ISSUES_RFIS_LIST = "issues_rfis_list"
    const val ISSUE_RFI_DETAIL = "issue_rfi_detail"
    const val SCHEDULE_SNAPSHOT = "schedule_snapshot"

    // Projects
    const val PROJECTS_HOME = "projects_home"
    const val PROJECT_SPACE = "project_space/{projectName}"

    // Capture
    const val CAPTURE_HOME = "capture_home"
    const val NEW_ISSUE_PUNCH = "new_issue_punch"
    const val NEW_RFI = "new_rfi"
    const val DAILY_LOG = "daily_log"
    const val PHOTO_VIDEO = "photo_video"
    const val SCAN_MATERIAL = "scan_material"
    const val OUTBOX = "outbox"

    // Reports
    const val REPORTS_HOME = "reports_home"
    const val GENERATE_OAC_REPORT = "generate_oac_report"
    const val OAC_REPORT_LIST = "oac_report_list"
    const val OAC_REPORT_DETAIL = "oac_report_detail"
    const val DASHBOARDS = "dashboards"

    // More
    const val MORE_HOME = "more_home"
    const val SETTINGS = "settings"
    const val OFFLINE_PROJECTS = "offline_projects"
    const val HELP_TRAINING = "help_training"
    const val ADMIN_PERMISSIONS = "admin_permissions"
    const val DESIGN_SYSTEM = "design_system"
}

/** One entry in the bottom navigation bar. */
data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(PunchlistRoutes.TODAY_HOME, "Today", Icons.Filled.Today),
    BottomNavTab(PunchlistRoutes.PROJECTS_HOME, "Projects", Icons.Filled.Business),
    BottomNavTab(PunchlistRoutes.CAPTURE_HOME, "Capture", Icons.Filled.AddAPhoto),
    BottomNavTab(PunchlistRoutes.REPORTS_HOME, "Reports", Icons.Filled.Assessment),
    BottomNavTab(PunchlistRoutes.MORE_HOME, "More", Icons.Filled.MoreHoriz),
)
