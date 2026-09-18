package com.solomondesign.app.ui.collab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.BrowseScaffold
import com.solomondesign.app.ui.designsystem.DiscussionBubble
import com.solomondesign.app.ui.designsystem.FieldEmptyState
import com.solomondesign.app.ui.designsystem.FieldWorkRow

/**
 * Pattern B — the index of every conversation in the project: free-standing topics and the
 * discussions attached to records, tasks and other objects. Contextual FAB starts a new topic.
 */
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
                // A linked thread names its object first — that is what the reader is looking
                // for; a free-standing topic shows its latest words instead.
                FieldWorkRow(
                    title = topic.title,
                    subtitle = CollabRepository.subjectLabel(topic.subject)
                        ?: CollabRepository.lastMessagePreview(topic.id).ifBlank { topic.subtitle() },
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

/**
 * Pattern B — one conversation, with an inline composer. No FAB here. A thread that belongs to
 * an object leads with a row that opens that object ([onOpenSubject]); the list stays scrolled
 * to the newest message.
 */
@Composable
fun CollabTopicScreen(
    topicId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSubject: ((CollabSubject) -> Unit)? = null,
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
    val me = CollabRepository.authorIdentity().id
    val subjectLabel = CollabRepository.subjectLabel(topic.subject)
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    BrowseScaffold(
        title = topic.title,
        subtitle = subjectLabel ?: topic.subtitle(),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("collabTopicScreen"),
        ) {
            val subject = topic.subject
            if (subject != null && subjectLabel != null) {
                FieldWorkRow(
                    title = subjectLabel,
                    subtitle = "Open ${subject.kind.label.lowercase()}",
                    statusColor = MaterialTheme.colorScheme.outline,
                    enabled = onOpenSubject != null,
                    onClick = { onOpenSubject?.invoke(subject) },
                    modifier = Modifier.testTag("collabSubjectRow"),
                )
            }

            if (messages.isEmpty()) {
                FieldEmptyState(message = "No messages yet.", modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        val author = DemoProjectRepository.crewMember(message.authorId)
                        DiscussionBubble(
                            authorName = message.authorName,
                            body = message.body,
                            isMine = message.authorId == me,
                            avatarColor = DemoProjectRepository.avatarColorFor(message.authorId),
                            avatarPhotoRes = author?.photoRes,
                            queued = message.queued,
                        )
                    }
                }
            }

            DiscussionComposer(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    CollabRepository.postMessage(topicId, draft)
                    draft = ""
                },
                fieldTestTag = "collabComposer",
                sendTestTag = "collabSend",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
