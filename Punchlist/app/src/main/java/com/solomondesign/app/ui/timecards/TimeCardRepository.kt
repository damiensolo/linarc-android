package com.solomondesign.app.ui.timecards

import androidx.compose.runtime.mutableStateListOf
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.OutboxItem

/** In-memory demo store for time entries. Snapshot state, no ViewModel. */
object TimeCardRepository {
    private val _entries = mutableStateListOf<TimeEntry>()
    val entries: List<TimeEntry> get() = _entries

    init {
        seed()
    }

    fun entriesFor(crewMemberId: String): List<TimeEntry> =
        _entries.filter { it.crewMemberId == crewMemberId }

    fun totalHours(crewMemberId: String): Double =
        entriesFor(crewMemberId).sumOf { it.hours + it.overtimeHours }

    fun addEntry(entry: TimeEntry) {
        _entries.add(0, entry)
        // New entries are queued, matching the prototype's offline story.
        DemoProjectRepository.outboxItems.add(
            OutboxItem(
                id = "outbox-${entry.id}",
                title = "Time card: ${formatHours(entry.hours + entry.overtimeHours)}",
                subtitle = "Queued · waiting for signal",
            ),
        )
    }

    fun clear() {
        _entries.clear()
        seed()
    }

    private fun seed() {
        _entries.addAll(
            listOf(
                TimeEntry("te-hector-mon", "hector-ortiz", "Mon, Aug 18", COST_CODES[0], 8.0),
                TimeEntry("te-hector-fri", "hector-ortiz", "Fri, Aug 15", COST_CODES[0], 8.0, 1.5),
                TimeEntry("te-dave-mon-caulk", "dave-miller", "Mon, Aug 18", COST_CODES[4], 6.0),
                TimeEntry("te-dave-mon-doors", "dave-miller", "Mon, Aug 18", COST_CODES[1], 2.0),
                TimeEntry("te-dave-fri", "dave-miller", "Fri, Aug 15", COST_CODES[0], 8.0, 2.0),
                TimeEntry("te-maria-mon", "maria-chen", "Mon, Aug 18", COST_CODES[2], 6.5),
                TimeEntry("te-maria-fri", "maria-chen", "Fri, Aug 15", COST_CODES[2], 8.0),
                // sam-reyes deliberately has no entries: he is off site, which exercises the
                // per-person empty state.
            ),
        )
    }
}
