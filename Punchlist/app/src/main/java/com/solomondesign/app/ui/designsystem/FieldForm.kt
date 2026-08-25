package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Form primitives for long create/edit flows: a sticky one-action footer, and the required-field
 * convention (`*` in the label plus a note at the top of the form) so required status never
 * relies on color alone.
 */

/**
 * Sticky footer for a scrolling form: exactly one primary action, always visible. Sits clear of
 * the gesture-nav area and rises above the keyboard (`navigationBarsPadding` + `imePadding`);
 * hand it to a `Scaffold`/[TaskFlowScaffold] `bottomBar` slot so the content viewport shrinks to
 * match and the last field is never hidden behind it.
 *
 * [statusMessage] is the save-attempt summary ("Complete 2 required fields to save"); it renders
 * above the button as a polite live region so TalkBack announces it when it appears.
 */
@Composable
fun FieldFormActionBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    buttonTestTag: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("formActionBarStatus"),
                    )
                }
                AppButton(
                    text = text,
                    onClick = onClick,
                    modifier = if (buttonTestTag != null) {
                        Modifier.testTag(buttonTestTag)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

/** Top-of-form legend explaining the `*` convention; place it before the first field. */
@Composable
fun FieldRequiredNote(modifier: Modifier = Modifier) {
    Text(
        text = "* Required fields",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics {
            contentDescription = "Fields marked with an asterisk are required"
        },
    )
}

/** Label slot for a required field — `Site name *` — so required status is text, not color. */
@Composable
fun FieldRequiredLabel(text: String) {
    Text("$text *")
}
