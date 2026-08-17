package com.solomondesign.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * High-contrast ("I contrast") field-prototype palette — true-black dark mode paired with a
 * bright-white light mode, each with independently tuned accent colors so alerts stay legible
 * against their own background instead of sharing one hex across both themes.
 */

/** Dark field-prototype tokens. */
val CanvasBlack = Color(0xFF000000)
val Raised = Color(0xFF1A1A1A)
val RaisedHover = Color(0xFF2C2C2E)
val Hairline = Color(0xFF3A3A3C)
val SecondaryText = Color(0xFFE0E0E0)
val OnDark = Color(0xFFFFFFFF)
val StatusProgress = Color(0xFFFFDF33)
val StatusUrgent = Color(0xFFFF7A22)
val PresenceOnSite = Color(0xFF26D07C)
val PresenceAssigned = Color(0xFFFFDF33)
val PresenceOffSite = Color(0xFF8E8E93)

/**
 * Dedicated mid-tone for M3's `outline` role (dividers, borders, selection-indicator fills) —
 * kept separate from [SecondaryText] so future text-contrast tuning can't silently darken/lighten
 * borders and fills that happen to share the role. Selected-nav-item pills etc. should read
 * against [surfaceContainerHigh]-family tokens, not `outline`, for the same reason.
 */
val Outline = Color(0xFF8E8E93)

/** Distinct avatar fills assigned by crew index so neighbors never share a color. */
object AvatarPalette {
    val colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF0D9488),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFFDB2777),
        Color(0xFF059669),
        Color(0xFFEA580C),
        Color(0xFF4F46E5),
    )

    fun colorAt(index: Int): Color = colors[index.mod(colors.size)]
}

val Accent = Color(0xFF4D90FF)
val ErrorRed = Color(0xFFFF7A22)

/** Light field-prototype surfaces. */
val LightBackground = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF111111)
val LightRaised = Color(0xFFF4F4F0)
val LightRaisedHover = Color(0xFFE5E5EA)
val LightHairline = Color(0xFFD1D1D6)
val LightSecondaryText = Color(0xFF333333)

/** Light counterpart to [Outline] — see its doc comment. */
val LightOutline = Color(0xFF8E8E93)

/** Light-mode counterparts to the accent tokens above (the base names carry the dark values). */
val LightAccent = Color(0xFF0052CC)
val LightErrorRed = Color(0xFFFF6600)
val LightStatusProgress = Color(0xFFFFD700)
val LightStatusUrgent = Color(0xFFFF6600)
val LightPresenceOnSite = Color(0xFF00875A)
val LightPresenceAssigned = Color(0xFFFFD700)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val BrandPrimary = Accent
val BrandPrimaryDark = Accent
val BrandError = ErrorRed
val BrandMuted = Raised
