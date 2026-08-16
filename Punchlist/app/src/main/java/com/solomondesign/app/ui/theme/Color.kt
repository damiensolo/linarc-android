package com.solomondesign.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Dark field-prototype tokens. */
val CanvasBlack = Color(0xFF000000)
val Raised = Color(0xFF1C1C1E)
val RaisedHover = Color(0xFF2C2C2E)
val Hairline = Color(0xFF3A3A3C)
val SecondaryText = Color(0xFF8E8E93)
val OnDark = Color(0xFFFFFFFF)
val StatusProgress = Color(0xFFF5C44D)
val StatusUrgent = Color(0xFFFF5C33)
val PresenceOnSite = Color(0xFF34C759)
val PresenceAssigned = Color(0xFFF5C44D)
val PresenceOffSite = Color(0xFF8E8E93)

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

val Accent = Color(0xFF5B8DEF)
val ErrorRed = Color(0xFFFF5C33)

/** Light field-prototype surfaces. */
val LightBackground = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF000000)
val LightRaised = Color(0xFFF2F2F7)
val LightRaisedHover = Color(0xFFE5E5EA)
val LightHairline = Color(0xFFD1D1D6)
val LightSecondaryText = Color(0xFF8E8E93)

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
