package com.solomondesign.app.ui.tools

import com.solomondesign.app.ui.persona.FieldPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformToolsTest {

    @Test
    fun catalog_includesEveryRequestedTool() {
        val labels = PlatformTools.catalog.map { it.label }
        assertEquals(
            listOf(
                "Field task",
                "Time card",
                "Crew",
                "Collaboration",
                "Images",
                "Plans",
                "RFIs",
                "Punch list",
                "Incidents",
                "Issues",
                "T & M",
                "Checklist",
                "Drive",
                "Toolbox Talks",
                "Scan",
            ),
            labels,
        )
    }

    @Test
    fun catalog_quickCreateOnlyOnCaptureTools() {
        val quickCreate = PlatformTools.catalog.filter { it.canQuickCreate }.map { it.label }.toSet()
        assertEquals(
            setOf(
                "Collaboration",
                "Images",
                "RFIs",
                "Punch list",
                "Incidents",
                "Issues",
                "Toolbox Talks",
            ),
            quickCreate,
        )
        assertTrue(PlatformTools.byId("images")!!.quickCreateUsesPhoto)
        assertFalse(PlatformTools.byId("rfis")!!.quickCreateUsesPhoto)
    }

    @Test
    fun catalog_idsAreUnique() {
        val ids = PlatformTools.catalog.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun catalogFor_crew_leadsWithCrewToolsAndKeepsEveryTool() {
        val crewCatalog = PlatformTools.catalogFor(FieldPersona.CREW)
        assertEquals(
            listOf("Field task", "Time card", "Images", "Toolbox Talks", "Checklist"),
            crewCatalog.take(5).map { it.label },
        )
        // Reordered, never reduced: same tools for every persona.
        assertEquals(PlatformTools.catalog.size, crewCatalog.size)
        assertEquals(
            PlatformTools.catalog.map { it.id }.toSet(),
            crewCatalog.map { it.id }.toSet(),
        )
    }

    @Test
    fun catalogFor_superintendent_leadsWithOversightToolsAndKeepsEveryTool() {
        val superCatalog = PlatformTools.catalogFor(FieldPersona.SUPERINTENDENT)
        assertEquals(
            listOf("Issues", "Punch list", "Incidents", "RFIs", "Checklist"),
            superCatalog.take(5).map { it.label },
        )
        assertEquals(PlatformTools.catalog.size, superCatalog.size)
        assertEquals(
            PlatformTools.catalog.map { it.id }.toSet(),
            superCatalog.map { it.id }.toSet(),
        )
    }

    @Test
    fun catalogFor_projectManager_leadsWithRfisAndDecisionsAndKeepsEveryTool() {
        val pmCatalog = PlatformTools.catalogFor(FieldPersona.PROJECT_MANAGER)
        assertEquals(
            listOf("RFIs", "Collaboration", "Issues", "T & M", "Drive"),
            pmCatalog.take(5).map { it.label },
        )
        assertEquals(PlatformTools.catalog.size, pmCatalog.size)
        assertEquals(
            PlatformTools.catalog.map { it.id }.toSet(),
            pmCatalog.map { it.id }.toSet(),
        )
    }

    @Test
    fun catalogFor_owner_leadsWithProgressTools_andDropsOnlyTimeCard() {
        val ownerCatalog = PlatformTools.catalogFor(FieldPersona.OWNER)
        assertEquals(
            listOf("Images", "Plans", "Collaboration", "Drive", "RFIs"),
            ownerCatalog.take(5).map { it.label },
        )
        // The one sanctioned removal in the whole persona system: the spec's Owner row says
        // "no time cards or voice log". Everything else stays.
        assertFalse(ownerCatalog.any { it.id == "time_card" })
        assertEquals(
            PlatformTools.catalog.map { it.id }.toSet() - "time_card",
            ownerCatalog.map { it.id }.toSet(),
        )
    }

    @Test
    fun catalogFor_subcontractor_leadsWithAssignedWorkAndKeepsEveryTool() {
        val subCatalog = PlatformTools.catalogFor(FieldPersona.SUBCONTRACTOR)
        assertEquals(
            listOf("Field task", "Checklist", "Punch list", "Images", "RFIs"),
            subCatalog.take(5).map { it.label },
        )
        assertEquals(PlatformTools.catalog.size, subCatalog.size)
        assertEquals(
            PlatformTools.catalog.map { it.id }.toSet(),
            subCatalog.map { it.id }.toSet(),
        )
    }

    @Test
    fun catalogFor_foreman_keepsCanonicalOrder() {
        assertEquals(PlatformTools.catalog, PlatformTools.catalogFor(FieldPersona.FOREMAN))
    }
}
