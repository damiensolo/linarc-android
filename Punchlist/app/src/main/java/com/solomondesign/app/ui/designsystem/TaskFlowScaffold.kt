package com.solomondesign.app.ui.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow

/**
 * Pattern A — full-screen task flow for editing, creating, or otherwise state-changing work.
 *
 * The bottom navigation is hidden by the shell (see `resolveChrome`); this scaffold supplies the
 * Close-top-left / confirm-top-right bar and the unsaved-changes guard.
 *
 * Both exits — the Close button and the system back gesture — route through a single
 * `requestClose` lambda, so the discard policy lives in exactly one place.
 *
 * @param onConfirm null renders no top-right action, for read-only full-screen surfaces such as
 *   an image viewer that has nothing to save.
 * @param hasUnsavedChanges when false, back deliberately falls through to the NavController so
 *   predictive-back animation is preserved.
 * @param interceptBackAlways set when [onClose] is not a plain single pop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnsavedChanges: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    confirmLabel: String = "Save",
    confirmEnabled: Boolean = true,
    closeContentDescription: String = "Close",
    interceptBackAlways: Boolean = false,
    discardTitle: String = "Discard changes?",
    discardMessage: String = "Your edits haven't been saved and will be lost.",
    discardConfirmLabel: String = "Discard",
    discardDismissLabel: String = "Keep editing",
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val requestClose: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onClose()
    }

    BackHandler(enabled = hasUnsavedChanges || interceptBackAlways) { requestClose() }

    Scaffold(
        modifier = modifier,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(
                        onClick = requestClose,
                        modifier = Modifier.testTag("taskFlowClose"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = closeContentDescription)
                    }
                },
                actions = {
                    if (onConfirm != null) {
                        TextButton(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            modifier = Modifier.testTag("taskFlowConfirm"),
                        ) {
                            Text(confirmLabel)
                        }
                    }
                },
            )
        },
        content = content,
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(discardTitle) },
            text = { Text(discardMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                    modifier = Modifier.testTag("discardConfirm"),
                ) {
                    Text(discardConfirmLabel)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.testTag("discardDismiss"),
                ) {
                    Text(discardDismissLabel)
                }
            },
            modifier = Modifier.testTag("discardDialog"),
        )
    }
}
