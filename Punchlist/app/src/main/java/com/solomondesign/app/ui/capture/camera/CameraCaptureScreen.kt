package com.solomondesign.app.ui.capture.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solomondesign.app.ui.capture.IssueDraft
import com.solomondesign.app.ui.capture.IssueDraftHolder
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.images.CapturedMediaStore
import com.solomondesign.app.ui.markup.MarkupEditorScreen
import com.solomondesign.app.ui.records.CameraAttachmentInbox
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordDraft
import com.solomondesign.app.ui.video.formatVideoDuration
import com.solomondesign.app.ui.voicenote.VoiceNotePhotoInbox
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Camera controls sit on a black surface with white iconography regardless of app theme — the
 * established Android camera convention (a light chrome over a live viewfinder is unreadable).
 * The deliberate hard-coded colors below are scoped to this file only.
 */
private val ViewfinderScrim = Color.Black.copy(alpha = 0.45f)
private val OnViewfinder = Color.White

/** Record affordance red — same camera-chrome hardcoded-color exception as the scrim above. */
private val RecordRed = Color(0xFFE53935)

/** Video clips are short walk-around takes; the recorder hard-stops at this cap. */
private const val MAX_VIDEO_DURATION_MILLIS = 90_000L
private const val MAX_VIDEO_DURATION_SECONDS = 90
private const val NANOS_PER_SECOND = 1_000_000_000L

private sealed interface CaptureStep {
    data object Viewfinder : CaptureStep

    /** Annotation right after the shutter — entered only when the markup chip was toggled on. */
    data class Markup(val absolutePath: String, val bitmap: Bitmap) : CaptureStep

    /** [annotated] rides along so Save can flag the published photo as marked up. */
    data class Review(
        val absolutePath: String,
        val bitmap: Bitmap,
        val annotated: Boolean = false,
    ) : CaptureStep

    /** "Describe what you saw" dictation, entered when a video recording finalizes. */
    data class Describe(val videoPath: String, val durationSeconds: Int) : CaptureStep

    /** Review: playback + parsed title/description + file-an-issue switch. */
    data class VideoReview(
        val videoPath: String,
        val durationSeconds: Int,
        val transcript: String,
    ) : CaptureStep
}

/**
 * The global Capture surface, opened by the bottom-bar Capture action: a full-screen CameraX
 * viewfinder (tap-to-focus and pinch-zoom come with [PreviewView] + controller for free) with
 * shutter, lens flip, torch, and a Photo|Video mode rail. A successful shot moves to an in-place
 * review step where the photo gets a title/description/tags and publishes through
 * [DemoProjectRepository.addPhoto] — landing on Today, as a Plan pin, and in the Images grid,
 * like every capture must.
 *
 * Video mode records a capped clip, then walks through "Describe what you saw"
 * dictation and a review, publishing via [DemoProjectRepository.addCapturedVideo]
 * (Today + Plan pin) — optionally continuing into a prefilled Quick issue.
 *
 * Voice note (bilingual voice capture) and Quick issue stay one tap away via quick chips, so
 * nothing the old Capture sheet offered was lost — and both work even when camera permission is
 * denied. (The chip opened the Voice daily log until 2026-08-25; that flow now demos from
 * Settings → Demo.)
 */
