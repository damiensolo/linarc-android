package com.solomondesign.app.ui.collab

/**
 * What a conversation is about. Conversation is one primitive in this app: a topic either
 * stands alone (subject `null`, started from the Collaboration tool) or is *the* discussion
 * for one object — a record, a task, a plan pin, a photo. Object detail screens render that
 * discussion inline through `DiscussionSection`; the Collaboration tool is the index of every
 * thread in the project. Never add a second comment model beside this one.
 */
data class CollabSubject(val kind: Kind, val id: String) {
    enum class Kind(val label: String) {
        RECORD("Record"),
        TASK("Task"),
        PLAN_PIN("Plan pin"),
        IMAGE("Photo"),
    }
}

data class CollabTopic(
    val id: String,
    val title: String,
    val location: String,
    /** [com.solomondesign.app.ui.demo.CrewMember.id] values, plus [CurrentUser.ID]. */
    val participantIds: List<String>,
    val unreadCount: Int = 0,
    val lastActivityMillis: Long,
    /** The object this thread belongs to; null for a free-standing topic. At most one topic per subject. */
    val subject: CollabSubject? = null,
)

data class CollabMessage(
    val id: String,
    val topicId: String,
    val authorId: String,
    /** Denormalised because the current user is not part of the crew roster. */
    val authorName: String,
    val body: String,
    val timestampMillis: Long,
    val queued: Boolean = false,
)

fun CollabTopic.subtitle(): String = "$location · ${participantIds.size} people"

/** The signed-in demo user. Kept here so Collaboration and Field tasks agree on "mine". */
object CurrentUser {
    const val ID = "alex-rivera"
    const val NAME = "Alex Rivera"
}
