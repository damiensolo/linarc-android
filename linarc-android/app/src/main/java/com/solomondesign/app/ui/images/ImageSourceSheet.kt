package com.solomondesign.app.ui.images

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.solomondesign.app.ui.designsystem.AppBottomSheet
import com.solomondesign.app.ui.designsystem.FieldWorkRow

/**
 * Pattern C — where a new image comes from. Pure reuse of the Capture sheet's shape; "Take photo"
 * hands off to the existing, working camera flow rather than duplicating it.
 */
@Composable
fun ImageSourceSheet(
    onTakePhoto: () -> Unit,
    onUseDemoImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        title = "Add image",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("imageSourceSheet"),
    ) { dismissThen ->
        FieldWorkRow(
            title = "Take photo",
            subtitle = "Use the camera and tag it",
            statusColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            onClick = { dismissThen(onTakePhoto) },
            modifier = Modifier.testTag("imageSourceCamera"),
        )
        FieldWorkRow(
            title = "Use demo image",
            subtitle = "Add a drawn sample photo",
            statusColor = MaterialTheme.colorScheme.tertiary,
            enabled = true,
            onClick = { dismissThen(onUseDemoImage) },
            modifier = Modifier.testTag("imageSourceDemo"),
        )
    }
}
