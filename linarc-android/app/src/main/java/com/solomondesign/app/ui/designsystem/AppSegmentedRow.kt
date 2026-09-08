package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Single-choice segmented control whose labels never wrap. Material 3's `SegmentedButton`
 * lets a label break onto a second line when the row is tight (four segments on a phone
 * width, larger system font, or the selected segment's check icon eating label room), which
 * makes one segment taller than its neighbours and knocks the whole row out of shape. Here
 * every label is one line: it shrinks (down to [MIN_LABEL_SP]) before it would wrap, and rows
 * with more than [MAX_OPTIONS_WITH_ICON] segments drop the selected check icon — the filled
 * container already marks the selection — so the label keeps its width.
 *
 * Use this for every segmented row in the app instead of composing `SegmentedButton` directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppSegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    testTag: (T) -> String,
    modifier: Modifier = Modifier,
    showSelectedIcon: Boolean = options.size <= MAX_OPTIONS_WITH_ICON,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = if (showSelectedIcon) {
                    { SegmentedButtonDefaults.Icon(active = isSelected) }
                } else {
                    {}
                },
                label = { SegmentedLabel(label(option)) },
                modifier = Modifier.testTag(testTag(option)),
            )
        }
    }
}

/** One-line label that auto-shrinks to fit its segment instead of wrapping. */
@Composable
private fun SegmentedLabel(text: String) {
    val style = LocalTextStyle.current
    BasicText(
        text = text,
        style = style.merge(color = LocalContentColor.current),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MIN_LABEL_SP.sp,
            maxFontSize = if (style.fontSize.isSp) style.fontSize else DEFAULT_LABEL_SP.sp,
            stepSize = 0.5.sp,
        ),
    )
}

/** Segment counts above this drop the selected check icon to protect label width. */
private const val MAX_OPTIONS_WITH_ICON = 3

/** Smallest label the auto-size may reach; below this the label clips rather than shrinks. */
private const val MIN_LABEL_SP = 11

/** Fallback ceiling when the inherited style has no sp size (labelLarge is 14sp). */
private const val DEFAULT_LABEL_SP = 14
