package com.solomondesign.app.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solomondesign.app.ui.capture.camera.CameraCaptureScreen
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
import com.solomondesign.app.ui.designsystem.FieldNavItemIcon
import com.solomondesign.app.ui.designsystem.fieldNavigationBarItemColors
import com.solomondesign.app.ui.images.ImageGridScreen
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ImageSourceSheet
import com.solomondesign.app.ui.images.ImageViewerScreen
import com.solomondesign.app.ui.images.ProjectImage
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.markup.ImageMarkupScreen
import com.solomondesign.app.ui.more.OutboxScreen
import com.solomondesign.app.ui.plan.PlanViewerScreen
import com.solomondesign.app.ui.plan.PlansScreen
import com.solomondesign.app.ui.profile.ProfileSheet
import com.solomondesign.app.ui.projects.ProjectListScreen
import com.solomondesign.app.ui.records.CameraAttachmentInbox
import com.solomondesign.app.ui.records.RECORD_LOCATIONS
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordCreateScreen
import com.solomondesign.app.ui.records.RecordDetailScreen
import com.solomondesign.app.ui.records.RecordDraft
import com.solomondesign.app.ui.records.RecordListScreen
import com.solomondesign.app.ui.splash.LinarcSplashScreen
import com.solomondesign.app.ui.splash.SplashVariant
import com.solomondesign.app.ui.tasks.FieldTaskDetailScreen
import com.solomondesign.app.ui.tasks.FieldTaskListScreen
import com.solomondesign.app.ui.timecards.NewTimeEntrySheet
import com.solomondesign.app.ui.timecards.TimeCardCrewListScreen
import com.solomondesign.app.ui.timecards.TimeCardDetailScreen
import com.solomondesign.app.ui.today.TodayScreen
import com.solomondesign.app.ui.tools.PlatformTools
import com.solomondesign.app.ui.tools.SettingsScreen
import com.solomondesign.app.ui.tools.ToolsScreen
import com.solomondesign.app.ui.voicelog.DailyLogHomeScreen
import com.solomondesign.app.ui.video.VideoPlaybackScreen
import com.solomondesign.app.ui.voicelog.DailyLogPlaybackScreen
import com.solomondesign.app.ui.voicelog.VoiceLogScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private fun dailyLogDetailRoute(recordId: String) =
    "daily_log_detail/${URLEncoder.encode(recordId, "UTF-8")}"

/** True for the three bottom-nav tab roots — switching between them is a sideways move, not a
 * push, so it gets a crossfade instead of a directional slide. */
private fun isTabRootRoute(route: String?): Boolean = bottomNavTabs.any { it.route == route }

/** Pattern A / immersive destinations already hide the bottom bar (see [resolveChrome]); reusing
 * that signal keeps motion rules and chrome rules from drifting apart. */
private fun isImmersiveRoute(route: String?): Boolean = !resolveChrome(route).showBottomBar

