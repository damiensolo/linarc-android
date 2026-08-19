package com.solomondesign.app.ui.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.solomondesign.app.R
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.images.ImageSource
import com.solomondesign.app.ui.images.ProjectImage
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.persona.FieldPersona
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

    val crew = listOf(
        CrewMember("Hector Ortiz", "Framing (Carpentry)", CrewPresence.ON_SITE, R.drawable.crew_hector),
        CrewMember("Dave Miller", "Framing (Carpentry)", CrewPresence.ON_SITE),
        CrewMember("Maria Chen", "Electrical", CrewPresence.ASSIGNED, R.drawable.crew_maria),
        CrewMember("Sam Reyes", "Plumbing", CrewPresence.OFF_SITE),
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
    }

    /**
     * Publishes a captured photo to Today, the Plan sheet, and the Images grid.
     *
     * [captureKey] is a [com.solomondesign.app.ui.images.CapturedBitmapStore] key rather than a
     * Bitmap, which keeps this repository free of `android.graphics` and unit-testable.
     */
    fun addPhoto(
        title: String,
        subtitle: String,
        createIssue: Boolean,
        captureKey: String? = null,
        tags: List<String> = emptyList(),
    ) {
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
                source = captureKey?.let(ImageSource::Captured)
                    ?: ImageSource.Swatch(seed = pins.size),
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
                title = "Issue from photo",
                location = AREA,
                note = subtitle,
                xFraction = 0.45f,
                yFraction = 0.52f,
            )
        }
    }

    fun addIssue(
        title: String,
        location: String,
        note: String,
        xFraction: Float = 0.58f,
        yFraction: Float = 0.62f,
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

    fun clear() {
        persona = FieldPersona.FOREMAN
        darkTheme = true
        dayStarted = false
        streamItems.clear()
        pins.clear()
        outboxItems.clear()
        seed()
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        streamItems.addAll(
            listOf(
                StreamItem(
                    id = "seed-task-1",
                    kind = StreamKind.TASK,
                    title = "Frame inspection",
                    subtitle = "Area B · gridline C",
                    timestampMillis = now - 3_600_000,
                ),
                StreamItem(
                    id = "seed-photo-1",
                    kind = StreamKind.PHOTO,
                    title = "Yesterday progress",
                    subtitle = "Area B · structural framing",
                    timestampMillis = now - 86_400_000,
                ),
            ),
        )
        pins.add(
            PlanPin(
                id = "seed-pin-photo",
                kind = PinKind.PHOTO,
                label = "Yesterday progress",
                snippet = "Area B · structural framing",
                xFraction = 0.28f,
                yFraction = 0.58f,
            ),
        )
        outboxItems.addAll(
            listOf(
                OutboxItem("outbox-1", "Photo: Area B framing", "Queued · waiting for signal"),
                OutboxItem("outbox-2", "Issue: Missing guardrail", "Queued · waiting for signal"),
            ),
        )
    }
}
