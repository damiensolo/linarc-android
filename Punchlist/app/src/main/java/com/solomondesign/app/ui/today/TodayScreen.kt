package com.solomondesign.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.CrewMember
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.StreamKind
import com.solomondesign.app.ui.demo.badgeColor
import com.solomondesign.app.ui.demo.statusLabel
import com.solomondesign.app.ui.designsystem.FieldCollapsibleSectionHeader
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import com.solomondesign.app.ui.designsystem.FieldSectionLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.profile.ProfileAvatarButton
import com.solomondesign.app.ui.theme.AvatarPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenVoiceLog: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onSwitchProject: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persona = DemoProjectRepository.persona
    val dayStarted = DemoProjectRepository.dayStarted
    val streamItems = DemoProjectRepository.streamItems.toList()
    val crew = DemoProjectRepository.crew
    var showStartMyDay by remember { mutableStateOf(false) }
    var crewExpanded by remember { mutableStateOf(true) }

    // Issued ≠ blocked: Blockers shows only rows explicitly marked blocking (records with the
    // Blocks work toggle, dictated delays). Everything else — including logged-not-blocking
    // issues — is activity, not a stoppage.
    val blockers = streamItems.filter { it.blocking }
    val captures = streamItems.filterNot { it.blocking }

    if (showStartMyDay) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStartMyDay = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            StartMyDaySheetContent(
                crew = crew,
                onConfirm = {
                    DemoProjectRepository.confirmStartMyDay()
                    showStartMyDay = false
                },
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("todayScreen"),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            FieldPageHeader(
                title = "Today",
                subtitle = "${persona.displayName} · ${DemoProjectRepository.AREA}",
                projectName = DemoProjectRepository.PROJECT_NAME,
                onSwitchProject = onSwitchProject,
                onOpenSettings = onOpenSettings,
                trailing = { ProfileAvatarButton(onClick = onOpenProfile) },
            )
        }

        if (!dayStarted) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                        .testTag("startMyDayCard"),
                ) {
                    Text("Start My Day", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Crew, Area B, and weather are ready. Confirm to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    AppButton(
                        text = "Start My Day",
                        onClick = { showStartMyDay = true },
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Day started · ${DemoProjectRepository.AREA}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        item {
            FieldCollapsibleSectionHeader(
                title = "Crew",
                count = crew.size,
                expanded = crewExpanded,
                onToggleExpanded = { crewExpanded = !crewExpanded },
                modifier = Modifier.testTag("crewSectionHeader"),
            )
        }
        if (crewExpanded) {
            itemsIndexed(crew, key = { _, member -> member.name }) { index, member ->
                FieldWorkRow(
                    title = member.name,
                    subtitle = "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
                    statusColor = member.presence.badgeColor(),
                    avatarName = member.name,
                    avatarColor = AvatarPalette.colorAt(index),
                    avatarPhotoRes = member.photoRes,
                )
            }
        }

        item { FieldSectionLabel("Blockers") }
        if (blockers.isEmpty()) {
            item {
                Text(
                    text = "No blockers. Issues land here only when marked as blocking work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        } else {
            items(blockers, key = { it.id }) { item ->
                // Every blocker opens the thing behind it: record-backed rows open the record
                // detail in its tool; voice-log rows open the daily log they came from.
                FieldWorkRow(
                    title = item.title,
                    subtitle = item.subtitle + " · " + formatTimestamp(item.timestampMillis),
                    statusColor = if (item.kind == StreamKind.ISSUE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    enabled = item.relatedFieldRecordId != null || item.relatedRecordId != null,
                    onClick = {
                        when {
                            item.relatedFieldRecordId != null -> onOpenRecord(item.relatedFieldRecordId)
                            item.relatedRecordId != null -> onOpenVoiceLog(item.relatedRecordId)
                        }
                    },
                    modifier = Modifier.testTag("streamItem_${item.id}"),
                )
            }
        }

        item { FieldSectionLabel("Recent captures") }
        items(captures, key = { it.id }) { item ->
            // Photo rows carry a live thumbnail and deep-link into the full-screen image viewer
            // (share / markup / delete / create). Deleting there removes this row too, so a
            // linked id never dangles. Video rows carry a camcorder glyph and deep-link into
            // video playback; record-backed rows (e.g. the seeded Frame inspection punch item)
            // deep-link into their tool's record detail.
            val linkedImage = item.relatedImageId?.let { ProjectImageRepository.find(it) }
            FieldWorkRow(
                title = item.title,
                subtitle = item.subtitle + " · " + formatTimestamp(item.timestampMillis),
                statusColor = if (item.kind == StreamKind.PHOTO || item.kind == StreamKind.VIDEO) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                leading = when {
                    linkedImage != null -> {
                        { ImageThumbnail(image = linkedImage) }
                    }

                    item.kind == StreamKind.VIDEO -> {
                        { VideoGlyph() }
                    }

                    else -> null
                },
                enabled = item.relatedFieldRecordId != null ||
                    item.relatedVideoId != null ||
                    linkedImage != null ||
                    item.relatedRecordId != null,
                onClick = {
                    when {
                        item.relatedFieldRecordId != null -> onOpenRecord(item.relatedFieldRecordId)
                        item.relatedVideoId != null -> onOpenVideo(item.relatedVideoId)
                        linkedImage != null -> onOpenImage(linkedImage.id)
                        // Daily logs and voice-dictated (non-blocking) issues open their log.
                        item.relatedRecordId != null -> onOpenVoiceLog(item.relatedRecordId)
                    }
                },
                modifier = Modifier.testTag("streamItem_${item.id}"),
            )
        }
    }
}

/**
 * Videos have no still frame to thumbnail (decoding one per list row is wasted work), so the
 * row leads with the same-size camcorder glyph instead — the playback screen shows the real
 * footage. Decorative: the row's own title/semantics describe the item.
 */
@Composable
private fun VideoGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StartMyDaySheetContent(
    crew: List<CrewMember>,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Start My Day", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FieldSectionLabel("Crew")
        crew.forEachIndexed { index, member ->
            FieldWorkRow(
                title = member.name,
                subtitle = "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
                statusColor = member.presence.badgeColor(),
                avatarName = member.name,
                avatarColor = AvatarPalette.colorAt(index),
                avatarPhotoRes = member.photoRes,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Weather",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            text = "Clear · 72°F · no delay",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        AppButton(text = "Confirm", onClick = onConfirm)
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
