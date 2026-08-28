package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomondesign.app.ui.theme.AppTheme
import com.solomondesign.app.ui.theme.LightPresenceOnSite
import com.solomondesign.app.ui.theme.PresenceOnSite

/** Content color is white across every filled variant; containers below track the active theme. */
private val OnColor = Color.White

private val ButtonLabelStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.5).sp,
)

enum class AppButtonType { Primary, Secondary, Success, Error }

enum class AppButtonSize { Large, Small }

/**
 * Full-width pill button used on Today, capture, and voice flows.
 * Wraps Material3 [Button] for ripple, 48dp touch target, and enabled semantics.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AppButtonType = AppButtonType.Primary,
    size: AppButtonSize = AppButtonSize.Large,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val secondaryMuted = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val onSecondary = if (isDark) Color.White else Color(0xFF1A1A1A)
    val disabled = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val onDisabled = Color(0xFF8E8E93)

    // Track the active theme instead of a fixed hex, so light mode gets its own high-contrast
    // accent (per the "I contrast" palette) rather than the dark-mode value.
    val primaryDefault = scheme.primary
    val primaryPressed = lerp(primaryDefault, Color.Black, 0.15f)
    val successDefault = if (isDark) PresenceOnSite else LightPresenceOnSite
    val errorDefault = scheme.error

    val baseContainerColor = when (type) {
        AppButtonType.Primary -> primaryDefault
        AppButtonType.Secondary -> secondaryMuted
        AppButtonType.Success -> successDefault
        AppButtonType.Error -> errorDefault
    }
    val containerColor = if (type == AppButtonType.Primary && isPressed) {
        primaryPressed
    } else {
        baseContainerColor
    }

    val shape = when (size) {
        AppButtonSize.Large -> RoundedCornerShape(36.dp)
        AppButtonSize.Small -> RoundedCornerShape(10.dp)
    }

    val widthModifier = if (size == AppButtonSize.Large) Modifier.fillMaxWidth() else Modifier

    Button(
        onClick = onClick,
        modifier = modifier.then(widthModifier).height(40.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = if (type == AppButtonType.Secondary) {
                onSecondary
            } else {
                OnColor
            },
            disabledContainerColor = disabled,
            disabledContentColor = onDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        interactionSource = interactionSource,
    ) {
        Text(text = text, style = ButtonLabelStyle)
    }
}

@Preview(showBackground = true, name = "Button variants")
@Composable
private fun AppButtonVariantsPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(text = "Label", onClick = {})
            AppButton(text = "Label", onClick = {}, enabled = false)
            AppButton(text = "Label", onClick = {}, type = AppButtonType.Secondary)
            AppButton(text = "Label", onClick = {}, type = AppButtonType.Error)
            AppButton(text = "Label", onClick = {}, type = AppButtonType.Success)
            AppButton(text = "Label", onClick = {}, size = AppButtonSize.Small)
            AppButton(
                text = "Label",
                onClick = {},
                type = AppButtonType.Secondary,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "Label",
                onClick = {},
                type = AppButtonType.Error,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "Label",
                onClick = {},
                type = AppButtonType.Success,
                size = AppButtonSize.Small,
            )
        }
    }
}
