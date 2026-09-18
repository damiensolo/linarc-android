package com.solomondesign.app.ui.collab

import androidx.compose.runtime.mutableStateListOf
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.PinKind
import com.solomondesign.app.ui.demo.PlanPin
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.images.imageIdOfPin
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.tasks.FieldTaskRepository

/** Who a message is posted as — see [CollabRepository.authorIdentity]. */
data class CollabAuthor(val id: String, val name: String)

/** In-memory demo store for collaboration topics and messages. Snapshot state, no ViewModel. */
object CollabRepository {
    private val _topics = mutableStateListOf<CollabTopic>()
    private val _messages = mutableStateListOf<CollabMessage>()

    /** Most recently active first. */
    val topics: List<CollabTopic> get() = _topics.sortedByDescending { it.lastActivityMillis }

    private var nextId = 0

    init {
        seed()
    }

    fun findTopic(id: String): CollabTopic? = _topics.firstOrNull { it.id == id }

    /** The one thread for [subject], or null until someone posts the first message. */
    fun topicFor(subject: CollabSubject): CollabTopic? = _topics.firstOrNull { it.subject == subject }

    fun messagesFor(topicId: String): List<CollabMessage> =
        _messages.filter { it.topicId == topicId }.sortedBy { it.timestampMillis }

    fun lastMessagePreview(topicId: String): String =
        messagesFor(topicId).lastOrNull()?.body.orEmpty()

    /**
     * Messages post as whoever the Demo: view as lens views the project through — Hector in the
     * Crew view, Sam in the Subcontractor view, the signed-in Foreman otherwise. This is the
     * same "mine" rule the Field task list applies, so a thread and a task never disagree about
     * who the reader is (decided 2026-09-18).
     */
    fun authorIdentity(): CollabAuthor {
        val lens = DemoProjectRepository.lensMember
        return if (lens != null) CollabAuthor(lens.id, lens.name) else CollabAuthor(CurrentUser.ID, CurrentUser.NAME)
    }

    /**
     * "Record · Med-gas conflict at Column 4" — the subject as a reader would name it, resolved
     * live from the owning store so a renamed object reads correctly. Null for free-standing
     * topics or when the object no longer exists.
     */
    fun subjectLabel(subject: CollabSubject?): String? {
        subject ?: return null
        val title = when (subject.kind) {
            CollabSubject.Kind.RECORD -> RecordRepository.find(subject.id)?.title
            CollabSubject.Kind.TASK -> FieldTaskRepository.find(subject.id)?.title
            CollabSubject.Kind.PLAN_PIN -> DemoProjectRepository.pins.firstOrNull { it.id == subject.id }?.label
            CollabSubject.Kind.IMAGE -> ProjectImageRepository.find(subject.id)?.title
        } ?: return null
        return "${subject.kind.label} · $title"
    }

    /**
     * The thread a plan pin opens. A pin is a *location* for something, not a thing of its own,
     * so a pin backed by a record shares the record's discussion and a photo pin shares the
     * photo's — one conversation per object wherever it is reached. Only video and log pins,
     * which have no discussable object behind them, get a thread of their own.
     */
    fun subjectForPin(pin: PlanPin): CollabSubject {
        // Issue pins are minted as "pin-<recordId>"; older seeds set no relatedRecordId, so
        // fall back to the id convention when it names a record that exists.
        val recordId = pin.relatedRecordId
            ?: pin.id.removePrefix("pin-").takeIf { pin.kind == PinKind.ISSUE && RecordRepository.find(it) != null }
        return when {
            recordId != null -> CollabSubject(CollabSubject.Kind.RECORD, recordId)
            pin.kind == PinKind.PHOTO -> CollabSubject(CollabSubject.Kind.IMAGE, imageIdOfPin(pin.id))
            else -> CollabSubject(CollabSubject.Kind.PLAN_PIN, pin.id)
        }
    }

    fun createTopic(title: String, firstMessage: String, participantIds: List<String>): String? {
        if (title.isBlank()) return null
        val id = addTopic(
            title = title,
            location = DemoProjectRepository.AREA,
            participantIds = participantIds,
            subject = null,
        )
        if (firstMessage.isNotBlank()) postMessage(id, firstMessage)
        return id
    }

    /**
     * Posts [body] into the thread for [subject], creating that thread on the first message
     * (lazily, so object detail screens never litter the Collaboration index with empty
     * topics). [title], [location] and [participantIds] seed the new topic only; an existing
     * thread keeps its own. Returns the topic id, or null when nothing was posted.
     */
    fun postToSubject(
        subject: CollabSubject,
        title: String,
        location: String,
        participantIds: List<String>,
        body: String,
    ): String? {
        if (body.isBlank()) return null
        val topicId = topicFor(subject)?.id ?: addTopic(
            title = title.ifBlank { subjectLabel(subject) ?: subject.kind.label },
            location = location.ifBlank { DemoProjectRepository.AREA },
            participantIds = participantIds,
            subject = subject,
        )
        postMessage(topicId, body)
        return topicId
    }

