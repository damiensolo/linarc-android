package com.solomondesign.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.solomondesign.app.ui.capture.CaptureSheet
import com.solomondesign.app.ui.capture.PhotoCaptureScreen
import com.solomondesign.app.ui.capture.QuickIssueScreen
import com.solomondesign.app.ui.more.MoreScreen
import com.solomondesign.app.ui.more.OutboxScreen
import com.solomondesign.app.ui.plan.PlanScreen
import com.solomondesign.app.ui.today.TodayScreen
import com.solomondesign.app.ui.voicelog.DailyLogHomeScreen
import com.solomondesign.app.ui.voicelog.DailyLogPlaybackScreen
import com.solomondesign.app.ui.voicelog.VoiceLogScreen
import java.net.URLDecoder
import java.net.URLEncoder

private fun dailyLogDetailRoute(recordId: String) =
    "daily_log_detail/${URLEncoder.encode(recordId, "UTF-8")}"

/**
 * Field prototype shell: Today / Plan / More plus a Material FAB for capture.
 * Immersive flows (voice, photo, issue, playback) hide the bar and FAB.
 * Page titles live in content, not a Material top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var showCaptureSheet by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val immersive = isImmersiveRoute(currentRoute)
    val colors = MaterialTheme.colorScheme

    Scaffold(
        floatingActionButton = {
            if (!immersive) {
                FloatingActionButton(
                    onClick = { showCaptureSheet = true },
                    modifier = Modifier.testTag("captureFab"),
                    shape = CircleShape,
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Capture")
                }
            }
        },
        bottomBar = {
            if (!immersive) {
                NavigationBar(containerColor = colors.surfaceContainer) {
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.onSurface,
                                selectedTextColor = colors.onSurface,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                                indicatorColor = colors.outline,
                            ),
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.TODAY_HOME,
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            composable(AppRoutes.TODAY_HOME) {
                TodayScreen(
                    onOpenVoiceLog = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                )
            }
            composable(AppRoutes.PLAN_HOME) {
                PlanScreen()
            }
            composable(AppRoutes.MORE_HOME) {
                MoreScreen(
                    onOpenOutbox = { navController.navigate(AppRoutes.OUTBOX) },
                    onOpenVoiceLogs = { navController.navigate(AppRoutes.DAILY_LOG_HISTORY) },
                )
            }
            composable(AppRoutes.VOICE_LOG) {
                VoiceLogScreen(onExit = { navController.popBackStack() })
            }
            composable(AppRoutes.DAILY_LOG_HISTORY) {
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
                        onRecordNew = { navController.navigate(AppRoutes.VOICE_LOG) },
                        onOpenRecord = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            composable(
                route = AppRoutes.DAILY_LOG_DETAIL,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
            ) { entry ->
                val encodedId = entry.arguments?.getString("recordId").orEmpty()
                DailyLogPlaybackScreen(
                    recordId = URLDecoder.decode(encodedId, "UTF-8"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.PHOTO_CAPTURE) {
                PhotoCaptureScreen(onDone = { navController.popBackStack() })
            }
            composable(AppRoutes.QUICK_ISSUE) {
                QuickIssueScreen(onDone = { navController.popBackStack() })
            }
            composable(AppRoutes.OUTBOX) {
                OutboxScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showCaptureSheet) {
        CaptureSheet(
            onVoice = {
                showCaptureSheet = false
                navController.navigate(AppRoutes.VOICE_LOG)
            },
            onPhoto = {
                showCaptureSheet = false
                navController.navigate(AppRoutes.PHOTO_CAPTURE)
            },
            onIssue = {
                showCaptureSheet = false
                navController.navigate(AppRoutes.QUICK_ISSUE)
            },
            onDismiss = { showCaptureSheet = false },
        )
    }
}