@Composable
fun CameraCaptureScreen(
    onClose: () -> Unit,
    /** Carries the published photo's id so a record form beneath can attach what it asked for. */
    onPhotoSaved: (photoId: String) -> Unit,
    onVideoSaved: () -> Unit,
    onVoiceNote: () -> Unit,
    onQuickIssue: () -> Unit,
    /** Photo review's "Save & create…": the photo is saved, then this opens the staged form. */
    onCreateRecord: (RecordCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
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
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Sound on video is best-effort: the clip must still record (silently) when denied.
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    val holder = remember { CameraStateHolder() }
    val state = holder.state
    var step by remember { mutableStateOf<CaptureStep>(CaptureStep.Viewfinder) }
    var initialized by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val controller = remember { LifecycleCameraController(context) }
    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            controller.unbind()
        }
    }

    LaunchedEffect(state.mode) {
        if (state.mode == CameraMode.VIDEO && !hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // One capture use case at a time: swapping on mode change sidesteps per-device limits on
    // combined photo+video configurations, and photo mode stays exactly what it was.
    LaunchedEffect(initialized, state.mode) {
        if (!initialized) return@LaunchedEffect
        controller.setEnabledUseCases(
            if (state.mode == CameraMode.VIDEO) {
                CameraController.VIDEO_CAPTURE
            } else {
                CameraController.IMAGE_CAPTURE
            },
        )
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        controller.bindToLifecycle(lifecycleOwner)
        controller.initializationFuture.addListener(
            {
                runCatching {
                    controller.initializationFuture.get()
                    val rear = runCatching { controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }
                        .getOrDefault(false)
                    val front = runCatching { controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }
                        .getOrDefault(false)
                    if (!rear && !front) {
                        initError = "No camera was found on this device."
                    } else {
                        holder.setAvailableLenses(hasRear = rear, hasFront = front)
                        initialized = true
                    }
                }.onFailure {
                    initError = it.message ?: "The camera couldn't start."
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    // Apply the lens choice, then re-resolve flash-unit availability. CameraInfo lags the
    // selector swap while the new camera opens, so poll briefly instead of reading once.
    LaunchedEffect(initialized, state.lens) {
        if (!initialized) return@LaunchedEffect
        controller.cameraSelector = if (state.lens == CameraLens.REAR) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        repeat(10) {
            delay(120)
            controller.cameraInfo?.let { holder.setTorchAvailable(it.hasFlashUnit()) }
        }
    }

    LaunchedEffect(state.torchOn) {
        if (initialized) controller.enableTorch(state.torchOn)
    }

    fun takePhoto() {
        if (!hasPermission || !initialized || initError != null) return
        if (!holder.beginCapture()) return
        captureError = null
        flashTick++
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                val bytes: ByteArray
                                val rotation: Int
                                try {
                                    bytes = PhotoProcessor.jpegBytesOf(image)
                                    rotation = image.imageInfo.rotationDegrees
                                } finally {
                                    image.close()
                                }
                                val bitmap = PhotoProcessor.toUprightBitmap(bytes, rotation)
                                val file = CapturedMediaStore.newPhotoFile(context)
                                PhotoProcessor.writeJpeg(bitmap, file)
                                file.absolutePath to bitmap
                            }
                        }
                        holder.endCapture()
                        result
                            .onSuccess { (path, bitmap) ->
                                step = if (holder.state.markupAfterCapture) {
                                    CaptureStep.Markup(path, bitmap)
                                } else {
                                    CaptureStep.Review(path, bitmap)
                                }
                            }
                            .onFailure { captureError = it.message ?: "Couldn't save the photo." }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    holder.endCapture()
                    captureError = exception.message ?: "Couldn't take the photo."
                }
            },
        )
    }

    fun startVideoRecording() {
        if (!hasPermission || !initialized || initError != null) return
        if (!holder.beginRecording()) return
        captureError = null
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val file = CapturedMediaStore.newVideoFile(context)
        activeRecording = controller.startRecording(
            FileOutputOptions.Builder(file)
                .setDurationLimitMillis(MAX_VIDEO_DURATION_MILLIS)
                .build(),
            audioConfigFor(context),
            ContextCompat.getMainExecutor(context),
        ) { event ->
            when (event) {
                is VideoRecordEvent.Status -> holder.setRecordingElapsed(
                    (event.recordingStats.recordedDurationNanos / NANOS_PER_SECOND).toInt(),
                )

                is VideoRecordEvent.Finalize -> {
                    val seconds =
                        (event.recordingStats.recordedDurationNanos / NANOS_PER_SECOND).toInt()
                    activeRecording = null
                    holder.endRecording()
                    // Hitting the duration cap is a successful capture — the cap is the feature,
                    // not a failure mode.
                    val kept = !event.hasError() ||
                        event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                    if (kept) {
                        step = CaptureStep.Describe(file.absolutePath, seconds)
                    } else {
                        CapturedMediaStore.delete(file.absolutePath)
                        captureError = "Couldn't record the video."
                    }
                }

                else -> Unit
            }
        }
    }

    when (val current = step) {
        CaptureStep.Viewfinder -> ViewfinderContent(
            state = state,
            hasPermission = hasPermission,
            permissionDenied = permissionDenied,
            initialized = initialized,
            initError = initError,
            captureError = captureError,
            controller = controller,
            flashTick = flashTick,
            hasMicPermission = hasMicPermission,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose,
            onToggleTorch = holder::toggleTorch,
            onToggleLens = holder::toggleLens,
            onSelectMode = holder::selectMode,
            onShutter = {
                when {
                    state.mode == CameraMode.PHOTO -> takePhoto()
                    state.isRecording -> activeRecording?.stop()
                    else -> startVideoRecording()
                }
            },
            onToggleMarkup = holder::toggleMarkupAfterCapture,
            onVoiceNote = onVoiceNote,
            onQuickIssue = onQuickIssue,
            modifier = modifier,
        )

        // The markup toggle promised annotation right after the shutter; the editor bakes the
        // annotations into the capture file so review and every downstream consumer see them.
        is CaptureStep.Markup -> MarkupEditorScreen(
            photo = current.bitmap,
            onCancel = {
                CapturedMediaStore.delete(current.absolutePath)
                step = CaptureStep.Viewfinder
            },
            onDone = { result, hasMarkup ->
                if (hasMarkup) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            PhotoProcessor.writeJpeg(result, File(current.absolutePath))
                        }
                        step = CaptureStep.Review(current.absolutePath, result, annotated = true)
                    }
                } else {
                    step = CaptureStep.Review(current.absolutePath, current.bitmap)
                }
            },
            doneLabel = "Continue",
            modifier = modifier,
        )

        is CaptureStep.Review -> PhotoReviewScreen(
            photo = current.bitmap,
            onRetake = {
                CapturedMediaStore.delete(current.absolutePath)
                step = CaptureStep.Viewfinder
            },
            onSave = { title, description, tags, createRecord, continueToVoice ->
                val photoId = DemoProjectRepository.addPhoto(
                    title = title,
                    subtitle = description.ifBlank {
                        tags.joinToString(" · ").ifBlank { DemoProjectRepository.AREA }
                    },
                    createIssue = false,
                    filePath = current.absolutePath,
                    tags = tags,
                    hasMarkup = current.annotated,
                )
                when {
                    createRecord != null -> {
                        // The photo is published either way; "Save & create…" continues into the
                        // chosen record form with it already attached and the fields seeded.
                        RecordDraft.begin(
                            createRecord,
                            System.currentTimeMillis(),
                            seedTitle = title,
                            seedDescription = description,
                            seedPhotoImageIds = listOf(photoId),
                        )
                        onCreateRecord(createRecord)
                    }
                    continueToVoice -> {
                        VoiceNotePhotoInbox.deposit(photoId)
                        onVoiceNote()
                    }
                    else -> onPhotoSaved(photoId)
                }
            },
            // Review's own markup entry: bake into the same capture file so the published photo
            // carries the annotations.
            onAnnotated = { result ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        PhotoProcessor.writeJpeg(result, File(current.absolutePath))
                    }
                    step = CaptureStep.Review(current.absolutePath, result, annotated = true)
                }
            },
            allowContinueToVoice = !CameraAttachmentInbox.isArmed(),
            modifier = modifier,
        )

        is CaptureStep.Describe -> VideoDescribeScreen(
            onCancel = {
                CapturedMediaStore.delete(current.videoPath)
                step = CaptureStep.Viewfinder
            },
            onDone = { transcript ->
                step = CaptureStep.VideoReview(
                    videoPath = current.videoPath,
                    durationSeconds = current.durationSeconds,
                    transcript = transcript,
                )
            },
            modifier = modifier,
        )

        is CaptureStep.VideoReview -> VideoReviewScreen(
            videoPath = current.videoPath,
            durationSeconds = current.durationSeconds,
            transcript = current.transcript,
            onRetake = {
                CapturedMediaStore.delete(current.videoPath)
                step = CaptureStep.Viewfinder
            },
            onSave = { title, note, location, fileIssue ->
                DemoProjectRepository.addCapturedVideo(
                    title = title,
                    note = note,
                    videoPath = current.videoPath,
                    transcript = current.transcript,
                    durationSeconds = current.durationSeconds,
                )
                if (fileIssue) {
                    // The clip is already published; the issue form opens prefilled with the
                    // dictated note and location — but never a title (decided 2026-09-03):
                    // transcript-derived titles were junk the reporter had to delete, so the
                    // reporter names the issue on the form.
                    IssueDraftHolder.set(
                        IssueDraft(
                            title = "",
                            location = location,
                            note = note,
                        ),
                    )
                    onQuickIssue()
                } else {
                    onVideoSaved()
                }
            },
            modifier = modifier,
        )
    }
}

