package com.solomondesign.punchlist.ui.demo

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
    val status: String,
)
