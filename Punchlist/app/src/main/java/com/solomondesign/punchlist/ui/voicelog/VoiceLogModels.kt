package com.solomondesign.punchlist.ui.voicelog

/**
 * The states of the Voice-to-Log demo flow (see `voice-to-log-spec.md`), driven by
 * local Compose state in [VoiceLogScreen] — this is a self-contained proof-of-concept
 * (no ViewModel, no repository, no real audio/AI pipeline), matching the scope of the
 * earlier design-system button demo.
 */
sealed interface VoiceLogUiState {
    data object Recording : VoiceLogUiState
    data class Parsing(val transcript: String) : VoiceLogUiState
    data object Review : VoiceLogUiState
    data object Submitted : VoiceLogUiState
}

/** One row under "Labor & Time Cards" on the review screen. */
data class LaborCard(
    val id: String,
    val name: String,
    val trade: String,
    val hours: Double,
)

/** One row under "Materials Installed" on the review screen. */
data class MaterialCard(
    val id: String,
    val quantity: Double,
    val unit: String,
    val description: String,
)

/** One row under "Site Delays & Weather" on the review screen. */
data class DelayCard(
    val id: String,
    val hours: Double,
    val cause: String,
)

/** One row under "Automated Issues / Progress Photos" on the review screen. */
data class IssueCard(
    val id: String,
    val title: String,
    val location: String,
)
