package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.collab.CollabRepository
import com.solomondesign.app.ui.images.ProjectImageRepository
import com.solomondesign.app.ui.records.RecordDraft
import com.solomondesign.app.ui.records.RecordRepository
import com.solomondesign.app.ui.video.VideoRepository
import com.solomondesign.app.ui.voicenote.VoiceNotePhotoInbox
import com.solomondesign.app.ui.tasks.FieldTaskRepository
import com.solomondesign.app.ui.timecards.TimeCardRepository
import com.solomondesign.app.ui.voicelog.DailyLogRepository

/**
 * Single reset point for the whole in-memory demo. Logout and tests call this rather than
 * [DemoProjectRepository.clear] alone, which only knows about the shared stores.
 *
 * This also closes an existing gap: logging out previously left recorded voice logs behind.
 */
object DemoSession {
    fun reset() {
        DemoProjectRepository.clear()
        DailyLogRepository.clear()
        FieldTaskRepository.clear()
        TimeCardRepository.clear()
        CollabRepository.clear()
        ProjectImageRepository.clear()
        VideoRepository.clear()
        RecordRepository.clear()
        RecordDraft.clear()
        VoiceNotePhotoInbox.reset()
    }
}
