package com.solomondesign.app.ui.images

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.DesignTokens
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.plan.PlanSheetRepository
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordChooserSheet
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Full-screen viewing: decode near typical display resolution, never the full sensor size. */
private const val VIEWER_DECODE_EDGE_PX = 2048

/** How the Images tool presents the same photo set. Grid stays the default working view. */
private enum class ImagesViewMode(val label: String) {
    GRID("Grid"),
    TIMELINE("Timeline"),
    ALBUMS("Albums"),
    MAP("Map"),
}

/**
 * Pattern B — the Images tool. One photo set, four presentations: the working grid (with tag
 * filters), a day-by-day timeline, albums (filed from the viewer's Album action), and a map —
 * captures pinned at their positions on the Level 2 sheet. Contextual FAB opens the source sheet.
 */
@Composable
fun ImageGridScreen(
    onOpenImage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by rememberSaveable { mutableStateOf(ImagesViewMode.GRID) }
    var activeTag by rememberSaveable { mutableStateOf<String?>(null) }

    BrowseScaffold(
        title = "Images",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().testTag("imageGridScreen")) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ImagesViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ImagesViewMode.entries.size,
                        ),
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("imagesView_${mode.name}"),
                    )
                }
            }

            when (viewMode) {
                ImagesViewMode.GRID -> ImagesGridView(
                    activeTag = activeTag,
                    onTagChange = { activeTag = it },
                    onOpenImage = onOpenImage,
                )

                ImagesViewMode.TIMELINE -> ImageSectionGrid(
                    sections = groupImagesByDay(
                        images = ProjectImageRepository.images,
                        today = LocalDate.now(),
                        zone = ZoneId.systemDefault(),
                    ),
                    emptyMessage = "No photos yet. Tap + to take one.",
                    onOpenImage = onOpenImage,
                    testTag = "imagesTimelineView",
                )

                ImagesViewMode.ALBUMS -> ImageSectionGrid(
                    sections = groupImagesByAlbum(ProjectImageRepository.images),
                    emptyMessage = "No photos yet. Tap + to take one.",
                    onOpenImage = onOpenImage,
                    testTag = "imagesAlbumsView",
                )

                ImagesViewMode.MAP -> ImagesMapView(onOpenImage = onOpenImage)
            }
        }
    }
}

