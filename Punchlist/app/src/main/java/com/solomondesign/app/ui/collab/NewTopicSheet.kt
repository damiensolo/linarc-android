package com.solomondesign.app.ui.collab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppBottomSheet
import com.solomondesign.app.ui.designsystem.AppButton

private const val MAX_TITLE_LENGTH = 80

/** Pattern C — compact sheet for starting a topic. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTopicSheet(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var firstMessage by remember { mutableStateOf("") }
    val participants = remember { mutableStateListOf<String>() }

    val tooLong = title.length > MAX_TITLE_LENGTH
    val canCreate = title.isNotBlank() && !tooLong

    AppBottomSheet(
        title = "New topic",
        subtitle = DemoProjectRepository.AREA,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("newTopicSheet"),
    ) { dismissThen ->
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Topic") },
                isError = tooLong,
                supportingText = if (tooLong) {
                    { Text("${title.length} / $MAX_TITLE_LENGTH characters") }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("newTopicTitle"),
            )

            OutlinedTextField(
                value = firstMessage,
                onValueChange = { firstMessage = it },
                label = { Text("First message (optional)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )

            Text(
                text = "Who's involved",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                DemoProjectRepository.crew.forEach { member ->
                    val selected = member.id in participants
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) participants.remove(member.id) else participants.add(member.id)
                        },
                        label = { Text(member.name) },
                    )
                }
            }

            AppButton(
                text = "Create topic",
                enabled = canCreate,
                onClick = {
                    val id = CollabRepository.createTopic(
                        title = title,
                        firstMessage = firstMessage,
                        participantIds = participants.toList(),
                    ) ?: return@AppButton
                    dismissThen { onCreated(id) }
                },
                modifier = Modifier.padding(top = 20.dp).testTag("newTopicCreate"),
            )
        }
    }
}
