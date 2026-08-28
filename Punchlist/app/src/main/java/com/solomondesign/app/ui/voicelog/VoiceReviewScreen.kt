package com.solomondesign.app.ui.voicelog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.DesignTokens
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType

/**
 * Screen C from `voice-to-log-spec.md`: the parsed entities as tap-cards grouped by
 * type, each editable (hours stepper) or deletable, with a single Submit action.
 * [onBack] (top-bar arrow or the system back gesture) returns to the recording screen,
 * matching the spec's "Edit Transcript" affordance rather than exiting the flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceReviewScreen(
    laborCards: List<LaborCard>,
    materialCards: List<MaterialCard>,
    delayCards: List<DelayCard>,
    issueCards: List<IssueCard>,
    onBack: () -> Unit,
    onDeleteLabor: (String) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onDeleteDelay: (String) -> Unit,
    onDeleteIssue: (String) -> Unit,
    onEditLaborHours: (String, Double) -> Unit,
    onEditDelayHours: (String, Double) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var editingLabor by remember { mutableStateOf<LaborCard?>(null) }
    var editingDelay by remember { mutableStateOf<DelayCard?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Proposed Site Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to recording")
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                AppButton(text = "Submit", onClick = onSubmit, type = AppButtonType.Primary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (laborCards.isEmpty() && materialCards.isEmpty() && delayCards.isEmpty() && issueCards.isEmpty()) {
                Text(
                    text = "Nothing was detected in this recording. Go back and add more detail, " +
                        "or submit an empty log.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (laborCards.isNotEmpty()) {
                CardSection(icon = Icons.Filled.Person, iconTint = DesignTokens.PrimaryAccent, title = "Labor & Time Cards") {
                    laborCards.forEach { card ->
                        EntityRow(
                            primaryText = card.name,
                            secondaryText = card.trade,
                            valueText = "${formatHours(card.hours)} hrs",
                            onEdit = { editingLabor = card },
                            onDelete = { onDeleteLabor(card.id) },
                        )
                    }
                }
            }
            if (materialCards.isNotEmpty()) {
                CardSection(icon = Icons.Filled.Inventory2, title = "Materials Installed") {
                    materialCards.forEach { card ->
                        EntityRow(
                            primaryText = card.description,
                            secondaryText = "${card.quantity.toInt()} ${card.unit}",
                            onDelete = { onDeleteMaterial(card.id) },
                        )
                    }
                }
            }
            if (delayCards.isNotEmpty()) {
                CardSection(icon = Icons.Filled.Schedule, title = "Site Delays & Weather") {
                    delayCards.forEach { card ->
                        EntityRow(
                            primaryText = card.cause,
                            valueText = "${formatHours(card.hours)} hrs",
                            onEdit = { editingDelay = card },
                            onDelete = { onDeleteDelay(card.id) },
                        )
                    }
                }
            }
            if (issueCards.isNotEmpty()) {
                CardSection(
                    icon = Icons.Filled.PhotoCamera,
                    iconTint = DesignTokens.ErrorAccent,
                    title = "Automated Issues / Photos",
                ) {
                    issueCards.forEach { card ->
                        EntityRow(
                            primaryText = card.title,
                            secondaryText = card.location,
                            onDelete = { onDeleteIssue(card.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    editingLabor?.let { card ->
        HoursStepperDialog(
            subject = card.name,
            initialHours = card.hours,
            onDismiss = { editingLabor = null },
            onConfirm = { hours ->
                onEditLaborHours(card.id, hours)
                editingLabor = null
            },
        )
    }
    editingDelay?.let { card ->
        HoursStepperDialog(
            subject = card.cause,
            initialHours = card.hours,
            onDismiss = { editingDelay = null },
            onConfirm = { hours ->
                onEditDelayHours(card.id, hours)
                editingDelay = null
            },
        )
    }
}

@Composable
private fun CardSection(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Add")
            }
        }
        Spacer(Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = RoundedCornerShape(DesignTokens.CardCornerRadius),
        ) { Column(content = content) }
    }
}

@Composable
private fun EntityRow(
    primaryText: String,
    secondaryText: String? = null,
    valueText: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(primaryText) },
            supportingContent = secondaryText?.let { { Text(it) } },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    valueText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit $primaryText")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove $primaryText")
                    }
                }
            },
        )
        HorizontalDivider()
    }
}

/** Simplified stand-in for the spec's spinner-wheel hours picker — a +/- stepper. */
@Composable
private fun HoursStepperDialog(
    subject: String,
    initialHours: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var hours by remember { mutableDoubleStateOf(initialHours) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust hours") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { hours = (hours - 0.5).coerceAtLeast(0.0) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease hours")
                    }
                    Text(
                        text = "${formatHours(hours)} hrs",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    IconButton(onClick = { hours += 0.5 }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase hours")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Confirmation screen shown after Submit — the [VoiceLogUiState.Submitted] state. */
@Composable
fun VoiceLogSubmittedScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Logged to ${FakeVoiceLogData.PROJECT_NAME}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Labor, materials, delays, and issues have been added to today's record.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        AppButton(text = "Done", onClick = onDone)
    }
}

private fun formatHours(hours: Double): String = "%.1f".format(hours)
