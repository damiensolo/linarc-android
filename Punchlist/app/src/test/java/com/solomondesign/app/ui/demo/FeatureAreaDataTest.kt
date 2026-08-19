package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ProjectImageRepository
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
