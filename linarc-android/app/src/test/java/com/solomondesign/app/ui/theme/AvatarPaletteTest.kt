package com.solomondesign.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarPaletteTest {

    @Test
    fun colorAt_usesDistinctColorsWithinPalette() {
        val assigned = AvatarPalette.colors.indices.map { AvatarPalette.colorAt(it) }.toSet()
        assertEquals(AvatarPalette.colors.size, assigned.size)
    }

    @Test
    fun colorAt_neighborsDoNotShareAColor() {
        val crewSize = 4
        val colors = (0 until crewSize).map { AvatarPalette.colorAt(it) }
        assertEquals(crewSize, colors.toSet().size)
        assertNotEquals(colors[0], colors[1])
    }
}
