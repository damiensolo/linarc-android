package com.solomondesign.punchlist.ui.voicelog

import androidx.compose.runtime.mutableStateListOf

/** One submitted voice-to-log entry: the real recorded audio plus what was parsed from it. */
data class DailyLogRecord(
    val id: String,
    val timestampMillis: Long,
    val projectName: String,
    val audioFilePath: String,
    val transcript: String,
    val laborCards: List<LaborCard>,
    val materialCards: List<MaterialCard>,
    val delayCards: List<DelayCard>,
    val issueCards: List<IssueCard>,
)

/**
 * In-memory store for submitted daily logs, scoped to this process. A real build would persist
 * this (e.g. Room) so it survives process death — that's a new Gradle dependency, which this
 * project's conventions say to ask about before adding. This is enough to prove the recording is
 * genuinely viewable/playable after submission within a running app session.
 */
object DailyLogRepository {
    private val _records = mutableStateListOf<DailyLogRecord>()
    val records: List<DailyLogRecord> get() = _records

    fun add(record: DailyLogRecord) {
        _records.add(0, record)
    }

    fun find(id: String): DailyLogRecord? = _records.firstOrNull { it.id == id }

    /** Resets state — this is a process-wide singleton, so tests must call this between runs. */
    fun clear() {
        _records.clear()
    }
}
