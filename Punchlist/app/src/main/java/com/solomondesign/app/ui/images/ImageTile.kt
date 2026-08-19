package com.solomondesign.app.ui.images

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.DesignTokens

/**
 * A square grid tile.
 *
 * Demo photos are *drawn* rather than bundled — the same approach `PlanScreen` already uses for
 * the Area B sheet — so the prototype ships no new binary assets and themes correctly in light
 * and dark for free.
 */
@Composable
fun ImageTile(
    image: ProjectImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val captured = (image.source as? ImageSource.Captured)?.let { CapturedBitmapStore.get(it.captureKey) }
    val bitmapMissing = image.source is ImageSource.Captured && captured == null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(DesignTokens.CardCornerRadius))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(image.title)
                    append(", ")
                    append(image.area)
                    if (image.tags.isNotEmpty()) {
                        append(", ")
                        append(image.tags.joinToString(", "))
                    }
                }
            },
    ) {
        when (val source = image.source) {
            is ImageSource.Drawable -> Image(
                painter = painterResource(source.resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            is ImageSource.Captured -> if (captured != null) {
                Image(
                    bitmap = captured.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SitePhotoSwatch(seed = image.id.hashCode(), modifier = Modifier.fillMaxSize())
            }

            is ImageSource.Swatch -> SitePhotoSwatch(
                seed = source.seed,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Scrim so the caption stays legible over bright photos as well as dark swatches.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                )
                .padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 6.dp),
        ) {
            Text(
                text = if (bitmapMissing) "Photo unavailable" else image.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A procedurally drawn stand-in for a site photo: a sky band, a floor band, and a few seed-varied
 * vertical studs, all from theme tokens.
 */
@Composable
fun SitePhotoSwatch(seed: Int, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceContainer
    val band = MaterialTheme.colorScheme.surfaceContainerHigh
    val stud = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = base, size = Size(w, h))
        drawRect(
            color = band,
            topLeft = Offset(0f, h * 0.62f),
            size = Size(w, h * 0.38f),
        )
        val studCount = 2 + (seed.mod(3))
        val spacing = w / (studCount + 1f)
        repeat(studCount) { index ->
            val x = spacing * (index + 1)
            drawRect(
                color = stud.copy(alpha = 0.45f),
                topLeft = Offset(x - w * 0.02f, h * (0.18f + 0.04f * ((seed + index).mod(3)))),
                size = Size(w * 0.04f, h * 0.5f),
            )
        }
        drawRect(
            color = stud.copy(alpha = 0.30f),
            topLeft = Offset(0f, h * 0.60f),
            size = Size(w, h * 0.02f),
        )
    }
}
