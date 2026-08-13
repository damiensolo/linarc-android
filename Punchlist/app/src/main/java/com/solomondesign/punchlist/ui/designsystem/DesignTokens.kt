package com.solomondesign.punchlist.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared color/shape tokens pulled from Figma file `tQZwm8SV0Nnsr5rbYdnVZN`
 * ("Mobile Design System V3") via `get_variable_defs` on node 5923:938 (the same
 * button sheet [com.solomondesign.punchlist.ui.designsystem.PunchlistButton] uses) —
 * this is the one part of that system the Figma MCP could resolve to concrete values
 * in this session (the design system's List/Card/Alert component pages weren't open
 * in the Figma desktop app, so their tokens couldn't be read the same way). Reusing
 * these confirmed values elsewhere keeps non-button surfaces visually tied to the same
 * system instead of falling back to generic Material3 defaults.
 */
object DesignTokens {
    /** Figma `surface/primary/muted` (#F7F7F7) — recessed/card surface, one step off page background. */
    val MutedSurface = Color(0xFFF7F7F7)

    /** Figma `surface/primary/default` (#2F69C7) — brand primary, used here to tag "people" content. */
    val PrimaryAccent = Color(0xFF2F69C7)

    /** Figma `surface/error/default` (#CF2D30) — used here to tag content needing attention/review. */
    val ErrorAccent = Color(0xFFCF2D30)

    /** Figma `corner_radius/md` (10dp) — the design system's standard (non-pill) component radius. */
    val CardCornerRadius = 10.dp
}
