package com.solomondesign.app.ui.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The search-or-add logic behind the capture flows' TagEditor. */
class TagQueryTest {

    private val allTags = listOf("Area B", "Framing", "Firestopping", "Progress", "Electrical")

    @Test
    fun tagMatches_isCaseInsensitiveContains_andSkipsSelected() {
        assertEquals(
            listOf("Framing", "Firestopping"),
            tagMatches("F", allTags, selected = emptySet()),
        )
        assertEquals(
            "matching is contains, not prefix, in vocabulary order",
            listOf("Firestopping", "Progress"),
            tagMatches("RES", allTags, selected = emptySet()),
        )
        assertEquals(
            "a selected tag is not offered again — case-insensitively",
            listOf("Firestopping"),
            tagMatches("F", allTags, selected = setOf("framing")),
        )
    }

    @Test
    fun tagMatches_blankQueryOffersNothing() {
        assertEquals(emptyList<String>(), tagMatches("   ", allTags, selected = emptySet()))
    }

    @Test
    fun newTagCandidate_trimsAndOffersOnlyGenuinelyNewTags() {
        assertEquals("Guardrail", newTagCandidate("  Guardrail ", allTags, selected = emptySet()))
        // An existing tag (any case) is offered by the match chip, never as a new duplicate.
        assertNull(newTagCandidate("framing", allTags, selected = emptySet()))
        assertNull(newTagCandidate("FRAMING", allTags, selected = emptySet()))
        // Already selected — nothing to add.
        assertNull(newTagCandidate("guardrail", allTags, selected = setOf("Guardrail")))
        assertNull(newTagCandidate("   ", allTags, selected = emptySet()))
    }
}
