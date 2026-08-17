package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomondesign.app.ui.theme.AvatarPalette
import com.solomondesign.app.ui.theme.OnDark

/**
 * Page header used at the top of Today/Plan/Tools. [trailing] is an optional slot for a
 * fixed-size element (e.g. the profile avatar) anchored to the upper-right; leave it null
 * for headers that don't need one.
 */
@Composable
fun FieldPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        trailing?.invoke()
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
            if (avatarName != null) {
                BadgedPersonAvatar(
                    name = avatarName,
                    color = avatarColor,
                    presenceColor = statusColor,
                    photoRes = avatarPhotoRes,
                )
            } else {
                StatusIndicator(color = statusColor)
            }
        },
    )
}
