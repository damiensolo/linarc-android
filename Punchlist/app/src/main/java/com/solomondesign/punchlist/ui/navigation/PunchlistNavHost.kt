package com.solomondesign.punchlist.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solomondesign.punchlist.ui.common.CtaSpec
import com.solomondesign.punchlist.ui.common.DetailPlaceholderScreen
import com.solomondesign.punchlist.ui.common.HomeMenuScreen
import com.solomondesign.punchlist.ui.common.ListPlaceholderScreen
import com.solomondesign.punchlist.ui.common.MenuItem
import com.solomondesign.punchlist.ui.designsystem.PunchlistButtonType
import com.solomondesign.punchlist.ui.projects.ProjectSpaceScreen
import com.solomondesign.punchlist.ui.punchhome.PunchHomeScreen
import java.net.URLDecoder
import java.net.URLEncoder

private fun projectSpaceRoute(projectName: String) =
    "project_space/${URLEncoder.encode(projectName, "UTF-8")}"

/**
 * Root navigation shell for the whole app: a bottom nav bar (5 tabs) wrapping a
 * [NavHost] that covers every screen in the information architecture. Leaf screens
 * are placeholders (see [com.solomondesign.punchlist.ui.common]) — this proves the
 * IA is fully reachable before any individual screen gets real content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchlistNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                bottomNavTabs.forEach { tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("bottomNavTab_${tab.route}"),
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = PunchlistRoutes.TODAY_HOME,
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            // ---- Today ----
            composable(PunchlistRoutes.TODAY_HOME) {
                HomeMenuScreen(
                    title = "Today",
                    items = listOf(
                        MenuItem("My Tasks") { navController.navigate(PunchlistRoutes.TASK_LIST) },
                        MenuItem("My Time") { navController.navigate(PunchlistRoutes.START_WORK) },
                        MenuItem("Issues & RFIs") { navController.navigate(PunchlistRoutes.ISSUES_RFIS_LIST) },
                        MenuItem("Schedule Snapshot") { navController.navigate(PunchlistRoutes.SCHEDULE_SNAPSHOT) },
                    ),
                    ctas = listOf(
                        CtaSpec("Start My Day", PunchlistButtonType.Primary) {
                            navController.navigate(PunchlistRoutes.START_WORK)
                        },
                        CtaSpec("View Full Schedule", PunchlistButtonType.Secondary) {
                            navController.navigate(PunchlistRoutes.SCHEDULE_SNAPSHOT)
                        },
                    ),
                )
            }
            composable(PunchlistRoutes.TASK_LIST) {
                ListPlaceholderScreen(
                    title = "Task List",
                    rows = listOf(
                        "Frame inspection - Bldg A",
                        "Punch walk - Unit 204",
                        "Concrete pour review",
                    ),
                    onItemClick = { navController.navigate(PunchlistRoutes.TASK_DETAIL) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.TASK_DETAIL) {
                DetailPlaceholderScreen(title = "Task Detail", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.START_WORK) {
                DetailPlaceholderScreen(
                    title = "Start Work",
                    subtitle = "Daily Log + Crew Time",
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.ISSUES_RFIS_LIST) {
                ListPlaceholderScreen(
                    title = "Issues & RFIs",
                    rows = listOf(
                        "Issue #142 - Water intrusion",
                        "RFI #58 - Beam clearance",
                        "Issue #143 - Missing anchor",
                    ),
                    onItemClick = { navController.navigate(PunchlistRoutes.ISSUE_RFI_DETAIL) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.ISSUE_RFI_DETAIL) {
                DetailPlaceholderScreen(title = "Issue / RFI Detail", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.SCHEDULE_SNAPSHOT) {
                DetailPlaceholderScreen(title = "Schedule Snapshot", onBack = { navController.popBackStack() })
            }

            // ---- Projects ----
            composable(PunchlistRoutes.PROJECTS_HOME) {
                ListPlaceholderScreen(
                    title = "Projects",
                    rows = listOf(
                        "Maple Street Apartments",
                        "Riverside Medical Center",
                        "Downtown Parking Structure",
                    ),
                    onItemClick = { projectName -> navController.navigate(projectSpaceRoute(projectName)) },
                )
            }
            composable(
                route = PunchlistRoutes.PROJECT_SPACE,
                arguments = listOf(navArgument("projectName") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encodedName = backStackEntry.arguments?.getString("projectName").orEmpty()
                ProjectSpaceScreen(
                    projectName = URLDecoder.decode(encodedName, "UTF-8"),
                    onBack = { navController.popBackStack() },
                )
            }

            // ---- Capture ----
            composable(PunchlistRoutes.CAPTURE_HOME) {
                HomeMenuScreen(
                    title = "Capture",
                    items = listOf(
                        MenuItem("New Issue / Punch") { navController.navigate(PunchlistRoutes.NEW_ISSUE_PUNCH) },
                        MenuItem("New RFI") { navController.navigate(PunchlistRoutes.NEW_RFI) },
                        MenuItem("Daily Log") { navController.navigate(PunchlistRoutes.DAILY_LOG) },
                        MenuItem("Photo / Video") { navController.navigate(PunchlistRoutes.PHOTO_VIDEO) },
                        MenuItem("Scan Material") { navController.navigate(PunchlistRoutes.SCAN_MATERIAL) },
                        MenuItem("Outbox") { navController.navigate(PunchlistRoutes.OUTBOX) },
                    ),
                )
            }
            composable(PunchlistRoutes.NEW_ISSUE_PUNCH) {
                DetailPlaceholderScreen(title = "New Issue / Punch", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.NEW_RFI) {
                DetailPlaceholderScreen(title = "New RFI", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.DAILY_LOG) {
                DetailPlaceholderScreen(title = "Daily Log", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.PHOTO_VIDEO) {
                DetailPlaceholderScreen(
                    title = "Photo / Video",
                    subtitle = "With markup",
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.SCAN_MATERIAL) {
                DetailPlaceholderScreen(
                    title = "Scan Material",
                    subtitle = "QR / barcode",
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.OUTBOX) {
                DetailPlaceholderScreen(
                    title = "Outbox",
                    subtitle = "Unsynced items",
                    onBack = { navController.popBackStack() },
                )
            }

            // ---- Reports ----
            composable(PunchlistRoutes.REPORTS_HOME) {
                HomeMenuScreen(
                    title = "Reports",
                    items = listOf(
                        MenuItem("Generate OAC Report") { navController.navigate(PunchlistRoutes.GENERATE_OAC_REPORT) },
                        MenuItem("OAC Report List") { navController.navigate(PunchlistRoutes.OAC_REPORT_LIST) },
                        MenuItem("Dashboards") { navController.navigate(PunchlistRoutes.DASHBOARDS) },
                    ),
                )
            }
            composable(PunchlistRoutes.GENERATE_OAC_REPORT) {
                DetailPlaceholderScreen(title = "Generate OAC Report", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.OAC_REPORT_LIST) {
                ListPlaceholderScreen(
                    title = "OAC Reports",
                    rows = listOf(
                        "OAC Report - Jul 2026",
                        "OAC Report - Jun 2026",
                        "OAC Report - May 2026",
                    ),
                    onItemClick = { navController.navigate(PunchlistRoutes.OAC_REPORT_DETAIL) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.OAC_REPORT_DETAIL) {
                DetailPlaceholderScreen(title = "OAC Report Detail", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.DASHBOARDS) {
                DetailPlaceholderScreen(
                    title = "Dashboards",
                    subtitle = "Schedule, cost, RFI aging",
                    onBack = { navController.popBackStack() },
                )
            }

            // ---- More ----
            composable(PunchlistRoutes.MORE_HOME) {
                HomeMenuScreen(
                    title = "More",
                    items = listOf(
                        MenuItem("Settings") { navController.navigate(PunchlistRoutes.SETTINGS) },
                        MenuItem("Offline Projects") { navController.navigate(PunchlistRoutes.OFFLINE_PROJECTS) },
                        MenuItem("Help & Training") { navController.navigate(PunchlistRoutes.HELP_TRAINING) },
                        MenuItem("Admin / Permissions") { navController.navigate(PunchlistRoutes.ADMIN_PERMISSIONS) },
                        MenuItem("Design System") { navController.navigate(PunchlistRoutes.DESIGN_SYSTEM) },
                    ),
                )
            }
            composable(PunchlistRoutes.SETTINGS) {
                DetailPlaceholderScreen(title = "Settings", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.OFFLINE_PROJECTS) {
                DetailPlaceholderScreen(title = "Offline Projects", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.HELP_TRAINING) {
                DetailPlaceholderScreen(title = "Help & Training", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.ADMIN_PERMISSIONS) {
                DetailPlaceholderScreen(title = "Admin / Permissions", onBack = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.DESIGN_SYSTEM) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Design System") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                        )
                    },
                ) { padding ->
                    PunchHomeScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
