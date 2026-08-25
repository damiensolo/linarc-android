package com.solomondesign.app.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.DesignTokens
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.profile.ProfileAvatarButton

private const val GridColumnCount = 3

@Composable
fun ToolsScreen(
    onOpenOutbox: () -> Unit,
    onOpenVoiceLogs: () -> Unit,
    onOpenProfile: () -> Unit,
    onSwitchProject: () -> Unit,
    onOpenTool: (PlatformTool) -> Unit,
    onQuickCreate: (PlatformTool) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val queuedOutbox = DemoProjectRepository.queuedOutboxCount
    var isGrid by rememberSaveable { mutableStateOf(true) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val progress = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("toolsScreen"),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            FieldPageHeader(
                title = "Tools",
                subtitle = "${persona.displayName} · ${DemoProjectRepository.PROJECT_NAME}",
                projectName = DemoProjectRepository.PROJECT_NAME,
                onSwitchProject = onSwitchProject,
                // Appearance and demo controls live behind here now — see SettingsScreen.
                onOpenSettings = onOpenSettings,
                actions = {
                    ToolsViewToggle(
                        isGrid = isGrid,
                        onChange = { isGrid = it },
                    )
                },
                trailing = { ProfileAvatarButton(onClick = onOpenProfile) },
            )
        }
            if (isGrid) {
                items(
                    PlatformTools.catalog.chunked(GridColumnCount),
                    key = { row -> row.joinToString { it.id } },
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { tool ->
                            ToolGridCard(
                                tool = tool,
                                onOpen = { onOpenTool(tool) },
                                onQuickCreate = { onQuickCreate(tool) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(GridColumnCount - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(PlatformTools.catalog, key = { it.id }) { tool ->
                    ToolListRow(
                        tool = tool,
                        onOpen = { onOpenTool(tool) },
                        onQuickCreate = { onQuickCreate(tool) },
                    )
                }
            }
            // Product activity lives on Tools; app configuration (Appearance, demo controls)
            // moved behind the header overflow's Settings item — see SettingsScreen.
            item { FieldSectionLabel("Activity Center") }
            item {
                FieldWorkRow(
                    title = "Outbox",
                    subtitle = if (queuedOutbox > 0) {
                        "$queuedOutbox queued · waiting for signal"
                    } else {
                        "All caught up"
                    },
                    statusColor = muted,
                    enabled = true,
                    onClick = onOpenOutbox,
                )
            }
            item {
                FieldWorkRow(
                    title = "Voice logs",
                    subtitle = "Recorded daily logs and playback",
                    statusColor = progress,
                    enabled = true,
                    onClick = onOpenVoiceLogs,
                )
            }
    }
}

@Composable
private fun ToolsViewToggle(
    isGrid: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = { onChange(!isGrid) },
        modifier = modifier
            .testTag("toolsViewToggle")
            .semantics {
                contentDescription = if (isGrid) "Show as list" else "Show as grid"
            },
    ) {
        Icon(
            imageVector = if (isGrid) {
                Icons.AutoMirrored.Filled.ViewList
            } else {
                Icons.Filled.GridView
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun ToolGridCard(
    tool: PlatformTool,
    onOpen: () -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = modifier
            .aspectRatio(0.92f)
            .testTag("toolCard_${tool.id}"),
        shape = RoundedCornerShape(DesignTokens.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onOpen,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (tool.canQuickCreate) {
                QuickCreateButton(
                    tool = tool,
                    onClick = onQuickCreate,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun ToolListRow(
    tool: PlatformTool,
    onOpen: () -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        modifier = modifier
            .clickable(onClick = onOpen)
            .testTag("toolCard_${tool.id}"),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = tool.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = muted,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tool.canQuickCreate) {
                    QuickCreateButton(tool = tool, onClick = onQuickCreate)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = muted,
                )
            }
        },
    )
}

@Composable
private fun QuickCreateButton(
    tool: PlatformTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = if (tool.quickCreateUsesPhoto) {
        "Take photo for ${tool.label}"
    } else {
        "Create ${tool.label}"
    }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .testTag("toolQuickCreate_${tool.id}")
            .semantics { contentDescription = actionLabel },
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
