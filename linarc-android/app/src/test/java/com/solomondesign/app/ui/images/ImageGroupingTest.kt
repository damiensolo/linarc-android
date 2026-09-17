package com.solomondesign.app.ui.images

import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.demo.PlanPin
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageGroupingTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 8, 24)

    private fun image(id: String, at: LocalDateTime, album: String? = null) = ProjectImage(
        id = id,
        title = id,
        area = "Area B",
        tags = emptyList(),
        capturedAtMillis = at.toInstant(ZoneOffset.UTC).toEpochMilli(),
        authorName = "Test",
        source = ImageSource.Swatch(seed = 0),
        album = album,
    )

    private fun pin(imageId: String, kind: PinKind = PinKind.PHOTO) = PlanPin(
        id = "pin-$imageId",
        kind = kind,
        label = imageId,
        snippet = "",
        xFraction = 0.5f,
        yFraction = 0.5f,
    )

    private val at = LocalDateTime.of(2026, 8, 24, 9, 0)

    @Test
    fun viewerImages_allScope_isTheWholeSetInRepositoryOrder() {
        val all = listOf(image("a", at), image("b", at), image("c", at))

        val result = viewerImages(ImageViewerScope.ALL, "b", all, pins = listOf(pin("c")))

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun viewerImages_planPinsScope_onlyPinnedPhotosInPinOrder() {
        val all = listOf(image("album-shot", at), image("p1", at), image("p2", at), image("p3", at))
        val pins = listOf(
            pin("p3"),
            PlanPin("issue-1", PinKind.ISSUE, "Issue", "", 0.1f, 0.1f),
            pin("p1", kind = PinKind.VIDEO),
            pin("gone"), // pin whose photo was deleted
            pin("p3"), // duplicate pin for the same photo
        )

        val result = viewerImages(ImageViewerScope.PLAN_PINS, "p1", all, pins)

        assertEquals(
            "unpinned album photos never appear; order follows the pins",
            listOf("p3", "p1"),
            result.map { it.id },
        )
    }

    @Test
    fun viewerImages_planPinsScope_anchorWithoutAPin_showsJustThatPhoto() {
        val all = listOf(image("loose", at), image("p1", at))

        val result = viewerImages(ImageViewerScope.PLAN_PINS, "loose", all, pins = listOf(pin("p1")))

        assertEquals(listOf("loose"), result.map { it.id })
    }

    @Test
    fun viewerScope_fromRoute_fallsBackToAll() {
        assertEquals(ImageViewerScope.PLAN_PINS, ImageViewerScope.fromRoute("plan"))
        assertEquals(ImageViewerScope.ALL, ImageViewerScope.fromRoute("all"))
        assertEquals(ImageViewerScope.ALL, ImageViewerScope.fromRoute(null))
        assertEquals(ImageViewerScope.ALL, ImageViewerScope.fromRoute("bogus"))
    }

    @Test
    fun timelineSections_labelTodayYesterdayThenDates_newestFirstThroughout() {
        val sections = groupImagesByDay(
            images = listOf(
                image("old", LocalDateTime.of(2026, 8, 20, 9, 0)),
                image("today-early", LocalDateTime.of(2026, 8, 24, 7, 0)),
                image("yesterday", LocalDateTime.of(2026, 8, 23, 15, 0)),
                image("today-late", LocalDateTime.of(2026, 8, 24, 16, 0)),
            ),
            today = today,
            zone = zone,
        )

        assertEquals(listOf("Today", "Yesterday", "Thu, Aug 20"), sections.map { it.title })
        assertEquals(
            "photos inside a day are newest first",
            listOf("today-late", "today-early"),
            sections.first().images.map { it.id },
        )
    }

    @Test
    fun albumSections_sortAlbumsAlphabetically_withUnfiledLast() {
        val sections = groupImagesByAlbum(
            listOf(
                image("b1", LocalDateTime.of(2026, 8, 24, 9, 0), album = "Punch walk"),
                image("loose", LocalDateTime.of(2026, 8, 24, 10, 0), album = null),
                image("a1", LocalDateTime.of(2026, 8, 24, 8, 0), album = "Crew"),
                image("blank", LocalDateTime.of(2026, 8, 24, 11, 0), album = "  "),
            ),
        )

        assertEquals(listOf("Crew", "Punch walk", UNFILED_ALBUM), sections.map { it.title })
        assertEquals(
            "blank albums count as unfiled, newest first",
            listOf("blank", "loose"),
            sections.last().images.map { it.id },
        )
    }

    @Test
    fun albumSections_omitUnfiledWhenEverythingIsFiled() {
        val sections = groupImagesByAlbum(
            listOf(image("a1", LocalDateTime.of(2026, 8, 24, 8, 0), album = "Crew")),
        )
        assertEquals(listOf("Crew"), sections.map { it.title })
    }

    /** The `pin-<imageId>` convention from `DemoProjectRepository.addPhoto`, inverted. */
    @Test
    fun pinIdsInvertBackToImageIds() {
        assertEquals("img-yesterday", imageIdOfPin("pin-img-yesterday"))
        assertEquals("photo-123", imageIdOfPin("pin-photo-123"))
    }
}
