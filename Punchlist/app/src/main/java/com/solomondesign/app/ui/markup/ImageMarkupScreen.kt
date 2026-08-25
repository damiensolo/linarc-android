package com.solomondesign.app.ui.markup

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.capture.camera.PhotoProcessor
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.images.CapturedMediaStore
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.images.decodeSampledFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Images-viewer entry into markup: loads a captured photo from disk, runs the editor, then
 * asks how to save — as a copy (default; the original stays and the copy fans out to Today,
 * Plans, and Images like any capture) or by replacing the original in place (same image id, so
 * its pins and Today row survive; the pixels land in a NEW file because decode is keyed on the
 * path, and rewriting the old file would leave stale bitmaps on screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMarkupScreen(
    imageId: String,
    onClose: () -> Unit,
    onSaved: (copyImageId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val image = ProjectImageRepository.find(imageId)
    val sourcePath = (image?.source as? ImageSource.CapturedFile)?.absolutePath

    if (image == null || sourcePath == null) {
        TaskFlowScaffold(title = "Markup", onClose = onClose, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "Markup needs a captured photo — this image can't be edited.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, sourcePath) {
        value = withContext(Dispatchers.IO) {
            decodeSampledFile(sourcePath, PhotoProcessor.MAX_EDGE_PX)
        }
    }
    var pendingResult by remember { mutableStateOf<Bitmap?>(null) }
    var saving by remember { mutableStateOf(false) }

    val photo = bitmap
    if (photo == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("markupLoading"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    MarkupEditorScreen(
        photo = photo,
        onCancel = onClose,
        onDone = { result, hasMarkup ->
            if (hasMarkup) pendingResult = result else onClose()
        },
        doneLabel = "Save",
        doneRequiresMarkup = true,
        modifier = modifier,
    )

    val result = pendingResult
    if (result != null) {
        fun writeAnd(complete: (newPath: String) -> Unit) {
            if (saving) return
            saving = true
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    CapturedMediaStore.newPhotoFile(context).also {
                        PhotoProcessor.writeJpeg(result, it)
                    }
                }
                complete(file.absolutePath)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { if (!saving) pendingResult = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.testTag("markupSaveSheet")) {
                Text(
                    text = "Save markup",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("Save as a copy") },
                    supportingContent = {
                        Text("Keeps the original photo. The copy lands on Today, Plans, and Images.")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable(enabled = !saving) {
                            writeAnd { newPath ->
                                val copyId = DemoProjectRepository.addPhoto(
                                    title = "${image.title} — marked up",
                                    subtitle = image.area,
                                    createIssue = false,
                                    filePath = newPath,
                                    tags = (image.tags + "Markup").distinct(),
                                    hasMarkup = true,
                                )
                                onSaved(copyId)
                            }
                        }
                        .testTag("markupSaveCopy"),
                )
                ListItem(
                    headlineContent = { Text("Replace the original") },
                    supportingContent = {
                        Text("Overwrites this photo with the marked-up version.")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable(enabled = !saving) {
                            writeAnd { newPath ->
                                ProjectImageRepository.replaceSource(
                                    image.id,
                                    ImageSource.CapturedFile(newPath),
                                )
                                onSaved(null)
                            }
                        }
                        .testTag("markupSaveReplace"),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
