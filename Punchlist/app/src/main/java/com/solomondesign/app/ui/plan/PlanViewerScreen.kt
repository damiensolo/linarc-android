package com.solomondesign.app.ui.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.demo.PlanPin
import com.solomondesign.app.ui.designsystem.DesignTokens
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Full-screen plan viewer (Pattern A): swipe or use the bottom-right arrows to move between
 * sheets, or jump anywhere via the sheet selector in the top bar. Pinch/double-tap zooms.
 *
 * Markup itself is demo-only in this build — the toolbar shows the intended tool set, and demo
 * markup plus live [PlanPin]s render on top of the drawings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanViewerScreen(
    sheetId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheets = PlanSheetRepository.sheets
    val startIndex = PlanSheetRepository.indexOf(sheetId).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex) { sheets.size }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPin by remember { mutableStateOf<PlanPin?>(null) }

    Scaffold(
        modifier = modifier.testTag("planViewerScreen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("planViewerBack")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    SheetTitleSelector(
                        sheets = sheets,
                        currentIndex = pagerState.currentPage,
                        onSelect = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                },
            )
        },
        bottomBar = {
            MarkupToolbar(
                onToolClick = { toolLabel ->
                    scope.launch {
                        snackbarHostState.showSnackbar("$toolLabel is demo-only in this build.")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            HorizontalPager(
                state = pagerState,
                key = { sheets[it].id },
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val sheet = sheets[page]
                SheetPage(
                    sheet = sheet,
                    isCurrent = pagerState.settledPage == page,
                    pins = if (sheet.isPinSheet) DemoProjectRepository.pins.toList() else emptyList(),
                    onPinClick = { selectedPin = it },
                )
            }
            PagerControls(
                page = pagerState.currentPage,
                total = sheets.size,
                onPrevious = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                onNext = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
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

/** Top-bar sheet switcher: tap the title to jump to any sheet without leaving the viewer. */
@Composable
private fun SheetTitleSelector(
    sheets: List<PlanSheet>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val current = sheets[currentIndex]
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = "Choose sheet") { menuOpen = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("planViewerTitleSelector"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.widthIn(max = 220.dp)) {
                Text(
                    text = current.number,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            sheets.forEachIndexed { index, sheet ->
                DropdownMenuItem(
                    text = { Text("${sheet.number} · ${sheet.title}") },
                    leadingIcon = if (index == currentIndex) {
                        { Icon(Icons.Filled.Check, contentDescription = "Current sheet") }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        onSelect(index)
                    },
                    modifier = Modifier.testTag("planViewerSheetOption_${sheet.id}"),
                )
            }
        }
    }
}

/**
 * One zoomable page. Single-finger drags at rest scale stay with the pager (swipe navigation);
 * pinch always zooms, and panning takes over only while zoomed in.
 */
@Composable
private fun SheetPage(
    sheet: PlanSheet,
    isCurrent: Boolean,
    pins: List<PlanPin>,
    onPinClick: (PlanPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember(sheet.id) { mutableFloatStateOf(1f) }
    var offset by remember(sheet.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(candidate: Offset, atScale: Float): Offset {
        val maxX = (containerSize.width * (atScale - 1f)) / 2f
        val maxY = (containerSize.height * (atScale - 1f)) / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offset = clampOffset(offset + panChange, scale)
    }

    // Reset zoom once the page is swiped away so returning always starts at fit-to-screen.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .transformable(state = transformState, canPan = { scale > 1f })
            .pointerInput(sheet.id) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1.5f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val target = 2.5f
                            scale = target
                            val center = Offset(size.width / 2f, size.height / 2f)
                            offset = clampOffset((center - tap) * (target - 1f), target)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val painter = painterResource(sheet.drawableRes)
        val intrinsic = painter.intrinsicSize
        val aspect = if (intrinsic.height > 0f) intrinsic.width / intrinsic.height else 1.5f
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                // Sized to the drawing's aspect ratio so pins and markup share its coordinates.
                .aspectRatio(aspect)
                .background(Color.White)
                .semantics { contentDescription = "${sheet.number} ${sheet.title} drawing" },
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            DemoMarkupOverlay(markup = sheet.demoMarkup, modifier = Modifier.fillMaxSize())
            SheetPinsOverlay(
                pins = pins,
                onPinClick = onPinClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DemoMarkupOverlay(markup: List<DemoMarkup>, modifier: Modifier = Modifier) {
    if (markup.isEmpty()) return
    val red = MaterialTheme.colorScheme.error
    val blue = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        markup.forEach { shape ->
            val color = when (shape.color) {
                MarkupColor.RED -> red
                MarkupColor.BLUE -> blue
                MarkupColor.AMBER -> amber
            }
            when (shape.kind) {
                MarkupKind.RECT -> {
                    val (start, end) = shape.points
                    drawRect(
                        color = color,
                        topLeft = Offset(start.first * size.width, start.second * size.height),
                        size = Size(
                            (end.first - start.first) * size.width,
                            (end.second - start.second) * size.height,
                        ),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
                MarkupKind.POLYLINE -> {
                    val path = Path()
                    shape.points.forEachIndexed { index, point ->
                        val x = point.first * size.width
                        val y = point.second * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

/** Live pins from the demo store, anchored by image-fraction so they zoom with the drawing. */
@Composable
private fun SheetPinsOverlay(
    pins: List<PlanPin>,
    onPinClick: (PlanPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pins.isEmpty()) return
    var widthPx by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }
    val pinTint = DesignTokens.ErrorAccent
    val photoTint = DesignTokens.PrimaryAccent
    val halfPinPx = with(LocalDensity.current) { 24.dp.roundToPx() }
    Box(
        modifier = modifier.onSizeChanged {
            widthPx = it.width
            heightPx = it.height
        },
    ) {
        if (widthPx > 0 && heightPx > 0) {
            pins.forEach { pin ->
                val x = (pin.xFraction * widthPx).roundToInt()
                val y = (pin.yFraction * heightPx).roundToInt()
                IconButton(
                    onClick = { onPinClick(pin) },
                    modifier = Modifier
                        .offset { IntOffset(x - halfPinPx, y - halfPinPx) }
                        .size(48.dp)
                        .testTag("planPin_${pin.id}")
                        .semantics { contentDescription = pin.label },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = if (pin.kind == PinKind.PHOTO) photoTint else pinTint,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PagerControls(
    page: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = page > 0,
                modifier = Modifier.testTag("planViewerPrev"),
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous sheet")
            }
            Text(
                text = "${page + 1} / $total",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("planViewerPageIndicator"),
            )
            IconButton(
                onClick = onNext,
                enabled = page < total - 1,
                modifier = Modifier.testTag("planViewerNext"),
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next sheet")
            }
        }
    }
}

/** The intended markup tool set; each tool is a stub that explains it's demo-only. */
@Composable
private fun MarkupToolbar(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = listOf(
        Icons.Outlined.Draw to "Pen",
        Icons.Outlined.Straighten to "Measure",
        Icons.Outlined.TextFields to "Text",
        Icons.Outlined.PushPin to "Pin",
        Icons.Outlined.Layers to "Layers",
    )
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tools.forEach { (icon, label) ->
                IconButton(
                    onClick = { onToolClick(label) },
                    modifier = Modifier.testTag("markupTool_$label"),
                ) {
                    Icon(icon, contentDescription = label)
                }
            }
        }
    }
}