/** The original working grid: tag filter chips over a three-column tile grid. */
@Composable
private fun ImagesGridView(
    activeTag: String?,
    onTagChange: (String?) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tags = ProjectImageRepository.visibleTags()
    val images = ProjectImageRepository.filterByTag(activeTag)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = activeTag == null,
                onClick = { onTagChange(null) },
                label = { Text("All") },
            )
            tags.forEach { tag ->
                FilterChip(
                    selected = activeTag == tag,
                    onClick = { onTagChange(if (activeTag == tag) null else tag) },
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

/** Timeline and Albums share this shape: full-width section headers over tile rows. */
@Composable
private fun ImageSectionGrid(
    sections: List<ImageSection>,
    emptyMessage: String,
    onOpenImage: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) {
        FieldEmptyState(message = emptyMessage, modifier = modifier.testTag(testTag))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
    ) {
        sections.forEach { section ->
            item(key = "header-${section.title}", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${section.images.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(section.images, key = { "${section.title}-${it.id}" }) { image ->
                ImageTile(
                    image = image,
                    onClick = { onOpenImage(image.id) },
                    modifier = Modifier.testTag("imageTile_${image.id}"),
                )
            }
        }
    }
}

/**
 * Captures at their pinned positions on the Level 2 sheet — the same pin coordinates the Plan
 * viewer shows, inverted back to photos via the `pin-<imageId>` convention. No device GPS in
 * this prototype: "map" deliberately means the site drawing, which is what a foreman navigates by.
 */
@Composable
private fun ImagesMapView(
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheet = PlanSheetRepository.sheets.firstOrNull { it.isPinSheet }
    if (sheet == null) {
        FieldEmptyState(message = "This demo has no pin sheet to map onto.")
        return
    }
    val pinnedImages = DemoProjectRepository.pins
        .filter { it.kind == PinKind.PHOTO }
        .mapNotNull { pin ->
            ProjectImageRepository.find(imageIdOfPin(pin.id))?.let { image -> pin to image }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("imagesMapView"),
    ) {
        Text(
            text = "${sheet.number} · ${sheet.title}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (pinnedImages.isEmpty()) {
                "Captured photos drop a pin here automatically — take one to see it placed."
            } else {
                "${pinnedImages.size} pinned capture${if (pinnedImages.size == 1) "" else "s"} — tap one to open it."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        val painter = painterResource(sheet.drawableRes)
        val intrinsic = painter.intrinsicSize
        val aspect = if (intrinsic.height > 0f) intrinsic.width / intrinsic.height else 1.5f
        var sizePx by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Sized to the drawing's aspect ratio so pin fractions land on real geometry.
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(DesignTokens.CardCornerRadius))
                .background(Color.White)
                .onSizeChanged { sizePx = it },
        ) {
            Image(
                painter = painter,
                contentDescription = "${sheet.number} ${sheet.title} drawing",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (sizePx != IntSize.Zero) {
                pinnedImages.forEach { (pin, image) ->
                    ImageThumbnail(
                        image = image,
                        size = 44.dp,
                        modifier = Modifier
                            .offset {
                                val half = 22.dp.roundToPx()
                                IntOffset(
                                    (pin.xFraction * sizePx.width).roundToInt() - half,
                                    (pin.yFraction * sizePx.height).roundToInt() - half,
                                )
                            }
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "Open photo") { onOpenImage(image.id) }
                            .semantics { contentDescription = image.title }
                            .testTag("mapPin_${pin.id}"),
                    )
                }
            }
        }
    }
}

/**
 * Pattern A — full-screen viewer. Read-only, so [TaskFlowScaffold] gets no confirm action and
 * there is nothing to discard; Close and system back both exit immediately. The Album action
 * (Pattern C sheet) files the photo for the Albums view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    imageId: String,
    onClose: () -> Unit,
    /** Create action: the chooser picked a record category; open its form with this photo. */
    onCreateRecord: (ProjectImage, RecordCategory) -> Unit,
    onMarkup: (ProjectImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = ProjectImageRepository.find(imageId)
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAlbumSheet by remember { mutableStateOf(false) }
    var showCreateChooser by remember { mutableStateOf(false) }

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
                    onMarkup = { onMarkup(image) },
                    onAlbum = { showAlbumSheet = true },
                    onDelete = { showDeleteDialog = true },
                    onCreate = { showCreateChooser = true },
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

                is ImageSource.CapturedFile -> FilePhoto(
                    absolutePath = source.absolutePath,
                    contentDescription = image.title,
                    maxEdgePx = VIEWER_DECODE_EDGE_PX,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        SitePhotoSwatch(seed = image.id.hashCode(), modifier = Modifier.fillMaxSize())
                    },
                )

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
                if (!image.album.isNullOrBlank()) {
                    Text(
                        text = "Album: ${image.album}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("viewerAlbumCaption"),
                    )
                }
            }
        }
    }

    if (showAlbumSheet) {
        AlbumPickerSheet(
            image = image,
            onDismiss = { showAlbumSheet = false },
        )
    }

    if (showCreateChooser) {
        RecordChooserSheet(
            title = "Create from this photo",
            onPick = { category ->
                showCreateChooser = false
                onCreateRecord(image, category)
            },
            onDismiss = { showCreateChooser = false },
        )
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

/**
 * Pattern C — file the photo under an album: pick an existing one, start a new one, or unfile.
 * Every action commits immediately and dismisses; there's no draft state to discard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    image: ProjectImage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var newAlbum by remember { mutableStateOf("") }
    val albums = ProjectImageRepository.albums()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag("albumSheet"),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("File under album", style = MaterialTheme.typography.titleLarge)
            albums.forEach { album ->
                ListItem(
                    headlineContent = { Text(album) },
                    trailingContent = if (album == image.album) {
                        { Icon(Icons.Filled.Check, contentDescription = "Current album") }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            ProjectImageRepository.setAlbum(image.id, album)
                            onDismiss()
                        }
                        .testTag("albumOption_$album"),
                )
            }
            if (!image.album.isNullOrBlank()) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Remove from ${image.album}",
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            ProjectImageRepository.setAlbum(image.id, null)
                            onDismiss()
                        }
                        .testTag("albumRemove"),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = newAlbum,
                    onValueChange = { newAlbum = it },
                    label = { Text("New album") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("albumNewField"),
                )
                TextButton(
                    onClick = {
                        ProjectImageRepository.setAlbum(image.id, newAlbum)
                        onDismiss()
                    },
                    enabled = newAlbum.isNotBlank(),
                    modifier = Modifier.testTag("albumCreate"),
                ) {
                    Text("Create")
                }
            }
        }
    }
}
