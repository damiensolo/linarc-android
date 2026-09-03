package com.solomondesign.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.PersonAvatar
import com.solomondesign.app.ui.theme.AvatarPalette

/**
 * Avatar shown at the trailing edge of [com.solomondesign.app.ui.designsystem.FieldPageHeader]
 * on Today/Plan/Tools. Tapping it opens [ProfileSheet].
 */
@Composable
fun ProfileAvatarButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open profile" }
            .testTag("profileAvatarButton"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(
            name = CurrentUser.NAME,
            color = AvatarPalette.colorAt(0),
            photoRes = CurrentUser.photoRes,
            size = 36.dp,
        )
    }
}

/**
 * Account sheet reached by tapping the header avatar. There's no backend/auth in this
 * prototype (per `Mobile Structure Validated v1.md`), so every action except Switch project and
 * Logout surfaces a message instead of faking a working flow — the same explain-don't-fake
 * treatment as unbuilt account actions. Switch project returns to the startup Project
 * List; Logout resets the local demo session via
 * [com.solomondesign.app.ui.demo.DemoProjectRepository.clear].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    onDismiss: () -> Unit,
    onSwitchProject: () -> Unit,
    onLogout: () -> Unit,
    onPlaceholderAction: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag("profileSheet"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(
                name = CurrentUser.NAME,
                color = AvatarPalette.colorAt(0),
                photoRes = CurrentUser.photoRes,
                size = 56.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = CurrentUser.NAME, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = CurrentUser.JOB_TITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        ProfileActionRow(
            label = "Switch project",
            icon = Icons.Filled.SwapHoriz,
            modifier = Modifier.testTag("profileSwitchProject"),
            onClick = onSwitchProject,
        )
        HorizontalDivider()
        ProfileActionRow(
            label = "Edit profile",
            icon = Icons.Filled.Edit,
            modifier = Modifier.testTag("profileEditProfile"),
            onClick = { onPlaceholderAction("Edit profile isn't part of this prototype yet") },
        )
        ProfileActionRow(
            label = "Help & Support",
            icon = Icons.Filled.HelpOutline,
            modifier = Modifier.testTag("profileHelpSupport"),
            onClick = { onPlaceholderAction("Help & Support isn't part of this prototype yet") },
        )
        ProfileActionRow(
            label = "Driving & Operating licenses",
            icon = Icons.Filled.Badge,
            modifier = Modifier.testTag("profileLicenses"),
            onClick = { onPlaceholderAction("Licenses aren't part of this prototype yet") },
        )
        ProfileActionRow(
            label = "Reset password",
            icon = Icons.Filled.Lock,
            modifier = Modifier.testTag("profileResetPassword"),
            onClick = { onPlaceholderAction("There's no real login in this prototype yet") },
        )
        HorizontalDivider()
        ProfileActionRow(
            label = "Logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            modifier = Modifier.testTag("profileLogout"),
            tint = MaterialTheme.colorScheme.error,
            onClick = onLogout,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label, color = tint) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick),
    )
}
