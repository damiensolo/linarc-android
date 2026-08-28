package com.solomondesign.app.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.splash.SplashVariant
import com.solomondesign.app.ui.today.OwnerTodayVariant
import kotlinx.coroutines.launch

/**
 * Pattern B — app settings, reached from the Tools header's overflow menu. Houses what used to
 * sit at the bottom of the Tools catalog: Appearance (theme) and the strategy-demo controls
 * (view as, splash animation, and the Voice daily log demo — the original Voice-to-Log flow,
 * moved off the camera's quick chip when Voice note took its place). Product activity (Outbox,
 * Voice logs) stayed on Tools as the Activity Center — settings configure the app, they aren't
 * work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPreviewSplash: () -> Unit,
    /** Launches the original Voice-to-Log daily-log flow, kept here as a scripted demo. */
    onOpenVoiceLogDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val darkTheme = DemoProjectRepository.darkTheme
    var showViewAs by remember { mutableStateOf(false) }
    var showSplashPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val raised = MaterialTheme.colorScheme.surfaceContainer
    val progress = MaterialTheme.colorScheme.tertiary

    BrowseScaffold(
        title = "Settings",
        subtitle = DemoProjectRepository.PROJECT_NAME,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("settingsScreen"),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item { FieldSectionLabel("Appearance") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("themeToggleRow"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark theme", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (darkTheme) "On · dark chrome" else "Off · light chrome",
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                            )
                        }
                        val colors = MaterialTheme.colorScheme
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { DemoProjectRepository.darkTheme = it },
                            modifier = Modifier.testTag("themeToggle"),
                            thumbContent = {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary,
                                checkedBorderColor = colors.primary,
                                checkedIconColor = colors.primary,
                                uncheckedThumbColor = colors.outline,
                                uncheckedTrackColor = colors.surfaceContainerHighest,
                                uncheckedBorderColor = colors.outline,
                                uncheckedIconColor = colors.surfaceContainerHighest,
                            ),
                        )
                    }
                }
                item { FieldSectionLabel("Demo") }
                item {
                    val liveCount = FieldPersona.entries.count { it.isLive }
                    FieldWorkRow(
                        title = "Demo: view as",
                        subtitle = "${persona.displayName} · $liveCount personas live",
                        statusColor = progress,
                        enabled = true,
                        onClick = { showViewAs = true },
                        modifier = Modifier.testTag("demoViewAsRow"),
                    )
                }
                item {
                    FieldWorkRow(
                        title = "Splash animation",
                        subtitle = DemoProjectRepository.splashVariant.title,
                        statusColor = progress,
                        enabled = true,
                        onClick = { showSplashPicker = true },
                        modifier = Modifier.testTag("demoSplashRow"),
                    )
                }
                item {
                    // The original Voice-to-Log flow, moved off the camera's quick chip
                    // (2026-08-25) in favor of the bilingual Voice note. Kept runnable for
                    // scripted demos: submitted logs still land on Today, Plans, and the
                    // Voice logs history on Tools.
                    FieldWorkRow(
                        title = "Voice daily log (demo)",
                        subtitle = "Dictate a day summary → parsed log cards",
                        statusColor = progress,
                        enabled = true,
                        onClick = onOpenVoiceLogDemo,
                        modifier = Modifier.testTag("demoVoiceLogRow"),
                    )
                }
            }
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (showViewAs) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showViewAs = false },
            sheetState = sheetState,
            containerColor = raised,
        ) {
            Text(
                text = "Demo: view as",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "Same tabs for every persona; only Today focus, tool order, and Plan " +
                    "emphasis change. Live: " +
                    FieldPersona.entries.filter { it.isLive }
                        .joinToString(", ") { it.displayName } + ".",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            FieldPersona.entries.forEach { option ->
                if (option == FieldPersona.OWNER) {
                    // Both Owner Today layouts are demoable straight from this picker — one
                    // row per layout, so the v1/v2 A/B is a single tap with no separate
                    // setting. Tapping sets the layout and switches to the Owner view.
                    OwnerTodayVariant.entries.forEach { variant ->
                        val viewingNow = option == persona &&
                            variant == DemoProjectRepository.ownerTodayVariant
                        FieldWorkRow(
                            title = "${option.displayName} — ${variant.title}",
                            subtitle = if (viewingNow) "Live · viewing now" else "Live · tap to view",
                            statusColor = progress,
                            enabled = true,
                            onClick = {
                                DemoProjectRepository.ownerTodayVariant = variant
                                DemoProjectRepository.selectPersona(option)
                                showViewAs = false
                            },
                            modifier = Modifier.testTag("persona_OWNER_${variant.name}"),
                        )
                    }
                } else {
                    FieldWorkRow(
                        title = option.displayName,
                        subtitle = when {
                            option == persona -> "Live · viewing now"
                            option.isLive -> "Live · tap to view"
                            else -> "Next — same tabs, different Today"
                        },
                        statusColor = if (option.isLive) progress else muted,
                        enabled = true,
                        onClick = {
                            if (option.isLive) {
                                DemoProjectRepository.selectPersona(option)
                                showViewAs = false
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "${option.displayName} view is next — ${option.nextFocus}",
                                    )
                                }
                                showViewAs = false
                            }
                        },
                        modifier = Modifier.testTag("persona_${option.name}"),
                    )
                }
            }
            Column(modifier = Modifier.padding(24.dp)) { }
        }
    }

    if (showSplashPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSplashPicker = false },
            sheetState = sheetState,
            containerColor = raised,
        ) {
            Text(
                text = "Splash animation",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "Plays on launch. Tap a version to preview it now.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            SplashVariant.entries.forEach { option ->
                val selected = option == DemoProjectRepository.splashVariant
                FieldWorkRow(
                    title = option.title,
                    subtitle = option.subtitle,
                    statusColor = if (selected) progress else muted,
                    enabled = true,
                    onClick = {
                        DemoProjectRepository.splashVariant = option
                        showSplashPicker = false
                        onPreviewSplash()
                    },
                    modifier = Modifier.testTag("splashVariant_${option.name}"),
                )
            }
            Column(modifier = Modifier.padding(24.dp)) { }
        }
    }
}
