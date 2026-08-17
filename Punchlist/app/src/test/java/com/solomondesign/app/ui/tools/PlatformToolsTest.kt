package com.solomondesign.app.ui.tools

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
}
