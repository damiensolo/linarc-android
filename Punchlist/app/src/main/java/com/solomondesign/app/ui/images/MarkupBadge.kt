package com.solomondesign.app.ui.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The "this photo carries markup" indicator overlaid on tiles and thumbnails. Hard-coded
 * white-on-scrim (the camera-chrome convention) because it sits over photo pixels in any theme.
 * Decorative for TalkBack — the hosting tile's semantics describe the photo.
 */
@Composable
fun MarkupBadge(modifier: Modifier = Modifier, diameter: Dp = 20.dp) {
    Box(
        modifier = modifier
            .size(diameter)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .testTag("markupBadge"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Draw,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(diameter * 0.62f),
        )
    }
}
