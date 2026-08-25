package com.solomondesign.app.ui.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** List thumbnails are ~48dp; decoding beyond this is wasted memory, not visible detail. */
private const val THUMBNAIL_DECODE_EDGE_PX = 128

/**
 * Small rounded thumbnail of a [ProjectImage] for list rows (e.g. Today's Recent captures).
 * Decorative by design — `contentDescription` stays null because the row's own title/semantics
 * describe the photo; falls back to the drawn swatch exactly like the grid tiles do.
 */
@Composable
fun ImageThumbnail(
    image: ProjectImage,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        when (val source = image.source) {
            is ImageSource.Drawable -> Image(
                painter = painterResource(source.resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            is ImageSource.Captured -> {
                val bitmap = CapturedBitmapStore.get(source.captureKey)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SitePhotoSwatch(seed = image.id.hashCode(), modifier = Modifier.fillMaxSize())
                }
            }

            is ImageSource.CapturedFile -> FilePhoto(
                absolutePath = source.absolutePath,
                contentDescription = null,
                maxEdgePx = THUMBNAIL_DECODE_EDGE_PX,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    SitePhotoSwatch(seed = image.id.hashCode(), modifier = Modifier.fillMaxSize())
                },
            )

            is ImageSource.Swatch -> SitePhotoSwatch(
                seed = source.seed,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (image.hasMarkup) {
            MarkupBadge(
                diameter = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp),
            )
        }
    }
}
