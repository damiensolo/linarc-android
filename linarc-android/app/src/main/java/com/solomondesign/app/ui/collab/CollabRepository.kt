package com.solomondesign.app.ui.collab

import androidx.compose.runtime.mutableStateListOf
import com.solomondesign.app.ui.demo.DemoProjectRepository

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

    fun messagesFor(topicId: String): List<CollabMessage> =
        _messages.filter { it.topicId == topicId }.sortedBy { it.timestampMillis }

    fun lastMessagePreview(topicId: String): String =
        messagesFor(topicId).lastOrNull()?.body.orEmpty()

    fun createTopic(title: String, firstMessage: String, participantIds: List<String>): String? {
        if (title.isBlank()) return null
        val now = System.currentTimeMillis()
        val id = "topic-new-${nextId++}"
        _topics.add(
            CollabTopic(
                id = id,
                title = title.trim(),
                location = DemoProjectRepository.AREA,
                participantIds = (participantIds + CurrentUser.ID).distinct(),
                unreadCount = 0,
                lastActivityMillis = now,
            ),
        )
        if (firstMessage.isNotBlank()) postMessage(id, firstMessage)
        return id
    }

    fun postMessage(topicId: String, body: String) {
        if (body.isBlank()) return
        val index = _topics.indexOfFirst { it.id == topicId }
        if (index < 0) return
        val now = System.currentTimeMillis()
        _messages.add(
            CollabMessage(
                id = "msg-new-${nextId++}",
                topicId = topicId,
                authorId = CurrentUser.ID,
                authorName = CurrentUser.NAME,
                body = body.trim(),
                timestampMillis = now,
                queued = true,
            ),
        )
        _topics[index] = _topics[index].copy(lastActivityMillis = now)
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

    private fun seed() {
        val now = System.currentTimeMillis()
        val minute = 60_000L

        _topics.addAll(
            listOf(
                CollabTopic(
                    id = "topic-col4-medgas",
                    title = "Column 4 med gas conflict",
                    location = "Area B · Column 4",
                    participantIds = listOf("sam-reyes", "maria-chen", CurrentUser.ID),
                    unreadCount = 2,
                    lastActivityMillis = now - 5 * minute,
                ),
                CollabTopic(
                    id = "topic-frame-inspection",
                    title = "Level 2 framing inspection window",
                    location = "Area B · Level 2",
                    participantIds = listOf("hector-ortiz", CurrentUser.ID),
                    unreadCount = 0,
                    lastActivityMillis = now - 90 * minute,
                ),
                CollabTopic(
                    id = "topic-headwall-heights",
                    title = "Headwall backing heights",
                    location = "Area B · rooms 5–8",
                    participantIds = listOf("dave-miller", CurrentUser.ID),
                    unreadCount = 1,
                    lastActivityMillis = now - 200 * minute,
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
