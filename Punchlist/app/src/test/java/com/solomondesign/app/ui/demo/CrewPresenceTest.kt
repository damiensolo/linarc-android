package com.solomondesign.app.ui.demo

import com.solomondesign.app.ui.theme.PresenceAssigned
import com.solomondesign.app.ui.theme.PresenceOffSite
import com.solomondesign.app.ui.theme.PresenceOnSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class CrewPresenceTest {

    @Test
    fun statusLabel_matchesPresence() {
        assertEquals("On site", CrewPresence.ON_SITE.statusLabel("Area B"))
        assertEquals("Assigned · Area B", CrewPresence.ASSIGNED.statusLabel("Area B"))
        assertEquals("Off site", CrewPresence.OFF_SITE.statusLabel("Area B"))
    }

    @Test
    fun badgeColors_areDistinctPerPresence() {
        val onSite = CrewPresence.ON_SITE.badgeColor()
        val assigned = CrewPresence.ASSIGNED.badgeColor()
        val offSite = CrewPresence.OFF_SITE.badgeColor()
        assertEquals(PresenceOnSite, onSite)
        assertEquals(PresenceAssigned, assigned)
        assertEquals(PresenceOffSite, offSite)
        assertNotEquals(onSite, assigned)
        assertNotEquals(assigned, offSite)
        assertNotEquals(onSite, offSite)
    }

    @Test
    fun demoCrew_mixesPhotosAndAllPresenceStates() {
        val crew = DemoProjectRepository.crew
        assertEquals(4, crew.size)
        assertNotNull(crew[0].photoRes)
        assertNull(crew[1].photoRes)
        assertNotNull(crew[2].photoRes)
        assertNull(crew[3].photoRes)
        assertEquals(
            setOf(CrewPresence.ON_SITE, CrewPresence.ASSIGNED, CrewPresence.OFF_SITE),
            crew.map { it.presence }.toSet(),
        )
    }
}
