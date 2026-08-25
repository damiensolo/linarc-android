package com.solomondesign.app.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pattern C — pick which record a photo (or capture) becomes. Shared by the image viewer's
 * Create action and the photo review's "Save & create…"; the FAB entry points skip this sheet
 * because the tool already names the category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordChooserSheet(
    title: String,
    onPick: (RecordCategory) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag("createRecordSheet"),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            RecordCategory.entries.forEach { category ->
                ListItem(
                    headlineContent = { Text(category.screenTitle) },
                    supportingContent = { Text(category.chooserHint) },
                    leadingContent = {
                        Icon(category.icon(), contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { onPick(category) }
                        .testTag("createRecord_${category.routeId}"),
                )
            }
        }
    }
}

private val RecordCategory.chooserHint: String
    get() = when (this) {
        RecordCategory.ISSUE -> "Track a field problem to resolution"
        RecordCategory.INCIDENT -> "Log a safety event or near miss"
        RecordCategory.PUNCH -> "Add a closeout item to the punch list"
    }

/** Icon identity lives here (not in the pure model file) to keep RecordModels JVM-clean. */
@Composable
fun RecordCategory.icon() = when (this) {
    RecordCategory.ISSUE -> Icons.Filled.ReportProblem
    RecordCategory.INCIDENT -> Icons.Filled.HealthAndSafety
    RecordCategory.PUNCH -> Icons.AutoMirrored.Filled.PlaylistAddCheck
}

/**
 * Pattern B — the tool list behind Tools → Issues / Incidents / Punch list. Rows carry the
 * basic capture data (type, location, event date, first photo); the contextual FAB (see
 * `AppChrome`) creates a new record of this category.
 */
@Composable
fun RecordListScreen(
    category: RecordCategory,
    onOpenRecord: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val records = RecordRepository.byCategory(category)

    BrowseScaffold(
        title = category.pluralLabel,
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (records.isEmpty()) {
            FieldEmptyState(
                message = "No ${category.label.lowercase()}s yet — tap + to create one.",
                modifier = Modifier
                    .padding(padding)
                    .testTag("recordListEmpty_${category.routeId}"),
            )
            return@BrowseScaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("recordList_${category.routeId}"),
        ) {
            items(records, key = { it.id }) { record ->
                val photo = record.attachments
                    .firstOrNull { it.kind == AttachmentKind.PHOTO }
                    ?.let { ProjectImageRepository.find(it.ref) }
                FieldWorkRow(
                    title = record.title,
                    subtitle = listOfNotNull(
                        "Blocks work".takeIf { record.blocksWork },
                        record.type,
                        record.location,
                        formatRecordDate(record.eventDateMillis),
                    ).joinToString(" · "),
                    // Red is reserved for actual work stoppages; a logged record stays calm.
                    statusColor = when {
                        record.blocksWork -> MaterialTheme.colorScheme.error
                        category == RecordCategory.PUNCH -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    leading = photo?.let { image -> { ImageThumbnail(image = image) } },
                    enabled = true,
                    onClick = { onOpenRecord(record.id) },
                    modifier = Modifier.testTag("recordRow_${record.id}"),
                )
            }
        }
    }
}

/** Pattern B — one record, read-only: every captured field plus tappable photo attachments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordDetailScreen(
    recordId: String,
    onBack: () -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val record = RecordRepository.find(recordId)
    if (record == null) {
        BrowseScaffold(title = "Record", onBack = onBack, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This record isn't available anymore.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    BrowseScaffold(
        title = record.category.label,
        subtitle = record.type,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("recordDetailScreen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(record.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${record.type} · ${record.location} · " +
                    "Event ${formatRecordDate(record.eventDateMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${record.severity.label} severity · ${record.impact.label} impact",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Logged ${formatRecordDate(record.createdAtMillis)} by ${record.authorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.blocksWork) {
                // The auditable stoppage: why, what's scoped, when it should clear, who can
                // clear it. A record without this banner is logged-and-active, nothing more.
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                        .testTag("recordBlockingBanner"),
                ) {
                    Text(
                        text = "Blocks work",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    if (record.blockingReason.isNotBlank()) {
                        Text(
                            text = record.blockingReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    val scope = listOf(record.affectedTrade, record.affectedTask, record.workPackage)
                        .filter { it.isNotBlank() }
                    if (scope.isNotEmpty()) {
                        Text(
                            text = "Scoped to: ${scope.joinToString(" · ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    val resolution = listOfNotNull(
                        record.expectedResolutionMillis?.let { "Resolve by ${formatRecordDate(it)}" },
                        record.resolutionAuthority.takeIf { it.isNotBlank() }
                            ?.let { "Cleared by: $it" },
                        DemoProjectRepository.crewMember(record.escalationContactId)?.name
                            ?.let { "Escalate to: $it" },
                    )
                    if (resolution.isNotEmpty()) {
                        Text(
                            text = resolution.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (record.acknowledgementRequired) {
                        Text(
                            text = "Crew acknowledgement required before starting scoped work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            if (record.description.isNotBlank()) {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(record.description, style = MaterialTheme.typography.bodyMedium)
            }
            val assignees = record.assigneeIds.mapNotNull { DemoProjectRepository.crewMember(it)?.name }
            if (assignees.isNotEmpty()) {
                Text("Assignees", style = MaterialTheme.typography.titleMedium)
                Text(assignees.joinToString(), style = MaterialTheme.typography.bodyMedium)
            }
            if (record.attachments.isNotEmpty()) {
                Text("Attachments", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    record.attachments.forEach { attachment ->
                        RecordAttachmentTile(
                            attachment = attachment,
                            onOpenPhoto = onOpenImage,
                            onRemove = null,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One attachment as a 64dp tile: photos render their thumbnail (tappable when [onOpenPhoto] is
 * given), files render a document glyph with the picked name beneath. [onRemove] non-null adds
 * the delete badge — the create form's editable strip and the detail's read-only one share this.
 */
@Composable
fun RecordAttachmentTile(
    attachment: RecordAttachment,
    onOpenPhoto: ((String) -> Unit)?,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(64.dp)) {
        Box {
            when (attachment.kind) {
                AttachmentKind.PHOTO -> {
                    val image = ProjectImageRepository.find(attachment.ref)
                    if (image != null) {
                        ImageThumbnail(
                            image = image,
                            size = 64.dp,
                            modifier = if (onOpenPhoto != null) {
                                Modifier
                                    .clickable(onClickLabel = "Open photo") { onOpenPhoto(image.id) }
                                    .semantics { contentDescription = image.title }
                            } else {
                                Modifier.semantics { contentDescription = image.title }
                            },
                        )
                    } else {
                        FileGlyphTile(label = "Photo")
                    }
                }

                AttachmentKind.FILE -> FileGlyphTile(label = attachment.ref)
            }
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clickable(onClickLabel = "Remove attachment", onClick = onRemove)
                        .testTag("recordAttachmentRemove_${attachment.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove attachment",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        if (attachment.kind == AttachmentKind.FILE) {
            Text(
                text = attachment.ref,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FileGlyphTile(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatRecordDate(millis: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
