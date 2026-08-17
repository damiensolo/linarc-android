package com.solomondesign.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.common.DetailPlaceholderScreen
import com.solomondesign.app.ui.common.ListPlaceholderScreen
import com.solomondesign.app.ui.more.OutboxScreen
import com.solomondesign.app.ui.plan.PlanScreen
import com.solomondesign.app.ui.profile.ProfileSheet
import com.solomondesign.app.ui.today.TodayScreen
import com.solomondesign.app.ui.tools.PlatformTools
import com.solomondesign.app.ui.tools.ToolsScreen
import com.solomondesign.app.ui.voicelog.DailyLogHomeScreen
import com.solomondesign.app.ui.voicelog.DailyLogPlaybackScreen
import com.solomondesign.app.ui.voicelog.VoiceLogScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private fun dailyLogDetailRoute(recordId: String) =
    "daily_log_detail/${URLEncoder.encode(recordId, "UTF-8")}"

/**
 * Field prototype shell: Today / Plan / Tools plus a Material FAB for capture.
 * Immersive flows (voice, photo, issue, playback, tool placeholders) hide the bar and FAB.
 * Page titles live in content, not a Material top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val immersive = isImmersiveRoute(currentRoute)
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!immersive) {
                FloatingActionButton(
                    onClick = { showCaptureSheet = true },
                    modifier = Modifier.testTag("captureFab"),
                    shape = FloatingActionButtonDefaults.shape,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(),
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
                                indicatorColor = colors.surfaceContainerHigh,
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
                    onOpenProfile = { showProfileSheet = true },
                )
            }
            composable(AppRoutes.PLAN_HOME) {
                PlanScreen(onOpenProfile = { showProfileSheet = true })
            }
            composable(AppRoutes.TOOLS_HOME) {
                ToolsScreen(
                    onOpenOutbox = { navController.navigate(AppRoutes.OUTBOX) },
                    onOpenVoiceLogs = { navController.navigate(AppRoutes.DAILY_LOG_HISTORY) },
                    onOpenProfile = { showProfileSheet = true },
                    onOpenTool = { tool -> navController.navigate(AppRoutes.toolHome(tool.id)) },
                    onQuickCreate = { tool ->
                        if (tool.quickCreateUsesPhoto) {
                            navController.navigate(AppRoutes.PHOTO_CAPTURE)
                        } else {
                            navController.navigate(AppRoutes.toolCreate(tool.id))
                        }
                    },
                )
            }
            composable(
                route = AppRoutes.TOOL_HOME,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
            ) { entry ->
                val tool = PlatformTools.byId(entry.arguments?.getString("toolId").orEmpty())
                ListPlaceholderScreen(
                    title = tool?.label ?: "Tool",
                    rows = tool?.placeholderRows.orEmpty(),
                    onItemClick = { label ->
                        navController.navigate(
                            AppRoutes.toolDetail(tool?.id.orEmpty(), label),
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppRoutes.TOOL_CREATE,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
            ) { entry ->
                val tool = PlatformTools.byId(entry.arguments?.getString("toolId").orEmpty())
                DetailPlaceholderScreen(
                    title = "New ${tool?.label ?: "item"}",
                    onBack = { navController.popBackStack() },
                    subtitle = "Quick create is a placeholder in this build.",
                )
            }
            composable(
                route = AppRoutes.TOOL_DETAIL,
                arguments = listOf(
                    navArgument("toolId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { entry ->
                val title = URLDecoder.decode(
                    entry.arguments?.getString("title").orEmpty(),
                    "UTF-8",
                )
                DetailPlaceholderScreen(
                    title = title,
                    onBack = { navController.popBackStack() },
                    subtitle = "Tool detail is a placeholder in this build.",
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

    if (showProfileSheet) {
        ProfileSheet(
            onDismiss = { showProfileSheet = false },
            onLogout = {
                showProfileSheet = false
                DemoProjectRepository.clear()
                navController.navigate(AppRoutes.TODAY_HOME) {
                    popUpTo(AppRoutes.TODAY_HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onPlaceholderAction = { message ->
                showProfileSheet = false
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )
    }
}
