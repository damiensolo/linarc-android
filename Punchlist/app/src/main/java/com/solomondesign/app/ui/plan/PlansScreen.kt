package com.solomondesign.app.ui.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.FieldCollapsibleSectionHeader
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.profile.ProfileAvatarButton

/**
 * Plans tab root: the full plan set for the project, searchable and grouped by discipline in
 * collapsible sections. Tapping a sheet opens the full-screen [PlanViewerScreen].
 *
 * Plans are the field's source of truth, so this list is one tap from anywhere via the bottom
 * nav — see "Field prototype" in Mobile Structure Validated v1.md.
 */
@Composable
fun PlansScreen(
    onOpenSheet: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onSwitchProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Stored as the enum name so rememberSaveable can bundle it without a custom Saver.
    var disciplineFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    var collapsed by remember { mutableStateOf(emptySet<PlanDiscipline>()) }

    val selectedDiscipline = disciplineFilterName?.let(PlanDiscipline::valueOf)
    // While searching or filtering, sections are forced open so hits are never hidden.
    val filtering = query.isNotBlank() || selectedDiscipline != null
    val filtered = filterPlanSheets(PlanSheetRepository.sheets, query, selectedDiscipline)
    val sections = PlanDiscipline.entries.mapNotNull { discipline ->
        filtered.filter { it.discipline == discipline }
            .takeIf { it.isNotEmpty() }
            ?.let { discipline to it }
    }
    val pinCount = DemoProjectRepository.pins.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("planScreen"),
    ) {
        FieldPageHeader(
            title = "Plans",
            subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${PlanSheetRepository.sheets.size} sheets",
            projectName = DemoProjectRepository.PROJECT_NAME,
            onSwitchProject = onSwitchProject,
            trailing = { ProfileAvatarButton(onClick = onOpenProfile) },
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("plansSearchField"),
            placeholder = { Text("Search sheets") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = CircleShape,
        )
        DisciplineFilterRow(
            selected = selectedDiscipline,
            onSelect = { disciplineFilterName = it?.name },
            modifier = Modifier.padding(top = 8.dp),
        )
        if (sections.isEmpty()) {
            FieldEmptyState(message = "No sheets match \"${query.trim()}\"")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                sections.forEach { (discipline, rows) ->
                    val expanded = filtering || discipline !in collapsed
                    item(key = "header_${discipline.name}") {
                        FieldCollapsibleSectionHeader(
                            title = discipline.label.uppercase(),
                            count = rows.size,
                            expanded = expanded,
                            onToggleExpanded = {
                                if (!filtering) {
                                    collapsed = if (discipline in collapsed) {
                                        collapsed - discipline
                                    } else {
                                        collapsed + discipline
                                    }
                                }
                            },
                            modifier = Modifier.testTag("planSection_${discipline.name}"),
                        )
                    }
                    if (expanded) {
                        items(rows, key = { it.id }) { sheet ->
                            PlanSheetRow(
                                sheet = sheet,
                                pinCount = if (sheet.isPinSheet) pinCount else 0,
                                onClick = { onOpenSheet(sheet.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisciplineFilterRow(
    selected: PlanDiscipline?,
    onSelect: (PlanDiscipline?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            modifier = Modifier.testTag("planFilterChip_all"),
        )
        PlanDiscipline.entries.forEach { discipline ->
            FilterChip(
                selected = selected == discipline,
                onClick = { onSelect(if (selected == discipline) null else discipline) },
                label = { Text(discipline.label) },
                modifier = Modifier.testTag("planFilterChip_${discipline.name}"),
            )
        }
    }
}

@Composable
private fun PlanSheetRow(
    sheet: PlanSheet,
    pinCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("planSheetRow_${sheet.id}"),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { SheetThumbnail(sheet) },
        overlineContent = {
            Text(
                text = sheet.number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(
                text = sheet.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = "${sheet.revision} · ${sheet.updatedLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pinCount > 0) {
                    PinCountBadge(count = pinCount)
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** Live pin count from the demo store, so Voice-to-Log publishes are visible from the list. */
@Composable
private fun PinCountBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(end = 8.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun SheetThumbnail(sheet: PlanSheet, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .width(84.dp)
            .height(60.dp)
            .clip(shape)
            // Drawings are white paper; a fixed white ground keeps thumbnails legible in dark theme.
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Image(
            painter = painterResource(sheet.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
