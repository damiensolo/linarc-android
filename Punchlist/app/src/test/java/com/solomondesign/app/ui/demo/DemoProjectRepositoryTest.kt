package com.solomondesign.app.ui.demo

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoProjectRepositoryTest {

    @After
    fun resetStore() {
        DemoProjectRepository.clear()
    }

    @Test
    fun darkTheme_togglesAndClearResetsToDark() {
        assertTrue(DemoProjectRepository.darkTheme)

        DemoProjectRepository.darkTheme = false
        assertFalse(DemoProjectRepository.darkTheme)

        DemoProjectRepository.clear()
        assertTrue(DemoProjectRepository.darkTheme)
    }
}
