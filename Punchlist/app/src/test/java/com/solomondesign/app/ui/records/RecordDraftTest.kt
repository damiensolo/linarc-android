package com.solomondesign.app.ui.records

import com.solomondesign.app.ui.capture.IssueDraft
import com.solomondesign.app.ui.capture.IssueDraftHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordDraftTest {

    @Before
    fun reset() {
        RecordDraft.clear()
        IssueDraftHolder.take()
        CameraAttachmentInbox.reset()
    }

    @Test
    fun begin_resetsEverythingAndSeedsThePhoto() {
        RecordDraft.title = "left over"
        RecordDraft.toggleAssignee("dave-miller")

        RecordDraft.begin(
            RecordCategory.PUNCH,
            nowMillis = 42L,
            seedTitle = "Scuffed corner bead",
            seedLocation = "Level 2",
            seedPhotoImageIds = listOf("img-1"),
        )

        assertEquals(RecordCategory.PUNCH, RecordDraft.category)
        assertEquals("Scuffed corner bead", RecordDraft.title)
        assertEquals(RecordCategory.PUNCH.typeOptions.first(), RecordDraft.type)
        assertEquals("Level 2", RecordDraft.location)
        assertEquals(42L, RecordDraft.eventDateMillis)
        assertEquals(emptyList<String>(), RecordDraft.assigneeIds)
        assertEquals(listOf("img-1"), RecordDraft.attachments.map { it.ref })
        assertEquals(AttachmentKind.PHOTO, RecordDraft.attachments.single().kind)
    }

    @Test
    fun staged_isConsumedExactlyOnce_andOnlyForTheMatchingCategory() {
        RecordDraft.begin(RecordCategory.INCIDENT, nowMillis = 1L)

        assertFalse(
            "a different category must not adopt a stale staging",
            RecordDraft.consumeStaged(RecordCategory.ISSUE),
        )
        RecordDraft.begin(RecordCategory.INCIDENT, nowMillis = 1L)
        assertTrue(RecordDraft.consumeStaged(RecordCategory.INCIDENT))
        assertFalse("staging is one-shot", RecordDraft.consumeStaged(RecordCategory.INCIDENT))
    }

    @Test
    fun beginForIssue_drainsTheDictatedHandOff_butExplicitSeedsWin() {
        IssueDraftHolder.set(IssueDraft(title = "Crack — Column 4", location = "Column 4", note = "dictated"))

        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L, seedDescription = "typed instead")

        assertEquals("Crack — Column 4", RecordDraft.title)
        assertEquals("Column 4", RecordDraft.location)
        assertEquals("explicit seeds beat the hand-off", "typed instead", RecordDraft.description)
        assertNull("the hand-off is one-shot", IssueDraftHolder.take())
    }

    @Test
    fun beginForNonIssue_leavesTheDictatedHandOffAlone() {
        IssueDraftHolder.set(IssueDraft(title = "Crack", location = null, note = ""))

        RecordDraft.begin(RecordCategory.PUNCH, nowMillis = 1L)

        assertEquals("", RecordDraft.title)
        assertEquals("Crack", IssueDraftHolder.take()?.title)
    }

    @Test
    fun photoAttachments_dedupe_andRemoveByIdWorks() {
        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L)
        RecordDraft.addPhoto("img-1")
        RecordDraft.addPhoto("img-1")
        RecordDraft.addFile("report.pdf")
        assertEquals(2, RecordDraft.attachments.size)

        RecordDraft.removeAttachment(RecordDraft.attachments.first().id)
        assertEquals(listOf("report.pdf"), RecordDraft.attachments.map { it.ref })
    }

    @Test
    fun hasEdits_tracksTitleDescriptionAndAttachments() {
        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L)
        assertFalse(RecordDraft.hasEdits)

        RecordDraft.addFile("report.pdf")
        assertTrue("an attachment alone counts as an edit", RecordDraft.hasEdits)
    }

    @Test
    fun toRecord_copiesEveryFieldAndTrims() {
        RecordDraft.begin(RecordCategory.INCIDENT, nowMillis = 5L, seedPhotoImageIds = listOf("img-2"))
        RecordDraft.title = "  Near miss at gate 2  "
        RecordDraft.selectType("Near miss")
        RecordDraft.description = " lift path blocked "
        RecordDraft.location = "Area B"
        RecordDraft.toggleAssignee("sam-reyes")

        val record = RecordDraft.toRecord(id = "rec-1", nowMillis = 9L, authorName = "Alex Kim")

        assertEquals("rec-1", record.id)
        assertEquals(RecordCategory.INCIDENT, record.category)
        assertEquals("Near miss at gate 2", record.title)
        assertEquals("lift path blocked", record.description)
        assertEquals(5L, record.eventDateMillis)
        assertEquals(9L, record.createdAtMillis)
        assertEquals(listOf("sam-reyes"), record.assigneeIds)
        assertEquals(listOf("img-2"), record.attachments.map { it.ref })
        assertEquals("Alex Kim", record.authorName)
    }

    @Test
    fun selectType_appliesTheConfiguredBlockingDefault() {
        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L)
        assertFalse("a fresh form never starts blocking", RecordDraft.blocksWork)

        RecordDraft.selectType("Safety hazard")
        assertTrue("safety hazards block by default", RecordDraft.blocksWork)
        assertTrue("blocking turns acknowledgement on", RecordDraft.acknowledgementRequired)

        RecordDraft.selectType("Observation")
        assertFalse("observations are logged, not blocking", RecordDraft.blocksWork)

        // The reporter's explicit choice still wins over the default.
        RecordDraft.setBlocking(true)
        assertTrue(RecordDraft.blocksWork)
    }

    @Test
    fun canSubmit_requiresABlockingReasonWhenBlocking() {
        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L)
        RecordDraft.title = "Med-gas clash"
        assertTrue(RecordDraft.canSubmit)

        RecordDraft.setBlocking(true)
        assertFalse("a block without a reason isn't auditable", RecordDraft.canSubmit)

        RecordDraft.blockingReason = "Re-route required before close-in"
        assertTrue(RecordDraft.canSubmit)
    }

    @Test
    fun toRecord_carriesBlockingScope_butStripsItWhenToggledOff() {
        RecordDraft.begin(RecordCategory.ISSUE, nowMillis = 1L)
        RecordDraft.title = "Med-gas clash"
        RecordDraft.setBlocking(true)
        RecordDraft.blockingReason = "  Re-route required  "
        RecordDraft.affectedTrade = "Electrical"
        RecordDraft.expectedResolutionMillis = 99L

        val blocking = RecordDraft.toRecord(id = "rec-b", nowMillis = 2L, authorName = "A")
        assertTrue(blocking.blocksWork)
        assertEquals("Re-route required", blocking.blockingReason)
        assertEquals("Electrical", blocking.affectedTrade)
        assertEquals(99L, blocking.expectedResolutionMillis!!)
        assertTrue(blocking.acknowledgementRequired)

        RecordDraft.setBlocking(false)
        val logged = RecordDraft.toRecord(id = "rec-l", nowMillis = 2L, authorName = "A")
        assertFalse(logged.blocksWork)
        assertEquals("a non-blocking record carries no blocking scope", "", logged.blockingReason)
        assertEquals("", logged.affectedTrade)
        assertNull(logged.expectedResolutionMillis)
        assertFalse(logged.acknowledgementRequired)
    }

    @Test
    fun cameraInbox_depositsOnlyWhenArmed_andTakeIsOneShot() {
        CameraAttachmentInbox.deposit("img-stray")
        assertNull("ordinary captures must not leak into a later form", CameraAttachmentInbox.take())

        CameraAttachmentInbox.arm()
        CameraAttachmentInbox.deposit("img-wanted")
        assertEquals("img-wanted", CameraAttachmentInbox.take())
        assertNull(CameraAttachmentInbox.take())

        // Disarmed after one deposit: the next capture is an ordinary one again.
        CameraAttachmentInbox.deposit("img-late")
        assertNull(CameraAttachmentInbox.take())
    }
}
