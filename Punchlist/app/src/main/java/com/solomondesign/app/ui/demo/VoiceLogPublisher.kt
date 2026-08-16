package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.voicelog.DailyLogRecord
import com.solomondesign.app.ui.voicelog.IssueCard

data class VoiceLogPublishResult(
    val items: List<StreamItem>,
    val pins: List<PlanPin>,
)

/**
 * Pure mapping from a submitted voice log onto Today stream items and Plan pins.
 * Column 4 (and similar "near …" locations) land on the Area B Column 4 pin slot.
 */
object VoiceLogPublisher {

    const val COLUMN_4_X = 0.72f
    const val COLUMN_4_Y = 0.38f

    fun from(record: DailyLogRecord): VoiceLogPublishResult {
        val items = mutableListOf<StreamItem>()
        val pins = mutableListOf<PlanPin>()

        items.add(
            StreamItem(
                id = "stream-log-${record.id}",
                kind = StreamKind.DAILY_LOG,
                title = "Voice daily log",
                subtitle = record.transcript.ifBlank { "Submitted from voice" }.take(120),
                timestampMillis = record.timestampMillis,
                relatedRecordId = record.id,
            ),
        )

        record.delayCards.forEach { delay ->
            items.add(
                StreamItem(
                    id = "stream-${delay.id}-${record.id}",
                    kind = StreamKind.BLOCKER,
                    title = delay.cause,
                    subtitle = "${delay.hours} hrs · from voice log",
                    timestampMillis = record.timestampMillis,
                    relatedRecordId = record.id,
                ),
            )
        }

        record.issueCards.forEach { issue ->
            items.add(
                StreamItem(
                    id = "stream-${issue.id}-${record.id}",
                    kind = StreamKind.ISSUE,
                    title = issue.title,
                    subtitle = issue.location,
                    timestampMillis = record.timestampMillis,
                    relatedRecordId = record.id,
                ),
            )
            pins.add(pinForIssue(issue, record.id))
        }

        return VoiceLogPublishResult(items = items, pins = pins)
    }

    fun pinForIssue(issue: IssueCard, recordId: String): PlanPin {
        val column4 = issue.location.contains("column 4", ignoreCase = true)
        return PlanPin(
            id = "pin-${issue.id}-$recordId",
            kind = PinKind.ISSUE,
            label = issue.title,
            snippet = issue.location.ifBlank { "From voice log" },
            xFraction = if (column4) COLUMN_4_X else 0.50f,
            yFraction = if (column4) COLUMN_4_Y else 0.55f,
            relatedRecordId = recordId,
        )
    }
}
