package com.solomondesign.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.solomondesign.app.ui.demo.DemoProjectRepository

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnDark,
    primaryContainer = RaisedHover,
    onPrimaryContainer = OnDark,
    secondary = SecondaryText,
    onSecondary = OnDark,
    tertiary = StatusProgress,
    onTertiary = CanvasBlack,
    background = CanvasBlack,
    onBackground = OnDark,
    surface = CanvasBlack,
    onSurface = OnDark,
    surfaceVariant = Raised,
    onSurfaceVariant = SecondaryText,
    surfaceBright = RaisedHover,
    surfaceDim = CanvasBlack,
    surfaceContainerLowest = CanvasBlack,
    surfaceContainerLow = CanvasBlack,
    surfaceContainer = Raised,
    surfaceContainerHigh = RaisedHover,
    surfaceContainerHighest = Hairline,
    outline = Outline,
    outlineVariant = Hairline,
    inverseSurface = OnDark,
    inverseOnSurface = CanvasBlack,
    inversePrimary = Accent,
    error = ErrorRed,
    onError = OnDark,
    scrim = CanvasBlack,
    surfaceTint = Color.Transparent,
)

private val LightScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = OnDark,
    primaryContainer = LightRaisedHover,
    onPrimaryContainer = LightOnSurface,
    secondary = LightSecondaryText,
    onSecondary = OnDark,
    tertiary = LightStatusProgress,
    onTertiary = CanvasBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightRaised,
    onSurfaceVariant = LightSecondaryText,
    surfaceBright = LightBackground,
    surfaceDim = LightRaised,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightRaised,
    surfaceContainerHigh = LightRaisedHover,
    surfaceContainerHighest = LightHairline,
    outline = LightOutline,
    outlineVariant = LightHairline,
    inverseSurface = CanvasBlack,
    inverseOnSurface = OnDark,
    inversePrimary = LightAccent,
    error = LightErrorRed,
    onError = OnDark,
    scrim = CanvasBlack,
    surfaceTint = Color.Transparent,
)

/**
 * Field prototype chrome. [darkTheme] defaults to the Tools → Appearance store so
 * toggling it recomposes the whole tree.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = DemoProjectRepository.darkTheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content,
    )
}
