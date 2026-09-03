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
import com.solomondesign.app.ui.tasks.FieldTask
import com.solomondesign.app.ui.theme.AvatarPalette
import com.solomondesign.app.ui.timecards.COST_CODES
import com.solomondesign.app.ui.timecards.TimeCardRepository
import com.solomondesign.app.ui.timecards.TimeEntry
import com.solomondesign.app.ui.timecards.shiftHoursBetween
import com.solomondesign.app.ui.today.OwnerTodayVariant
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

    /**
     * Which Owner Today layout renders — both versions stay demoable side-by-side: the
     * Demo: view as picker lists Owner as one row per layout, and tapping a row sets this
     * and selects the Owner persona together. Resets to the dashboard on [clear]: unlike
     * [splashVariant] this is a per-demo comparison pick, not a brand choice.
     */
    var ownerTodayVariant by mutableStateOf(OwnerTodayVariant.DASHBOARD)

    var dayStarted by mutableStateOf(false)
        private set

    /**
     * The crew member the Crew persona views the day through. Demo: view as is a strategy
     * lens, not a login — the signed-in profile identity stays fixed (see
     * [com.solomondesign.app.ui.profile.CurrentUser]) — so the Crew view borrows Hector
     * Ortiz, who carries a real assignment, checklist, and time entries in the seed data.
     * Null for every other persona.
     */
    val crewViewMember: CrewMember?
        get() = if (persona == FieldPersona.CREW) crewMember(CREW_VIEW_MEMBER_ID) else null

    private const val CREW_VIEW_MEMBER_ID = "hector-ortiz"

    /**
     * The crew member the Subcontractor persona views the project through — same borrowed-lens
     * pattern as [crewViewMember]. Sam Reyes carries the plumbing trade, whose med-gas task is
     * blocked on RFI-118 in the seed data — the strongest "assigned work" demo. The sub's
     * scope is the whole trade (see `forTrade`), not just this one member's assignments.
     * Null for every other persona.
     */
    val subcontractorMember: CrewMember?
        get() = if (persona == FieldPersona.SUBCONTRACTOR) crewMember(SUB_VIEW_MEMBER_ID) else null

    private const val SUB_VIEW_MEMBER_ID = "sam-reyes"

    /** Title of the most recent inspection request this session — the card's receipt line. */
    var lastInspectionRequestTitle by mutableStateOf<String?>(null)
        private set

    /** When the Crew view's running shift started, or null while off shift. */
    var shiftStartedAtMillis by mutableStateOf<Long?>(null)
        private set

    /** Hours of the most recently ended shift this session — the My shift card's receipt. */
    var lastShiftHours by mutableStateOf<Double?>(null)
        private set

    val streamItems = mutableStateListOf<StreamItem>()
    val pins = mutableStateListOf<PlanPin>()
    val outboxItems = mutableStateListOf<OutboxItem>()

    val queuedOutboxCount: Int
        get() = outboxItems.count { it.status == OutboxStatus.QUEUED }

    /**
     * Every publish-style action funnels here: it commits locally, then waits for signal.
     * Publishers pass the one related id matching what they queued (see [OutboxItem]) so the
     * Outbox row can deep-link back into the owning tool's detail.
     */
    fun queueOutbox(
        id: String,
        title: String,
        detail: String = "",
        relatedImageId: String? = null,
        relatedVideoId: String? = null,
        relatedFieldRecordId: String? = null,
        relatedLogId: String? = null,
        relatedTaskId: String? = null,
        relatedTopicId: String? = null,
        relatedCrewMemberId: String? = null,
    ) {
        outboxItems.add(
            OutboxItem(
                id = id,
                title = title,
                detail = detail,
                relatedImageId = relatedImageId,
                relatedVideoId = relatedVideoId,
                relatedFieldRecordId = relatedFieldRecordId,
                relatedLogId = relatedLogId,
                relatedTaskId = relatedTaskId,
                relatedTopicId = relatedTopicId,
                relatedCrewMemberId = relatedCrewMemberId,
            ),
        )
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

    fun startShift(nowMillis: Long = System.currentTimeMillis()) {
        if (shiftStartedAtMillis == null) shiftStartedAtMillis = nowMillis
    }

    /**
     * Ends the running shift: logs a real [TimeEntry] on the crew-view member's time card —
     * which queues one Outbox entry through [TimeCardRepository.addEntry], like any publish —
     * and returns the logged hours. Null (a no-op) when no shift is running or no crew member
     * is in view, so a stale End tap can never fabricate an entry.
     */
    fun endShift(nowMillis: Long = System.currentTimeMillis()): Double? {
        val started = shiftStartedAtMillis ?: return null
        val member = crewViewMember ?: return null
        val hours = shiftHoursBetween(started, nowMillis)
        TimeCardRepository.addEntry(
            TimeEntry(
                id = "te-shift-$nowMillis",
                crewMemberId = member.id,
                dateLabel = "Today",
                costCode = COST_CODES[0],
                hours = hours,
                note = "Logged from Today's shift clock",
                queued = true,
            ),
        )
        shiftStartedAtMillis = null
        lastShiftHours = hours
        return hours
    }

    fun publishVoiceLog(record: DailyLogRecord) {
        val published = VoiceLogPublisher.from(record)
        published.items.forEach { streamItems.add(0, it) }
        published.pins.forEach { pins.add(it) }
        queueOutbox(
            id = "outbox-log-${record.id}",
            title = "Voice daily log",
            detail = record.transcript.ifBlank { "Submitted from voice" }.take(60),
            relatedLogId = record.id,
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
            relatedImageId = photoId,
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
        queueOutbox(
            id = "outbox-$videoId",
            title = "Video: $title",
            detail = subtitle,
            relatedVideoId = videoId,
        )
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
            relatedFieldRecordId = record.id,
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

    /**
     * The Subcontractor's Request Inspection: publishes a request row on Today (linked back
     * to the task, so it never dead-ends) and queues ONE Outbox entry — offline-first, like
     * every publish. No record is created: in this prototype the request is a message to the
     * GC, not a QC artifact; the inspector's punch item is what comes back.
     */
    fun requestInspection(task: FieldTask) {
        val now = System.currentTimeMillis()
        streamItems.add(
            0,
            StreamItem(
                id = "inspect-$now",
                kind = StreamKind.TASK,
                title = "Inspection requested: ${task.title}",
                subtitle = task.location,
                timestampMillis = now,
                relatedTaskId = task.id,
            ),
        )
        queueOutbox(
            id = "outbox-inspect-$now",
            title = "Inspection request: ${task.title}",
            detail = task.location,
            relatedTaskId = task.id,
        )
        lastInspectionRequestTitle = task.title
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
        ownerTodayVariant = OwnerTodayVariant.DASHBOARD
        dayStarted = false
        shiftStartedAtMillis = null
        lastShiftHours = null
        lastInspectionRequestTitle = null
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
                // Linked to real seeds (ProjectImageRepository / RecordRepository) so the queue
                // taps through to detail from first launch — an Outbox row never dead-ends.
                OutboxItem(
                    "outbox-1",
                    "Photo: Area B framing",
                    "Area B",
                    relatedImageId = "img-corridor-c",
                ),
                OutboxItem(
                    "outbox-2",
                    "Issue: Missing guardrail",
                    "Level 2",
                    relatedFieldRecordId = "rec-seed-guardrail",
                ),
            ),
        )
    }
}
