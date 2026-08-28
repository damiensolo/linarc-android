package com.solomondesign.app.ui.timecards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppBottomSheet
import com.solomondesign.app.ui.designsystem.AppButton

/**
 * Pattern C — compact sheet for adding a time entry.
 *
 * Validation comes from [TimeEntryDraft.validate], which is pure and unit-tested; this composable
 * only decides when to surface it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTimeEntrySheet(
    defaultCrewMemberId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var draft by remember {
        mutableStateOf(
            TimeEntryDraft(
                crewMemberId = defaultCrewMemberId ?: DemoProjectRepository.crew.firstOrNull()?.id,
                costCode = COST_CODES.first(),
            ),
        )
    }
    var showErrors by remember { mutableStateOf(false) }
    var crewExpanded by remember { mutableStateOf(false) }
    var codeExpanded by remember { mutableStateOf(false) }

    val errors = draft.validate()
    val hoursError = showErrors && errors.any {
        it == TimeEntryError.HoursRequired ||
            it == TimeEntryError.HoursNotANumber ||
            it == TimeEntryError.HoursOutOfRange
    }

    AppBottomSheet(
        title = "New time entry",
        subtitle = "Today · ${DemoProjectRepository.AREA}",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("newTimeEntrySheet"),
    ) { dismissThen ->
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            val selectedMember = draft.crewMemberId?.let(DemoProjectRepository::crewMember)
            ExposedDropdownMenuBox(
                expanded = crewExpanded,
                onExpandedChange = { crewExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedMember?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Crew member") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(crewExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("timeEntryCrewField"),
                )
                ExposedDropdownMenu(
                    expanded = crewExpanded,
                    onDismissRequest = { crewExpanded = false },
                ) {
                    DemoProjectRepository.crew.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member.name) },
                            onClick = {
                                draft = draft.copy(crewMemberId = member.id)
                                crewExpanded = false
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = codeExpanded,
                onExpandedChange = { codeExpanded = it },
                modifier = Modifier.padding(top = 12.dp),
            ) {
                OutlinedTextField(
                    value = draft.costCode.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cost code") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(codeExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = codeExpanded,
                    onDismissRequest = { codeExpanded = false },
                ) {
                    COST_CODES.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                draft = draft.copy(costCode = code)
                                codeExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.hoursText,
                onValueChange = { draft = draft.copy(hoursText = it) },
                label = { Text("Hours") },
                isError = hoursError,
                supportingText = if (hoursError) {
                    { Text(errors.first().message()) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("timeEntryHoursField"),
            )

            OutlinedTextField(
                value = draft.overtimeText,
                onValueChange = { draft = draft.copy(overtimeText = it) },
                label = { Text("Overtime (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )

            AppButton(
                text = "Save entry",
                enabled = draft.isValid,
                onClick = {
                    showErrors = true
                    val entry = draft.toEntry(
                        id = "te-new-${System.currentTimeMillis()}",
                        dateLabel = "Mon, Aug 18",
                    ) ?: return@AppButton
                    TimeCardRepository.addEntry(entry)
                    dismissThen(onSaved)
                },
                modifier = Modifier.padding(top = 20.dp).testTag("timeEntrySave"),
            )
        }
    }
}
