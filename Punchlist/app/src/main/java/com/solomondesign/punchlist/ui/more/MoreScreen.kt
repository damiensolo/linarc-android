package com.solomondesign.punchlist.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.punchlist.ui.demo.DemoProjectRepository
import com.solomondesign.punchlist.ui.persona.FieldPersona
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenOutbox: () -> Unit,
    onOpenVoiceLogs: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val outboxCount = DemoProjectRepository.outboxItems.size
    var showViewAs by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag("moreScreen"),
        ) {
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )

            ListItem(
                headlineContent = { Text("Demo: view as") },
                supportingContent = { Text("${persona.displayName} · Foreman is live") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier
                    .clickable { showViewAs = true }
                    .testTag("demoViewAsRow"),
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Outbox") },
                supportingContent = { Text("$outboxCount items queued") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onOpenOutbox),
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Voice logs") },
                supportingContent = { Text("Recorded daily logs and playback") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onOpenVoiceLogs),
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Design system") },
                supportingContent = { Text("Button gallery") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onOpenDesignSystem),
            )
            HorizontalDivider()
        }
        SnackbarHost(hostState = snackbarHostState)
    }

    if (showViewAs) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showViewAs = false },
            sheetState = sheetState,
        ) {
            Text(
                text = "Demo: view as",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "Same tabs for every persona. Only Foreman is live in this build.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            FieldPersona.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(option.displayName) },
                    supportingContent = {
                        Text(
                            if (option.isLive) {
                                "Live"
                            } else {
                                "Next — same tabs, different Today"
                            },
                        )
                    },
                    trailingContent = {
                        if (option == persona) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected")
                        }
                    },
                    modifier = Modifier
                        .testTag("persona_${option.name}")
                        .clickable {
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
                )
                HorizontalDivider()
            }
            Column(modifier = Modifier.padding(24.dp)) { }
        }
    }
}
