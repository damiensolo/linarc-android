package com.solomondesign.app.ui.records

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pattern A — the one create form behind Issues, Incidents, and the Punch list; [category]
 * decides the title, the type options, and how `DemoProjectRepository.addRecord` publishes.
 *
 * Field state lives in [RecordDraft] (a singleton, not `remember`) because the Camera
 * attachment source navigates away to the real camera and back — see [RecordDraft]'s KDoc.
 * Reachable from tool FABs and quick-create, the camera's Issue chip, the dictated-video
 * hand-off, the photo review's "Save & create…", and the image viewer's Create action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordCreateScreen(
    category: RecordCategory,
    onClose: () -> Unit,
    onSaved: (RecordCategory) -> Unit,
    onAttachCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Runs once per back-stack entry (not again when returning from the camera): a caller that
    // staged the draft wins; otherwise this is a plain entry and the form starts itself blank.
    rememberSaveable {
        if (!RecordDraft.consumeStaged(category)) {
            RecordDraft.begin(category, System.currentTimeMillis())
        }
        true
    }

    // A photo captured via the Camera chip below lands here when the camera pops back.
    LaunchedEffect(CameraAttachmentInbox.pending) {
        CameraAttachmentInbox.take()?.let(RecordDraft::addPhoto)
    }

    var typeExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { RecordDraft.addFile(displayNameOf(context, it)) } }

    val save = {
        val now = System.currentTimeMillis()
        val savedCategory = RecordDraft.category
        DemoProjectRepository.addRecord(
            RecordDraft.toRecord(id = "rec-$now", nowMillis = now, authorName = CurrentUser.NAME),
        )
        RecordDraft.clear()
        onSaved(savedCategory)
    }

    TaskFlowScaffold(
        title = category.screenTitle,
        onClose = onClose,
        modifier = modifier,
        hasUnsavedChanges = RecordDraft.hasEdits,
        onConfirm = save,
        confirmLabel = "Save",
        confirmEnabled = RecordDraft.canSubmit,
        discardTitle = "Discard ${category.label.lowercase()}?",
        discardMessage = "This ${category.label.lowercase()} hasn't been submitted and will be lost.",
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("recordCreateScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = RecordDraft.title,
                onValueChange = { RecordDraft.title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recordTitleField"),
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = RecordDraft.type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("recordTypeField"),
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    category.typeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                // Applies the type's configured blocking default too.
                                RecordDraft.selectType(option)
                                typeExpanded = false
                            },
                            modifier = Modifier.testTag("recordType_$option"),
                        )
                    }
                }
            }

            RecordChoiceChips(
                label = "Severity",
                options = RecordSeverity.entries.map { it.label },
                selected = RecordDraft.severity.label,
                onSelect = { picked ->
                    RecordDraft.severity = RecordSeverity.entries.first { it.label == picked }
                },
                tagPrefix = "recordSeverity",
            )

            RecordChoiceChips(
                label = "Impact",
                options = RecordImpact.entries.map { it.label },
                selected = RecordDraft.impact.label,
                onSelect = { picked ->
                    RecordDraft.impact = RecordImpact.entries.first { it.label == picked }
                },
                tagPrefix = "recordImpact",
            )

            OutlinedTextField(
                value = RecordDraft.description,
                onValueChange = { RecordDraft.description = it },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recordDescriptionField"),
            )

            Text("Attachments", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        // Arm first: the camera deposits the saved photo's id on its way back.
                        CameraAttachmentInbox.arm()
                        onAttachCamera()
                    },
                    label = { Text("Camera") },
                    leadingIcon = {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("recordAttachCamera"),
                )
                AssistChip(
                    onClick = { showPhotoPicker = true },
                    label = { Text("Photos") },
                    leadingIcon = {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("recordAttachPhotos"),
                )
                AssistChip(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    label = { Text("Files") },
                    leadingIcon = {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("recordAttachFiles"),
                )
            }
            if (RecordDraft.attachments.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecordDraft.attachments.forEach { attachment ->
                        RecordAttachmentTile(
                            attachment = attachment,
                            onOpenPhoto = null,
                            onRemove = { RecordDraft.removeAttachment(attachment.id) },
                            modifier = Modifier.testTag("recordAttachment_${attachment.id}"),
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it },
            ) {
                OutlinedTextField(
                    value = RecordDraft.location,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("recordLocationField"),
                )
                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false },
                ) {
                    RECORD_LOCATIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                RecordDraft.location = option
                                locationExpanded = false
                            },
                        )
                    }
                }
            }

            // Issued ≠ blocked: logging a record never stops work by itself. Blocking is this
            // explicit toggle (or a type's configured default), scoped to a task/trade/package.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Blocks work?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (RecordDraft.blocksWork) {
                            "Blocks only the scoped task, area, or work package — not the crew."
                        } else {
                            "Off by default — submitting logs the ${category.label.lowercase()} " +
                                "without stopping work."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = RecordDraft.blocksWork,
                    onCheckedChange = { RecordDraft.setBlocking(it) },
                    modifier = Modifier.testTag("recordBlocksWork"),
                )
            }

            if (RecordDraft.blocksWork) {
                Text("Blocking details", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = RecordDraft.blockingReason,
                    onValueChange = { RecordDraft.blockingReason = it },
                    label = { Text("Blocking reason") },
                    supportingText = { Text("Required to submit a blocking record") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recordBlockingReason"),
                )

                RecordDropdownField(
                    label = "Affected trade",
                    value = RecordDraft.affectedTrade,
                    options = AFFECTED_TRADES,
                    onSelect = { RecordDraft.affectedTrade = it },
                    testTag = "recordTradeField",
                )
                RecordDropdownField(
                    label = "Affected task",
                    value = RecordDraft.affectedTask,
                    options = FieldTaskRepository.tasks.map { it.title },
                    onSelect = { RecordDraft.affectedTask = it },
                    testTag = "recordTaskField",
                )
                RecordDropdownField(
                    label = "Work package",
                    value = RecordDraft.workPackage,
                    options = WORK_PACKAGES,
                    onSelect = { RecordDraft.workPackage = it },
                    testTag = "recordWorkPackageField",
                )

                // Same disabled-field-plus-overlay construction as the event date below.
                Box {
                    OutlinedTextField(
                        value = RecordDraft.expectedResolutionMillis
                            ?.let(::formatEventDate) ?: "Not set",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Expected resolution") },
                        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClickLabel = "Pick expected resolution date") {
                                showResolutionPicker = true
                            }
                            .testTag("recordExpectedResolution"),
                    )
                }

                RecordDropdownField(
                    label = "Escalation contact",
                    value = DemoProjectRepository.crewMember(RecordDraft.escalationContactId)
                        ?.name.orEmpty(),
                    options = DemoProjectRepository.crew.map { it.name },
                    onSelect = { picked ->
                        RecordDraft.escalationContactId =
                            DemoProjectRepository.crew.first { it.name == picked }.id
                    },
                    testTag = "recordEscalationField",
                )
                RecordDropdownField(
                    label = "Resolution authority",
                    value = RecordDraft.resolutionAuthority,
                    options = RESOLUTION_AUTHORITIES,
                    onSelect = { RecordDraft.resolutionAuthority = it },
                    testTag = "recordAuthorityField",
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Acknowledgement required", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Affected crew must acknowledge before starting scoped work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = RecordDraft.acknowledgementRequired,
                        onCheckedChange = { RecordDraft.acknowledgementRequired = it },
                        modifier = Modifier.testTag("recordAckRequired"),
                    )
                }
            }

            // Disabled text field + transparent click target: readOnly fields swallow taps, and
            // this is the smallest reliable "field that opens a dialog" construction.
            Box {
                OutlinedTextField(
                    value = formatEventDate(RecordDraft.eventDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Event date") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClickLabel = "Pick event date") { showDatePicker = true }
                        .testTag("recordEventDate"),
                )
            }

            // Multi-select with search: typing filters the crew, tapping toggles membership
            // without closing the menu, and the picks land below as removable chips.
            var assigneeQuery by remember { mutableStateOf("") }
            var assigneesExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = assigneesExpanded,
                onExpandedChange = { assigneesExpanded = it },
            ) {
                OutlinedTextField(
                    value = assigneeQuery,
                    onValueChange = {
                        assigneeQuery = it
                        assigneesExpanded = true
                    },
                    label = { Text("Assignees") },
                    placeholder = { Text("Search crew") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(assigneesExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        .testTag("recordAssigneesField"),
                )
                ExposedDropdownMenu(
                    expanded = assigneesExpanded,
                    onDismissRequest = { assigneesExpanded = false },
                ) {
                    val matches = DemoProjectRepository.crew.filter { member ->
                        assigneeQuery.isBlank() ||
                            member.name.contains(assigneeQuery, ignoreCase = true) ||
                            member.trade.contains(assigneeQuery, ignoreCase = true)
                    }
                    if (matches.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No crew matches \"$assigneeQuery\"") },
                            onClick = {},
                            enabled = false,
                        )
                    }
                    matches.forEach { member ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(member.name)
                                    Text(
                                        text = member.trade,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            leadingIcon = {
                                Checkbox(
                                    checked = member.id in RecordDraft.assigneeIds,
                                    onCheckedChange = null,
                                )
                            },
                            onClick = { RecordDraft.toggleAssignee(member.id) },
                            modifier = Modifier.testTag("recordAssigneeOption_${member.id}"),
                        )
                    }
                }
            }
            if (RecordDraft.assigneeIds.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecordDraft.assigneeIds
                        .mapNotNull { DemoProjectRepository.crewMember(it) }
                        .forEach { member ->
                            InputChip(
                                selected = true,
                                onClick = { RecordDraft.toggleAssignee(member.id) },
                                label = { Text(member.name) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove ${member.name}",
                                        modifier = Modifier.size(InputChipDefaults.IconSize),
                                    )
                                },
                                modifier = Modifier.testTag("recordAssigneeChip_${member.id}"),
                            )
                        }
                }
            }

            AppButton(
                text = "Submit ${category.label.lowercase()}",
                enabled = RecordDraft.canSubmit,
                onClick = save,
                modifier = Modifier.testTag("recordSubmit"),
            )
        }
    }

    if (showPhotoPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("recordPhotoPicker"),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text("Attach a project photo", style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    ProjectImageRepository.images.forEach { image ->
                        ImageThumbnail(
                            image = image,
                            size = 64.dp,
                            modifier = Modifier
                                .clickable(onClickLabel = "Attach photo") {
                                    RecordDraft.addPhoto(image.id)
                                    showPhotoPicker = false
                                }
                                .semantics { contentDescription = image.title }
                                .testTag("recordPickPhoto_${image.id}"),
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = RecordDraft.eventDateMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { RecordDraft.eventDateMillis = it }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("recordEventDateOk"),
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showResolutionPicker) {
        val resolutionState = rememberDatePickerState(
            initialSelectedDateMillis = RecordDraft.expectedResolutionMillis
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showResolutionPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        resolutionState.selectedDateMillis?.let {
                            RecordDraft.expectedResolutionMillis = it
                        }
                        showResolutionPicker = false
                    },
                    modifier = Modifier.testTag("recordExpectedResolutionOk"),
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolutionPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = resolutionState)
        }
    }
}

/** Single-select chip row for short enumerations (severity, impact). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordChoiceChips(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    modifier = Modifier.testTag("${tagPrefix}_$option"),
                )
            }
        }
    }
}

/** Read-only dropdown field; used by the blocking-scope pickers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value.ifBlank { "Not set" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(testTag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("${testTag}_$option"),
                )
            }
        }
    }
}

private fun formatEventDate(millis: Long): String =
    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(millis))

/** Resolves a picked document's human name; the prototype stores only this, never the bytes. */
private fun displayNameOf(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment ?: "Document"
