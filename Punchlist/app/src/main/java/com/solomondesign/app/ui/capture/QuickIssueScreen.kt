package com.solomondesign.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold

private val locations = listOf("Area B", "Column 4", "Level 2")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickIssueScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    // A captured video can arrive with a drafted issue (see IssueDraftHolder) — its fields
    // seed the form and are then owned by the user like any typed value. take() clears the
    // hand-off, so a plain visit to this screen still starts blank.
    val draft = remember { IssueDraftHolder.take() }
    var title by remember { mutableStateOf(draft?.title.orEmpty()) }
    var location by remember {
        mutableStateOf(draft?.location?.takeIf { it in locations } ?: locations.first())
    }
    var note by remember { mutableStateOf(draft?.note.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    val submit = {
        DemoProjectRepository.addIssue(title = title.trim(), location = location, note = note.trim())
        onDone()
    }

    TaskFlowScaffold(
        title = "New issue",
        onClose = onDone,
        modifier = modifier,
        // Location defaults to a value, so only the free-text fields count as user edits.
        hasUnsavedChanges = title.isNotBlank() || note.isNotBlank(),
        onConfirm = submit,
        confirmLabel = "Save",
        confirmEnabled = title.isNotBlank(),
        discardMessage = "This issue hasn't been submitted and will be lost.",
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = location,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    locations.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                location = option
                                expanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            AppButton(
                text = "Submit issue",
                enabled = title.isNotBlank(),
                onClick = submit,
            )
        }
    }
}
