package com.solomondesign.app.ui.projects

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProject
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldNavItemIcon
import com.solomondesign.app.ui.designsystem.LinarcWordmarkOnDark
import com.solomondesign.app.ui.designsystem.fieldNavigationBarItemColors
import com.solomondesign.app.ui.theme.CanvasBlack

private enum class PickerTab(val label: String) {
    PROJECTS("Projects"),
    ACCOUNTS("Accounts"),
}

/**
 * Startup Project List: the one-time picker between Splash and Today. Sits above the
 * Today/Plan/Tools chassis with its own small Projects/Accounts footer nav — see "Startup flow"
 * in Mobile Structure Validated v1.md. This is not the barred "Projects tab": it never coexists
 * with the Today/Plan/Tools bottom bar, only one or the other is on screen at a time.
 */
@Composable
fun ProjectListScreen(
    onSelectProject: (DemoProject) -> Unit,
    modifier: Modifier = Modifier,
    projects: List<DemoProject> = DemoProjectRepository.projects,
) {
    var tab by remember { mutableStateOf(PickerTab.PROJECTS) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("projectListScreen"),
        bottomBar = {
            NavigationBar {
                PickerTab.entries.forEach { entry ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("pickerNavTab_${entry.name}"),
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {
                            FieldNavItemIcon(
                                imageVector = if (entry == PickerTab.PROJECTS) {
                                    Icons.Filled.Folder
                                } else {
                                    Icons.Filled.AccountCircle
                                },
                                selected = tab == entry,
                                contentDescription = entry.label,
                            )
                        },
                        label = { Text(entry.label) },
                        colors = fieldNavigationBarItemColors(),
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ProjectListBrandBar()
            Crossfade(targetState = tab, label = "pickerTab") { current ->
                when (current) {
                    PickerTab.PROJECTS -> ProjectsTabContent(projects = projects, onSelectProject = onSelectProject)
                    PickerTab.ACCOUNTS -> FieldEmptyState(
                        message = "Accounts isn't part of this prototype yet.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsTabContent(
    projects: List<DemoProject>,
    onSelectProject: (DemoProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, projects) {
        if (query.isBlank()) {
            projects
        } else {
            projects.filter {
                it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("projectSearchField"),
            placeholder = { Text("Search projects") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )
        if (filtered.isEmpty()) {
            FieldEmptyState(
                message = "No projects match \"$query\".",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("projectList"),
            ) {
                itemsIndexed(filtered, key = { _, project -> project.id }) { index, project ->
                    ProjectRow(
                        project = project,
                        onClick = { onSelectProject(project) },
                    )
                    if (index != filtered.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ProjectListBrandBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBlack)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinarcWordmarkOnDark(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProjectRow(
    project: DemoProject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("projectRow_${project.id}"),
        headlineContent = {
            Text(text = project.name, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(
                text = project.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
    )
}
