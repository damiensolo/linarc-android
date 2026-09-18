package com.solomondesign.app.ui.collab

import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.DemoSession
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollabRepositoryTest {

    @After
    fun resetStores() {
        DemoSession.reset()
    }

    @Test
    fun seededThreads_linkToObjectsThatExist() {
        val linked = CollabRepository.topics.mapNotNull { it.subject }
        assertEquals(3, linked.size)
        linked.forEach { subject ->
            val exists = when (subject.kind) {
                CollabSubject.Kind.RECORD -> RecordRepository.find(subject.id) != null
                CollabSubject.Kind.TASK -> FieldTaskRepository.find(subject.id) != null
                else -> false
            }
            assertTrue("seed subject $subject must resolve", exists)
            assertNotNull(CollabRepository.subjectLabel(subject))
        }
        assertEquals(
            "Record · Med-gas conflict at Column 4",
            CollabRepository.subjectLabel(CollabSubject(CollabSubject.Kind.RECORD, "rec-seed-issue")),
        )
        assertNull(CollabRepository.subjectLabel(null))
        assertNull(CollabRepository.subjectLabel(CollabSubject(CollabSubject.Kind.TASK, "missing")))
    }

    @Test
    fun postToSubject_createsOneThreadLazily_andReusesIt() {
        val subject = CollabSubject(CollabSubject.Kind.RECORD, "rec-seed-punch")
        val before = CollabRepository.topics.size
        assertNull(CollabRepository.topicFor(subject))

        assertNull(CollabRepository.postToSubject(subject, "Touch up", "Exam 6", listOf("dave-miller"), "   "))
        assertNull(CollabRepository.topicFor(subject))
        assertEquals(before, CollabRepository.topics.size)

        val first = CollabRepository.postToSubject(subject, "Touch up", "Exam 6", listOf("dave-miller"), "Paint arrives Monday")
        val second = CollabRepository.postToSubject(subject, "ignored", "ignored", emptyList(), "Crew booked")
        assertNotNull(first)
        assertEquals(first, second)
        assertEquals(before + 1, CollabRepository.topics.size)

        val topic = CollabRepository.topicFor(subject)!!
        assertEquals("Touch up", topic.title)
        assertEquals("Exam 6", topic.location)
        assertTrue("dave-miller" in topic.participantIds)
        assertTrue(CurrentUser.ID in topic.participantIds)
        assertEquals(listOf("Paint arrives Monday", "Crew booked"), CollabRepository.messagesFor(topic.id).map { it.body })
    }

    @Test
    fun postMessage_queuesOneOutboxEntryPerMessage_linkedToTheThread() {
        val queuedBefore = DemoProjectRepository.outboxItems.size
        CollabRepository.postMessage("topic-saturday-pour", "Gate 2 opens at six")
        CollabRepository.postMessage("topic-saturday-pour", "")
        CollabRepository.postMessage("no-such-topic", "lost")

        val queued = DemoProjectRepository.outboxItems.drop(queuedBefore)
        assertEquals(1, queued.size)
        assertEquals("topic-saturday-pour", queued.single().relatedTopicId)
        assertTrue(CollabRepository.messagesFor("topic-saturday-pour").last().queued)
    }

    @Test
    fun authorIdentity_followsTheDemoLens() {
        assertEquals(CollabAuthor(CurrentUser.ID, CurrentUser.NAME), CollabRepository.authorIdentity())

        DemoProjectRepository.selectPersona(FieldPersona.CREW)
        assertEquals("hector-ortiz", CollabRepository.authorIdentity().id)
        CollabRepository.postMessage("topic-saturday-pour", "Rolling in at five")
        val posted = CollabRepository.messagesFor("topic-saturday-pour").last()
        assertEquals("hector-ortiz", posted.authorId)
        assertEquals("Hector Ortiz", posted.authorName)

        DemoProjectRepository.selectPersona(FieldPersona.SUBCONTRACTOR)
        assertEquals("sam-reyes", CollabRepository.authorIdentity().id)

        DemoProjectRepository.selectPersona(FieldPersona.FOREMAN)
        assertEquals(CurrentUser.ID, CollabRepository.authorIdentity().id)
    }

    @Test
    fun mentions_resolveByFullOrFirstName_andJoinTheThread() {
        val crew = listOf("hector-ortiz" to "Hector Ortiz", "dave-miller" to "Dave Miller", "maria-chen" to "Maria Chen")
        assertEquals(listOf("hector-ortiz"), mentionedIds("@Hector Ortiz can you check?", crew))
        assertEquals(listOf("dave-miller"), mentionedIds("ask @dave about the studs", crew))
        assertEquals(listOf("hector-ortiz", "maria-chen"), mentionedIds("@Hector and @Maria Chen", crew))
        assertEquals(emptyList<String>(), mentionedIds("email hector@site.com, no mention", crew))
        assertEquals(emptyList<String>(), mentionedIds("@Davey is not Dave", crew))

        val before = CollabRepository.findTopic("topic-saturday-pour")!!.participantIds
        assertTrue("maria-chen" !in before)
        CollabRepository.postMessage("topic-saturday-pour", "@Maria Chen can your rack wait until Monday?")
        val after = CollabRepository.findTopic("topic-saturday-pour")!!.participantIds
        assertTrue("maria-chen" in after)
        assertEquals(before.size + 1, after.size)
    }

    @Test
    fun topicFilters_scopeToMineAndUnread() {
        val topics = CollabRepository.topics
        assertEquals(topics, topics.matching(TopicFilter.ALL, CurrentUser.ID))
        assertEquals(setOf("topic-col4-medgas", "topic-headwall-heights"), topics.matching(TopicFilter.UNREAD, CurrentUser.ID).map { it.id }.toSet())
        // The Foreman is in every seeded thread; Hector (the Crew lens) only in two.
        assertEquals(4, topics.matching(TopicFilter.MINE, CurrentUser.ID).size)
        assertEquals(
            setOf("topic-frame-inspection", "topic-saturday-pour"),
            topics.matching(TopicFilter.MINE, "hector-ortiz").map { it.id }.toSet(),
        )
    }

    @Test
    fun markRead_clearsUnread_andClearReseeds() {
        assertEquals(2, CollabRepository.findTopic("topic-col4-medgas")!!.unreadCount)
        CollabRepository.markRead("topic-col4-medgas")
        assertEquals(0, CollabRepository.findTopic("topic-col4-medgas")!!.unreadCount)

        CollabRepository.clear()
        assertEquals(2, CollabRepository.findTopic("topic-col4-medgas")!!.unreadCount)
        assertEquals(4, CollabRepository.topics.size)
    }
}