    fun postMessage(topicId: String, body: String) {
        if (body.isBlank()) return
        val index = _topics.indexOfFirst { it.id == topicId }
        if (index < 0) return
        val now = System.currentTimeMillis()
        val author = authorIdentity()
        _messages.add(
            CollabMessage(
                id = "msg-new-${nextId++}",
                topicId = topicId,
                authorId = author.id,
                authorName = author.name,
                body = body.trim(),
                timestampMillis = now,
                queued = true,
            ),
        )
        // Naming someone with @ pulls them into the thread: the author, plus every mention.
        val mentioned = mentionedIds(body, DemoProjectRepository.crew.map { it.id to it.name })
        _topics[index] = _topics[index].copy(
            lastActivityMillis = now,
            participantIds = (_topics[index].participantIds + author.id + mentioned).distinct(),
        )
        DemoProjectRepository.queueOutbox(
            id = "outbox-msg-$nextId",
            title = "Message: ${_topics[index].title}",
            relatedTopicId = topicId,
        )
    }

    fun markRead(topicId: String) {
        val index = _topics.indexOfFirst { it.id == topicId }
        if (index >= 0 && _topics[index].unreadCount != 0) {
            _topics[index] = _topics[index].copy(unreadCount = 0)
        }
    }

    fun clear() {
        _topics.clear()
        _messages.clear()
        nextId = 0
        seed()
    }

    private fun addTopic(
        title: String,
        location: String,
        participantIds: List<String>,
        subject: CollabSubject?,
    ): String {
        val id = "topic-new-${nextId++}"
        _topics.add(
            CollabTopic(
                id = id,
                title = title.trim(),
                location = location,
                participantIds = (participantIds + authorIdentity().id).distinct(),
                unreadCount = 0,
                lastActivityMillis = System.currentTimeMillis(),
                subject = subject,
            ),
        )
        return id
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val minute = 60_000L

        // Seeded threads are the conversations behind seeded objects, so they link to them:
        // the med-gas thread is the Column 4 issue's discussion, the headwall thread belongs to
        // RFI-121, the inspection thread to the corridor C framing task. Ids come from
        // RecordRepository / FieldTaskRepository seeds; CollabRepositoryTest checks they resolve.
        _topics.addAll(
            listOf(
                CollabTopic(
                    id = "topic-col4-medgas",
                    title = "Column 4 med gas conflict",
                    location = "Area B · Column 4",
                    participantIds = listOf("sam-reyes", "maria-chen", CurrentUser.ID),
                    unreadCount = 2,
                    lastActivityMillis = now - 5 * minute,
                    subject = CollabSubject(CollabSubject.Kind.RECORD, "rec-seed-issue"),
                ),
                CollabTopic(
                    id = "topic-frame-inspection",
                    title = "Level 2 framing inspection window",
                    location = "Area B · Level 2",
                    participantIds = listOf("hector-ortiz", CurrentUser.ID),
                    unreadCount = 0,
                    lastActivityMillis = now - 90 * minute,
                    subject = CollabSubject(CollabSubject.Kind.TASK, "task-frame-corridor-c"),
                ),
                CollabTopic(
                    id = "topic-headwall-heights",
                    title = "Headwall backing heights",
                    location = "Area B · rooms 5–8",
                    participantIds = listOf("dave-miller", CurrentUser.ID),
                    unreadCount = 1,
                    lastActivityMillis = now - 200 * minute,
                    subject = CollabSubject(CollabSubject.Kind.RECORD, "rec-seed-rfi-121"),
                ),
                CollabTopic(
                    id = "topic-saturday-pour",
                    title = "Concrete pour Saturday — access",
                    location = "Area B",
                    participantIds = listOf("hector-ortiz", "dave-miller", CurrentUser.ID),
                    unreadCount = 0,
                    lastActivityMillis = now - 400 * minute,
                ),
            ),
        )

        _messages.addAll(
            listOf(
                CollabMessage(
                    "msg-col4-1", "topic-col4-medgas", "sam-reyes", "Sam Reyes",
                    "Med gas line at column 4 is hitting the 4-inch storm. Can't rough-in " +
                        "until we get direction.",
                    now - 25 * minute,
                ),
                CollabMessage(
                    "msg-col4-2", "topic-col4-medgas", "maria-chen", "Maria Chen",
                    "My conduit rack is in the same bay. If they move the storm I lose three " +
                        "inches of clearance.",
                    now - 18 * minute,
                ),
                CollabMessage(
                    "msg-col4-3", "topic-col4-medgas", CurrentUser.ID, CurrentUser.NAME,
                    "Logged it as an issue and pinned it on the Area B sheet. RFI-118 goes out today.",
                    now - 9 * minute,
                ),
                CollabMessage(
                    "msg-col4-4", "topic-col4-medgas", "sam-reyes", "Sam Reyes",
                    "Copy. Crew is on the level 2 restroom group until then.",
                    now - 5 * minute,
                ),
                CollabMessage(
                    "msg-frame-1", "topic-frame-inspection", "hector-ortiz", "Hector Ortiz",
                    "Corridor C studs will be done this afternoon. Ready for inspection tomorrow AM?",
                    now - 120 * minute,
                ),
                CollabMessage(
                    "msg-frame-2", "topic-frame-inspection", CurrentUser.ID, CurrentUser.NAME,
                    "Booking the inspector for 9am. Keep the head-of-wall clips exposed.",
                    now - 90 * minute,
                ),
                CollabMessage(
                    "msg-headwall-1", "topic-headwall-heights", "dave-miller", "Dave Miller",
                    "Are headwalls centred at 60 inches or 54? Drawings disagree with the submittal.",
                    now - 200 * minute,
                ),
                CollabMessage(
                    "msg-pour-1", "topic-saturday-pour", CurrentUser.ID, CurrentUser.NAME,
                    "Gate 2 is the only access Saturday. Park on the north lot and walk in.",
                    now - 400 * minute,
                ),
            ),
        )
    }
}
