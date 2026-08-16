package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.voicelog.DailyLogRecord
import com.solomondesign.app.ui.voicelog.DelayCard
import com.solomondesign.app.ui.voicelog.IssueCard
import com.solomondesign.app.ui.voicelog.LaborCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLogPublisherTest {

    @Test
    fun from_hectorIssue_pinsSpallingAtColumnFour() {
        val record = DailyLogRecord(
            id = "log-test",
            timestampMillis = 1_000L,
            projectName = "Riverside Medical",
            audioFilePath = "",
            transcript = "Spalling near column 4",
            laborCards = listOf(
                LaborCard("labor-1", "Hector Ortiz", "Framing (Carpentry)", 8.0),
            ),
            materialCards = emptyList(),
            delayCards = listOf(
                DelayCard("delay-1", 1.0, "Material Delivery: Delayed"),
            ),
            issueCards = listOf(
                IssueCard("issue-1", "Spalling", "Column 4"),
            ),
        )

        val published = VoiceLogPublisher.from(record)

        assertTrue(published.items.any { it.kind == StreamKind.DAILY_LOG })
        assertTrue(published.items.any { it.kind == StreamKind.BLOCKER })
        assertTrue(published.items.any { it.kind == StreamKind.ISSUE && it.title == "Spalling" })
        val pin = published.pins.single()
        assertEquals(PinKind.ISSUE, pin.kind)
        assertEquals(VoiceLogPublisher.COLUMN_4_X, pin.xFraction, 0.0f)
        assertEquals(VoiceLogPublisher.COLUMN_4_Y, pin.yFraction, 0.0f)
        assertEquals("log-test", pin.relatedRecordId)
    }

    @Test
    fun onlyForemanIsLiveForThisBuild() {
        assertTrue(FieldPersona.FOREMAN.isLive)
        FieldPersona.entries.filterNot { it == FieldPersona.FOREMAN }.forEach { persona ->
            assertFalse(persona.displayName, persona.isLive)
        }
    }
}
