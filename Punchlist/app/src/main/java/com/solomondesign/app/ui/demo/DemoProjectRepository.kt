package com.solomondesign.app.ui.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.solomondesign.app.R
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ProjectImage
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.video.CapturedVideo
import com.solomondesign.app.ui.video.VideoRepository
import com.solomondesign.app.ui.video.formatVideoDuration
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.records.AttachmentKind
import com.solomondesign.app.ui.records.FieldRecord
import com.solomondesign.app.ui.records.RecordCategory
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.splash.SplashVariant
import com.solomondesign.app.ui.theme.AvatarPalette
import com.solomondesign.app.ui.voicelog.DailyLogRecord

/**
 * In-memory demo store for the Foreman prototype: persona, Start My Day, Today stream, Plan pins.
 * Compose snapshot state so screens recompose when Voice-to-Log publishes into this store.
 */
object DemoProjectRepository {
    const val PROJECT_NAME = "Riverside Medical"
    const val AREA = "Area B"

    var persona by mutableStateOf(FieldPersona.FOREMAN)
        private set

    var darkTheme by mutableStateOf(true)

    /** Launch-brand treatment. Kept across [clear] so A/B picks survive logout. */
    var splashVariant by mutableStateOf(SplashVariant.DEPTH)

    var dayStarted by mutableStateOf(false)
        private set

    val streamItems = mutableStateListOf<StreamItem>()
    val pins = mutableStateListOf<PlanPin>()
    val outboxItems = mutableStateListOf<OutboxItem>()

    val queuedOutboxCount: Int
        get() = outboxItems.count { it.status == OutboxStatus.QUEUED }

    /** Every publish-style action funnels here: it commits locally, then waits for signal. */
    fun queueOutbox(id: String, title: String, detail: String = "") {
        outboxItems.add(OutboxItem(id = id, title = title, detail = detail))
    }

    /**
     * Flips the oldest queued entry to [OutboxStatus.SENT] and returns it, or null once the
     * queue is drained. The Outbox screen's "send all" walks this one entry at a time so the
     * demo shows the queue visibly draining when signal "returns" — no real sync happens.
     */
    fun sendNextQueuedOutboxItem(): OutboxItem? {
        val index = outboxItems.indexOfFirst { it.status == OutboxStatus.QUEUED }
        if (index == -1) return null
        val sent = outboxItems[index].copy(status = OutboxStatus.SENT)
        outboxItems[index] = sent
        return sent
    }

    /** Pin id -> its comment thread. A state map so the sheet viewer recomposes on new comments. */
    private val pinComments = mutableStateMapOf<String, List<PinComment>>()

    val crew = listOf(
        CrewMember("Hector Ortiz", "Framing (Carpentry)", CrewPresence.ON_SITE, R.drawable.crew_hector),
        CrewMember("Dave Miller", "Framing (Carpentry)", CrewPresence.ON_SITE),
        CrewMember("Maria Chen", "Electrical", CrewPresence.ASSIGNED, R.drawable.crew_maria),
        CrewMember("Sam Reyes", "Plumbing", CrewPresence.OFF_SITE),
    )

    /**
     * Startup Project List rows. Only [PROJECT_NAME] carries real seeded data — the rest are
     * demo flavor so the picker doesn't read as a single-item list. Selecting any of them opens
     * the same seeded Today/Plan/Tools data; see "Startup flow" in Mobile Structure Validated v1.
     */
    val projects = listOf(
        DemoProject("riverside-medical", PROJECT_NAME, "220 Riverside Dr · $AREA"),
        DemoProject("harbor-bridge", "Harbor Bridge Widening", "4th St & Harbor Ave"),
        DemoProject("maple-street-school", "Maple Street School Addition", "88 Maple St"),
        DemoProject("downtown-transit-hub", "Downtown Transit Hub", "500 Transit Way"),
    )

    init {
        seed()
    }

    fun crewMember(id: String): CrewMember? = crew.firstOrNull { it.id == id }

