package com.solomondesign.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Tag picker for capture/create flows: the caller's suggested tags stay one-tap chips, and the
 * search field finds any existing project tag or mints a new one (added 2026-09-03 — before
 * this, suggestions were the only tags a capture could carry).
 *
 * - Selected tags and unselected suggestions render as [FilterChip]s; tapping toggles.
 * - Typing offers case-insensitive matches from [allTags] (minus what's selected) as tap-to-add
 *   chips, plus an "Add \"…\"" chip when the text names no existing tag. IME Done adds the
 *   typed text directly. Adding clears the field, ready for the next tag.
 *
 * The caller owns the selection state; [allTags] is the project-wide vocabulary (e.g.
 * `ProjectImageRepository.visibleTags()`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditor(
    selectedTags: Set<String>,
    suggestedTags: List<String>,
    allTags: List<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        // Case-insensitive dedupe: adding "framing" while "Framing" is selected is a no-op.
        if (selectedTags.none { it.equals(trimmed, ignoreCase = true) }) {
            onSelectedTagsChange(selectedTags + trimmed)
        }
        query = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Selected first (suggestion or custom), then the unselected suggestions — so a
            // custom tag is visible as a removable chip, not hidden behind the search field.
            (selectedTags + suggestedTags.filterNot { it in selectedTags }).forEach { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = {
                        onSelectedTagsChange(
                            if (tag in selectedTags) selectedTags - tag else selectedTags + tag,
                        )
                    },
                    label = { Text(tag) },
                    modifier = Modifier.testTag("tagChip_$tag"),
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search or add a tag") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addTag(query) }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tagSearchField"),
        )
        val matches = tagMatches(query, allTags, selectedTags)
        val newTag = newTagCandidate(query, allTags, selectedTags)
        if (matches.isNotEmpty() || newTag != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                newTag?.let { candidate ->
                    AssistChip(
                        onClick = { addTag(candidate) },
                        label = { Text("Add \"$candidate\"") },
                        leadingIcon = {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        },
                        modifier = Modifier.testTag("tagAddNew"),
                    )
                }
                matches.forEach { tag ->
                    AssistChip(
                        onClick = { addTag(tag) },
                        label = { Text(tag) },
                        modifier = Modifier.testTag("tagMatch_$tag"),
                    )
                }
            }
        }
    }
}
