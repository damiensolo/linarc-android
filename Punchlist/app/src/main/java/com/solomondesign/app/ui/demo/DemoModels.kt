package com.solomondesign.app.ui.demo

import androidx.compose.ui.graphics.Color
import com.solomondesign.app.ui.theme.LightPresenceAssigned
import com.solomondesign.app.ui.theme.LightPresenceOnSite
import com.solomondesign.app.ui.theme.PresenceAssigned
import com.solomondesign.app.ui.theme.PresenceOffSite
import com.solomondesign.app.ui.theme.PresenceOnSite

enum class StreamKind {
    CREW,
    BLOCKER,
    ISSUE,
    PHOTO,
    VIDEO,
    DAILY_LOG,
    TASK,
}

enum class PinKind {
    ISSUE,
    PHOTO,
    VIDEO,
    LOG,
}

data class StreamItem(
    val id: String,
    val kind: StreamKind,
    val title: String,
    val subtitle: String,
    val timestampMillis: Long,
    val relatedRecordId: String? = null,
    /**
     * For [StreamKind.PHOTO] rows: the [com.solomondesign.app.ui.images.ProjectImage] id this
     * entry was published from. Today renders a thumbnail and deep-links into the full-screen
     * image viewer when set.
     */
    val relatedImageId: String? = null,
    /**
     * For [StreamKind.VIDEO] rows: the
     * [com.solomondesign.app.ui.video.CapturedVideo] id this entry was published
     * from. Today deep-links into the video playback screen when set.
     */
    val relatedVideoId: String? = null,
    /**
     * For rows published from a [com.solomondesign.app.ui.records.FieldRecord] (issue,
     * incident, or punch item): the record id. Today deep-links into that tool's record
     * detail when set. Distinct from [relatedRecordId], which is a voice daily-log id.
     */
    val relatedFieldRecordId: String? = null,
    /**
     * For rows raised about a [com.solomondesign.app.ui.tasks.FieldTask] (the Subcontractor's
     * inspection requests): the task id. Today deep-links into the Field task detail when set,
     * so a request row never dead-ends.
     */
    val relatedTaskId: String? = null,
    /**
     * True only for rows that represent an active work stoppage — records explicitly marked
     * "blocks work" and dictated delays. Today's Blockers section shows exactly these; a
     * logged-but-not-blocking issue stays off it (issued ≠ blocked).
     */
    val blocking: Boolean = false,
)

data class PlanPin(
    val id: String,
    val kind: PinKind,
    val label: String,
    val snippet: String,
    val xFraction: Float,
    val yFraction: Float,
    val relatedRecordId: String? = null,
)

enum class OutboxStatus {
    QUEUED,
    SENT,
}

/**
 * One publish-style action waiting for signal. Everything a user "sends" (records, time cards,
 * messages, photos, videos, daily logs, pin-comment batches) commits on-device first and queues
 * here; the Outbox screen's "send all" flips entries to [OutboxStatus.SENT] to demo signal
 * returning. There is still no real sync engine — that is an explicit prototype non-goal.
 */
data class OutboxItem(
    val id: String,
    val title: String,
    /** What is inside the entry (e.g. "2 comments", "Column 4"), not its queue state. */
    val detail: String = "",
    val status: OutboxStatus = OutboxStatus.QUEUED,
    // Same never-dead-end rule as [StreamItem]: an Outbox row links back to what it published,
    // so the queue is a receipt list, not a dead list. At most one of these is set per entry;
    // the Outbox screen deep-links into the owning tool's detail when the target still resolves.
    /** A published photo: the [com.solomondesign.app.ui.images.ProjectImage] id. */
    val relatedImageId: String? = null,
    /** A published video: the [com.solomondesign.app.ui.video.CapturedVideo] id. */
    val relatedVideoId: String? = null,
    /** An issue / incident / punch item: the [com.solomondesign.app.ui.records.FieldRecord] id. */
    val relatedFieldRecordId: String? = null,
    /** A voice daily log: the [com.solomondesign.app.ui.voicelog.DailyLogRecord] id. */
    val relatedLogId: String? = null,
    /** An inspection request: the [com.solomondesign.app.ui.tasks.FieldTask] id it was raised on. */
    val relatedTaskId: String? = null,
    /** A posted message: the [com.solomondesign.app.ui.collab.CollabTopic] id. */
    val relatedTopicId: String? = null,
    /** A time entry: the [CrewMember.id] whose time card it landed on. */
    val relatedCrewMemberId: String? = null,
)

/** Row subtitle combining [OutboxItem.detail] with queue state. Pure so the wording is testable. */
fun OutboxItem.statusLine(): String {
    val state = when (status) {
        OutboxStatus.QUEUED -> "Queued · waiting for signal"
        OutboxStatus.SENT -> "Sent to project"
    }
    return if (detail.isBlank()) state else "$detail · $state"
}

/**
 * One comment on a [PlanPin]'s thread in the sheet viewer. [published] flips when the thread is
 * pushed to the team — publish queues an outbox entry, matching this prototype's offline-first
 * story (nothing actually leaves the device).
 */
data class PinComment(
    val id: String,
    val authorName: String,
    val text: String,
    val timestampMillis: Long,
    val published: Boolean = false,
)

/** A row in the startup Project List picker. See [DemoProjectRepository.projects]. */
data class DemoProject(
    val id: String,
    val name: String,
    val address: String,
)

data class CrewMember(
    val name: String,
    val trade: String,
    val presence: CrewPresence,
    val photoRes: Int? = null,
    /**
     * Stable, route-safe identity. Demo data has no server ids, so it is derived from [name].
     * Declared last with a default so every existing constructor call is unaffected — Kotlin
     * default values may reference earlier parameters.
     */
    val id: String = crewIdFor(name),
)

/** "Hector Ortiz" -> "hector-ortiz". Pure so it can be unit tested on the JVM. */
fun crewIdFor(name: String): String =
    name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

enum class CrewPresence {
    ON_SITE,
    ASSIGNED,
    OFF_SITE,
}

fun CrewPresence.statusLabel(area: String): String = when (this) {
    CrewPresence.ON_SITE -> "On site"
    CrewPresence.ASSIGNED -> "Assigned · $area"
    CrewPresence.OFF_SITE -> "Off site"
}

/** Not @Composable: reads [DemoProjectRepository.darkTheme] directly, safe since callers already
 * recompose on that state (it drives [com.solomondesign.app.ui.theme.AppTheme] at the root). */
fun CrewPresence.badgeColor(): Color {
    val dark = DemoProjectRepository.darkTheme
    return when (this) {
        CrewPresence.ON_SITE -> if (dark) PresenceOnSite else LightPresenceOnSite
        CrewPresence.ASSIGNED -> if (dark) PresenceAssigned else LightPresenceAssigned
        CrewPresence.OFF_SITE -> PresenceOffSite
    }
}