    fun crewIndexOf(id: String): Int = crew.indexOfFirst { it.id == id }

    /**
     * The same avatar colour the Today roster uses, so a person looks identical on every screen.
     * [AvatarPalette] is index-driven, so resolving by id rather than by list position is what
     * keeps Crew, Time cards, and Today in agreement.
     */
    fun avatarColorFor(id: String?): Color =
        AvatarPalette.colorAt(crewIndexOf(id.orEmpty()).coerceAtLeast(0))

    fun selectPersona(next: FieldPersona) {
        if (next.isLive) {
            persona = next
        }
    }

    fun confirmStartMyDay() {
        dayStarted = true
    }

    fun publishVoiceLog(record: DailyLogRecord) {
        val published = VoiceLogPublisher.from(record)
        published.items.forEach { streamItems.add(0, it) }
        published.pins.forEach { pins.add(it) }
        queueOutbox(
            id = "outbox-log-${record.id}",
            title = "Voice daily log",
            detail = record.transcript.ifBlank { "Submitted from voice" }.take(60),
        )
    }

    /**
     * Publishes a captured photo to Today, the Plan sheet, and the Images grid.
     *
     * [filePath] points at a full-resolution capture in
     * [com.solomondesign.app.ui.images.CapturedMediaStore]; [captureKey] is the older in-memory
     * [com.solomondesign.app.ui.images.CapturedBitmapStore] key. Both are plain strings, which
     * keeps this repository free of `android.graphics` and unit-testable.
     */
    fun addPhoto(
        title: String,
        subtitle: String,
        createIssue: Boolean,
        captureKey: String? = null,
        filePath: String? = null,
        tags: List<String> = emptyList(),
        hasMarkup: Boolean = false,
    ): String {
        val now = System.currentTimeMillis()
        val photoId = "photo-$now"
        ProjectImageRepository.add(
            ProjectImage(
                id = photoId,
                title = title,
                area = AREA,
                tags = tags,
                capturedAtMillis = now,
                authorName = CurrentUser.NAME,
                source = filePath?.let(ImageSource::CapturedFile)
                    ?: captureKey?.let(ImageSource::Captured)
                    ?: ProjectImageRepository.demoPhotoSource(seed = pins.size),
                hasMarkup = hasMarkup,
            ),
        )
        streamItems.add(
            0,
            StreamItem(
                id = "stream-$photoId",
                kind = StreamKind.PHOTO,
                title = title,
                subtitle = subtitle,
                timestampMillis = now,
                relatedImageId = photoId,
            ),
        )
        pins.add(
            PlanPin(
                id = "pin-$photoId",
                kind = PinKind.PHOTO,
                label = title,
                snippet = subtitle,
                xFraction = 0.42f,
                yFraction = 0.48f,
            ),
        )
        if (createIssue) {
            addIssue(
                title = title.ifBlank { "Issue from photo" },
                location = AREA,
                note = subtitle,
                xFraction = 0.45f,
                yFraction = 0.52f,
            )
        }
        queueOutbox(
            id = "outbox-$photoId",
            title = "Photo: ${title.ifBlank { "Site photo" }}",
            detail = subtitle,
        )
        return photoId
    }

