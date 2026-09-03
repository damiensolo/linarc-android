package com.solomondesign.app.ui.voicenote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppBottomSheet
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType
import com.solomondesign.app.ui.designsystem.TaskFlowScaffold
import com.solomondesign.app.ui.images.ImageThumbnail
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.records.CameraAttachmentInbox
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.voicelog.audio.AndroidSpeechTranscriber
import com.solomondesign.app.ui.voicelog.audio.DictationController

private enum class VoiceNoteStage { RECORDING, REVIEW }

/**
 * Voice note — the Capture chip's bilingual create workflow (superseded the old Voice daily log
 * chip, which now demos from Settings → Demo). Live on-device dictation in English or Español
 * with a one-tap toggle plus best-effort auto-detection; the finished note gets the same floating
 * toolbar treatment as the photo viewer (share / translate / re-record / delete / create), and
 * Create prefills an Issue, Incident, or Punch item with the note text in whichever language is
 * showing at that moment (original or translation — never both merged). Review preselects the
 * category and Blocks work from keywords, can attach a photo sequentially, and can Add to an
 * existing task or record instead of creating a new one. Confident title + location + category
 * can Save from a short confirm sheet; Edit details opens the full form.
 *
 * The note itself is ephemeral by design (decided 2026-08-25): the records made from it are the
 * durable artifacts. No audio file is recorded — transcription-only sidesteps the known
 * mic-contention failure documented in the voice log's recording screen.
 */
@Composable
fun VoiceNoteScreen(
    onExit: () -> Unit,
    /**
     * Open the chosen record form seeded from this note. Always the full form: the seeds
     * carry no title (dictated text fills only the description — decided 2026-09-03), and a
     * record needs one, so the reporter types it there. The old confident short-confirm quick
     * save went with the derived title it depended on.
     */
    onCreateRecord: (RecordCategory, VoiceNoteSeeds) -> Unit,
    /** Stack the in-app camera; the saved photo returns here via [CameraAttachmentInbox]. */
    onAddPhoto: () -> Unit,
    /** Append this note onto an existing task or field record. */
    onAppendToExisting: (VoiceNoteMatch, VoiceNoteSeeds) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    if (!hasPermission) {
        VoiceNotePermissionScreen(
            denied = permissionDenied,
            onCancel = onExit,
            onRetry = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = modifier,
        )
        return
    }

    VoiceNoteFlow(
        onExit = onExit,
        onCreateRecord = onCreateRecord,
        onAddPhoto = onAddPhoto,
        onAppendToExisting = onAppendToExisting,
        modifier = modifier,
    )
}

