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