    /**
     * Publishes a captured video to Today and the Plan sheet. The mp4 itself lives
     * in [com.solomondesign.app.ui.images.CapturedMediaStore]; metadata (including the spoken
     * description) lands in [VideoRepository] for the playback screen.
     */
    fun addCapturedVideo(
        title: String,
        note: String,
        videoPath: String,
        transcript: String,
        durationSeconds: Int,
    ): String {
        val now = System.currentTimeMillis()
        val videoId = "video-$now"
        VideoRepository.add(
            CapturedVideo(
                id = videoId,
                title = title,
                note = note,
                videoPath = videoPath,
                transcript = transcript,
                durationSeconds = durationSeconds,
                capturedAtMillis = now,
                authorName = CurrentUser.NAME,
            ),
        )
        val subtitle = listOf("Video · ${formatVideoDuration(durationSeconds)}", note)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        streamItems.add(
            0,
            StreamItem(
                id = "stream-$videoId",
                kind = StreamKind.VIDEO,
                title = title,
                subtitle = subtitle,
                timestampMillis = now,
                relatedVideoId = videoId,
            ),
        )
        pins.add(
            PlanPin(
                id = "pin-$videoId",
                kind = PinKind.VIDEO,
                label = title,
                snippet = subtitle,
                xFraction = 0.36f,
                yFraction = 0.44f,
            ),
        )
        queueOutbox(id = "outbox-$videoId", title = "Video: $title", detail = subtitle)
        return videoId
    }

    /**
     * Raises a blocker: a blocking row on Today plus a Plan pin. Only call this for actual
     * work stoppages — a merely logged issue publishes through [addRecord] without it.
     * [recordId] is the [FieldRecord] behind the blocker, when there is one — it makes the
     * Today row tappable through to the record detail.
     */
    fun addIssue(
        title: String,
        location: String,
        note: String,
        xFraction: Float = 0.58f,
        yFraction: Float = 0.62f,
        recordId: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val issueId = "issue-$now"
        streamItems.add(
            0,
            StreamItem(
                id = "stream-$issueId",
                kind = StreamKind.ISSUE,
                title = title,
                subtitle = listOf(location, note).filter { it.isNotBlank() }.joinToString(" · "),
                timestampMillis = now,
                relatedFieldRecordId = recordId,
                blocking = true,
            ),
        )
        pins.add(
            PlanPin(
                id = "pin-$issueId",
                kind = PinKind.ISSUE,
                label = title,
                snippet = location.ifBlank { note },
                xFraction = if (location.contains("column 4", ignoreCase = true)) {
                    VoiceLogPublisher.COLUMN_4_X
                } else {
                    xFraction
                },
                yFraction = if (location.contains("column 4", ignoreCase = true)) {
                    VoiceLogPublisher.COLUMN_4_Y
                } else {
                    yFraction
                },
            ),
        )
    }

    /**
     * Publishes a created record (issue / incident / punch item) everywhere it belongs:
     * the [RecordRepository] behind its tool list, one queued Outbox entry (offline-first),
     * and a Plan pin at its location. Issued ≠ blocked: only a record explicitly marked
     * [FieldRecord.blocksWork] (by the reporter or its type's configured default) also lands
     * on Today's Blockers — and it blocks its scoped task/trade/work package, never the crew.
     * Photo attachments are linked back to the record so the viewer shows the association.
     */
    fun addRecord(record: FieldRecord) {
        RecordRepository.add(record)
        queueOutbox(
            id = "outbox-${record.id}",
            title = "${record.category.label}: ${record.title}",
            detail = record.location,
        )
        if (record.blocksWork) {
            addIssue(
                title = record.title,
                location = record.location,
                note = record.blockingReason.ifBlank { record.description },
                recordId = record.id,
            )
        } else {
            pins.add(
                PlanPin(
                    id = "pin-${record.id}",
                    kind = PinKind.ISSUE,
                    label = record.title,
                    snippet = listOf(record.location, record.description)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    xFraction = 0.52f,
                    yFraction = 0.36f,
                ),
            )
        }
        record.attachments
            .filter { it.kind == AttachmentKind.PHOTO }
            .forEach { ProjectImageRepository.linkRecord(it.ref, record.id) }
    }

    fun pinCommentsFor(pinId: String): List<PinComment> = pinComments[pinId].orEmpty()

    /** Adds a comment (as the signed-in user) to a pin's thread; blank text is rejected. */
    fun addPinComment(pinId: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val comment = PinComment(
            id = "pin-comment-${System.currentTimeMillis()}-${pinCommentsFor(pinId).size}",
            authorName = CurrentUser.NAME,
            text = trimmed,
            timestampMillis = System.currentTimeMillis(),
        )
        pinComments[pinId] = pinCommentsFor(pinId) + comment
        return true
    }

