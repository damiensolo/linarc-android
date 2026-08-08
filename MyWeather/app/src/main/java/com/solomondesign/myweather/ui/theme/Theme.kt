package com.solomondesign.myweather.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MagentaPrimaryDark,
    secondary = MagentaSecondaryDark,
    tertiary = MagentaTertiaryDark,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    // Keep dark theme background very dark for contrast
    background = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = MagentaPrimary,
    secondary = MagentaSecondary,
    tertiary = MagentaTertiary
)

@Composable
fun MyWeatherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}