package com.solomondesign.app.ui.collab

data class CollabTopic(
    val id: String,
    val title: String,
    val location: String,
    /** [com.solomondesign.app.ui.demo.CrewMember.id] values, plus [CurrentUser.ID]. */
    val participantIds: List<String>,
    val unreadCount: Int = 0,
    val lastActivityMillis: Long,
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
