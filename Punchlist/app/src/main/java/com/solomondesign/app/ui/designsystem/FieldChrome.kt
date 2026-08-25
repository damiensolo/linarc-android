package com.solomondesign.app.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomondesign.app.ui.theme.AvatarPalette
import com.solomondesign.app.ui.theme.OnDark

/**
 * Page header used at the top of Today/Plan/Tools. [trailing] is an optional slot for a
 * fixed-size element (e.g. the profile avatar) anchored to the upper-right; leave it null for
 * headers that don't need one.
 *
 * [projectName] and [onSwitchProject] are optional together: when both are set, the header grows
 * a small tappable project chip above [title] and an overflow menu next to [trailing], so
 * switching projects doesn't require a trip through Profile. Same handler, two shortcuts — see
 * "Startup flow" in Mobile Structure Validated v1.md.
 *
 * [actions] sits immediately before the overflow menu (Tools uses this for the grid/list toggle).
 */
@Composable
fun FieldPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    projectName: String? = null,
    onSwitchProject: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (projectName != null && onSwitchProject != null) {
                ProjectSwitcherChip(
                    projectName = projectName,
                    onClick = onSwitchProject,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        actions?.invoke()
        if (onSwitchProject != null) {
            HeaderOverflowMenu(onSwitchProject = onSwitchProject)
        }
        trailing?.invoke()
    }
}

@Composable
private fun ProjectSwitcherChip(
    projectName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick, onClickLabel = "Switch project", role = Role.Button)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("headerProjectChip"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun HeaderOverflowMenu(
    onSwitchProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("headerOverflowMenu"),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Switch project") },
                onClick = {
                    expanded = false
                    onSwitchProject()
                },
                modifier = Modifier.testTag("headerSwitchProjectMenuItem"),
            )
        }
    }
}

@Composable
fun FieldSectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

/**
 * Section label that can be collapsed, e.g. to tuck a long crew list out of the way. Shows
 * [title] and a live [count]; tapping toggles [expanded] via [onToggleExpanded]. The caller owns
 * the expanded state and is responsible for conditionally emitting the section's content.
 */
@Composable
fun FieldCollapsibleSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "collapsibleSectionChevron",
    )
    val actionLabel = if (expanded) "Collapse" else "Expand"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onToggleExpanded)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevronRotation),
        )
    }
}

/**
 * Color roles for every bottom `NavigationBar` in the app.
 *
 * Material 3's default indicator uses `secondaryContainer` (a muted chip). That is the wrong
 * token: selected tabs must match [AppButton] Primary — saturated [ColorScheme.primary], white
 * icon. We draw that pill in [FieldNavItemIcon] and keep the M3 indicator transparent so it
 * cannot wash the accent out.
 *
 * Capture is an action, not a destination. It always uses the unselected
 * [ColorScheme.onSurfaceVariant] colors. Never tint it primary at rest — that made Capture look
 * selected while the real tab used a weaker blue.
 */
@Composable
fun fieldNavigationBarItemColors(): NavigationBarItemColors {
    val scheme = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.onPrimary,
        selectedTextColor = scheme.primary,
        indicatorColor = Color.Transparent,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
}

private val NavSelectedPillWidth = 64.dp
private val NavSelectedPillHeight = 32.dp

/** Filled primary pill behind the selected nav icon; unselected icons stay on the bar surface. */
@Composable
fun FieldNavItemIcon(
    imageVector: ImageVector,
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(NavSelectedPillWidth)
            .height(NavSelectedPillHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primary else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = OnDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PersonAvatar(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    photoRes: Int? = null,
    size: Dp = 40.dp,
) {
    if (photoRes != null) {
        Image(
            painter = painterResource(photoRes),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        InitialsAvatar(name = name, color = color, modifier = modifier, size = size)
    }
}

/**
 * Presence badge on a persona avatar. Material [BadgedBox] pins to top-end,
 * so this uses a [Box] overlay with [Badge] on the bottom-end edge.
 */
@Composable
private fun BadgedPersonAvatar(
    name: String,
    color: Color,
    presenceColor: Color,
    modifier: Modifier = Modifier,
    photoRes: Int? = null,
) {
    Box(modifier = modifier) {
        PersonAvatar(name = name, color = color, photoRes = photoRes)
        Badge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                .size(12.dp),
            containerColor = presenceColor,
        )
    }
}

/** 10dp status dot centered in the 24dp M3 small leading-icon slot. */
@Composable
private fun StatusIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/**
 * @param leading fully custom leading content (e.g. a photo thumbnail); when set it wins over
 *   [avatarName] and the status dot, and [statusColor] goes unused.
 */
@Composable
fun FieldWorkRow(
    title: String,
    subtitle: String?,
    statusColor: Color,
    modifier: Modifier = Modifier,
    avatarName: String? = null,
    avatarColor: Color = AvatarPalette.colorAt(0),
    avatarPhotoRes: Int? = null,
    enabled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    ListItem(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            when {
                leading != null -> leading()
                avatarName != null -> BadgedPersonAvatar(
                    name = avatarName,
                    color = avatarColor,
                    presenceColor = statusColor,
                    photoRes = avatarPhotoRes,
                )
                else -> StatusIndicator(color = statusColor)
            }
        },
        trailingContent = trailing,
    )
}

/**
 * Uniform empty state for lists that can legitimately have nothing in them. Replaces the
 * hand-rolled muted-Text blocks that were repeated per screen.
 */
@Composable
fun FieldEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = 16.dp)) { action() }
        }
    }
}
