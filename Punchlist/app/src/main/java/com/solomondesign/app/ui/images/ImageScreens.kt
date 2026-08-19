package com.solomondesign.app.ui.images

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold

/** Pattern B — image grid with tag filters. Contextual FAB opens the source sheet. */
@Composable
fun ImageGridScreen(
    onOpenImage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTag by rememberSaveable { mutableStateOf<String?>(null) }
    val tags = ProjectImageRepository.visibleTags()
    val images = ProjectImageRepository.filterByTag(activeTag)

    BrowseScaffold(
        title = "Images",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().testTag("imageGridScreen")) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = activeTag == null,
                    onClick = { activeTag = null },
                    label = { Text("All") },
                )
                tags.forEach { tag ->
                    FilterChip(
                        selected = activeTag == tag,
                        onClick = { activeTag = if (activeTag == tag) null else tag },
                        label = { Text(tag) },
                    )
                }
            }

            if (images.isEmpty()) {
                FieldEmptyState(message = "No photos yet. Tap + to take one.")
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(images, key = { it.id }) { image ->
                    ImageTile(
                        image = image,
                        onClick = { onOpenImage(image.id) },
                        modifier = Modifier.testTag("imageTile_${image.id}"),
                    )
                }
            }
        }
    }
}

/**
 * Pattern A — full-screen viewer. Read-only, so [TaskFlowScaffold] gets no confirm action and
 * there is nothing to discard; Close and system back both exit immediately.
 */
@Composable
fun ImageViewerScreen(
    imageId: String,
    onClose: () -> Unit,
    onCreateIssue: (ProjectImage) -> Unit,
    onMarkupUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = ProjectImageRepository.find(imageId)
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (image == null) {
        TaskFlowScaffold(title = "Photo", onClose = onClose, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This photo is no longer available.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    TaskFlowScaffold(
        title = image.title,
        onClose = onClose,
        modifier = modifier,
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ImageViewerToolbar(
                    onShare = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                buildString {
                                    append(image.title)
                                    append(" · ")
                                    append(DemoProjectRepository.PROJECT_NAME)
                                    append(" · ")
                                    append(image.area)
                                    if (image.tags.isNotEmpty()) {
                                        append(" · ")
                                        append(image.tags.joinToString(", "))
                                    }
                                },
                            )
                        }
                        ContextCompat.startActivity(
                            context,
                            Intent.createChooser(send, "Share photo"),
                            null,
                        )
                    },
                    onMarkup = onMarkupUnavailable,
                    onDelete = { showDeleteDialog = true },
                    onCreate = { onCreateIssue(image) },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .testTag("imageViewerScreen"),
            contentAlignment = Alignment.Center,
        ) {
            val captured = (image.source as? ImageSource.Captured)
                ?.let { CapturedBitmapStore.get(it.captureKey) }
            when (val source = image.source) {
                is ImageSource.Drawable -> Image(
                    painter = painterResource(source.resId),
                    contentDescription = image.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )

                is ImageSource.Captured -> if (captured != null) {
                    Image(
                        bitmap = captured.asImageBitmap(),
                        contentDescription = image.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SitePhotoSwatch(seed = image.id.hashCode(), modifier = Modifier.fillMaxSize())
                }

                is ImageSource.Swatch -> SitePhotoSwatch(
                    seed = source.seed,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Text(
                    text = "${image.area} · ${image.authorName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (image.linkedRecordId != null) {
                    Text(
                        text = "Linked to an issue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.testTag("viewerLinkedCaption"),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this photo?") },
            text = {
                Text(
                    "This also removes the Plan pin and the Today entry it created. " +
                        "This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        ProjectImageRepository.delete(image.id)
                        onClose()
                    },
                    modifier = Modifier.testTag("viewerDeleteConfirm"),
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            modifier = Modifier.testTag("viewerDeleteDialog"),
        )
    }
}
