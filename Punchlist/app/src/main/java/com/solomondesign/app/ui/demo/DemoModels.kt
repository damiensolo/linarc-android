package com.solomondesign.app.ui.demo

import androidx.compose.ui.graphics.Color
import com.solomondesign.app.ui.theme.PresenceAssigned
import com.solomondesign.app.ui.theme.PresenceOffSite
import com.solomondesign.app.ui.theme.PresenceOnSite

enum class StreamKind {
    CREW,
    BLOCKER,
    ISSUE,
    PHOTO,
    DAILY_LOG,
    TASK,
}

enum class PinKind {
    ISSUE,
    PHOTO,
    LOG,
}

data class StreamItem(
    val id: String,
    val kind: StreamKind,
    val title: String,
    val subtitle: String,
    val timestampMillis: Long,
    val relatedRecordId: String? = null,
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

data class OutboxItem(
    val id: String,
    val title: String,
    val subtitle: String,
)

data class CrewMember(
    val name: String,
    val trade: String,
    val presence: CrewPresence,
    val photoRes: Int? = null,
)

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

fun CrewPresence.badgeColor(): Color = when (this) {
    CrewPresence.ON_SITE -> PresenceOnSite
    CrewPresence.ASSIGNED -> PresenceAssigned
    CrewPresence.OFF_SITE -> PresenceOffSite
}
