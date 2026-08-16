package com.solomondesign.app.ui.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.demo.PlanPin
import com.solomondesign.app.ui.designsystem.DesignTokens
import com.solomondesign.app.ui.designsystem.FieldPageHeader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(modifier: Modifier = Modifier) {
    val pins = DemoProjectRepository.pins.toList()
    var selectedPin by remember { mutableStateOf<PlanPin?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("planScreen"),
    ) {
        FieldPageHeader(
            title = "Plan",
            subtitle = "${DemoProjectRepository.PROJECT_NAME} · ${DemoProjectRepository.AREA} · Level 2",
        )
        AreaBSheet(
            pins = pins,
            onPinClick = { selectedPin = it },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .padding(bottom = 72.dp),
        )
    }

    selectedPin?.let { pin ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedPin = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(pin.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = pin.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AreaBSheet(
    pins: List<PlanPin>,
    onPinClick: (PlanPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }
    val pinTint = DesignTokens.ErrorAccent
    val photoTint = DesignTokens.PrimaryAccent

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .onSizeChanged {
                widthPx = it.width
                heightPx = it.height
            }
            .semantics { contentDescription = "Area B drawing" },
    ) {
        SitePlanCanvas(modifier = Modifier.fillMaxSize())
        if (widthPx > 0 && heightPx > 0) {
            Text(
                text = "Area B",
                style = MaterialTheme.typography.labelMedium,
                color = DesignTokens.PrimaryAccent,
                modifier = Modifier.offset {
                    IntOffset((0.62f * widthPx).roundToInt(), (0.16f * heightPx).roundToInt())
                },
            )
            Text(
                text = "Column 4",
                style = MaterialTheme.typography.labelSmall,
                color = DesignTokens.PrimaryAccent,
                modifier = Modifier.offset {
                    IntOffset((0.64f * widthPx).roundToInt(), (0.30f * heightPx).roundToInt())
                },
            )
            pins.forEach { pin ->
                val x = (pin.xFraction * widthPx).roundToInt()
                val y = (pin.yFraction * heightPx).roundToInt()
                val tint = if (pin.kind == PinKind.PHOTO) photoTint else pinTint
                IconButton(
                    onClick = { onPinClick(pin) },
                    modifier = Modifier
                        .offset { IntOffset(x - 48, y - 48) }
                        .size(48.dp)
                        .testTag("planPin_${pin.id}")
                        .semantics { contentDescription = pin.label },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SitePlanCanvas(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val wall = colors.outline
    val fill = colors.background
    val sheetFill = colors.surfaceContainer
    val areaFill = colors.surfaceContainerHigh
    val columnFill = colors.outline

    Canvas(modifier = modifier) {
        drawRect(color = fill)

        val pad = size.width * 0.06f
        val inner = Size(size.width - pad * 2, size.height - pad * 2)
        drawRect(color = sheetFill, topLeft = Offset(pad, pad), size = inner)
        drawRect(color = wall, topLeft = Offset(pad, pad), size = inner, style = Stroke(width = 4f))

        val midX = pad + inner.width * 0.48f
        drawLine(wall, Offset(midX, pad), Offset(midX, pad + inner.height), strokeWidth = 3f)

        val areaB = Size(inner.width * 0.46f, inner.height * 0.55f)
        val areaBOrigin = Offset(midX + 8f, pad + inner.height * 0.12f)
        drawRect(color = areaFill, topLeft = areaBOrigin, size = areaB)
        drawRect(color = wall, topLeft = areaBOrigin, size = areaB, style = Stroke(width = 2f))

        val colSize = Size(inner.width * 0.06f, inner.width * 0.06f)
        val col4 = Offset(
            pad + inner.width * 0.72f - colSize.width / 2f,
            pad + inner.height * 0.38f - colSize.height / 2f,
        )
        drawRect(color = columnFill, topLeft = col4, size = colSize)
        drawRect(color = wall, topLeft = col4, size = colSize, style = Stroke(width = 2f))
    }
}