/**
 * Field prototype shell: Today / Plans / Tools tabs plus the [CaptureNavAction] button between
 * Today and Plans (an action in the bar, not a destination — it opens the full-screen camera and
 * never shows a selected state). The FAB slot is contextual-only (time entry, new topic, add
 * image), resolved per destination by [resolveChrome] — screens never touch the
 * [androidx.navigation.NavController] themselves. Tab-root page titles live in content
 * ([com.solomondesign.app.ui.designsystem.FieldPageHeader]); pushed screens use a Material top
 * app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(playLaunchSplash: Boolean = false, showProjectPicker: Boolean = false) {
    val navController = rememberNavController()
    var activeSheet by remember { mutableStateOf<AppSheet?>(null) }
    // Startup gate: Splash -> Project List -> chassis. Defaults to already-selected so every
    // existing test/preview that calls AppNavHost() with no args still lands on Today directly;
    // only MainActivity opts into the picker for a real cold launch.
    var projectSelected by remember { mutableStateOf(!showProjectPicker) }
    var splashVisible by remember { mutableStateOf(playLaunchSplash) }
    var splashPlaybackId by remember { mutableIntStateOf(0) }
    var homeReveal by remember { mutableFloatStateOf(0f) }
    val splashVariant = DemoProjectRepository.splashVariant
    val blurHome = splashVisible && splashVariant.revealsHome
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val chrome = resolveChrome(currentRoute)
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun previewSplash() {
        homeReveal = 0f
        splashPlaybackId += 1
        splashVisible = true
    }

    fun switchProject() {
        activeSheet = null
        // Same graph reset as logout, minus the data wipe: every project shares the same seeded
        // demo data in this build, so switching just re-shows the picker and lets the next
        // selection land on a clean Today rather than wherever the stack was.
        navController.navigate(AppRoutes.TODAY_GRAPH) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
        projectSelected = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (blurHome && homeReveal < 0.995f) {
                    // Radius is in dp so the frost reads on dense screens.
                    // graphicsLayer(RenderEffect) alone often never promotes a layer.
                    Modifier.blur(
                        radius = ((1f - homeReveal) * 24f).dp,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
    Crossfade(targetState = projectSelected, label = "projectGate") { selected ->
    if (selected) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    bottomNavTabs.forEachIndexed { index, tab ->
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
                            icon = { FieldNavItemIcon(tab.icon, selected, tab.label) },
                            label = { Text(tab.label) },
                            colors = fieldNavigationBarItemColors(),
                        )
                        // Capture sits to the right of Today: an action in the bar, not a
                        // destination — never selected, no back stack of its own. Same unselected
                        // colors as Today/Plan/Tools; primary is reserved for the selected tab.
                        if (index == 0) {
                            NavigationBarItem(
                                modifier = Modifier.testTag(CaptureNavAction.testTag),
                                selected = false,
                                onClick = {
                                    navController.navigate(CaptureNavAction.route) {
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    FieldNavItemIcon(
                                        CaptureNavAction.icon.asImageVector(),
                                        selected = false,
                                        contentDescription = CaptureNavAction.contentDescription,
                                    )
                                },
                                label = { Text(CaptureNavAction.label) },
                                colors = fieldNavigationBarItemColors(),
                            )
                        }
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.TODAY_GRAPH,
            modifier = Modifier.padding(scaffoldPadding),
            // Tab-to-tab: crossfade only, sliding would imply a hierarchy that isn't there.
            // Into/out of an immersive (Pattern A) destination: slide vertically, like a task
            // taking over the screen. Everything else (Pattern B push/pop): the standard Android
            // parallax slide — new content enters fully from the side, old content only partly
            // exits, and reverses symmetrically on the way back.
            enterTransition = {
                val target = targetState.destination.route
                when {
                    isTabRootRoute(target) && isTabRootRoute(initialState.destination.route) ->
                        fadeIn(tween(220))
                    isImmersiveRoute(target) -> slideInVertically { it } + fadeIn()
                    else -> slideInHorizontally { it } + fadeIn()
                }
            },
            exitTransition = {
                val initial = initialState.destination.route
                when {
                    isTabRootRoute(initial) && isTabRootRoute(targetState.destination.route) ->
                        fadeOut(tween(220))
                    isImmersiveRoute(targetState.destination.route) -> fadeOut()
                    else -> slideOutHorizontally { -it / 3 } + fadeOut()
                }
            },
            popEnterTransition = {
                val initial = initialState.destination.route
                if (isImmersiveRoute(initial)) fadeIn() else slideInHorizontally { -it / 3 } + fadeIn()
            },
            popExitTransition = {
                val initial = initialState.destination.route
                if (isImmersiveRoute(initial)) {
                    slideOutVertically { it } + fadeOut()
                } else {
                    slideOutHorizontally { it } + fadeOut()
                }
            },
        ) {
            // Layout rule: Pattern B destinations live inside their tab's graph so the tab owns
            // their back stack; Pattern A / immersive destinations live at the root.

            navigation(startDestination = AppRoutes.TODAY_HOME, route = AppRoutes.TODAY_GRAPH) {
                composable(AppRoutes.TODAY_HOME) {
                    TodayScreen(
                        onOpenVoiceLog = { recordId -> navController.navigate(dailyLogDetailRoute(recordId)) },
                        // Recent-capture photo rows open the same full-screen viewer the Images
                        // tool uses; it lives at the nav-graph root, so it's reachable from here.
                        onOpenImage = { imageId ->
                            navController.navigate(AppRoutes.imageViewer(imageId))
                        },
                        onOpenVideo = { videoId ->
                            navController.navigate(AppRoutes.videoPlayback(videoId))
                        },
                        // Blockers and record-backed rows open the record detail owned by its
                        // tool (Issues / Incidents / Punch list) — same destination the lists use.
                        onOpenRecord = { recordId ->
                            navController.navigate(AppRoutes.recordDetail(recordId))
                        },
                        onOpenProfile = { activeSheet = AppSheet.PROFILE },
                        onSwitchProject = { switchProject() },
                        onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                    )
                }
            }

            navigation(startDestination = AppRoutes.PLAN_HOME, route = AppRoutes.PLAN_GRAPH) {
                composable(AppRoutes.PLAN_HOME) {
                    PlansScreen(
                        onOpenSheet = { sheetId ->
                            navController.navigate(AppRoutes.planViewer(sheetId))
                        },
                        onOpenProfile = { activeSheet = AppSheet.PROFILE },
                        onSwitchProject = { switchProject() },
                        onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                    )
                }
            }

            navigation(startDestination = AppRoutes.TOOLS_HOME, route = AppRoutes.TOOLS_GRAPH) {
                composable(AppRoutes.TOOLS_HOME) {
                    ToolsScreen(
                        onOpenOutbox = { navController.navigate(AppRoutes.OUTBOX) },
                        onOpenVoiceLogs = { navController.navigate(AppRoutes.DAILY_LOG_HISTORY) },
                        onOpenProfile = { activeSheet = AppSheet.PROFILE },
                        onSwitchProject = { switchProject() },
                        onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                        // Tools with a real screen route there; the rest keep the placeholder.
                        onOpenTool = { tool ->
                            navController.navigate(tool.homeRoute ?: AppRoutes.toolHome(tool.id))
                        },
                        onQuickCreate = { tool ->
                            when {
                                tool.quickCreateUsesPhoto -> navController.navigate(AppRoutes.CAMERA)
                                tool.quickCreateRoute != null ->
                                    navController.navigate(tool.quickCreateRoute)
                                else -> navController.navigate(AppRoutes.toolCreate(tool.id))
                            }
                        },
                    )
                }
                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onPreviewSplash = { previewSplash() },
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

                // ---- Record tools: Issues / Incidents / Punch list (Pattern B lists) ----
                composable(AppRoutes.RECORD_LIST_ISSUES) {
                    RecordListScreen(
                        category = RecordCategory.ISSUE,
                        onOpenRecord = { id -> navController.navigate(AppRoutes.recordDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppRoutes.RECORD_LIST_INCIDENTS) {
                    RecordListScreen(
                        category = RecordCategory.INCIDENT,
                        onOpenRecord = { id -> navController.navigate(AppRoutes.recordDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppRoutes.RECORD_LIST_PUNCH) {
                    RecordListScreen(
                        category = RecordCategory.PUNCH,
                        onOpenRecord = { id -> navController.navigate(AppRoutes.recordDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppRoutes.RECORD_DETAIL,
                    arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
                ) { entry ->
                    RecordDetailScreen(
                        recordId = decodeArg(entry.arguments?.getString("recordId")),
                        onBack = { navController.popBackStack() },
                        onOpenImage = { imageId ->
                            navController.navigate(AppRoutes.imageViewer(imageId))
                        },
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
            composable(
                route = AppRoutes.PLAN_VIEWER,
                arguments = listOf(navArgument("sheetId") { type = NavType.StringType }),
            ) { entry ->
                PlanViewerScreen(
                    sheetId = decodeArg(entry.arguments?.getString("sheetId")),
                    onClose = { navController.popBackStack() },
                    // A capture pin's photo opens in the same full-screen viewer as everywhere
                    // else; it stacks on the plan viewer so Back returns to the sheet.
                    onOpenImage = { imageId ->
                        navController.navigate(AppRoutes.imageViewer(imageId))
                    },
                )
            }
            composable(AppRoutes.CAMERA) {
                CameraCaptureScreen(
                    onClose = { navController.popBackStack() },
                    onPhotoSaved = { photoId ->
                        // A record form beneath may have asked for this capture; the deposit
                        // no-ops unless the form armed the inbox before opening the camera.
                        CameraAttachmentInbox.deposit(photoId)
                        navController.popBackStack()
                        scope.launch {
                            snackbarHostState.showSnackbar("Photo saved — on Today, Plans, and Images")
                        }
                    },
                    onVideoSaved = {
                        navController.popBackStack()
                        scope.launch {
                            snackbarHostState.showSnackbar("Video saved — on Today and Plans")
                        }
                    },
                    // The quick chips swap flows rather than stack them: replacing the camera in
                    // the back stack means Back from voice/issue lands where capture began, not
                    // on a stale viewfinder.
                    onVoiceLog = {
                        navController.navigate(AppRoutes.VOICE_LOG) {
                            popUpTo(AppRoutes.CAMERA) { inclusive = true }
                        }
                    },
                    onQuickIssue = {
                        navController.navigate(AppRoutes.recordCreate(RecordCategory.ISSUE.routeId)) {
                            popUpTo(AppRoutes.CAMERA) { inclusive = true }
                        }
                    },
                    onCreateRecord = { category ->
                        navController.navigate(AppRoutes.recordCreate(category.routeId)) {
                            popUpTo(AppRoutes.CAMERA) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = AppRoutes.RECORD_CREATE,
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                val category = RecordCategory.fromRouteId(entry.arguments?.getString("category"))
                    ?: RecordCategory.ISSUE
                RecordCreateScreen(
                    category = category,
                    onClose = { navController.popBackStack() },
                    onSaved = { saved ->
                        navController.popBackStack()
                        scope.launch {
                            // "Queued in Outbox" is the offline-first story: saved on this
                            // device now, synced when signal returns (see OutboxScreen).
                            snackbarHostState.showSnackbar(
                                when (saved) {
                                    RecordCategory.ISSUE ->
                                        "Issue saved — on Today, Plans, and Issues · queued in Outbox"
                                    RecordCategory.INCIDENT ->
                                        "Incident saved — on Today, Plans, and Incidents · queued in Outbox"
                                    RecordCategory.PUNCH ->
                                        "Punch item saved — on Plans and the Punch list · queued in Outbox"
                                },
                            )
                        }
                    },
                    // Attach-from-camera stacks the regular camera on the form; the form's
                    // draft is a singleton, so its fields survive the round trip.
                    onAttachCamera = { navController.navigate(AppRoutes.CAMERA) },
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
                route = AppRoutes.IMAGE_VIEWER,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType }),
            ) { entry ->
                ImageViewerScreen(
                    imageId = decodeArg(entry.arguments?.getString("imageId")),
                    onClose = { navController.popBackStack() },
                    // The chooser picked a category; stage the form with this photo attached
                    // and its metadata seeded, then stack the form so Back returns here.
                    onCreateRecord = { image, category ->
                        RecordDraft.begin(
                            category,
                            System.currentTimeMillis(),
                            seedTitle = image.title,
                            seedLocation = RECORD_LOCATIONS.firstOrNull { image.area.contains(it) },
                            seedPhotoImageIds = listOf(image.id),
                        )
                        navController.navigate(AppRoutes.recordCreate(category.routeId))
                    },
                    // Markup edits pixels, so it needs a real capture file; the seeded demo
                    // tiles are procedurally drawn and get an explanation instead.
                    onMarkup = { image ->
                        if (image.source is ImageSource.CapturedFile) {
                            navController.navigate(AppRoutes.imageMarkup(image.id))
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Demo tiles can't be marked up — capture a photo to try it.",
                                )
                            }
                        }
                    },
                )
            }
            composable(
                route = AppRoutes.IMAGE_MARKUP,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType }),
            ) { entry ->
                ImageMarkupScreen(
                    imageId = decodeArg(entry.arguments?.getString("imageId")),
                    onClose = { navController.popBackStack() },
                    onSaved = { copyImageId ->
                        navController.popBackStack()
                        if (copyImageId != null) {
                            // Show the result: the copy's viewer stacks on the original's, so
                            // Back walks copy → original.
                            navController.navigate(AppRoutes.imageViewer(copyImageId))
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Marked-up copy saved — the original is unchanged",
                                )
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Markup saved") }
                        }
                    },
                )
            }
            composable(
                route = AppRoutes.VIDEO_PLAYBACK,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
            ) { entry ->
                VideoPlaybackScreen(
                    videoId = decodeArg(entry.arguments?.getString("videoId")),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
    } else {
        ProjectListScreen(onSelectProject = { projectSelected = true })
    }
    }
    }

    // Pattern C sheets are hoisted here rather than being nav destinations: navigation-compose
    // has no built-in bottom-sheet destination, and adding one needs a new dependency.
    when (activeSheet) {
        AppSheet.PROFILE -> ProfileSheet(
            onDismiss = { activeSheet = null },
            onSwitchProject = { switchProject() },
            onLogout = {
                activeSheet = null
                DemoSession.reset()
                // Clear the whole graph, not just Today: popping to TODAY_HOME alone would leave
                // the other tabs' saved back stacks alive across a logout.
                navController.navigate(AppRoutes.TODAY_GRAPH) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                previewSplash()
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
                navController.navigate(AppRoutes.CAMERA)
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

        if (blurHome && splashVariant != SplashVariant.DEPTH && homeReveal < 0.99f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - homeReveal) * 0.5f)),
            )
        }

        if (splashVisible) {
            LinarcSplashScreen(
                variant = splashVariant,
                playbackId = splashPlaybackId,
                onRevealProgress = { homeReveal = it },
                onFinished = {
                    splashVisible = false
                    homeReveal = 1f
                },
            )
        }
    }
}

/** Nav args are URL-encoded by the [AppRoutes] builders. */
private fun decodeArg(raw: String?): String =
    URLDecoder.decode(raw.orEmpty(), "UTF-8")
