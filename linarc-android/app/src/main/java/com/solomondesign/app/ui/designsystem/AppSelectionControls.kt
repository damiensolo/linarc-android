package com.solomondesign.app.ui.designsystem

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Monochrome selection controls.
 *
 * Material 3 paints every checked/selected state in `primary`, so a form with a few toggles
 * and a checklist ended up with more blue than the actual call-to-action button. Selection
 * state is not a call to action: these controls (and the selected nav pill in [FieldNavItemIcon],
 * the active segment in [AppSegmentedRow]) render in [ColorScheme.inverseSurface] — white on the
 * dark theme, black on the light theme — with [ColorScheme.inverseOnSurface] marks. Blue and the
 * other accent/status colors stay reserved for [AppButton] Primary and status chips.
 *
 * Use these instead of composing `Switch`, `Checkbox`, or `RadioButton` directly.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = thumbContent,
        colors = SwitchDefaults.colors(
            checkedThumbColor = scheme.inverseOnSurface,
            checkedTrackColor = scheme.inverseSurface,
            checkedBorderColor = scheme.inverseSurface,
            checkedIconColor = scheme.inverseSurface,
            uncheckedThumbColor = scheme.outline,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedBorderColor = scheme.outline,
            uncheckedIconColor = scheme.surfaceContainerHighest,
        ),
    )
}

/** See [AppSwitch]. Pass `onCheckedChange = null` when the enclosing row owns the click. */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = scheme.inverseSurface,
            checkmarkColor = scheme.inverseOnSurface,
            uncheckedColor = scheme.outline,
        ),
    )
}

/** See [AppSwitch]. Pass `onClick = null` when the enclosing selectable row owns the click. */
@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = RadioButtonDefaults.colors(
            selectedColor = scheme.inverseSurface,
            unselectedColor = scheme.outline,
        ),
    )
}
