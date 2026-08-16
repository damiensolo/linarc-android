package com.solomondesign.punchlist.ui.capture

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.solomondesign.punchlist.ui.demo.DemoProjectRepository
import com.solomondesign.punchlist.ui.designsystem.PunchlistButton
import com.solomondesign.punchlist.ui.designsystem.PunchlistButtonType

private val suggestedTags = listOf("Framing", "Area B", "Progress")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoCaptureScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showTags by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(suggestedTags.toSet()) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { shot ->
        if (shot != null) {
            bitmap = shot
            showTags = true
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Photo") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showTags) {
                Text(
                    text = "Capture a progress photo. Suggested tags are a scripted demo, not a vision model.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PunchlistButton(text = "Take photo", onClick = { camera.launch(null) })
                PunchlistButton(
                    text = "Use demo photo",
                    type = PunchlistButtonType.Secondary,
                    onClick = { showTags = true },
                )
            } else {
                bitmap?.let { shot ->
                    Image(
                        bitmap = shot.asImageBitmap(),
                        contentDescription = "Captured photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop,
                    )
                } ?: Text(
                    text = "Demo photo · Area B framing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Suggested tags", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestedTags.forEach { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                PunchlistButton(
                    text = "Save photo",
                    onClick = {
                        DemoProjectRepository.addPhoto(
                            title = "Progress photo",
                            subtitle = selectedTags.joinToString(" · ").ifBlank { "Area B" },
                            createIssue = false,
                        )
                        onDone()
                    },
                )
                PunchlistButton(
                    text = "Create issue?",
                    type = PunchlistButtonType.Secondary,
                    onClick = {
                        DemoProjectRepository.addPhoto(
                            title = "Progress photo",
                            subtitle = selectedTags.joinToString(" · ").ifBlank { "Area B" },
                            createIssue = true,
                        )
                        onDone()
                    },
                )
            }
        }
    }
}
