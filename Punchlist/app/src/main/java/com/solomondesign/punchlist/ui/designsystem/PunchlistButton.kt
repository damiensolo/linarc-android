package com.solomondesign.punchlist.ui.designsystem

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomondesign.punchlist.ui.theme.PunchlistTheme

/**
 * Literal color tokens taken from Figma file `tQZwm8SV0Nnsr5rbYdnVZN`
 * ("Mobile Design System V3"), node 5923:938 ("AndroidButtonDark" button sheet).
 * Not yet wired into the app-wide MaterialTheme.colorScheme — scoped to this
 * component only until the rest of the theme is aligned to this design system.
 */
private object ButtonTokens {
    val PrimaryDefault = Color(0xFF2F69C7)
    val PrimaryPressed = Color(0xFF235097)
    val SecondaryMuted = Color(0xFFF7F7F7)
    val SuccessDefault = Color(0xFF1B9E4B)
    val ErrorDefault = Color(0xFFCF2D30)
    val Disabled = Color(0xFFCCCCCC)
    val OnDisabled = Color(0xFF484848)
    val OnColor = Color.White

    // Figma's Secondary/Default token specifies white text on this near-white
    // (#F7F7F7) container, which measures ~1.07:1 contrast — effectively invisible
    // (WCAG AA requires 4.5:1). Overridden here with a dark on-surface color so the
    // button is actually readable; the container color still matches Figma exactly.
    val OnSecondary = Color(0xFF1A1A1A)
}

/** Figma spec: font_size/sm (14sp), weight Medium, letter_spacing/xs (-0.5sp), line_height/sm (20sp). */
private val ButtonLabelStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.5).sp,
)

enum class PunchlistButtonType { Primary, Secondary, Success, Error }

enum class PunchlistButtonSize { Large, Small }

/**
 * Design-system button matching the Figma "AndroidButtonDark" component
 * (node 5923:938). Wraps Material3 [Button] so it keeps standard Android button
 * behavior for free: ripple/state-layer feedback, a minimum 48dp touch target,
 * and correct enabled/disabled semantics for accessibility services.
 *
 * Pressed and Disabled colors were only documented for [PunchlistButtonType.Primary]
 * in the source Figma node; Disabled styling is applied uniformly across types
 * (a common design-system convention), while Pressed falls back to the default
 * Material ripple for the other types.
 */
@Composable
fun PunchlistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: PunchlistButtonType = PunchlistButtonType.Primary,
    size: PunchlistButtonSize = PunchlistButtonSize.Large,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseContainerColor = when (type) {
        PunchlistButtonType.Primary -> ButtonTokens.PrimaryDefault
        PunchlistButtonType.Secondary -> ButtonTokens.SecondaryMuted
        PunchlistButtonType.Success -> ButtonTokens.SuccessDefault
        PunchlistButtonType.Error -> ButtonTokens.ErrorDefault
    }
    val containerColor = if (type == PunchlistButtonType.Primary && isPressed) {
        ButtonTokens.PrimaryPressed
    } else {
        baseContainerColor
    }

    val shape = when (size) {
        PunchlistButtonSize.Large -> RoundedCornerShape(36.dp)
        PunchlistButtonSize.Small -> RoundedCornerShape(10.dp)
    }

    val widthModifier = if (size == PunchlistButtonSize.Large) Modifier.fillMaxWidth() else Modifier

    Button(
        onClick = onClick,
        modifier = modifier.then(widthModifier).height(40.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = if (type == PunchlistButtonType.Secondary) {
                ButtonTokens.OnSecondary
            } else {
                ButtonTokens.OnColor
            },
            disabledContainerColor = ButtonTokens.Disabled,
            disabledContentColor = ButtonTokens.OnDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        interactionSource = interactionSource,
    ) {
        Text(text = text, style = ButtonLabelStyle)
    }
}

@Preview(showBackground = true, name = "Button variants")
@Composable
private fun PunchlistButtonVariantsPreview() {
    PunchlistTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PunchlistButton(text = "Label", onClick = {})
            PunchlistButton(text = "Label", onClick = {}, enabled = false)
            PunchlistButton(text = "Label", onClick = {}, type = PunchlistButtonType.Secondary)
            PunchlistButton(text = "Label", onClick = {}, type = PunchlistButtonType.Error)
            PunchlistButton(text = "Label", onClick = {}, type = PunchlistButtonType.Success)
            PunchlistButton(text = "Label", onClick = {}, size = PunchlistButtonSize.Small)
            PunchlistButton(
                text = "Label",
                onClick = {},
                type = PunchlistButtonType.Secondary,
                size = PunchlistButtonSize.Small,
            )
            PunchlistButton(
                text = "Label",
                onClick = {},
                type = PunchlistButtonType.Error,
                size = PunchlistButtonSize.Small,
            )
            PunchlistButton(
                text = "Label",
                onClick = {},
                type = PunchlistButtonType.Success,
                size = PunchlistButtonSize.Small,
            )
        }
    }
}