@Composable
private fun VoiceNoteFlow(
    onExit: () -> Unit,
    onCreateRecord: (RecordCategory, VoiceNoteSeeds) -> Unit,
    onAddPhoto: () -> Unit,
    onAppendToExisting: (VoiceNoteMatch, VoiceNoteSeeds) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val transcriber = remember { AndroidSpeechTranscriber(context) }
    val dictation = remember {
        DictationController(transcriber).apply { setLanguage(VoiceNoteLanguage.ENGLISH.speechTag) }
    }
    val translator = remember { MlKitNoteTranslator() }

    var stage by remember { mutableStateOf(VoiceNoteStage.RECORDING) }
    var spokenLanguage by remember { mutableStateOf(VoiceNoteLanguage.ENGLISH) }
    var displayLanguage by remember { mutableStateOf(VoiceNoteLanguage.ENGLISH) }
    var autoSwitched by remember { mutableStateOf(false) }
    // Detection only ever looks at text captured after the last language switch, so a switch
    // (manual or automatic) can't be immediately re-triggered by the older other-language text.
    var textLengthAtSwitch by remember { mutableIntStateOf(0) }
    var noteText by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf<String?>(null) }
    var translating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var translateRetryTick by remember { mutableIntStateOf(0) }
    var photoImageIds by remember { mutableStateOf(VoiceNotePhotoInbox.takeAll()) }

    val existingCandidates = FieldTaskRepository.tasks.map {
        VoiceNoteMatch(it.id, it.title, VoiceNoteMatch.Kind.TASK)
    } + RecordRepository.records.map {
        VoiceNoteMatch(it.id, it.title, VoiceNoteMatch.Kind.RECORD)
    }

    // Both models start downloading immediately so review-time translation is instant. Offline
    // and never-downloaded is the one case that fails; translate() surfaces it.
    LaunchedEffect(Unit) { translator.prepare() }

    LaunchedEffect(CameraAttachmentInbox.pending) {
        CameraAttachmentInbox.take()?.let { id ->
            if (id !in photoImageIds) photoImageIds = photoImageIds + id
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            dictation.stop()
            transcriber.destroy()
            translator.close()
            CameraAttachmentInbox.reset()
        }
    }

    LaunchedEffect(stage) {
        if (stage == VoiceNoteStage.RECORDING) dictation.start()
    }

    // Best-effort auto-detect: watch fresh recognizer text; when it clearly reads as the other
    // language, flip the toggle and re-arm the recognizer in that language.
    LaunchedEffect(stage) {
        if (stage != VoiceNoteStage.RECORDING) return@LaunchedEffect
        snapshotFlow { "${dictation.transcript} ${dictation.partial}".trim() }
            .collect { combined ->
                val fresh = combined.drop(textLengthAtSwitch).trim()
                val detected = VoiceNoteLanguageDetector.detect(fresh)
                if (detected != null && detected != spokenLanguage) {
                    spokenLanguage = detected
                    displayLanguage = detected
                    autoSwitched = true
                    textLengthAtSwitch = combined.length
                    dictation.setLanguage(detected.speechTag)
                }
            }
    }

    LaunchedEffect(stage, translateRetryTick) {
        if (stage != VoiceNoteStage.REVIEW || translation != null || noteText.isBlank()) {
            return@LaunchedEffect
        }
        translating = true
        translationError = null
        val result = translator.translate(noteText, from = spokenLanguage)
        translating = false
        result
            .onSuccess { translation = it }
            .onFailure {
                translationError =
                    "Translation isn't ready — connect to the internet once so the language pack can download."
            }
    }

    when (stage) {
        VoiceNoteStage.RECORDING -> VoiceNoteRecordingStage(
            dictation = dictation,
            spokenLanguage = spokenLanguage,
            autoSwitched = autoSwitched,
            onSelectLanguage = { language ->
                if (language != spokenLanguage) {
                    spokenLanguage = language
                    displayLanguage = language
                    autoSwitched = false
                    textLengthAtSwitch = "${dictation.transcript} ${dictation.partial}".trim().length
                    dictation.setLanguage(language.speechTag)
                }
            },
            onCancel = onExit,
            onDone = { captured ->
                dictation.stop()
                noteText = captured
                stage = VoiceNoteStage.REVIEW
            },
            modifier = modifier,
        )

        VoiceNoteStage.REVIEW -> VoiceNoteReviewStage(
            noteText = noteText,
            translation = translation,
            translating = translating,
            translationError = translationError,
            spokenLanguage = spokenLanguage,
            displayLanguage = displayLanguage,
            onSelectDisplayLanguage = { displayLanguage = it },
            onRetryTranslate = { translateRetryTick++ },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        buildString {
                            // What you see is what you share: the currently displayed language.
                            append(
                                buildVoiceNoteSeeds(
                                    noteText,
                                    translation,
                                    spokenLanguage,
                                    displayLanguage,
                                    photoImageIds = photoImageIds,
                                    existingCandidates = existingCandidates,
                                ).description,
                            )
                            append("\n\nVoice note · ")
                            append(DemoProjectRepository.PROJECT_NAME)
                        },
                    )
                }
                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(send, "Share voice note"),
                    null,
                )
            },
            onRerecord = {
                dictation.reset()
                noteText = ""
                translation = null
                translationError = null
                translating = false
                autoSwitched = false
                textLengthAtSwitch = 0
                stage = VoiceNoteStage.RECORDING
            },
            onDelete = onExit,
            onCreateRecord = { category, seeds -> onCreateRecord(category, seeds) },
            onAppendToExisting = { match, seeds -> onAppendToExisting(match, seeds) },
            onAddPhoto = onAddPhoto,
            photoImageIds = photoImageIds,
            existingCandidates = existingCandidates,
            onClose = onExit,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceNoteRecordingStage(
    dictation: DictationController,
    spokenLanguage: VoiceNoteLanguage,
    autoSwitched: Boolean,
    onSelectLanguage: (VoiceNoteLanguage) -> Unit,
    onCancel: () -> Unit,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEmptyHint by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Voice note", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel voice note")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppButton(
                    text = "Cancel",
                    onClick = onCancel,
                    type = AppButtonType.Secondary,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "Done",
                    onClick = {
                        val captured = "${dictation.transcript} ${dictation.partial}".trim()
                        if (captured.isBlank()) {
                            showEmptyHint = true
                        } else {
                            onDone(captured)
                        }
                    },
                    type = AppButtonType.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voiceNoteDone"),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("voiceNoteRecording"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LanguageToggle(selected = spokenLanguage, onSelect = onSelectLanguage)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (autoSwitched) {
                    "Heard ${spokenLanguage.displayName} — switched automatically"
                } else {
                    "Auto-detects English or Español as you speak"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(24.dp))
            ListeningPill()
            Spacer(Modifier.height(24.dp))

            val liveText = "${dictation.transcript} ${dictation.partial}".trim()
            Text(
                text = if (liveText.isBlank()) "Listening…" else "“$liveText”",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("voiceNoteLiveText"),
            )

            if (showEmptyHint && liveText.isBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Nothing captured yet — say a few words, then tap Done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            dictation.errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceNoteReviewStage(
    noteText: String,
    translation: String?,
    translating: Boolean,
    translationError: String?,
    spokenLanguage: VoiceNoteLanguage,
    displayLanguage: VoiceNoteLanguage,
    onSelectDisplayLanguage: (VoiceNoteLanguage) -> Unit,
    onRetryTranslate: () -> Unit,
    onShare: () -> Unit,
    onRerecord: () -> Unit,
    onDelete: () -> Unit,
    onCreateRecord: (RecordCategory, VoiceNoteSeeds) -> Unit,
    onAppendToExisting: (VoiceNoteMatch, VoiceNoteSeeds) -> Unit,
    onAddPhoto: () -> Unit,
    photoImageIds: List<String>,
    existingCandidates: List<VoiceNoteMatch>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inferred = buildVoiceNoteSeeds(
        original = noteText,
        translation = translation,
        spokenLanguage = spokenLanguage,
        displayLanguage = displayLanguage,
        photoImageIds = photoImageIds,
        existingCandidates = existingCandidates,
    )

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToSheet by remember { mutableStateOf(false) }
    var categoryTouched by remember { mutableStateOf(false) }
    var blockingTouched by remember { mutableStateOf(false) }
    var existingTouched by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(inferred.category) }
    var selectedBlocksWork by remember { mutableStateOf(inferred.blocksWork) }
    var selectedExisting by remember { mutableStateOf(inferred.existing) }

    LaunchedEffect(inferred.category, inferred.blocksWork, inferred.existing?.id) {
        if (!categoryTouched) selectedCategory = inferred.category
        if (!blockingTouched) selectedBlocksWork = inferred.blocksWork
        if (!existingTouched) selectedExisting = inferred.existing
    }

    val seeds = inferred.copy(
        category = selectedCategory,
        blocksWork = selectedBlocksWork,
        blockingReason = if (selectedBlocksWork) {
            inferred.blockingReason.ifBlank { inferred.description }
        } else {
            ""
        },
        existing = selectedExisting,
    )
    // Always the full form: the seeds carry no title (dictated text fills only the
    // description, decided 2026-09-03), and a record requires one — so the confident
    // short-confirm quick save is gone until title inference is genuinely trustworthy.
    val createFromReview: () -> Unit = { onCreateRecord(selectedCategory, seeds) }

    TaskFlowScaffold(
        title = "Voice note",
        onClose = onClose,
        modifier = modifier,
        hasUnsavedChanges = true,
        discardTitle = "Discard this voice note?",
        discardMessage = "The transcript and its translation will be lost.",
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VoiceNoteToolbar(
                    onShare = onShare,
                    onTranslate = { onSelectDisplayLanguage(displayLanguage.other()) },
                    onRerecord = onRerecord,
                    onDelete = { showDeleteDialog = true },
                    onCreate = createFromReview,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("voiceNoteReview"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LanguageToggle(selected = displayLanguage, onSelect = onSelectDisplayLanguage)
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("Spoken in ${spokenLanguage.displayName}")
                    if (displayLanguage != spokenLanguage) {
                        append(" · showing ${displayLanguage.displayName} translation")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(24.dp))

            val showingTranslation = displayLanguage != spokenLanguage
            when {
                !showingTranslation -> NoteBody(text = noteText)
                translation != null -> NoteBody(text = translation)
                translating -> Text(
                    text = "Translating…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = translationError ?: "Couldn't translate this note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        text = "Retry translation",
                        onClick = onRetryTranslate,
                        type = AppButtonType.Secondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            VoiceNotePhotoRow(photoImageIds = photoImageIds, onAddPhoto = onAddPhoto)
            Spacer(Modifier.height(16.dp))

            Text(
                text = buildString {
                    append("Looks like ${selectedCategory.label.lowercase()}")
                    if (selectedBlocksWork) append(" · blocking")
                },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceNoteIntentHint"),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RecordCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            categoryTouched = true
                            selectedCategory = category
                        },
                        label = { Text(category.label) },
                        modifier = Modifier.testTag("voiceNoteCategory_${category.routeId}"),
                    )
                }
                FilterChip(
                    selected = selectedBlocksWork,
                    onClick = {
                        blockingTouched = true
                        selectedBlocksWork = !selectedBlocksWork
                    },
                    label = { Text("Blocks work") },
                    modifier = Modifier.testTag("voiceNoteBlocksWork"),
                )
            }

            Spacer(Modifier.height(16.dp))
            selectedExisting?.let { match ->
                Text(
                    text = "Matches ${match.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voiceNoteExistingHint"),
                )
                Spacer(Modifier.height(8.dp))
                AppButton(
                    text = "Add to this instead",
                    type = AppButtonType.Secondary,
                    onClick = { onAppendToExisting(match, seeds.copy(existing = match)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voiceNoteAddToSuggested"),
                )
                Spacer(Modifier.height(8.dp))
            }
            AppButton(
                text = "Add to…",
                type = AppButtonType.Secondary,
                onClick = { showAddToSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceNoteAddTo"),
            )
            Spacer(Modifier.height(12.dp))
            AppButton(
                text = "Create ${selectedCategory.label.lowercase()}",
                type = AppButtonType.Primary,
                onClick = createFromReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceNoteCreateConfirm"),
            )
        }
    }

    if (showAddToSheet) {
                VoiceNoteAddToSheet(
            candidates = listOfNotNull(selectedExisting) +
                existingCandidates.filter { it.id != selectedExisting?.id },
            onPick = { match ->
                existingTouched = true
                selectedExisting = match
                showAddToSheet = false
                onAppendToExisting(match, seeds.copy(existing = match))
            },
            onDismiss = { showAddToSheet = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this voice note?") },
            text = { Text("The transcript and its translation will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("noteDeleteConfirm"),
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            modifier = Modifier.testTag("noteDeleteDialog"),
        )
    }
}

@Composable
private fun VoiceNotePhotoRow(
    photoImageIds: List<String>,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photoImageIds.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photoImageIds.forEach { id ->
                    val image = ProjectImageRepository.find(id)
                    if (image != null) {
                        ImageThumbnail(image = image, size = 64.dp)
                    }
                }
            }
        }
        AppButton(
            text = if (photoImageIds.isEmpty()) "Add photo" else "Add another photo",
            type = AppButtonType.Secondary,
            onClick = onAddPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voiceNoteAddPhoto"),
        )
    }
}

@Composable
private fun VoiceNoteAddToSheet(
    candidates: List<VoiceNoteMatch>,
    onPick: (VoiceNoteMatch) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        title = "Add to existing",
        subtitle = "Comment on a task or record instead of creating a new one",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("voiceNoteAddToSheet"),
    ) { dismiss ->
        if (candidates.isEmpty()) {
            Text(
                text = "No tasks or records to add to yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        } else {
            candidates.forEach { match ->
                ListItem(
                    headlineContent = { Text(match.title) },
                    supportingContent = {
                        Text(if (match.kind == VoiceNoteMatch.Kind.TASK) "Field task" else "Record")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { dismiss { onPick(match) } }
                        .testTag("voiceNoteAddTo_${match.id}"),
                )
            }
        }
    }
}

@Composable
private fun NoteBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "“$text”",
        style = MaterialTheme.typography.bodyLarge,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        modifier = modifier.testTag("voiceNoteBody"),
    )
}

@Composable
private fun LanguageToggle(
    selected: VoiceNoteLanguage,
    onSelect: (VoiceNoteLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        VoiceNoteLanguage.entries.forEachIndexed { index, language ->
            SegmentedButton(
                selected = selected == language,
                onClick = { onSelect(language) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = VoiceNoteLanguage.entries.size,
                ),
                modifier = Modifier.testTag("voiceNoteLang_${language.name}"),
            ) {
                Text(language.displayName)
            }
        }
    }
}

/** Same floating pill as the photo viewer's toolbar, with note actions. */
@Composable
private fun VoiceNoteToolbar(
    onShare: () -> Unit,
    onTranslate: () -> Unit,
    onRerecord: () -> Unit,
    onDelete: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = modifier
            .padding(16.dp)
            .testTag("voiceNoteToolbar"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NoteToolbarAction(Icons.Filled.Share, "Share", "noteShare", onShare)
            NoteToolbarAction(Icons.Filled.Translate, "Translate", "noteTranslate", onTranslate)
            NoteToolbarAction(Icons.Filled.Replay, "Re-record", "noteRerecord", onRerecord)
            NoteToolbarAction(
                icon = Icons.Filled.DeleteOutline,
                label = "Delete",
                testTag = "noteDelete",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
            NoteToolbarAction(Icons.Filled.AddCircleOutline, "Create", "noteCreate", onCreate)
        }
    }
}

@Composable
private fun NoteToolbarAction(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.sizeIn(minWidth = 64.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun ListeningPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
            Text(
                text = "LISTENING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceNotePermissionScreen(
    denied: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (denied) "Microphone permission was denied" else "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Voice notes need the microphone to transcribe what you say.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            AppButton(text = "Allow Microphone Access", onClick = onRetry)
        }
    }
}
