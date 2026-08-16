package com.solomondesign.punchlist.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solomondesign.punchlist.ui.capture.CaptureSheet
import com.solomondesign.punchlist.ui.capture.PhotoCaptureScreen
import com.solomondesign.punchlist.ui.capture.QuickIssueScreen
import com.solomondesign.punchlist.ui.demo.DemoProjectRepository
import com.solomondesign.punchlist.ui.more.MoreScreen
import com.solomondesign.punchlist.ui.more.OutboxScreen
import com.solomondesign.punchlist.ui.plan.PlanScreen
import com.solomondesign.punchlist.ui.punchhome.PunchHomeScreen
import com.solomondesign.punchlist.ui.today.TodayScreen
import com.solomondesign.punchlist.ui.voicelog.DailyLogHomeScreen
import com.solomondesign.punchlist.ui.voicelog.DailyLogPlaybackScreen
import com.solomondesign.punchlist.ui.voicelog.VoiceLogScreen
import java.net.URLDecoder
import java.net.URLEncoder

private fun dailyLogDetailRoute(recordId: String) =
    "daily_log_detail/${URLEncoder.encode(recordId, "UTF-8")}"

/**
 * Field prototype shell: Today / Plan / More plus a Material FAB for capture.
 * Immersive flows (voice, photo, issue, playback) hide the bar and FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchlistNavHost() {
    val navController = rememberNavController()
    var showCaptureSheet by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val immersive = isImmersiveRoute(currentRoute)

    Scaffold(
        topBar = {
            if (!immersive) {
                TopAppBar(title = { Text(DemoProjectRepository.PROJECT_NAME) })
            }
        },
        floatingActionButton = {
            if (!immersive) {
                FloatingActionButton(
                    onClick = { showCaptureSheet = true },
                    modifier = Modifier.testTag("captureFab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Capture")
                }
            }
        },
        bottomBar = {
            if (!immersive) {
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
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = PunchlistRoutes.TODAY_HOME,
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            composable(PunchlistRoutes.TODAY_HOME) {
                TodayScreen(
                    onOpenVoiceLog = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                )
            }
            composable(PunchlistRoutes.PLAN_HOME) {
                PlanScreen()
            }
            composable(PunchlistRoutes.MORE_HOME) {
                MoreScreen(
                    onOpenOutbox = { navController.navigate(PunchlistRoutes.OUTBOX) },
                    onOpenVoiceLogs = { navController.navigate(PunchlistRoutes.DAILY_LOG_HISTORY) },
                    onOpenDesignSystem = { navController.navigate(PunchlistRoutes.DESIGN_SYSTEM) },
                )
            }
            composable(PunchlistRoutes.VOICE_LOG) {
                VoiceLogScreen(onExit = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.DAILY_LOG_HISTORY) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Voice logs") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                        )
                    },
                ) { padding ->
                    DailyLogHomeScreen(
                        onRecordNew = { navController.navigate(PunchlistRoutes.VOICE_LOG) },
                        onOpenRecord = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            composable(
                route = PunchlistRoutes.DAILY_LOG_DETAIL,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
            ) { entry ->
                val encodedId = entry.arguments?.getString("recordId").orEmpty()
                DailyLogPlaybackScreen(
                    recordId = URLDecoder.decode(encodedId, "UTF-8"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PunchlistRoutes.PHOTO_CAPTURE) {
                PhotoCaptureScreen(onDone = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.QUICK_ISSUE) {
                QuickIssueScreen(onDone = { navController.popBackStack() })
            }
            composable(PunchlistRoutes.OUTBOX) {
                OutboxScreen(onBack = { navController.popBackStack() })
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

    if (showCaptureSheet) {
        CaptureSheet(
            onVoice = {
                showCaptureSheet = false
                navController.navigate(PunchlistRoutes.VOICE_LOG)
            },
            onPhoto = {
                showCaptureSheet = false
                navController.navigate(PunchlistRoutes.PHOTO_CAPTURE)
            },
            onIssue = {
                showCaptureSheet = false
                navController.navigate(PunchlistRoutes.QUICK_ISSUE)
            },
            onDismiss = { showCaptureSheet = false },
        )
    }
}
