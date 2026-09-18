package com.solomondesign.app.ui.collab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonSize
import com.solomondesign.app.ui.designsystem.DiscussionBubble
import com.solomondesign.app.ui.voicelog.audio.FieldDictationBroker
import com.solomondesign.app.ui.voicenote.SpeakableTextField

/**
 * The message composer every discussion uses: a compact Speak-enabled field plus a small Send
 * that stays disabled while the draft is blank. Sending stops any active dictation first (one
 * in-app take at a time) and clears the draft.
 */
@Composable
fun DiscussionComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    fieldTestTag: String,
    sendTestTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpeakableTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Message") },
            fieldTestTag = fieldTestTag,
            compact = true,
        )
        AppButton(
            text = "Send",
            size = AppButtonSize.Small,
            enabled = value.isNotBlank(),
            onClick = {
                FieldDictationBroker.stopActive()
                onSend()
            },
            modifier = Modifier.testTag(sendTestTag),
        )
    }
}

/**
 * An object's discussion, inline on its detail screen: the thread for [subject] (if any) as
 * bubbles, then the composer. The first message creates the topic in [CollabRepository] with
 * [topicTitle], [location] and [participantIds]; the object's screen owns the section heading
 * so it can match its own typography. [onOpenTopic] adds an "Open in Collaboration" link once
 * a thread exists — the same conversation, full screen.
 */
@Composable
fun DiscussionSection(
    subject: CollabSubject,
    topicTitle: String,
    location: String,
    participantIds: List<String>,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "discussion",
    onOpenTopic: ((String) -> Unit)? = null,
) {
    val topic = CollabRepository.topicFor(subject)
    val messages = topic?.let { CollabRepository.messagesFor(it.id) }.orEmpty()
    val me = CollabRepository.authorIdentity().id
    var draft by rememberSaveable(subject.id) { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}Section_${subject.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (messages.isEmpty()) {
            Text(
                text = "No discussion yet — start one for the team.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        messages.forEach { message ->
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
        DiscussionComposer(
            value = draft,
            onValueChange = { draft = it },
            onSend = {
                CollabRepository.postToSubject(
                    subject = subject,
                    title = topicTitle,
                    location = location,
                    participantIds = participantIds,
                    body = draft,
                )
                draft = ""
            },
            fieldTestTag = "${testTagPrefix}Composer",
            sendTestTag = "${testTagPrefix}Send",
        )
        if (topic != null && onOpenTopic != null) {
            TextButton(
                onClick = { onOpenTopic(topic.id) },
                modifier = Modifier.testTag("${testTagPrefix}OpenTopic"),
            ) {
                Text("Open in Collaboration")
            }
        }
    }
}
