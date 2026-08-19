package com.solomondesign.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solomondesign.app.ui.capture.CaptureSheet
import com.solomondesign.app.ui.capture.PhotoCaptureScreen
import com.solomondesign.app.ui.capture.QuickIssueScreen
import com.solomondesign.app.ui.collab.CollabTopicListScreen
import com.solomondesign.app.ui.collab.CollabTopicScreen
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.collab.NewTopicSheet
import com.solomondesign.app.ui.crew.CrewDetailScreen
import com.solomondesign.app.ui.crew.CrewListScreen
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.common.DetailPlaceholderScreen
import com.solomondesign.app.ui.common.ListPlaceholderScreen
import com.solomondesign.app.ui.images.ImageGridScreen
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ImageSourceSheet
import com.solomondesign.app.ui.images.ImageViewerScreen
import com.solomondesign.app.ui.images.ProjectImage
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.more.OutboxScreen
import com.solomondesign.app.ui.plan.PlanScreen
import com.solomondesign.app.ui.profile.ProfileSheet
import com.solomondesign.app.ui.tasks.FieldTaskDetailScreen
import com.solomondesign.app.ui.tasks.FieldTaskListScreen
import com.solomondesign.app.ui.timecards.NewTimeEntrySheet
import com.solomondesign.app.ui.timecards.TimeCardCrewListScreen
import com.solomondesign.app.ui.timecards.TimeCardDetailScreen
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
 * Field prototype shell: Today / Plan / Tools plus a shared Material FAB.
 *
 * Bottom-bar visibility and the FAB's icon/action are resolved per destination by
 * [resolveChrome] — screens never touch the [androidx.navigation.NavController] themselves.
 * Tab-root page titles live in content ([com.solomondesign.app.ui.designsystem.FieldPageHeader]);
 * pushed screens use a Material top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var activeSheet by remember { mutableStateOf<AppSheet?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val chrome = resolveChrome(currentRoute)
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Null chrome.fab emits nothing, so the Scaffold reserves no space for a FAB.
            chrome.fab?.let { fab ->
                FloatingActionButton(
                    onClick = {
                        when (val action = fab.action) {
                            is FabAction.Navigate -> navController.navigate(action.route)
                            is FabAction.OpenSheet -> activeSheet = action.sheet
                        }
                    },
                    modifier = Modifier.testTag(fab.testTag),
                    shape = FloatingActionButtonDefaults.shape,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(),
                ) {
                    Icon(fab.icon.asImageVector(), contentDescription = fab.contentDescription)
                }
            }
        },
        bottomBar = {
            if (chrome.showBottomBar) {
                NavigationBar(containerColor = colors.surfaceContainer) {
                    bottomNavTabs.forEach { tab ->
                        // Match on the graph, not the destination, so the correct tab stays lit
                        // on nested Pattern B screens.
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == tab.graphRoute } == true
                        NavigationBarItem(
                            modifier = Modifier.testTag("bottomNavTab_${tab.route}"),
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    // Reselect returns this tab to its root. Already at the root,
                                    // popBackStack returns false and does nothing.
                                    navController.popBackStack(tab.route, inclusive = false)
                                } else {
                                    navController.navigate(tab.graphRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            startDestination = AppRoutes.TODAY_GRAPH,
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            // Layout rule: Pattern B destinations live inside their tab's graph so the tab owns
            // their back stack; Pattern A / immersive destinations live at the root.

            navigation(startDestination = AppRoutes.TODAY_HOME, route = AppRoutes.TODAY_GRAPH) {
                composable(AppRoutes.TODAY_HOME) {
                    TodayScreen(
                        onOpenVoiceLog = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                        onOpenProfile = { activeSheet = AppSheet.PROFILE },
                    )
                }
            }

            navigation(startDestination = AppRoutes.PLAN_HOME, route = AppRoutes.PLAN_GRAPH) {
                composable(AppRoutes.PLAN_HOME) {
                    PlanScreen(onOpenProfile = { activeSheet = AppSheet.PROFILE })
                }
            }

            navigation(startDestination = AppRoutes.TOOLS_HOME, route = AppRoutes.TOOLS_GRAPH) {
                composable(AppRoutes.TOOLS_HOME) {
                    ToolsScreen(
                        onOpenOutbox = { navController.navigate(AppRoutes.OUTBOX) },
                        onOpenVoiceLogs = { navController.navigate(AppRoutes.DAILY_LOG_HISTORY) },
                        onOpenProfile = { activeSheet = AppSheet.PROFILE },
                        // Tools with a real screen route there; the rest keep the placeholder.
                        onOpenTool = { tool ->
                            navController.navigate(tool.homeRoute ?: AppRoutes.toolHome(tool.id))
                        },
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
                composable(AppRoutes.OUTBOX) {
                    OutboxScreen(onBack = { navController.popBackStack() })
                }

                // ---- Field Tasks ----
                composable(AppRoutes.FIELD_TASK_LIST) {
                    FieldTaskListScreen(
                        onOpenTask = { id -> navController.navigate(AppRoutes.fieldTaskDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppRoutes.FIELD_TASK_DETAIL,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                ) { entry ->
                    FieldTaskDetailScreen(
                        taskId = decodeArg(entry.arguments?.getString("taskId")),
                        onBack = { navController.popBackStack() },
                        onOpenCrewMember = { id -> navController.navigate(AppRoutes.crewDetail(id)) },
                    )
                }

                // ---- Time Cards ----
                composable(AppRoutes.TIME_CARD_LIST) {
                    TimeCardCrewListScreen(
                        onOpenCrewMember = { id ->
                            navController.navigate(AppRoutes.timeCardDetail(id))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppRoutes.TIME_CARD_DETAIL,
                    arguments = listOf(navArgument("crewMemberId") { type = NavType.StringType }),
                ) { entry ->
                    TimeCardDetailScreen(
                        crewMemberId = decodeArg(entry.arguments?.getString("crewMemberId")),
                        onBack = { navController.popBackStack() },
                    )
                }

                // ---- Crew ----
                composable(AppRoutes.CREW_LIST) {
                    CrewListScreen(
                        onOpenCrewMember = { id -> navController.navigate(AppRoutes.crewDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppRoutes.CREW_DETAIL,
                    arguments = listOf(navArgument("crewMemberId") { type = NavType.StringType }),
                ) { entry ->
                    CrewDetailScreen(
                        crewMemberId = decodeArg(entry.arguments?.getString("crewMemberId")),
                        onBack = { navController.popBackStack() },
                        onOpenTask = { id -> navController.navigate(AppRoutes.fieldTaskDetail(id)) },
                        onOpenTimeCard = { id ->
                            navController.navigate(AppRoutes.timeCardDetail(id))
                        },
                    )
                }

                // ---- Collaboration ----
                composable(AppRoutes.COLLAB_TOPIC_LIST) {
                    CollabTopicListScreen(
                        onOpenTopic = { id -> navController.navigate(AppRoutes.collabTopic(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppRoutes.COLLAB_TOPIC_DETAIL,
                    arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
                ) { entry ->
                    CollabTopicScreen(
                        topicId = decodeArg(entry.arguments?.getString("topicId")),
                        onBack = { navController.popBackStack() },
                    )
                }

                // ---- Images (grid is Pattern B; the viewer is Pattern A, at root) ----
                composable(AppRoutes.IMAGE_GRID) {
                    ImageGridScreen(
                        onOpenImage = { id -> navController.navigate(AppRoutes.imageViewer(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            // Root level: Pattern A and immersive flows. They hide the bottom bar, and
            // daily_log_detail additionally must stay here because it is reachable from both
            // Today and Tools — nesting it would make one tab's saved stack clobber the other's.
            composable(AppRoutes.VOICE_LOG) {
                VoiceLogScreen(onExit = { navController.popBackStack() })
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
                route = AppRoutes.IMAGE_VIEWER,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType }),
            ) { entry ->
                ImageViewerScreen(
                    imageId = decodeArg(entry.arguments?.getString("imageId")),
                    onClose = { navController.popBackStack() },
                    onCreateIssue = { image ->
                        DemoProjectRepository.addIssue(
                            title = "Issue from ${image.title}",
                            location = image.area,
                            note = image.tags.joinToString(" · "),
                        )
                        ProjectImageRepository.linkRecord(image.id, "issue-${image.id}")
                        scope.launch {
                            snackbarHostState.showSnackbar("Issue created from this photo")
                        }
                    },
                    onMarkupUnavailable = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Markup is not part of this build.")
                        }
                    },
                )
            }
        }
    }

    // Pattern C sheets are hoisted here rather than being nav destinations: navigation-compose
    // has no built-in bottom-sheet destination, and adding one needs a new dependency.
    when (activeSheet) {
        AppSheet.CAPTURE -> CaptureSheet(
            onVoice = {
                activeSheet = null
                navController.navigate(AppRoutes.VOICE_LOG)
            },
            onPhoto = {
                activeSheet = null
                navController.navigate(AppRoutes.PHOTO_CAPTURE)
            },
            onIssue = {
                activeSheet = null
                navController.navigate(AppRoutes.QUICK_ISSUE)
            },
            onDismiss = { activeSheet = null },
        )

        AppSheet.PROFILE -> ProfileSheet(
            onDismiss = { activeSheet = null },
            onLogout = {
                activeSheet = null
                DemoSession.reset()
                // Clear the whole graph, not just Today: popping to TODAY_HOME alone would leave
                // the other tabs' saved back stacks alive across a logout.
                navController.navigate(AppRoutes.TODAY_GRAPH) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onPlaceholderAction = { message ->
                activeSheet = null
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )

        // Contextual sheets read their subject from the current nav args. Opening a sheet does
        // not change the back stack, so backStackEntry still points at the screen underneath.
        AppSheet.TIME_ENTRY -> NewTimeEntrySheet(
            defaultCrewMemberId = backStackEntry?.arguments?.getString("crewMemberId")
                ?.let { URLDecoder.decode(it, "UTF-8") },
            onDismiss = { activeSheet = null },
            onSaved = {
                activeSheet = null
                scope.launch { snackbarHostState.showSnackbar("Time entry queued") }
            },
        )

        AppSheet.NEW_TOPIC -> NewTopicSheet(
            onDismiss = { activeSheet = null },
            onCreated = { topicId ->
                activeSheet = null
                navController.navigate(AppRoutes.collabTopic(topicId))
            },
        )

        AppSheet.IMAGE_SOURCE -> ImageSourceSheet(
            onTakePhoto = {
                activeSheet = null
                navController.navigate(AppRoutes.PHOTO_CAPTURE)
            },
            onUseDemoImage = {
                activeSheet = null
                val now = System.currentTimeMillis()
                ProjectImageRepository.add(
                    ProjectImage(
                        id = "img-demo-$now",
                        title = "Demo photo",
                        area = DemoProjectRepository.AREA,
                        tags = listOf(DemoProjectRepository.AREA, "Progress"),
                        capturedAtMillis = now,
                        authorName = CurrentUser.NAME,
                        source = ImageSource.Swatch(seed = now.toInt()),
                    ),
                )
            },
            onDismiss = { activeSheet = null },
        )

        null -> Unit
    }
}

/** Nav args are URL-encoded by the [AppRoutes] builders. */
private fun decodeArg(raw: String?): String =
    URLDecoder.decode(raw.orEmpty(), "UTF-8")
