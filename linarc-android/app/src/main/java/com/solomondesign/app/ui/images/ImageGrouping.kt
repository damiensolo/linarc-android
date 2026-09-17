package com.solomondesign.app.ui.images

import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.demo.PlanPin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure grouping logic behind the Images tool's Timeline and Albums views, kept free of Compose
 * so `./gradlew testDebugUnitTest` covers it — the same JVM split as `AppChrome`/`PlanSheetModels`.
 */

/** One header-plus-tiles section of a grouped Images view. */
data class ImageSection(
    val title: String,
    val images: List<ProjectImage>,
)

/** Albums view bucket for photos that haven't been filed into an album yet. */
const val UNFILED_ALBUM = "Unfiled"

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

/**
 * Timeline sections: newest day first, photos inside a day newest first. [today] is a parameter
 * (not read from the clock here) so tests can pin it.
 */
fun groupImagesByDay(
    images: List<ProjectImage>,
    today: LocalDate,
    zone: ZoneId,
): List<ImageSection> =
    images
        .sortedByDescending { it.capturedAtMillis }
        .groupBy { Instant.ofEpochMilli(it.capturedAtMillis).atZone(zone).toLocalDate() }
        .map { (day, dayImages) ->
            val title = when (day) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> DAY_FORMAT.format(day)
            }
            ImageSection(title = title, images = dayImages)
        }

/**
 * Albums view sections: named albums alphabetically, then [UNFILED_ALBUM] last — filing photos
 * is the point of the view, so the to-do pile sits at the end rather than crowding the albums.
 * Empty albums can't exist (an album is only ever a value on some image).
 */
fun groupImagesByAlbum(images: List<ProjectImage>): List<ImageSection> {
    val (filed, unfiled) = images.partition { !it.album.isNullOrBlank() }
    val albumSections = filed
        .groupBy { it.album!! }
        .toSortedMap()
        .map { (album, albumImages) ->
            ImageSection(album, albumImages.sortedByDescending { it.capturedAtMillis })
        }
    val unfiledSection = if (unfiled.isEmpty()) {
        emptyList()
    } else {
        listOf(ImageSection(UNFILED_ALBUM, unfiled.sortedByDescending { it.capturedAtMillis }))
    }
    return albumSections + unfiledSection
}

/**
 * Every capture publishes its Plan pin as `pin-<imageId>` (see `DemoProjectRepository.addPhoto`),
 * so the Map view and the plan viewer's pin sheet invert that convention to find the photo.
 */
fun imageIdOfPin(pinId: String): String = pinId.removePrefix("pin-")

/**
 * Which photos the full-screen viewer pages through. The viewer is one screen, but the two
 * places that open it mean different things: the Images tool browses the whole set, while a
 * plan pin is about *this location* — swiping there must only reach other photos pinned on
 * the plan, never unrelated album shots.
 */
enum class ImageViewerScope(
    /** Stable value carried in the nav route's `scope` argument. */
    val routeValue: String,
) {
    /** Every photo, repository order (the Grid view's order). Images, Today, Outbox, records. */
    ALL("all"),

    /** Only photos with a capture pin on the plan, in pin order. Opened from a plan pin. */
    PLAN_PINS("plan"),
    ;

    companion object {
        /** Unknown or missing route values fall back to [ALL] — never a crash on a stale link. */
        fun fromRoute(value: String?): ImageViewerScope =
            entries.firstOrNull { it.routeValue == value } ?: ALL
    }
}

/**
 * The ordered set the viewer pages through for [scope], anchored on [anchorImageId]. The
 * anchor is always included: if it exists but isn't in the scope (a photo opened from a pin
 * that has since lost its pin), the viewer shows just that photo rather than jumping scopes.
 */
fun viewerImages(
    scope: ImageViewerScope,
    anchorImageId: String,
    images: List<ProjectImage>,
    pins: List<PlanPin>,
): List<ProjectImage> {
    val scoped = when (scope) {
        ImageViewerScope.ALL -> images
        ImageViewerScope.PLAN_PINS -> {
            val byId = images.associateBy { it.id }
            pins.asSequence()
                .filter { it.kind == PinKind.PHOTO || it.kind == PinKind.VIDEO }
                .mapNotNull { byId[imageIdOfPin(it.id)] }
                .distinct()
                .toList()
        }
    }
    if (scoped.any { it.id == anchorImageId }) return scoped
    return listOfNotNull(images.firstOrNull { it.id == anchorImageId })
}