/** Silent-video fallback: recording must still work when the mic permission is denied. */
private fun audioConfigFor(context: Context): AudioConfig =
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        AudioConfig.create(true)
    } else {
        AudioConfig.AUDIO_DISABLED
    }

@Composable
private fun ViewfinderContent(
    state: CameraUiState,
    hasPermission: Boolean,
    permissionDenied: Boolean,
    initialized: Boolean,
    initError: String?,
    captureError: String?,
    controller: LifecycleCameraController,
    flashTick: Int,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLens: () -> Unit,
    onSelectMode: (CameraMode) -> Unit,
    onShutter: () -> Unit,
    onToggleMarkup: () -> Unit,
    onVoiceNote: () -> Unit,
    onQuickIssue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewActive = hasPermission && initError == null
    // Disabled until the camera has actually bound: a dead-feeling shutter is worse than a
    // briefly greyed one, and takePhoto() would drop the tap anyway. While recording it stays
    // enabled — it's the stop button.
    val shutterEnabled = previewActive && initialized && !state.isCapturing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cameraCaptureScreen"),
    ) {
        when {
            !hasPermission -> CameraStatePane(
                icon = Icons.Filled.PhotoCamera,
                title = if (permissionDenied) "Camera permission was denied" else "Camera access needed",
                body = "Capture uses the camera to document work, conditions, and issues on site.",
                actionLabel = "Allow camera access",
                onAction = onRequestPermission,
                paneTestTag = "cameraPermissionPane",
            )

            initError != null -> CameraStatePane(
                icon = Icons.Filled.ErrorOutline,
                title = "Camera isn't available",
                body = initError,
                paneTestTag = "cameraUnavailablePane",
            )

            else -> AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        this.controller = controller
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Shutter feedback: a brief white pulse over the frame on every press.
        val flashAlpha = remember { Animatable(0f) }
        LaunchedEffect(flashTick) {
            if (flashTick > 0) {
                flashAlpha.snapTo(0.8f)
                flashAlpha.animateTo(0f, tween(durationMillis = 240))
            }
        }
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value)),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No Close mid-recording: stop is the only way out, so a recording can't be
            // abandoned by accident. The torch stays — lighting the scene is why it exists.
            if (!state.isRecording) {
                ViewfinderIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close camera",
                    testTag = "cameraClose",
                    onClick = onClose,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (previewActive) {
                ViewfinderIconButton(
                    icon = if (state.torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = if (state.torchOn) "Turn flashlight off" else "Turn flashlight on",
                    testTag = "cameraTorch",
                    onClick = onToggleTorch,
                    enabled = state.torchAvailable,
                )
                if (state.canFlip) {
                    ViewfinderIconButton(
                        icon = Icons.Filled.Cameraswitch,
                        contentDescription = if (state.lens == CameraLens.REAR) {
                            "Switch to front camera"
                        } else {
                            "Switch to rear camera"
                        },
                        testTag = "cameraFlip",
                        onClick = onToggleLens,
                    )
                }
            }
        }

        if (state.isRecording) {
            Surface(
                color = ViewfinderScrim,
                contentColor = OnViewfinder,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .testTag("cameraRecordingTimer"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(modifier = Modifier.size(8.dp).background(RecordRed, CircleShape))
                    Text(
                        text = "${formatVideoDuration(state.recordedSeconds)} / " +
                            formatVideoDuration(MAX_VIDEO_DURATION_SECONDS),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (captureError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("cameraCaptureError"),
                ) {
                    Text(
                        text = captureError,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            // Everything but the stop button and torch clears out while recording: nothing here
            // may swap flows or use cases under an active recording.
            if (!state.isRecording) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Markup leads the row: it changes what the shutter does, so it reads before
                    // the flow-switching chips. Toggled on, the shot lands in the editor
                    // pre-review. It's a photo concept, so video mode drops it.
                    if (state.mode == CameraMode.PHOTO) {
                        ViewfinderChip(
                            text = if (state.markupAfterCapture) "Markup on" else "Markup",
                            icon = Icons.Filled.Draw,
                            onClick = onToggleMarkup,
                            testTag = "cameraMarkupToggle",
                            selected = state.markupAfterCapture,
                        )
                    }
                    ViewfinderChip(
                        text = "Voice note",
                        icon = Icons.Filled.Mic,
                        onClick = onVoiceNote,
                        testTag = "cameraQuickVoice",
                    )
                    ViewfinderChip(
                        text = "Issue",
                        icon = Icons.Filled.ReportProblem,
                        onClick = onQuickIssue,
                        testTag = "cameraQuickIssue",
                    )
                }
                if (state.mode == CameraMode.VIDEO && !hasMicPermission) {
                    Text(
                        text = "No microphone access — video records without sound.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnViewfinder.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton(
                        text = "Photo",
                        selected = state.mode == CameraMode.PHOTO,
                        testTag = "cameraModePhoto",
                        onClick = { onSelectMode(CameraMode.PHOTO) },
                    )
                    ModeButton(
                        text = "Video",
                        selected = state.mode == CameraMode.VIDEO,
                        testTag = "cameraModeVideo",
                        onClick = { onSelectMode(CameraMode.VIDEO) },
                    )
                }
            }
            ShutterButton(
                enabled = shutterEnabled,
                mode = state.mode,
                isRecording = state.isRecording,
                onClick = onShutter,
            )
        }
    }
}

/** Photo|Video selector in the classic above-the-shutter position. */
@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) OnViewfinder else ViewfinderScrim,
        contentColor = if (selected) Color.Black else OnViewfinder,
        modifier = Modifier
            .testTag(testTag)
            .semantics {
                contentDescription = "$text mode"
                this.selected = selected
            },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

/** The one full-screen state pane (permission / unavailable) shown in place of the preview. */
@Composable
private fun CameraStatePane(
    icon: ImageVector,
    title: String,
    body: String,
    paneTestTag: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag(paneTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnViewfinder.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnViewfinder,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OnViewfinder.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun ViewfinderIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(4.dp)
            .background(ViewfinderScrim, CircleShape)
            .testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) OnViewfinder else OnViewfinder.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun ViewfinderChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            // Selected inverts to white-on-black so the active toggle reads at a glance.
            containerColor = if (selected) OnViewfinder else ViewfinderScrim,
            labelColor = if (selected) Color.Black else OnViewfinder,
            leadingIconContentColor = if (selected) Color.Black else OnViewfinder,
            disabledContainerColor = ViewfinderScrim.copy(alpha = 0.25f),
            disabledLabelColor = OnViewfinder.copy(alpha = 0.5f),
            disabledLeadingIconContentColor = OnViewfinder.copy(alpha = 0.5f),
        ),
        border = null,
        modifier = Modifier.testTag(testTag),
    )
}

/**
 * The classic ring shutter; disabled while a photo capture is writing. The inner fill states
 * the mode: white disc = photo, red disc = ready to record, red square = recording (tap stops).
 */
@Composable
private fun ShutterButton(
    enabled: Boolean,
    mode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val description = when {
        mode == CameraMode.PHOTO -> "Take photo"
        isRecording -> "Stop recording"
        else -> "Start recording"
    }
    Box(
        modifier = Modifier
            .size(76.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .border(width = 4.dp, color = OnViewfinder, shape = CircleShape)
            .padding(7.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag("cameraShutter"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mode == CameraMode.PHOTO -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OnViewfinder, CircleShape),
            )

            isRecording -> Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(RecordRed, RoundedCornerShape(6.dp)),
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RecordRed, CircleShape),
            )
        }
    }
}
