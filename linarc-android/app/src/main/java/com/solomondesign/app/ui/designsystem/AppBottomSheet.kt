package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Pattern C — a modal bottom sheet for compact contextual actions or one-or-two parameter changes.
 *
 * Wraps the `ModalBottomSheet` + `rememberModalBottomSheetState(skipPartiallyExpanded = true)` +
 * title + trailing-spacer combination that every sheet in the app repeats.
 *
 * It also fixes a bug those hand-rolled copies share: dismissing by flipping a boolean removes the
 * sheet from composition immediately and skips the Material hide animation. [content] receives a
 * `dismiss` lambda that animates out first, so rows should call that rather than the caller's
 * `onDismiss` directly. Scrim taps and swipes already animate, so `onDismissRequest` stays direct.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.(dismiss: (() -> Unit) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    /** Animate the sheet away, then run [andThen] once it is actually hidden. */
    val dismissThen: (() -> Unit) -> Unit = { andThen ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) andThen()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
            }
            content(dismissThen)
            Spacer(Modifier.height(24.dp))
        }
    }
}
