package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.capture.IssueDraft
import com.solomondesign.app.ui.capture.IssueDraftHolder
import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.records.AttachmentKind
import com.solomondesign.app.ui.records.FieldRecord
import com.solomondesign.app.ui.records.RecordAttachment
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.video.VideoRepository
import com.solomondesign.app.ui.voicelog.DailyLogRecord
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.tasks.TaskFilter
import com.solomondesign.app.ui.tasks.TaskStatus
import com.solomondesign.app.ui.tasks.applyFilter
import com.solomondesign.app.ui.timecards.TimeCardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Covers the demo data and mutations behind the five Tools areas. */
class FeatureAreaDataTest {

    @Before
    fun reset() {
        DemoSession.reset()
    }

    // ---- crew identity ----

    @Test
    fun crewIdsAreDerivedNormalisedAndUnique() {
        assertEquals("hector-ortiz", crewIdFor("Hector Ortiz"))
        assertEquals("t-m-lead", crewIdFor("  T & M Lead "))
        val ids = DemoProjectRepository.crew.map { it.id }
        assertEquals("crew ids must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun crewLookupsResolve() {
        assertEquals("Electrical", DemoProjectRepository.crewMember("maria-chen")?.trade)
        assertNull(DemoProjectRepository.crewMember("nobody"))
        assertEquals(0, DemoProjectRepository.crewIndexOf("hector-ortiz"))
        assertEquals(-1, DemoProjectRepository.crewIndexOf("nobody"))
    }

    // ---- field tasks ----

    @Test
    fun everyTaskAssigneeResolvesToRealCrew() {
        FieldTaskRepository.tasks.forEach { task ->
            val id = task.assigneeId ?: return@forEach
            assertNotNull(
                "task ${task.id} references unknown crew '$id'",
                DemoProjectRepository.crewMember(id),
            )
        }
    }

    @Test
    fun seedCoversEveryTaskStatus() {
        val statuses = FieldTaskRepository.tasks.map { it.status }.toSet()
        assertEquals(TaskStatus.entries.toSet(), statuses)
    }

    @Test
    fun statusAndChecklistMutationsApplyAndMissingIdsAreNoOps() {
        FieldTaskRepository.setStatus("task-door-bucks", TaskStatus.DONE)
        assertEquals(TaskStatus.DONE, FieldTaskRepository.find("task-door-bucks")?.status)

        val before = FieldTaskRepository.tasks.size
        FieldTaskRepository.setStatus("does-not-exist", TaskStatus.DONE)
        FieldTaskRepository.toggleCheckItem("does-not-exist", "nope")
        assertEquals(before, FieldTaskRepository.tasks.size)

        val task = FieldTaskRepository.find("task-frame-corridor-c")!!
        val item = task.checklist.first { !it.done }
        FieldTaskRepository.toggleCheckItem(task.id, item.id)
        val updated = FieldTaskRepository.find(task.id)!!
        assertTrue(updated.checklist.first { it.id == item.id }.done)
        // Siblings are untouched.
        assertEquals(
            task.checklist.filter { it.id != item.id }.map { it.done },
            updated.checklist.filter { it.id != item.id }.map { it.done },
        )
    }

    @Test
    fun taskFiltersNarrowTheList() {
        val all = FieldTaskRepository.tasks
        assertEquals(all.size, all.applyFilter(TaskFilter.ALL, CurrentUser.ID).size)
        assertTrue(
            all.applyFilter(TaskFilter.BLOCKED, CurrentUser.ID)
                .all { it.status == TaskStatus.BLOCKED },
        )
        // Nothing is assigned to the current user, so "Mine" is the empty-state trigger.
        assertTrue(all.applyFilter(TaskFilter.MINE, CurrentUser.ID).isEmpty())
    }

    // ---- time cards ----

    @Test
    fun timeTotalsIncludeOvertimeAndEmptyCrewReadsZero() {
        assertEquals(18.0, TimeCardRepository.totalHours("dave-miller"), 0.001)
        assertEquals(0.0, TimeCardRepository.totalHours("sam-reyes"), 0.001)
        assertEquals(3, TimeCardRepository.entriesFor("dave-miller").size)
    }

    @Test
    fun addingATimeEntryQueuesItAndPushesOneOutboxItem() {
        val outboxBefore = DemoProjectRepository.outboxItems.size
        TimeCardRepository.addEntry(
            com.solomondesign.app.ui.timecards.TimeEntry(
                id = "te-test",
                crewMemberId = "sam-reyes",
                dateLabel = "Mon, Aug 18",
                costCode = "07-8400 Firestopping",
                hours = 4.0,
                queued = true,
            ),
        )
        assertEquals(4.0, TimeCardRepository.totalHours("sam-reyes"), 0.001)
        assertEquals("te-test", TimeCardRepository.entries.first().id)
        assertEquals(outboxBefore + 1, DemoProjectRepository.outboxItems.size)
    }

    // ---- collaboration ----

    @Test
    fun topicsAreSortedByMostRecentActivity() {
        val activity = CollabRepository.topics.map { it.lastActivityMillis }
        assertEquals(activity.sortedDescending(), activity)
    }

    @Test
    fun everyMessageAuthorResolves() {
        CollabRepository.topics.forEach { topic ->
            CollabRepository.messagesFor(topic.id).forEach { message ->
                val known = message.authorId == CurrentUser.ID ||
                    DemoProjectRepository.crewMember(message.authorId) != null
                assertTrue("unknown author ${message.authorId}", known)
            }
        }
    }

    @Test
    fun postingBumpsActivityQueuesAndRejectsBlanks() {
        val before = CollabRepository.messagesFor("topic-saturday-pour").size
        CollabRepository.postMessage("topic-saturday-pour", "   ")
        assertEquals(before, CollabRepository.messagesFor("topic-saturday-pour").size)

        CollabRepository.postMessage("topic-saturday-pour", "On my way")
        val after = CollabRepository.messagesFor("topic-saturday-pour")
        assertEquals(before + 1, after.size)
        assertTrue(after.last().queued)
        // Posting moves the topic to the top of the list.
        assertEquals("topic-saturday-pour", CollabRepository.topics.first().id)

        CollabRepository.postMessage("no-such-topic", "hello")
        assertNull(CollabRepository.findTopic("no-such-topic"))
    }

    @Test
    fun creatingATopicRequiresATitle() {
        assertNull(CollabRepository.createTopic("  ", "body", emptyList()))
        val id = CollabRepository.createTopic("Gate 2 access", "", listOf("dave-miller"))
        assertNotNull(id)
        assertEquals(0, CollabRepository.messagesFor(id!!).size)

        val withMessage = CollabRepository.createTopic("Lift plan", "Crane at 7am", emptyList())
        assertEquals(1, CollabRepository.messagesFor(withMessage!!).size)
    }

    @Test
    fun markReadClearsTheUnreadBadge() {
        assertTrue(CollabRepository.findTopic("topic-col4-medgas")!!.unreadCount > 0)
        CollabRepository.markRead("topic-col4-medgas")
        assertEquals(0, CollabRepository.findTopic("topic-col4-medgas")!!.unreadCount)
    }

    // ---- images ----

    @Test
    fun imageSeedIsWellFormed() {
        val ids = ProjectImageRepository.images.map { it.id }
        assertEquals(8, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ProjectImageRepository.find("img-crew-hector")?.source is ImageSource.Drawable)
        assertEquals(ProjectImageRepository.visibleTags().sorted(), ProjectImageRepository.visibleTags())
    }

    @Test
    fun tagFilteringNarrowsTheGrid() {
        assertTrue(ProjectImageRepository.filterByTag("Framing").all { "Framing" in it.tags })
        assertTrue(ProjectImageRepository.filterByTag("Nope").isEmpty())
        assertEquals(
            ProjectImageRepository.images.size,
            ProjectImageRepository.filterByTag(null).size,
        )
    }

    /**
     * The highest-value test here: deleting a photo must also remove the Plan pin and the Today
     * entry it created, or the app is left with orphaned references.
     */
    @Test
    fun deletingAnImageAlsoRemovesItsPinAndStreamItem() {
        DemoProjectRepository.addPhoto(
            title = "Fresh capture",
            subtitle = "Area B · framing",
            createIssue = false,
        )
        val image = ProjectImageRepository.images.first()
        assertNotNull(DemoProjectRepository.pins.firstOrNull { it.id == "pin-${image.id}" })
        assertNotNull(
            DemoProjectRepository.streamItems.firstOrNull { it.id == "stream-${image.id}" },
        )

        ProjectImageRepository.delete(image.id)

        assertNull(ProjectImageRepository.find(image.id))
        assertNull(DemoProjectRepository.pins.firstOrNull { it.id == "pin-${image.id}" })
        assertNull(DemoProjectRepository.streamItems.firstOrNull { it.id == "stream-${image.id}" })

        val before = ProjectImageRepository.images.size
        ProjectImageRepository.delete("does-not-exist")
        assertEquals(before, ProjectImageRepository.images.size)
    }

    @Test
    fun capturingAPhotoFansOutToStreamPinAndGrid() {
        val images = ProjectImageRepository.images.size
        DemoProjectRepository.addPhoto(
            title = "Progress photo",
            subtitle = "Framing",
            createIssue = false,
            captureKey = "cap-1",
            tags = listOf("Framing"),
        )
        assertEquals(images + 1, ProjectImageRepository.images.size)
        val added = ProjectImageRepository.images.first()
        assertEquals(ImageSource.Captured("cap-1"), added.source)
        assertEquals(listOf("Framing"), added.tags)

        // The Today row must link back to the image so Recent captures can render a thumbnail
        // and deep-link into the viewer.
        val streamItem = DemoProjectRepository.streamItems.first()
        assertEquals("stream-${added.id}", streamItem.id)
        assertEquals(added.id, streamItem.relatedImageId)
    }

    /**
     * The seeded "Yesterday progress" trio (image, Today row, Plan pin) shares one identity —
     * ids follow the same stream-/pin-<imageId> convention as live captures — so deleting the
     * image from the viewer cleans all three, exactly like a photo captured in session.
     */
    @Test
    fun seededYesterdayPhoto_todayRowLinksToTheImage_andDeleteCleansTheTrio() {
        val row = DemoProjectRepository.streamItems.first { it.id == "stream-img-yesterday" }
        assertEquals("img-yesterday", row.relatedImageId)
        assertNotNull(ProjectImageRepository.find("img-yesterday"))
        assertNotNull(DemoProjectRepository.pins.firstOrNull { it.id == "pin-img-yesterday" })

        ProjectImageRepository.delete("img-yesterday")

        assertNull(DemoProjectRepository.streamItems.firstOrNull { it.id == "stream-img-yesterday" })
        assertNull(DemoProjectRepository.pins.firstOrNull { it.id == "pin-img-yesterday" })
    }

    /**
     * "Replace the original" in the markup editor: the id keeps its pin and Today row, only the
     * source swaps — and [DemoProjectRepository.addPhoto] returns the id the editor's
     * "save as a copy" needs to open the copy's viewer.
     */
    @Test
    fun replacingAnImageSourceKeepsItsIdentity() {
        val id = DemoProjectRepository.addPhoto(
            title = "Marked up",
            subtitle = "Area B",
            createIssue = false,
        )
        assertEquals(id, ProjectImageRepository.images.first().id)
        assertFalse(ProjectImageRepository.find(id)!!.hasMarkup)

        ProjectImageRepository.replaceSource(id, ImageSource.CapturedFile("/captures/new.jpg"))

        assertEquals(
            ImageSource.CapturedFile("/captures/new.jpg"),
            ProjectImageRepository.find(id)?.source,
        )
        assertTrue(
            "replace-the-original is markup's save; the badge flag must flip",
            ProjectImageRepository.find(id)!!.hasMarkup,
        )
        assertNotNull(DemoProjectRepository.streamItems.firstOrNull { it.id == "stream-$id" })
        assertNotNull(DemoProjectRepository.pins.firstOrNull { it.id == "pin-$id" })

        val before = ProjectImageRepository.images.size
        ProjectImageRepository.replaceSource("does-not-exist", ImageSource.Swatch(seed = 9))
        assertEquals(before, ProjectImageRepository.images.size)
    }

    // ---- albums ----

    @Test
    fun albumFilingListsAssignsAndUnfiles() {
        assertEquals(listOf("Crew", "Deficiencies", "Progress set"), ProjectImageRepository.albums())

        ProjectImageRepository.setAlbum("img-door-bucks", "Punch walk")
        assertEquals("Punch walk", ProjectImageRepository.find("img-door-bucks")?.album)
        assertTrue("Punch walk" in ProjectImageRepository.albums())

        // Blank means unfile, and a vanished album drops out of the list entirely.
        ProjectImageRepository.setAlbum("img-door-bucks", "   ")
        assertNull(ProjectImageRepository.find("img-door-bucks")?.album)
        assertFalse("Punch walk" in ProjectImageRepository.albums())

        val before = ProjectImageRepository.images
        ProjectImageRepository.setAlbum("does-not-exist", "Nope")
        assertEquals(before, ProjectImageRepository.images)
    }

    // ---- plan pin comments ----

    @Test
    fun pinCommentsRejectBlanksAndPublishQueuesOneOutboxBatch() {
        val pinId = "pin-img-yesterday"
        assertFalse(DemoProjectRepository.addPinComment(pinId, "   "))
        assertEquals(emptyList<PinComment>(), DemoProjectRepository.pinCommentsFor(pinId))

        assertTrue(DemoProjectRepository.addPinComment(pinId, "Formwork looks short here"))
        assertTrue(DemoProjectRepository.addPinComment(pinId, "Flagging for the AM walk"))
        val thread = DemoProjectRepository.pinCommentsFor(pinId)
        assertEquals(2, thread.size)
        assertEquals(CurrentUser.NAME, thread.first().authorName)
        assertTrue("comments start unpublished", thread.none { it.published })

        val outboxBefore = DemoProjectRepository.outboxItems.size
        assertEquals(2, DemoProjectRepository.publishPinComments(pinId))
        assertTrue(DemoProjectRepository.pinCommentsFor(pinId).all { it.published })
        assertEquals(
            "one outbox entry per publish batch, not per comment",
            outboxBefore + 1,
            DemoProjectRepository.outboxItems.size,
        )
        assertTrue(DemoProjectRepository.outboxItems.last().title.contains("Yesterday progress"))

        // Nothing left to publish: a second publish is a no-op and queues nothing.
        assertEquals(0, DemoProjectRepository.publishPinComments(pinId))
        assertEquals(outboxBefore + 1, DemoProjectRepository.outboxItems.size)

        // A new comment after publishing republishes just that one.
        DemoProjectRepository.addPinComment(pinId, "Confirmed fixed")
        assertEquals(1, DemoProjectRepository.publishPinComments(pinId))
        assertEquals(outboxBefore + 2, DemoProjectRepository.outboxItems.size)
    }

    // ---- outbox ----

    @Test
    fun outboxSendAllDrainsOldestFirstThenReportsNothingLeft() {
        val seededQueued = DemoProjectRepository.queuedOutboxCount
        assertTrue("seeds keep the outbox demoable from first launch", seededQueued >= 2)
        assertTrue(DemoProjectRepository.outboxItems.all { it.status == OutboxStatus.QUEUED })

        val first = DemoProjectRepository.sendNextQueuedOutboxItem()
        assertEquals("oldest entry sends first", "outbox-1", first?.id)
        assertEquals(OutboxStatus.SENT, first?.status)
        assertEquals(seededQueued - 1, DemoProjectRepository.queuedOutboxCount)

        while (DemoProjectRepository.sendNextQueuedOutboxItem() != null) {
            // The Outbox screen walks this same loop with a delay between beats.
        }
        assertEquals(0, DemoProjectRepository.queuedOutboxCount)
        assertTrue(DemoProjectRepository.outboxItems.all { it.status == OutboxStatus.SENT })
        assertNull("a drained queue sends nothing", DemoProjectRepository.sendNextQueuedOutboxItem())
    }

    @Test
    fun outboxStatusLineCombinesDetailWithQueueState() {
        val queued = OutboxItem("x", "Photo: strap", "Area B")
        assertEquals("Area B · Queued · waiting for signal", queued.statusLine())
        assertEquals("Area B · Sent to project", queued.copy(status = OutboxStatus.SENT).statusLine())
        assertEquals("Queued · waiting for signal", OutboxItem("y", "Message").statusLine())
    }

    /** Photos, videos, and voice logs queue too — the offline story covers every capture type. */
    @Test
    fun everyCaptureTypeQueuesOneOutboxEntry() {
        val before = DemoProjectRepository.queuedOutboxCount

        DemoProjectRepository.addPhoto(title = "Header strap", subtitle = "Area B", createIssue = false)
        assertEquals(before + 1, DemoProjectRepository.queuedOutboxCount)
        assertTrue(DemoProjectRepository.outboxItems.last().title.startsWith("Photo:"))

        DemoProjectRepository.addCapturedVideo(
            title = "Slab pour walk",
            note = "",
            videoPath = "video/test.mp4",
            transcript = "",
            durationSeconds = 12,
        )
        assertEquals(before + 2, DemoProjectRepository.queuedOutboxCount)
        assertTrue(DemoProjectRepository.outboxItems.last().title.startsWith("Video:"))

        DemoProjectRepository.publishVoiceLog(
            DailyLogRecord(
                id = "log-test",
                timestampMillis = 0L,
                projectName = DemoProjectRepository.PROJECT_NAME,
                audioFilePath = "",
                transcript = "Poured slab, two hour pump delay",
                laborCards = emptyList(),
                materialCards = emptyList(),
                delayCards = emptyList(),
                issueCards = emptyList(),
            ),
        )
        assertEquals(before + 3, DemoProjectRepository.queuedOutboxCount)
        assertEquals("Voice daily log", DemoProjectRepository.outboxItems.last().title)
    }

    // ---- records (issues / incidents / punch items) ----

    @Test
    fun recordSeedsCoverEveryCategory() {
        RecordCategory.entries.forEach { category ->
            assertTrue(
                "the ${category.pluralLabel} tool must not open empty",
                RecordRepository.byCategory(category).isNotEmpty(),
            )
        }
        val ids = RecordRepository.records.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    /** Nothing on Today may dead-end: every record-linked seed row resolves to a real record. */
    @Test
    fun seededTodayRowsResolveToRealRecords() {
        val linked = DemoProjectRepository.streamItems.filter { it.relatedFieldRecordId != null }
        assertTrue("seeds must demo the blocker → record link", linked.isNotEmpty())
        linked.forEach { row ->
            assertNotNull(
                "Today row '${row.title}' links to a missing record",
                RecordRepository.find(row.relatedFieldRecordId!!),
            )
        }
        assertEquals(
            "rec-seed-inspection",
            DemoProjectRepository.streamItems
                .first { it.title == "Frame inspection" }
                .relatedFieldRecordId,
        )
        assertEquals(
            "rec-seed-issue",
            DemoProjectRepository.streamItems
                .first { it.title == "Med-gas conflict at Column 4" }
                .relatedFieldRecordId,
        )
    }

    /**
     * Issued ≠ blocked: a default issue is logged (tool + outbox + pin, photo linked back) but
     * does NOT land on Today's Blockers — creating a record never stops work by itself.
     */
    @Test
    fun addRecord_defaultIssueLogsAndPinsWithoutBlockingToday() {
        val outboxBefore = DemoProjectRepository.outboxItems.size
        val record = FieldRecord(
            id = "rec-test-issue",
            category = RecordCategory.ISSUE,
            title = "Cracked slab at column 4",
            type = "Quality",
            description = "Hairline crack radiating from the base plate",
            location = "Column 4",
            eventDateMillis = 1_000L,
            assigneeIds = listOf("maria-chen"),
            attachments = listOf(
                RecordAttachment("att-test", AttachmentKind.PHOTO, "img-yesterday"),
            ),
            createdAtMillis = 2_000L,
            authorName = CurrentUser.NAME,
        )

        DemoProjectRepository.addRecord(record)

        assertEquals(record, RecordRepository.find("rec-test-issue"))
        assertEquals(outboxBefore + 1, DemoProjectRepository.outboxItems.size)
        assertTrue(DemoProjectRepository.outboxItems.last().title.startsWith("Issue:"))
        assertTrue(
            "a non-blocking issue must stay off Today",
            DemoProjectRepository.streamItems.none { it.title == record.title },
        )
        assertTrue(DemoProjectRepository.pins.any { it.id == "pin-rec-test-issue" })
        assertEquals(
            "the attached photo must link back to the record",
            "rec-test-issue",
            ProjectImageRepository.find("img-yesterday")?.linkedRecordId,
        )
    }

    /**
     * The explicit blocking status is what shouts: a record marked blocks-work lands on Today's
     * Blockers as a blocking row linked back to its record, with the reason as the note.
     */
    @Test
    fun addRecord_blockingIssueLandsOnTodayBlockers() {
        DemoProjectRepository.addRecord(
            FieldRecord(
                id = "rec-test-blocking",
                category = RecordCategory.ISSUE,
                title = "Failed rough-in inspection, exam 5",
                type = "Failed inspection",
                description = "",
                location = "Level 2",
                eventDateMillis = 1_000L,
                assigneeIds = emptyList(),
                attachments = emptyList(),
                createdAtMillis = 2_000L,
                authorName = CurrentUser.NAME,
                blocksWork = true,
                blockingReason = "No cover until re-inspection passes",
            ),
        )

        val row = DemoProjectRepository.streamItems
            .first { it.title == "Failed rough-in inspection, exam 5" }
        assertTrue("the Today row is a blocker", row.blocking)
        assertEquals("rec-test-blocking", row.relatedFieldRecordId)
        assertTrue(
            "the blocking reason travels to the row",
            row.subtitle.contains("No cover until re-inspection passes"),
        )
        assertTrue(DemoProjectRepository.pins.any { it.label == "Failed rough-in inspection, exam 5" })
    }

    @Test
    fun addRecord_punchItemPinsWithoutShoutingOnToday() {
        val streamBefore = DemoProjectRepository.streamItems.size
        val outboxBefore = DemoProjectRepository.outboxItems.size

        DemoProjectRepository.addRecord(
            FieldRecord(
                id = "rec-test-punch",
                category = RecordCategory.PUNCH,
                title = "Paint touch-up at exam 3",
                type = "Touch-up",
                description = "",
                location = "Level 2",
                eventDateMillis = 1_000L,
                assigneeIds = emptyList(),
                attachments = emptyList(),
                createdAtMillis = 2_000L,
                authorName = CurrentUser.NAME,
            ),
        )

        assertTrue(DemoProjectRepository.pins.any { it.id == "pin-rec-test-punch" })
        assertEquals(
            "punch items are location work, not Today blockers",
            streamBefore,
            DemoProjectRepository.streamItems.size,
        )
        assertEquals(outboxBefore + 1, DemoProjectRepository.outboxItems.size)
    }

    // ---- captured video ----

    /**
     * The video counterpart of the photo fan-out rule: saving a clip must land it on
     * Today (linked for playback) and as a Plan pin, plus in its own repository — never only in
     * a private list.
     */
    @Test
    fun savingACapturedVideoFansOutToStreamPinAndRepository() {
        val id = DemoProjectRepository.addCapturedVideo(
            title = "Crack — Column 4",
            note = "Crack in the slab near column 4",
            videoPath = "/captures/video-1.mp4",
            transcript = "there's a crack in the slab near column 4",
            durationSeconds = 95,
        )

        val video = VideoRepository.find(id)
        assertNotNull(video)
        assertEquals("/captures/video-1.mp4", video!!.videoPath)

        val row = DemoProjectRepository.streamItems.first()
        assertEquals("stream-$id", row.id)
        assertEquals(StreamKind.VIDEO, row.kind)
        assertEquals("Today must link back for playback", id, row.relatedVideoId)
        assertTrue("subtitle carries the mm:ss duration", row.subtitle.contains("1:35"))

        val pin = DemoProjectRepository.pins.first { it.id == "pin-$id" }
        assertEquals(PinKind.VIDEO, pin.kind)
        assertEquals("Crack — Column 4", pin.label)
    }

    @Test
    fun issueDraftHandOffIsOneShot() {
        IssueDraftHolder.set(IssueDraft(title = "Crack", location = "Column 4", note = "note"))

        assertEquals("Crack", IssueDraftHolder.take()?.title)
        assertNull("a second visit to Quick issue must start blank", IssueDraftHolder.take())
    }

    @Test
    fun resetRestoresEveryStore() {
        FieldTaskRepository.setStatus("task-door-bucks", TaskStatus.DONE)
        ProjectImageRepository.delete("img-yesterday")
        DemoSession.reset()
        assertEquals(TaskStatus.NOT_STARTED, FieldTaskRepository.find("task-door-bucks")?.status)
        assertNotNull(ProjectImageRepository.find("img-yesterday"))
        assertFalse(DemoProjectRepository.dayStarted)
    }
}
