package com.solomondesign.app.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DesignTokens {
    val CardCornerRadius = 12.dp

    /** Tracks [MaterialTheme.colorScheme] so it flips with light/dark instead of a fixed hex. */
    val PrimaryAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val ErrorAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error
}
