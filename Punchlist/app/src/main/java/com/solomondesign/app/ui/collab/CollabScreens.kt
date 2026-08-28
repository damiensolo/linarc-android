package com.solomondesign.app.ui.collab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonSize
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldWorkRow
import com.solomondesign.app.ui.designsystem.PersonAvatar

/** Pattern B — topic list. Contextual FAB starts a new topic. */
@Composable
fun CollabTopicListScreen(
    onOpenTopic: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topics = CollabRepository.topics

    BrowseScaffold(
        title = "Collaboration",
        subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA}",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (topics.isEmpty()) {
            FieldEmptyState(
                message = "No topics yet. Tap + to start one.",
                modifier = Modifier.padding(padding),
            )
            return@BrowseScaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("collabTopicListScreen"),
        ) {
            items(topics, key = { it.id }) { topic ->
                FieldWorkRow(
                    title = topic.title,
                    subtitle = CollabRepository.lastMessagePreview(topic.id)
                        .ifBlank { topic.subtitle() },
                    statusColor = if (topic.unreadCount > 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    enabled = true,
                    onClick = { onOpenTopic(topic.id) },
                    modifier = Modifier.testTag("topicRow_${topic.id}"),
                    trailing = if (topic.unreadCount > 0) {
                        { Badge { Text(topic.unreadCount.toString()) } }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** Pattern B — one conversation, with an inline composer. No FAB here. */
@Composable
fun CollabTopicScreen(
    topicId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topic = CollabRepository.findTopic(topicId)

    LaunchedEffect(topicId) { CollabRepository.markRead(topicId) }

    if (topic == null) {
        BrowseScaffold(title = "Collaboration", onBack = onBack, modifier = modifier) { padding ->
            FieldEmptyState(
                message = "This topic is no longer available.",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    var draft by rememberSaveable { mutableStateOf("") }
    val messages = CollabRepository.messagesFor(topicId)

    BrowseScaffold(
        title = topic.title,
        subtitle = topic.subtitle(),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("collabTopicScreen"),
        ) {
            if (messages.isEmpty()) {
                FieldEmptyState(message = "No messages yet.", modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        CollabMessageBubble(
                            message = message,
                            isMine = message.authorId == CurrentUser.ID,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("collabComposer"),
                )
                AppButton(
                    text = "Send",
                    size = AppButtonSize.Small,
                    enabled = draft.isNotBlank(),
                    onClick = {
                        CollabRepository.postMessage(topicId, draft)
                        draft = ""
                    },
                    modifier = Modifier.testTag("collabSend"),
                )
            }
        }
    }
}

/**
 * Screen-scoped, not a design-system component: [FieldWorkRow] wraps `ListItem`, which cannot
 * express an alignment-varying bubble. Kept private until a second feature needs it.
 */
@Composable
private fun CollabMessageBubble(
    message: CollabMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    val author = DemoProjectRepository.crewMember(message.authorId)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            PersonAvatar(
                name = message.authorName,
                color = DemoProjectRepository.avatarColorFor(message.authorId),
                photoRes = author?.photoRes,
                size = 32.dp,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                if (!isMine) {
                    Text(
                        text = message.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (message.queued) {
                    Text(
                        text = "Queued · waiting for signal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