    /**
     * Publishes a pin's unpublished comments to the team: flips them to published and queues ONE
     * outbox entry for the batch (offline-first — the outbox is where "sent" things wait for
     * signal). Returns how many were published; 0 means nothing changed and nothing was queued.
     */
    fun publishPinComments(pinId: String): Int {
        val thread = pinCommentsFor(pinId)
        val unpublishedCount = thread.count { !it.published }
        if (unpublishedCount == 0) return 0
        pinComments[pinId] = thread.map { it.copy(published = true) }
        val label = pins.firstOrNull { it.id == pinId }?.label ?: "Plan pin"
        queueOutbox(
            // Size suffix keeps ids unique even for two publishes inside one millisecond.
            id = "outbox-$pinId-${System.currentTimeMillis()}-${outboxItems.size}",
            title = "Pin comments: $label",
            detail = "$unpublishedCount comment${if (unpublishedCount == 1) "" else "s"}",
        )
        return unpublishedCount
    }

    fun clear() {
        persona = FieldPersona.FOREMAN
        darkTheme = true
        dayStarted = false
        streamItems.clear()
        pins.clear()
        outboxItems.clear()
        pinComments.clear()
        seed()
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        streamItems.addAll(
            listOf(
                // Today's face of RecordRepository's seeded "rec-seed-issue", which is marked
                // blocksWork — so the seed mirrors what addRecord does live for a blocking
                // record: a linked Blockers row plus a Column 4 pin. (The other record seeds
                // are logged-not-blocking, so they stay off Today by design.)
                StreamItem(
                    id = "stream-rec-seed-issue",
                    kind = StreamKind.ISSUE,
                    title = "Med-gas conflict at Column 4",
                    subtitle = "Column 4 · blocks close-in until re-route",
                    timestampMillis = now - 4 * 3_600_000,
                    relatedFieldRecordId = "rec-seed-issue",
                    blocking = true,
                ),
                // Linked to RecordRepository's "rec-seed-inspection" punch item so the row
                // opens a real record — nothing on Today should dead-end.
                StreamItem(
                    id = "seed-task-1",
                    kind = StreamKind.TASK,
                    title = "Frame inspection",
                    subtitle = "Area B · gridline C",
                    timestampMillis = now - 3_600_000,
                    relatedFieldRecordId = "rec-seed-inspection",
                ),
                // Ids follow the stream-/pin-<imageId> convention (not seed-*) on purpose: this
                // row is the Today face of ProjectImageRepository's seeded "img-yesterday", so
                // ProjectImageRepository.delete cleans all three together — same as a live capture.
                StreamItem(
                    id = "stream-img-yesterday",
                    kind = StreamKind.PHOTO,
                    title = "Yesterday progress",
                    subtitle = "Area B · structural framing",
                    timestampMillis = now - 86_400_000,
                    relatedImageId = "img-yesterday",
                ),
            ),
        )
        pins.add(
            PlanPin(
                id = "pin-img-yesterday",
                kind = PinKind.PHOTO,
                label = "Yesterday progress",
                snippet = "Area B · structural framing",
                xFraction = 0.28f,
                yFraction = 0.58f,
            ),
        )
        pins.add(
            PlanPin(
                id = "pin-rec-seed-issue",
                kind = PinKind.ISSUE,
                label = "Med-gas conflict at Column 4",
                snippet = "Column 4",
                xFraction = VoiceLogPublisher.COLUMN_4_X,
                yFraction = VoiceLogPublisher.COLUMN_4_Y,
            ),
        )
        outboxItems.addAll(
            listOf(
                OutboxItem("outbox-1", "Photo: Area B framing", "Area B"),
                OutboxItem("outbox-2", "Issue: Missing guardrail", "Level 2"),
            ),
        )
    }
}
