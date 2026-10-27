package com.solomondesign.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold

/**
 * The one screen every unbuilt tool opens — any [PlatformTool] without a [PlatformTool.homeRoute]
 * (RFIs, T & M, Checklist, Drive, Toolbox Talks, Scan) and the quick-create `+` on those cards.
 *
 * It says plainly that the tool is a preview, so a demo never mistakes catalog IA for a broken
 * feature, and it carries the same copy as the amber placeholder frames (70–75) in the Figma app
 * map. Fill the tools in one by one: give a tool a `homeRoute` (and `quickCreateRoute`) and this
 * screen stops appearing for it — nothing else to delete.
 *
 * Chrome follows the route: the tool home is Pattern B ([BrowseScaffold], bottom nav stays);
 * [quickCreate] is Pattern A ([TaskFlowScaffold], Close), matching `AppChrome`.
 */
@Composable
fun ToolPlaceholderScreen(
    tool: PlatformTool?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    quickCreate: Boolean = false,
) {
    val name = tool?.label ?: "This tool"
    val content: @Composable (PaddingValues) -> Unit = { padding ->
        PlaceholderBody(
            tool = tool,
            previewOf = if (quickCreate) "quick create for $name" else name,
            onBackToTools = onBack,
            modifier = Modifier.padding(padding),
        )
    }
    if (quickCreate) {
        TaskFlowScaffold(
            title = "New $name",
            onClose = onBack,
            modifier = modifier,
            content = content,
        )
    } else {
        BrowseScaffold(
            title = name,
            onBack = onBack,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
private fun PlaceholderBody(
    tool: PlatformTool?,
    previewOf: String,
    onBackToTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp)
            .testTag("toolPlaceholder_${tool?.id ?: "unknown"}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (tool != null) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null, // The headline right below carries the meaning.
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = PLACEHOLDER_HEADLINE,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = placeholderBody(previewOf),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        AppButton(
            text = "Back to Tools",
            onClick = onBackToTools,
            type = AppButtonType.Secondary,
            modifier = Modifier
                .padding(top = 12.dp)
                .testTag("toolPlaceholderBackToTools"),
        )
    }
}

/** Shared with the Figma placeholder frames — keep the two in sync. */
const val PLACEHOLDER_HEADLINE = "This tool isn’t interactive in this prototype"

fun placeholderBody(previewOf: String): String =
    "You’ve reached a preview of $previewOf. This area isn’t interactive, " +
        "but you can return to explore other parts of the prototype."
