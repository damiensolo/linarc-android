package com.solomondesign.app.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.OutboxItem
import com.solomondesign.app.ui.demo.OutboxStatus
import com.solomondesign.app.ui.demo.statusLine
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.records.AttachmentKind
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.video.VideoRepository
import com.solomondesign.app.ui.voicelog.DailyLogRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The offline-first receipt drawer: every publish-style action in the prototype (records, time
 * cards, messages, photos, videos, daily logs, pin-comment batches) queues here instead of
 * blocking on connectivity. "Signal restored — send all" drains the queue one entry at a time
 * so the story is visible on stage; no bytes actually leave the device (a real sync engine is
 * an explicit prototype non-goal).
 *
 * Rows are receipts, not dead-ends: an entry linked to what it published (see
 * [OutboxItem]) taps through to the owning tool's detail, and photo-backed entries — a
 * published photo, or a record carrying a photo attachment — lead with its thumbnail, same as
 * the record lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboxScreen(
    onBack: () -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenLog: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenTimeCard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = DemoProjectRepository.outboxItems.toList()
    val queuedCount = DemoProjectRepository.queuedOutboxCount
    val sentCount = items.size - queuedCount
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Outbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Everything you capture commits on this device first and waits " +
                            "here for signal — a dead zone never blocks the work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = when {
                            queuedCount > 0 -> "$queuedCount queued · $sentCount sent"
                            items.isEmpty() -> "Nothing waiting"
                            else -> "All caught up · $sentCount sent"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag("outboxSummary"),
                    )
                }
            }
            item {
                AppButton(
                    text = if (sending) "Sending…" else "Signal restored — send all",
                    enabled = queuedCount > 0 && !sending,
                    onClick = {
                        scope.launch {
                            sending = true
                            // One entry per beat so the queue is seen draining, oldest first.
                            while (DemoProjectRepository.sendNextQueuedOutboxItem() != null) {
                                delay(350)
                            }
                            sending = false
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp)
                        .testTag("outboxSendAll"),
                )
            }
            items(items, key = { it.id }) { item ->
                // Resolve rather than trust: a linked target can be gone (e.g. the photo was
                // deleted from the Images grid), and a stale link must not render a tappable row.
                val image = item.relatedImageId?.let { ProjectImageRepository.find(it) }
                val record = item.relatedFieldRecordId?.let { RecordRepository.find(it) }
                val recordPhoto = record?.attachments
                    ?.firstOrNull { it.kind == AttachmentKind.PHOTO }
                    ?.let { ProjectImageRepository.find(it.ref) }
                val onOpen: (() -> Unit)? = when {
                    image != null -> {
                        { onOpenImage(image.id) }
                    }
                    record != null -> {
                        { onOpenRecord(record.id) }
                    }
                    item.relatedVideoId?.let(VideoRepository::find) != null -> {
                        { onOpenVideo(item.relatedVideoId!!) }
                    }
                    item.relatedLogId?.let(DailyLogRepository::find) != null -> {
                        { onOpenLog(item.relatedLogId!!) }
                    }
                    item.relatedTaskId?.let(FieldTaskRepository::find) != null -> {
                        { onOpenTask(item.relatedTaskId!!) }
                    }
                    item.relatedTopicId?.let(CollabRepository::findTopic) != null -> {
                        { onOpenTopic(item.relatedTopicId!!) }
                    }
                    item.relatedCrewMemberId
                        ?.let(DemoProjectRepository::crewMember) != null -> {
                        { onOpenTimeCard(item.relatedCrewMemberId!!) }
                    }
                    else -> null
                }
                val thumbnail = image ?: recordPhoto
                FieldWorkRow(
                    title = item.title,
                    subtitle = item.statusLine(),
                    statusColor = when (item.status) {
                        OutboxStatus.QUEUED -> MaterialTheme.colorScheme.tertiary
                        OutboxStatus.SENT -> MaterialTheme.colorScheme.primary
                    },
                    leading = thumbnail?.let { img -> { ImageThumbnail(image = img) } },
                    enabled = onOpen != null,
                    onClick = { onOpen?.invoke() },
                    modifier = Modifier.testTag("outboxRow_${item.id}"),
                )
            }
        }
    }
}
