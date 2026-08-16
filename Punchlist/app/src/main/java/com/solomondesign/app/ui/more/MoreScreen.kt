package com.solomondesign.app.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.theme.StatusProgress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenOutbox: () -> Unit,
    onOpenVoiceLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val outboxCount = DemoProjectRepository.outboxItems.size
    val darkTheme = DemoProjectRepository.darkTheme
    var showViewAs by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val raised = MaterialTheme.colorScheme.surfaceContainer

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag("moreScreen"),
        ) {
            FieldPageHeader(title = "More", subtitle = "${persona.displayName} · demo controls")

            FieldWorkRow(
                title = "Demo: view as",
                subtitle = "${persona.displayName} · Foreman is live",
                statusColor = StatusProgress,
                enabled = true,
                onClick = { showViewAs = true },
                modifier = Modifier.testTag("demoViewAsRow"),
            )
            FieldWorkRow(
                title = "Outbox",
                subtitle = "$outboxCount items queued",
                statusColor = muted,
                enabled = true,
                onClick = onOpenOutbox,
            )
            FieldWorkRow(
                title = "Voice logs",
                subtitle = "Recorded daily logs and playback",
                statusColor = StatusProgress,
                enabled = true,
                onClick = onOpenVoiceLogs,
            )

            FieldSectionLabel("Appearance")
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
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { DemoProjectRepository.darkTheme = it },
                    modifier = Modifier.testTag("themeToggle"),
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState)
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
                text = "Same tabs for every persona. Only Foreman is live in this build.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            FieldPersona.entries.forEach { option ->
                FieldWorkRow(
                    title = option.displayName,
                    subtitle = if (option.isLive) "Live" else "Next — same tabs, different Today",
                    statusColor = if (option.isLive) StatusProgress else muted,
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
            Column(modifier = Modifier.padding(24.dp)) { }
        }
    }
}
