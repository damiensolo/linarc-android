package com.solomondesign.punchlist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    secondary = BrandPrimaryDark,
    tertiary = BrandPrimaryDark,
    error = BrandError,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandPrimary,
    tertiary = BrandPrimary,
    error = BrandError,
    surface = Color.White,
    background = Color.White,
    surfaceVariant = BrandMuted,
    primaryContainer = BrandMuted,
    onPrimaryContainer = BrandPrimary,
)

@Composable
fun PunchlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
